package tn.esprit.forums_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Lightweight row for forum home “top comments” widget. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForumTopCommentDTO {
    private Long id;
    private Long postId;
    private String postTitle;
    private String contentPreview;
    private String authorName;
    private int likeCount;
    private LocalDateTime createdAt;
}
