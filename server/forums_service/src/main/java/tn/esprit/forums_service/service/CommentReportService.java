package tn.esprit.forums_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.forums_service.dto.ResolveReportRequest;
import tn.esprit.forums_service.entity.*;
import tn.esprit.forums_service.client.UserDisplayClient;
import tn.esprit.forums_service.exception.ResourceNotFoundException;
import tn.esprit.forums_service.repository.CommentRepository;
import tn.esprit.forums_service.repository.ForumCommentReportRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommentReportService {

    private static final int MAX_REPORTS_PER_DOCTOR = 200;

    private final ForumCommentReportRepository reportRepository;
    private final CommentRepository commentRepository;
    private final ForumBanService forumBanService;
    private final PostInteractionService postInteractionService;
    private final UserDisplayClient userDisplayClient;

    @Transactional
    public ForumCommentReport createReport(Long commentId, Long reporterUserId, String reason) {
        if (reporterUserId == null || reporterUserId <= 0) {
            throw new IllegalArgumentException("Valid reporterUserId required");
        }
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));
        Post post = comment.getPost();
        Long postId = post.getId();
        Long postAuthorId = post.getUserId();
        if (postAuthorId == null) {
            throw new IllegalStateException("Post has no author");
        }
        if (comment.getUserId().equals(reporterUserId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot report your own comment");
        }

        ForumCommentReport r = ForumCommentReport.builder()
                .commentId(commentId)
                .postId(postId)
                .postAuthorId(postAuthorId)
                .reporterUserId(reporterUserId)
                .reportedUserId(comment.getUserId())
                .reason(reason != null ? reason.trim() : "")
                .status(ReportStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        return reportRepository.save(r);
    }

    /** Pending + resolved (doctor can change decision except after comment deletion). Newest first, capped. */
    public List<ForumCommentReport> listPendingForDoctor(Long doctorId) {
        if (doctorId == null || doctorId <= 0) {
            return List.of();
        }
        return reportRepository.findByPostAuthorIdOrderByCreatedAtDesc(
                doctorId, PageRequest.of(0, MAX_REPORTS_PER_DOCTOR));
    }

    /**
     * First resolution: {@code PENDING} → {@code RESOLVED}.
     * Subsequent calls: change decision (ban / dismiss / delete comment) unless the comment was already
     * removed — then {@link ModerationAction#DELETE_COMMENT} is final.
     */
    @Transactional
    public ForumCommentReport resolve(Long reportId, ResolveReportRequest req) {
        ForumCommentReport r = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        if (!r.getPostAuthorId().equals(req.getDoctorId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the thread owner can resolve this report");
        }
        if (r.getStatus() == ReportStatus.RESOLVED) {
            return changeResolution(r, req);
        }
        if (r.getStatus() != ReportStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invalid report state");
        }
        return firstResolve(r, req);
    }

    private ForumCommentReport firstResolve(ForumCommentReport r, ResolveReportRequest req) {
        long reportId = r.getId();
        ModerationAction action = req.getAction();
        if (action == ModerationAction.LIFT_BAN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "LIFT_BAN is only valid when updating an already resolved report that had a ban");
        }
        r.setResolutionAction(action);
        r.setResolvedAt(LocalDateTime.now());
        r.setResolvedByDoctorId(req.getDoctorId());
        r.setStatus(ReportStatus.RESOLVED);
        ForumCommentReport saved = reportRepository.saveAndFlush(r);
        applyModerationAction(saved, action, reportId, req.getDoctorId());
        return saved;
    }

    private ForumCommentReport changeResolution(ForumCommentReport r, ResolveReportRequest req) {
        if (r.getResolutionAction() == ModerationAction.DELETE_COMMENT) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot change decision after the comment was deleted");
        }
        ModerationAction old = r.getResolutionAction();
        ModerationAction neu = req.getAction();
        if (neu == ModerationAction.LIFT_BAN) {
            boolean hadBan = old == ModerationAction.BAN_1_DAY || old == ModerationAction.BAN_3_DAYS
                    || old == ModerationAction.BAN_7_DAYS;
            if (!hadBan) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "LIFT_BAN only applies when the current decision is a temporary ban");
            }
        }
        undoModerationAction(old, r);
        r.setResolutionAction(neu);
        r.setResolvedAt(LocalDateTime.now());
        r.setResolvedByDoctorId(req.getDoctorId());
        ForumCommentReport saved = reportRepository.saveAndFlush(r);
        applyModerationAction(saved, neu, saved.getId(), req.getDoctorId());
        return saved;
    }

    private void undoModerationAction(ModerationAction old, ForumCommentReport r) {
        if (old == null) {
            return;
        }
        switch (old) {
            case BAN_1_DAY, BAN_3_DAYS, BAN_7_DAYS -> forumBanService.liftBan(r.getReportedUserId());
            case DISMISS, LIFT_BAN, DELETE_COMMENT -> { /* nothing */ }
        }
    }

    private void applyModerationAction(ForumCommentReport r, ModerationAction action, long reportId, Long doctorId) {
        switch (action) {
            case DISMISS -> { /* no side effect */ }
            case LIFT_BAN -> forumBanService.liftBan(r.getReportedUserId());
            case DELETE_COMMENT -> deleteCommentForReport(r.getCommentId());
            case BAN_1_DAY -> forumBanService.banUser(r.getReportedUserId(), 1, doctorId, "Report #" + reportId);
            case BAN_3_DAYS -> forumBanService.banUser(r.getReportedUserId(), 3, doctorId, "Report #" + reportId);
            case BAN_7_DAYS -> forumBanService.banUser(r.getReportedUserId(), 5, doctorId, "Report #" + reportId);
        }
    }

    private void deleteCommentForReport(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));
        Long postId = comment.getPost().getId();
        commentRepository.delete(comment);
        postInteractionService.touch(postId);
    }

    /** JSON-friendly map for Angular. */
    public Map<String, Object> toMap(ForumCommentReport r) {
        String preview = "";
        Comment c = commentRepository.findById(r.getCommentId()).orElse(null);
        if (c != null && c.getContent() != null) {
            String s = c.getContent().trim();
            preview = s.length() > 120 ? s.substring(0, 117) + "…" : s;
        }
        if (preview.isEmpty() && r.getResolutionAction() == ModerationAction.DELETE_COMMENT) {
            preview = "[Comment removed]";
        }
        boolean canChange = r.getStatus() == ReportStatus.PENDING
                || (r.getStatus() == ReportStatus.RESOLVED
                        && r.getResolutionAction() != ModerationAction.DELETE_COMMENT);

        Map<Long, String> displayNames = userDisplayClient.resolveDisplayNames(
                List.of(r.getReporterUserId(), r.getReportedUserId()));

        Map<String, Object> m = new HashMap<>();
        m.put("id", r.getId());
        m.put("commentId", r.getCommentId());
        m.put("postId", r.getPostId());
        m.put("postAuthorId", r.getPostAuthorId());
        m.put("reporterUserId", r.getReporterUserId());
        m.put("reportedUserId", r.getReportedUserId());
        m.put("reporterName", displayNames.getOrDefault(r.getReporterUserId(), "User " + r.getReporterUserId()));
        m.put("reportedUserName", displayNames.getOrDefault(r.getReportedUserId(), "User " + r.getReportedUserId()));
        m.put("reason", r.getReason() != null ? r.getReason() : "");
        m.put("status", r.getStatus().name());
        m.put("createdAt", r.getCreatedAt().toString());
        m.put("commentPreview", preview);
        m.put("resolutionAction", r.getResolutionAction() != null ? r.getResolutionAction().name() : null);
        m.put("resolvedAt", r.getResolvedAt() != null ? r.getResolvedAt().toString() : null);
        m.put("canChangeDecision", canChange);
        return m;
    }
}
