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
public class ForumHomeDTO {
    private List<CategoryDTO> categories;
    /** Paginated / sorted threads for the main list. */
    private List<PostDTO> posts;
    /** Distinct users who published a thread or commented (public posts only). */
    private Long totalMemberCount;
    /** Published active threads (same scope as public forum). */
    private Long totalThreadCount;
    /** Total matching the current sort (for “load more”). */
    private Long totalPostCount;
    /** Top threads by engagement (comments). */
    private List<PostDTO> topPosts;
    /** Top comments by likes (public threads only). */
    private List<ForumTopCommentDTO> topComments;
}
