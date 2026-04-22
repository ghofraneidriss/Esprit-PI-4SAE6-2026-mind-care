package tn.esprit.forums_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ForumStatsDto {
    private long totalPosts;
    private long totalComments;
    private long inactivePosts;
    private List<ForumTopCategoryDto> topCategories;
}
