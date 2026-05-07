package tn.esprit.users_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Target user (admin or caregiver)
    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String message;

    // INFO, WARNING, CRITICAL
    @Column(nullable = false)
    private String type;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    // Optional: reference to the incident that triggered this
    private Long incidentId;

    /** Deep-link to forum post (MindCare forum: follow, inactive archive, best answer, …). */
    private Long postId;

    /**
     * Forum event discriminator for UI (icons, colours): FORUM_COMMENT_FOLLOW, FORUM_COMMENT_AUTHOR,
     * FORUM_LIKE, FORUM_REACTION, FORUM_RATING, FORUM_THREAD_FOLLOW, FORUM_BEST_ANSWER, …
     */
    @Column(length = 48)
    private String eventKind;

    /** Display name of the user who triggered the event (French UI). */
    @Column(length = 200)
    private String actorName;

    /** Short post title for display. */
    @Column(length = 300)
    private String postTitle;

    /** Optional excerpt (e.g. comment preview). */
    @Column(length = 500)
    private String snippet;

    /** Actor user id — helps dedupe on client. */
    private Long actorUserId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
