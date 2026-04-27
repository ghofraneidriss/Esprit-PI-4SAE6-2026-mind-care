package tn.esprit.forums_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostDTO {
    private Long id;
    private String title;
    private String content;
    private Long userId;
    private String author;
    private LocalDateTime createdAt;
    private Long categoryId;
    private String categoryName;
    private String status;
    private long commentCount;

    /** Distinct logged-in users who opened the post (counted once per user). */
    private long viewCount;

    /** Counts per reaction type (LIKE, LOVE, …). */
    private Map<String, Long> reactionCounts;

    /** Current user's reaction on this post, if userId was passed on GET. */
    private String myReaction;

    private Double averageRating;
    private long ratingCount;

    /** Current user's 1–5 rating, if any. */
    private Integer myRating;

    /** Archived from public lists (no interaction for configured idle period). */
    private boolean inactive;

    private LocalDateTime inactiveSince;

    /** True when {@code post_media} rows exist (photos stored as LONGBLOB). */
    private boolean hasImages;

    /** Current user follows this thread (GET with {@code userId}). */
    private Boolean following;
}
