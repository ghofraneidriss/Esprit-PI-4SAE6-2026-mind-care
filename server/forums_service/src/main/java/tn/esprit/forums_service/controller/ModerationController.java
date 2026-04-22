package tn.esprit.forums_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.forums_service.dto.ResolveReportRequest;
import tn.esprit.forums_service.entity.ForumCommentReport;
import tn.esprit.forums_service.service.CommentReportService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/moderation")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class ModerationController {

    private final CommentReportService commentReportService;

    /** Pending + recent resolved (doctor may change decision except after comment deletion). */
    @GetMapping("/comment-reports")
    public List<Map<String, Object>> commentReports(@RequestParam Long doctorId) {
        return commentReportService.listPendingForDoctor(doctorId).stream()
                .map(commentReportService::toMap)
                .toList();
    }

    @PostMapping("/comment-reports/{reportId}/resolve")
    public ResponseEntity<Map<String, Object>> resolve(
            @PathVariable Long reportId,
            @Valid @RequestBody ResolveReportRequest req) {
        ForumCommentReport r = commentReportService.resolve(reportId, req);
        return ResponseEntity.ok(commentReportService.toMap(r));
    }
}
