package tn.esprit.forums_service.dto;

/**
 * Pre-aggregated KPIs for the staff forum dashboard (COUNT-only, like incident analytics).
 */
public record StaffDashboardTotals(
        long totalPostCount,
        long totalCommentCount,
        long totalViewCount,
        long publishedPostCount,
        long draftPostCount
) {}
