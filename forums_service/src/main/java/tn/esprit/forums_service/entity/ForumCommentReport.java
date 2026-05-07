package tn.esprit.forums_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "forum_comment_report",
        indexes = {
                @Index(name = "idx_fcr_status_author", columnList = "status,post_author_id"),
                @Index(name = "idx_fcr_comment", columnList = "comment_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForumCommentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "comment_id", nullable = false)
    private Long commentId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    /** Post author (doctor thread owner) — for filtering moderation queue. */
    @Column(name = "post_author_id", nullable = false)
    private Long postAuthorId;

    @Column(name = "reporter_user_id", nullable = false)
    private Long reporterUserId;

    @Column(name = "reported_user_id", nullable = false)
    private Long reportedUserId;

    @Column(length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_action", length = 32)
    private ModerationAction resolutionAction;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by_doctor_id")
    private Long resolvedByDoctorId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = ReportStatus.PENDING;
        }
    }
}
