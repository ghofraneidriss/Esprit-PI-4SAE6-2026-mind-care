package tn.esprit.forums_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.forums_service.client.UserDisplayClient;
import tn.esprit.forums_service.dto.ForumHomeDTO;
import tn.esprit.forums_service.dto.ForumTopCommentDTO;
import tn.esprit.forums_service.dto.PostDTO;
import tn.esprit.forums_service.entity.Comment;
import tn.esprit.forums_service.entity.Post;
import tn.esprit.forums_service.repository.CommentRepository;
import tn.esprit.forums_service.repository.PostRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ForumHomeService {

    private static final int TOP_POSTS = 5;
    private static final int TOP_COMMENTS = 5;
    private static final int MAX_PAGE_SIZE = 48;
    private static final int PREVIEW_LEN = 160;

    private final CategoryService categoryService;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostEngagementService postEngagementService;
    private final UserDisplayClient userDisplayClient;
    private final CommentToxicityModerationService commentToxicityModerationService;

    @Transactional(readOnly = true)
    public ForumHomeDTO buildHome(String postSort, int postPage, int postSize, String searchQuery, Long categoryIdParam) {
        int size = Math.min(Math.max(postSize, 1), MAX_PAGE_SIZE);
        int page = Math.max(0, postPage);
        int offset = page * size;

        String sort = postSort == null ? "recent" : postSort.trim().toLowerCase();
        if (!sort.equals("recent") && !sort.equals("hot") && !sort.equals("views")) {
            sort = "recent";
        }

        String q = searchQuery == null ? "" : searchQuery.trim();
        if (q.length() > 200) {
            q = q.substring(0, 200);
        }
        boolean hasSearch = !q.isEmpty();
        Long categoryId = (categoryIdParam != null && categoryIdParam > 0) ? categoryIdParam : null;

        var categories = categoryService.getAllCategoryDtos();
        long totalThreads = postRepository.countPublishedActive();
        long members = postRepository.countDistinctForumParticipantUserIds();
        long listTotal;
        if (hasSearch) {
            listTotal = postRepository.countPublishedActiveSearch(q, categoryId);
        } else if (categoryId != null) {
            listTotal = postRepository.countPublishedActiveFiltered(categoryId);
        } else {
            listTotal = totalThreads;
        }

        List<Long> mainIds;
        if (hasSearch) {
            mainIds = switch (sort) {
                case "hot" -> postRepository.findPublishedActiveIdsByCommentCountSearch(q, categoryId, offset, size);
                case "views" -> postRepository.findPublishedActiveIdsByViewCountSearch(q, categoryId, offset, size);
                default -> postRepository.findPublishedActiveIdsByNewestSearch(q, categoryId, offset, size);
            };
        } else {
            mainIds = switch (sort) {
                case "hot" -> postRepository.findPublishedActiveIdsByCommentCount(categoryId, offset, size);
                case "views" -> postRepository.findPublishedActiveIdsByViewCount(categoryId, offset, size);
                default -> postRepository.findPublishedActiveIdsByNewest(categoryId, offset, size);
            };
        }
        List<Post> mainPosts = orderedPostsByIds(mainIds);
        Map<Long, Long> commentCountsMain = commentCountsFor(mainPosts);
        List<PostDTO> postDtos = postEngagementService.toDtos(mainPosts, commentCountsMain, null);

        List<Long> topIds = postRepository.findPublishedActiveIdsByCommentCount(null, 0, TOP_POSTS);
        List<Post> topPostEntities = orderedPostsByIds(topIds);
        Map<Long, Long> topCc = commentCountsFor(topPostEntities);
        List<PostDTO> topPostDtos = postEngagementService.toDtos(topPostEntities, topCc, null);

        List<Comment> topCommentEntities = commentRepository.findTopForPublicForum(PageRequest.of(0, TOP_COMMENTS));
        List<ForumTopCommentDTO> topCommentDtos = mapTopComments(topCommentEntities);

        return ForumHomeDTO.builder()
                .categories(categories)
                .posts(postDtos)
                .totalMemberCount(members)
                .totalThreadCount(totalThreads)
                .totalPostCount(listTotal)
                .topPosts(topPostDtos)
                .topComments(topCommentDtos)
                .build();
    }

    private List<Post> orderedPostsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Map<Long, Post> byId = postRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Post::getId, p -> p, (a, b) -> a, LinkedHashMap::new));
        List<Post> out = new ArrayList<>();
        for (Long id : ids) {
            Post p = byId.get(id);
            if (p != null) {
                out.add(p);
            }
        }
        return out;
    }

    private Map<Long, Long> commentCountsFor(List<Post> posts) {
        List<Long> ids = posts.stream().map(Post::getId).collect(Collectors.toList());
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

    private List<ForumTopCommentDTO> mapTopComments(List<Comment> comments) {
        if (comments.isEmpty()) {
            return List.of();
        }
        Set<Long> userIds = comments.stream().map(Comment::getUserId).collect(Collectors.toSet());
        Map<Long, String> names = userDisplayClient.resolveDisplayNames(userIds);
        List<ForumTopCommentDTO> out = new ArrayList<>();
        for (Comment c : comments) {
            Post post = c.getPost();
            Long pid = post != null ? post.getId() : null;
            String title = post != null && post.getTitle() != null ? post.getTitle() : "Thread";
            String raw = c.getContent() != null ? c.getContent().trim() : "";
            raw = commentToxicityModerationService.maskContentForDisplay(raw);
            String preview = raw.length() <= PREVIEW_LEN ? raw : raw.substring(0, PREVIEW_LEN - 1) + "…";
            Long uid = c.getUserId();
            out.add(ForumTopCommentDTO.builder()
                    .id(c.getId())
                    .postId(pid)
                    .postTitle(title)
                    .contentPreview(preview)
                    .authorName(names.getOrDefault(uid, "User " + uid))
                    .likeCount(c.getLikeCount())
                    .createdAt(c.getCreatedAt())
                    .build());
        }
        return out;
    }
}
