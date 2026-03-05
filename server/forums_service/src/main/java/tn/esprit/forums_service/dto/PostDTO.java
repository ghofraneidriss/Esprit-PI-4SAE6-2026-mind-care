package tn.esprit.forums_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
}
