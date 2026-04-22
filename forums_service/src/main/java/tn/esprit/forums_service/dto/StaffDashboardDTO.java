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
public class StaffDashboardDTO {
    private List<CategoryDTO> categories;
    private List<PostDTO> posts;

    /**
     * Global or doctor-scoped totals (COUNT queries — same idea as /api/incidents/stats).
     * Lets the UI show KPIs without scanning the full {@link #posts} list.
     */
    private long totalPostCount;
    private long totalCommentCount;
    private long totalViewCount;
    private long publishedPostCount;
    private long draftPostCount;

    /** Forum-wide: posts archived (inactive) — hidden from public lists, kept in history. */
    private long inactivePostCount;

    /** Top categories by post count (global forum). */
    private List<ForumTopCategoryDto> topCategories;
}
