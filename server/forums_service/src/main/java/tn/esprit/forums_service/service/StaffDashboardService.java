package tn.esprit.forums_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.forums_service.dto.CategoryDTO;
import tn.esprit.forums_service.dto.ForumStatsDto;
import tn.esprit.forums_service.dto.PostDTO;
import tn.esprit.forums_service.dto.PostListProjection;
import tn.esprit.forums_service.dto.StaffDashboardDTO;
import tn.esprit.forums_service.dto.StaffDashboardTotals;
import tn.esprit.forums_service.repository.CommentRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Single read-only transaction for staff dashboard: avoids parallel {@code CompletableFuture}
 * threads each opening their own persistence context (slower and harder on the pool).
 */
@Service
@RequiredArgsConstructor
public class StaffDashboardService {

    private final CategoryService categoryService;
    private final PostService postService;
    private final PostEngagementService postEngagementService;
    private final CommentRepository commentRepository;
    private final ForumStatsService forumStatsService;

    @Transactional(readOnly = true)
    public StaffDashboardDTO build(Long authorId, int limit) {
        int n = Math.max(1, Math.min(limit, PostService.STAFF_API_LIST_MAX));
        List<CategoryDTO> categories = categoryService.getAllCategoryDtos();
        StaffDashboardTotals totals = postService.computeStaffDashboardTotals(authorId);
        List<PostListProjection> rows =
                authorId != null && authorId > 0
                        ? postService.getStaffListingForDashboardByAuthor(authorId, n)
                        : postService.getStaffListingForDashboard(n);
        Map<Long, Long> commentCounts = commentCountsForProjections(rows);
        List<PostDTO> posts = postEngagementService.buildStaffListingDtos(rows, commentCounts);
        ForumStatsDto fs = forumStatsService.buildStats();
        return StaffDashboardDTO.builder()
                .categories(categories)
                .posts(posts)
                .totalPostCount(totals.totalPostCount())
                .totalCommentCount(totals.totalCommentCount())
                .totalViewCount(totals.totalViewCount())
                .publishedPostCount(totals.publishedPostCount())
                .draftPostCount(totals.draftPostCount())
                .inactivePostCount(fs.getInactivePosts())
                .topCategories(fs.getTopCategories())
                .build();
    }

    /**
     * Fast path for admin overview: COUNTs + categories only — no post rows, no view/media aggregation.
     */
    @Transactional(readOnly = true)
    public StaffDashboardDTO buildKpisOnly(Long authorId) {
        List<CategoryDTO> categories = categoryService.getAllCategoryDtos();
        StaffDashboardTotals totals = postService.computeStaffDashboardTotals(authorId);
        ForumStatsDto fs = forumStatsService.buildStats();
        return StaffDashboardDTO.builder()
                .categories(categories)
                .posts(List.of())
                .totalPostCount(totals.totalPostCount())
                .totalCommentCount(totals.totalCommentCount())
                .totalViewCount(totals.totalViewCount())
                .publishedPostCount(totals.publishedPostCount())
                .draftPostCount(totals.draftPostCount())
                .inactivePostCount(fs.getInactivePosts())
                .topCategories(fs.getTopCategories())
                .build();
    }

    /**
     * Recent staff table rows only (for parallel load after {@link #buildKpisOnly(Long)}).
     */
    @Transactional(readOnly = true)
    public List<PostDTO> buildRecentPosts(Long authorId, int limit) {
        int n = Math.max(1, Math.min(limit, PostService.STAFF_API_LIST_MAX));
        List<PostListProjection> rows =
                authorId != null && authorId > 0
                        ? postService.getStaffListingForDashboardByAuthor(authorId, n)
                        : postService.getStaffListingForDashboard(n);
        Map<Long, Long> commentCounts = commentCountsForProjections(rows);
        return postEngagementService.buildStaffListingDtos(rows, commentCounts);
    }

    private Map<Long, Long> commentCountsForProjections(List<PostListProjection> rows) {
        List<Long> ids = rows.stream().map(PostListProjection::getId).collect(Collectors.toList());
        return commentCountsForIds(ids);
    }

    private Map<Long, Long> commentCountsForIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> map = new HashMap<>();
        for (Long id : ids) {
            map.put(id, 0L);
        }
        List<Object[]> countRows = commentRepository.countByPostIdIn(ids);
        for (Object[] row : countRows) {
            map.put((Long) row[0], (Long) row[1]);
        }
        return map;
    }
}
