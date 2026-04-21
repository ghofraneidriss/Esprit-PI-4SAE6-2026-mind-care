package tn.esprit.forums_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "post",
        indexes = {
                @Index(name = "idx_post_user_id", columnList = "user_id"),
                @Index(name = "idx_post_category_id", columnList = "category_id"),
                @Index(name = "idx_post_inactive", columnList = "inactive"),
                @Index(name = "idx_post_last_interaction", columnList = "last_interaction_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    private String title;

    @Lob
    @NotBlank(message = "Content is required")
    private String content;

    private String status; // PUBLISHED, DRAFT

    private LocalDateTime createdAt;

    /** Last user activity: comments, reactions, ratings, new views (first view per user already in engagement). */
    private LocalDateTime lastInteractionAt;

    /**
     * Hidden from public forum lists when true; kept in staff “post inactifs” history.
     * Nullable so legacy/imported MySQL rows (NULL) still appear in public lists; treated as false in app code.
     */
    @Column(nullable = true)
    @Builder.Default
    private Boolean inactive = Boolean.FALSE;

    private LocalDateTime inactiveSince;

    @NotNull(message = "User ID is required")
    private Long userId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    @JsonIgnoreProperties({"posts"}) // Prevent serialization of category.posts list
    private Category category;

    /** Référence utilisateur côté {@code users-service} (pas de FK inter-microservice). */
    @Builder.Default
    @OneToMany(mappedBy = "post", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    @JsonIgnoreProperties({"post"})
    private List<Comment> comments = new ArrayList<>();

    /** Images du post (LONGBLOB dans {@code post_media}). */
    @Builder.Default
    @OneToMany(mappedBy = "post", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<PostMedia> media = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        if (lastInteractionAt == null) {
            lastInteractionAt = now;
        }
        if (inactive == null) {
            inactive = Boolean.FALSE;
        }
    }

    /** Public API / controllers: null or false = visible in forum. */
    public boolean isInactive() {
        return Boolean.TRUE.equals(inactive);
    }
}
