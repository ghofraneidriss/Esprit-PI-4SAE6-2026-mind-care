package tn.esprit.forums_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.forums_service.dto.CommentReportRequest;
import tn.esprit.forums_service.dto.CommentUpdateRequest;
import tn.esprit.forums_service.entity.Comment;
import tn.esprit.forums_service.entity.ForumCommentReport;
import tn.esprit.forums_service.service.CommentLikeService;
import tn.esprit.forums_service.service.CommentReportService;
import tn.esprit.forums_service.service.CommentService;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200") // Allow Angular Frontend
public class CommentController {

    private final CommentService commentService;
    private final CommentLikeService commentLikeService;
    private final CommentReportService commentReportService;

    @PostMapping("/{id}/like")
    public ResponseEntity<Comment> toggleCommentLike(@PathVariable Long id, @RequestParam Long userId) {
        return ResponseEntity.ok(commentLikeService.toggleLike(id, userId));
    }

    @PostMapping("/{id}/dislike")
    public ResponseEntity<Comment> toggleCommentDislike(@PathVariable Long id, @RequestParam Long userId) {
        return ResponseEntity.ok(commentLikeService.toggleDislike(id, userId));
    }

    @PostMapping("/post/{postId}")
    public ResponseEntity<Comment> addComment(@Valid @RequestBody Comment comment, @PathVariable Long postId) {
        System.out.println(">>> Request: addComment to post " + postId);
        System.out.println(">>> Content: " + comment.getContent());
        Comment created = commentService.addComment(postId, comment);
        System.out.println(">>> Comment created with ID: " + created.getId());
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    public List<Comment> getAllComments() {
        return commentService.getAllComments();
    }

    /** Comments on posts authored by {@code authorId} (for doctor-scoped moderation). */
    @GetMapping("/by-post-author/{authorId}")
    public List<Comment> getCommentsByPostAuthor(@PathVariable Long authorId) {
        return commentService.getCommentsByPostAuthor(authorId);
    }

    @GetMapping("/post/{postId}")
    public List<Comment> getCommentsByPostId(
            @PathVariable Long postId,
            @RequestParam(required = false) Long viewerUserId) {
        return commentService.getCommentsByPostId(postId, viewerUserId);
    }

    @PostMapping("/{id}/report")
    public ResponseEntity<ForumCommentReport> reportComment(
            @PathVariable Long id,
            @Valid @RequestBody CommentReportRequest body) {
        ForumCommentReport r = commentReportService.createReport(id, body.getReporterUserId(), body.getReason());
        return ResponseEntity.ok(r);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Comment> updateComment(@PathVariable Long id, @Valid @RequestBody CommentUpdateRequest body) {
        return ResponseEntity.ok(commentService.updateComment(id, body.getContent()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id) {
        commentService.deleteComment(id);
        return ResponseEntity.noContent().build();
    }
}
