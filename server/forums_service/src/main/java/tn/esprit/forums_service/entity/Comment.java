package tn.esprit.forums_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "forum_comment",
        indexes = @Index(name = "idx_comment_post_id", columnList = "post_id")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @NotBlank(message = "Content is required")
    private String content;

    private LocalDateTime createdAt;

    @NotNull(message = "User ID is required")
    private Long userId;

    /** Filled when returning JSON — not persisted. */
    @Transient
    private String authorName;

    /** Set when {@code viewerUserId} is passed on comment list API. */
    @Transient
    private Boolean likedByMe;

    @Column(nullable = false)
    private boolean bestAnswer = false;

    @Column(nullable = false)
    private int likeCount = 0;

    @Column(nullable = false)
    private int dislikeCount = 0;

    /** Set when {@code viewerUserId} is passed on comment list API. */
    @Transient
    private Boolean dislikedByMe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private Post post;

    // Expose postId in JSON without causing circular reference
    @Transient
    public Long getPostId() {
        return post != null ? post.getId() : null;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
