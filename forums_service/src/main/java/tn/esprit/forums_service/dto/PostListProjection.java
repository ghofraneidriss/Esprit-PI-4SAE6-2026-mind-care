package tn.esprit.forums_service.dto;

import java.time.LocalDateTime;

/**
 * Lightweight listing row — no LOB content (fast back-office / staff tables).
 */
public interface PostListProjection {
    Long getId();

    String getTitle();

    Long getUserId();

    LocalDateTime getCreatedAt();

    Long getCategoryId();

    String getCategoryName();

    Boolean getInactive();

    java.time.LocalDateTime getInactiveSince();

    /** PUBLISHED | DRAFT — required for honest staff listing vs public forum. */
    String getStatus();
}
