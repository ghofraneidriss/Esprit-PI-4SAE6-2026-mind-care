package tn.esprit.forums_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.forums_service.dto.PostDTO;
import tn.esprit.forums_service.dto.PostMediaInfoDTO;
import tn.esprit.forums_service.dto.PostListProjection;
import tn.esprit.forums_service.dto.RatingRequest;
import tn.esprit.forums_service.dto.ReactionRequest;
import tn.esprit.forums_service.dto.StaffDashboardDTO;
import tn.esprit.forums_service.dto.ViewRequest;
import tn.esprit.forums_service.entity.Post;
import tn.esprit.forums_service.entity.PostMedia;
import tn.esprit.forums_service.exception.ResourceNotFoundException;
import tn.esprit.forums_service.repository.CommentRepository;
import tn.esprit.forums_service.repository.PostMediaRepository;
import tn.esprit.forums_service.service.PostEngagementService;
import tn.esprit.forums_service.service.PostFollowService;
import tn.esprit.forums_service.service.PostService;
import tn.esprit.forums_service.service.StaffDashboardService;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class PostController {

    private final PostService postService;
    private final CommentRepository commentRepository;
    private final PostEngagementService postEngagementService;
    private final PostMediaRepository postMediaRepository;
    private final StaffDashboardService staffDashboardService;
    private final PostFollowService postFollowService;

    /**
     * KPIs + categories only (fast) — use with {@code /staff-dashboard/recent} for incremental admin load.
     */
    @GetMapping("/staff-dashboard/kpis")
    public StaffDashboardDTO getStaffDashboardKpis(@RequestParam(required = false) Long authorId) {
        return staffDashboardService.buildKpisOnly(authorId);
    }

    /**
     * Recent staff post rows only — heavier aggregation; parallel to {@code /staff-dashboard/kpis}.
     */
    @GetMapping("/staff-dashboard/recent")
    public List<PostDTO> getStaffRecentPosts(
            @RequestParam(required = false) Long authorId,
            @RequestParam(defaultValue = "12") int limit
    ) {
        int n = Math.max(1, Math.min(limit, PostService.STAFF_API_LIST_MAX));
        return staffDashboardService.buildRecentPosts(authorId, n);
    }

    /**
     * Full staff dashboard in one response (list views, backward compatibility).
     */
    @GetMapping("/staff-dashboard")
    public StaffDashboardDTO getStaffDashboard(
            @RequestParam(required = false) Long authorId,
            @RequestParam(defaultValue = "120") int limit
    ) {
        int n = Math.max(1, Math.min(limit, PostService.STAFF_API_LIST_MAX));
        return staffDashboardService.build(authorId, n);
    }

    @PostMapping("/category/{categoryId}")
    public ResponseEntity<Post> createPost(@Valid @RequestBody Post post, @PathVariable Long categoryId) {
        return new ResponseEntity<>(postService.createPost(post, categoryId), HttpStatus.CREATED);
    }

    /**
     * Create post with optional photos (each file stored as LONGBLOB in {@code post_media}).
     */
    @PostMapping(value = "/category/{categoryId}/with-photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Post> createPostWithPhotos(
            @PathVariable Long categoryId,
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam Long userId,
            @RequestParam(required = false, defaultValue = "PUBLISHED") String status,
            @RequestParam(value = "photos", required = false) List<MultipartFile> photos
    ) throws IOException {
        Post p = new Post();
        p.setTitle(title);
        p.setContent(content);
        p.setUserId(userId);
        p.setStatus(status);
        Post saved = postService.createPostWithImages(p, categoryId, photos != null ? photos : List.of());
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    /** Metadata only (no bytes) — for admin edit UI. */
    @GetMapping("/{postId}/media-meta")
    public List<PostMediaInfoDTO> listPostMediaMeta(@PathVariable Long postId) {
        return postService.listPostMediaMeta(postId);
    }

    @PostMapping(value = "/{postId}/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> appendPostMedia(
            @PathVariable Long postId,
            @RequestParam(value = "photos", required = false) List<MultipartFile> photos
    ) throws IOException {
        postService.appendImagesToPost(postId, photos != null ? photos : List.of());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{postId}/media/{mediaId}")
    public ResponseEntity<Void> deletePostMedia(@PathVariable Long postId, @PathVariable Long mediaId) {
        postService.deletePostMedia(postId, mediaId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{postId}/media/{mediaId}")
    public ResponseEntity<byte[]> getPostMedia(@PathVariable Long postId, @PathVariable Long mediaId) {
        PostMedia m = postMediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found with id: " + mediaId));
        if (m.getPost() == null || !m.getPost().getId().equals(postId)) {
            return ResponseEntity.notFound().build();
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(m.getContentType()));
        headers.setCacheControl("public, max-age=3600");
        return new ResponseEntity<>(m.getImageData(), headers, HttpStatus.OK);
    }

    @GetMapping("/{postId}/media/cover")
    public ResponseEntity<byte[]> getPostCoverImage(@PathVariable Long postId) {
        return postMediaRepository.findFirstByPost_IdOrderBySortOrderAsc(postId)
                .map(m -> {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.parseMediaType(m.getContentType()));
                    headers.setCacheControl("public, max-age=600");
                    return new ResponseEntity<>(m.getImageData(), headers, HttpStatus.OK);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public List<PostDTO> getAllPosts(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "false") boolean staffList
    ) {
        if (staffList) {
            List<PostListProjection> rows = postService.getStaffListingAll();
            return postEngagementService.buildStaffListingDtos(rows, commentCountsForProjections(rows));
        }
        List<Post> posts = postService.getAllPosts();
        Map<Long, Long> commentCounts = commentCountsFor(posts);
        return postEngagementService.toDtos(posts, commentCounts, userId);
    }

    /** Posts written by {@code authorId} (staff: each doctor sees only their own in the back office). */
    @GetMapping("/author/{authorId}")
    public List<PostDTO> getPostsByAuthor(
            @PathVariable Long authorId,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "false") boolean staffList
    ) {
        if (staffList) {
            List<PostListProjection> rows = postService.getStaffListingByAuthor(authorId);
            return postEngagementService.buildStaffListingDtos(rows, commentCountsForProjections(rows));
        }
        List<Post> posts = postService.getPostsByAuthorId(authorId);
        Map<Long, Long> commentCounts = commentCountsFor(posts);
        return postEngagementService.toDtos(posts, commentCounts, userId);
    }

    /** Historique staff : posts marqués inactifs (retirés du forum public). */
    @GetMapping("/inactive-history")
    public List<PostDTO> getInactivePostsHistory(
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "200") int limit
    ) {
        List<Post> posts = postService.getInactivePostsHistory(limit);
        Map<Long, Long> commentCounts = commentCountsFor(posts);
        return postEngagementService.toDtos(posts, commentCounts, userId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostDTO> getPostById(
            @PathVariable Long id,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "false") boolean includeInactive
    ) {
        Post post = postService.getPostById(id);
        if (post.isInactive() && !includeInactive) {
            return ResponseEntity.notFound().build();
        }
        long commentCount = safeCommentCount(id);
        PostDTO dto = postEngagementService.toDto(post, commentCount, userId);
        return ResponseEntity.ok(dto);
    }

    /** S’abonner au fil : notifications à chaque nouveau commentaire (via users-service). */
    @PostMapping("/{id}/follow")
    public ResponseEntity<Void> followPost(@PathVariable Long id, @RequestParam Long userId) {
        postFollowService.follow(userId, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/follow")
    public ResponseEntity<Void> unfollowPost(@PathVariable Long id, @RequestParam Long userId) {
        postFollowService.unfollow(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<Void> recordView(@PathVariable Long id, @Valid @RequestBody ViewRequest body) {
        postEngagementService.recordView(id, body.getUserId());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/reaction")
    public ResponseEntity<Void> setReaction(@PathVariable Long id, @Valid @RequestBody ReactionRequest body) {
        postEngagementService.setReaction(id, body.getUserId(), body.getType());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/reaction")
    public ResponseEntity<Void> clearReaction(@PathVariable Long id, @RequestParam Long userId) {
        postEngagementService.clearReaction(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/rating")
    public ResponseEntity<Void> setRating(@PathVariable Long id, @Valid @RequestBody RatingRequest body) {
        postEngagementService.setRating(id, body.getUserId(), body.getValue());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/category/{categoryId}")
    public List<PostDTO> getPostsByCategoryId(
            @PathVariable Long categoryId,
            @RequestParam(required = false) Long userId
    ) {
        List<Post> posts = postService.getPostsByCategoryId(categoryId);
        Map<Long, Long> commentCounts = commentCountsFor(posts);
        return postEngagementService.toDtos(posts, commentCounts, userId);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Post> updatePost(@PathVariable Long id, @Valid @RequestBody Post postDetails) {
        return ResponseEntity.ok(postService.updatePost(id, postDetails));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }

    /** Staff: put an archived (inactive) thread back on the public forum. */
    @PostMapping("/{id}/reactivate")
    public ResponseEntity<Post> reactivatePost(@PathVariable Long id) {
        return ResponseEntity.ok(postService.unarchivePost(id));
    }

    private Map<Long, Long> commentCountsFor(List<Post> posts) {
        return commentCountsForIds(posts.stream().map(Post::getId).collect(Collectors.toList()));
    }

    private Map<Long, Long> commentCountsForProjections(List<PostListProjection> rows) {
        return commentCountsForIds(rows.stream().map(PostListProjection::getId).collect(Collectors.toList()));
    }

    private Map<Long, Long> commentCountsForIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> map = new HashMap<>();
        for (Long id : ids) {
            map.put(id, 0L);
        }
        List<Object[]> rows = commentRepository.countByPostIdIn(ids);
        for (Object[] row : rows) {
            map.put((Long) row[0], (Long) row[1]);
        }
        return map;
    }

    private long safeCommentCount(Long postId) {
        try {
            return commentRepository.countByPost_Id(postId);
        } catch (Exception e) {
            return 0;
        }
    }
}
