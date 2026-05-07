package tn.esprit.forums_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.forums_service.dto.PostListProjection;
import tn.esprit.forums_service.dto.PostMediaInfoDTO;
import tn.esprit.forums_service.dto.StaffDashboardTotals;
import tn.esprit.forums_service.entity.Category;
import tn.esprit.forums_service.entity.Post;
import tn.esprit.forums_service.exception.ResourceNotFoundException;
import tn.esprit.forums_service.repository.CategoryRepository;
import tn.esprit.forums_service.repository.CommentRepository;
import tn.esprit.forums_service.entity.PostMedia;
import tn.esprit.forums_service.repository.PostMediaRepository;
import tn.esprit.forums_service.repository.PostRepository;
import tn.esprit.forums_service.repository.PostViewRepository;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.forums_service.util.ForumImageBytes;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    /** Staff list endpoints: cap rows so view/comment batch queries stay bounded. */
    public static final int STAFF_API_LIST_MAX = 2000;

    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final CommentRepository commentRepository;
    private final PostViewRepository postViewRepository;
    private final PostMediaRepository postMediaRepository;

    public Post createPost(Post post, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
        post.setCategory(category);
        normalizeStatusOnCreate(post);
        post.setInactive(false);
        return postRepository.save(post);
    }

    /** Public forum lists only PUBLISHED (+ null). Default new posts to PUBLISHED when status omitted. */
    private void normalizeStatusOnCreate(Post post) {
        String s = post.getStatus();
        if (s == null || s.isBlank()) {
            post.setStatus("PUBLISHED");
            return;
        }
        post.setStatus(s.trim().toUpperCase());
    }

    @Transactional
    public Post createPostWithImages(Post post, Long categoryId, List<MultipartFile> images) throws IOException {
        Post saved = createPost(post, categoryId);
        if (images == null || images.isEmpty()) {
            return saved;
        }
        int order = 0;
        for (MultipartFile file : images) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            byte[] data = file.getBytes();
            if (data.length == 0) {
                continue;
            }
            String ct = file.getContentType() != null && !file.getContentType().isBlank()
                    ? file.getContentType()
                    : "image/jpeg";
            if (data.length > ForumImageBytes.MAX_ALLOWED) {
                try {
                    data = ForumImageBytes.ensureUnderLimit(data, ct);
                    ct = "image/jpeg";
                } catch (IOException e) {
                    throw new IllegalArgumentException("Could not process image: " + e.getMessage());
                }
            }
            postMediaRepository.save(PostMedia.builder()
                    .post(saved)
                    .imageData(data)
                    .contentType(ct)
                    .sortOrder(order++)
                    .build());
        }
        return saved;
    }

    /** Append images to an existing post (staff edit). */
    @Transactional
    public void appendImagesToPost(Long postId, List<MultipartFile> images) throws IOException {
        Post post = getPostById(postId);
        if (images == null || images.isEmpty()) {
            return;
        }
        List<PostMedia> existing = postMediaRepository.findByPost_IdOrderBySortOrderAsc(postId);
        int order = existing.stream().mapToInt(PostMedia::getSortOrder).max().orElse(-1) + 1;
        for (MultipartFile file : images) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            byte[] data = file.getBytes();
            if (data.length == 0) {
                continue;
            }
            String ct = file.getContentType() != null && !file.getContentType().isBlank()
                    ? file.getContentType()
                    : "image/jpeg";
            if (data.length > ForumImageBytes.MAX_ALLOWED) {
                try {
                    data = ForumImageBytes.ensureUnderLimit(data, ct);
                    ct = "image/jpeg";
                } catch (IOException e) {
                    throw new IllegalArgumentException("Could not process image: " + e.getMessage());
                }
            }
            postMediaRepository.save(PostMedia.builder()
                    .post(post)
                    .imageData(data)
                    .contentType(ct)
                    .sortOrder(order++)
                    .build());
        }
    }

    public List<PostMediaInfoDTO> listPostMediaMeta(Long postId) {
        getPostById(postId);
        return postMediaRepository.findByPost_IdOrderBySortOrderAsc(postId).stream()
                .map(m -> PostMediaInfoDTO.builder()
                        .id(m.getId())
                        .sortOrder(m.getSortOrder())
                        .contentType(m.getContentType())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void deletePostMedia(Long postId, Long mediaId) {
        PostMedia m = postMediaRepository.findByIdAndPost_Id(mediaId, postId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Media not found with id: " + mediaId + " for post: " + postId));
        postMediaRepository.delete(m);
    }

    public List<Post> getAllPosts() {
        return postRepository.findAll();
    }

    /** Newest posts first, capped — public forum home (excludes inactive / non-published). */
    public List<Post> getRecentPosts(int limit) {
        int n = Math.max(1, Math.min(limit, 100));
        return postRepository.findRecentActivePublished(
                PageRequest.of(0, n, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    /** Fast listing for /admin forum — no post body loaded from DB. */
    public List<PostListProjection> getStaffListingAll() {
        return postRepository.findStaffListingAll(
                PageRequest.of(0, STAFF_API_LIST_MAX, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    public List<PostListProjection> getStaffListingByAuthor(Long authorUserId) {
        return postRepository.findStaffListingByAuthor(
                authorUserId,
                PageRequest.of(0, STAFF_API_LIST_MAX, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    /**
     * Staff dashboard: recent slice only; KPIs come from {@link #computeStaffDashboardTotals(Long)}.
     */
    public List<PostListProjection> getStaffListingForDashboard(int limit) {
        int n = Math.max(1, Math.min(limit, STAFF_API_LIST_MAX));
        return postRepository.findStaffListingAll(
                PageRequest.of(0, n, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    public List<PostListProjection> getStaffListingForDashboardByAuthor(Long authorUserId, int limit) {
        int n = Math.max(1, Math.min(limit, STAFF_API_LIST_MAX));
        return postRepository.findStaffListingByAuthor(
                authorUserId,
                PageRequest.of(0, n, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    /** Same pattern as incident analytics: O(1) COUNT queries, not scanning all post rows. */
    public StaffDashboardTotals computeStaffDashboardTotals(Long authorIdOptional) {
        if (authorIdOptional != null && authorIdOptional > 0) {
            long uid = authorIdOptional;
            return new StaffDashboardTotals(
                    postRepository.countByUserId(uid),
                    commentRepository.countCommentsForPostsByAuthor(uid),
                    postViewRepository.countViewsForPostsByAuthor(uid),
                    postRepository.countPublishedByAuthor(uid),
                    postRepository.countDraftByAuthor(uid));
        }
        return new StaffDashboardTotals(
                postRepository.count(),
                commentRepository.countCommentsTotal(),
                postViewRepository.countAllViews(),
                postRepository.countPublishedPosts(),
                postRepository.countDraftPosts());
    }

    public Post getPostById(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + id));
    }

    public List<Post> getPostsByCategoryId(Long categoryId) {
        return postRepository.findActivePublishedByCategoryId(categoryId);
    }

    /** Staff / historique : posts marqués inactifs (retirés du forum public). */
    public List<Post> getInactivePostsHistory(int limit) {
        int n = Math.max(1, Math.min(limit, 500));
        return postRepository.findInactivePostsHistory(
                PageRequest.of(0, n, Sort.by(Sort.Direction.DESC, "inactiveSince")));
    }

    /** Posts created by a given user (e.g. doctor's own articles). Newest first. */
    public List<Post> getPostsByAuthorId(Long authorUserId) {
        return postRepository.findByUserIdOrderByCreatedAtDesc(authorUserId);
    }

    public Post updatePost(Long id, Post postDetails) {
        Post post = getPostById(id);
        post.setTitle(postDetails.getTitle());
        post.setContent(postDetails.getContent());
        if (postDetails.getCategory() != null && postDetails.getCategory().getId() != null) {
            Category c = categoryRepository.findById(postDetails.getCategory().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Category not found with id: " + postDetails.getCategory().getId()));
            post.setCategory(c);
        }
        if (postDetails.getStatus() != null && !postDetails.getStatus().isBlank()) {
            post.setStatus(postDetails.getStatus().trim().toUpperCase());
        }
        return postRepository.save(post);
    }

    public void deletePost(Long id) {
        Post post = getPostById(id);
        postRepository.delete(post);
    }

    /** Restore a thread to the public forum and reset idle tracking so it is not immediately re-archived. */
    @Transactional
    public Post unarchivePost(Long id) {
        Post post = getPostById(id);
        post.setInactive(false);
        post.setInactiveSince(null);
        post.setLastInteractionAt(LocalDateTime.now());
        return postRepository.save(post);
    }
}
