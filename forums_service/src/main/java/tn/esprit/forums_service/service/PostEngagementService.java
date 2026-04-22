package tn.esprit.forums_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.forums_service.client.UserDisplayClient;
import tn.esprit.forums_service.dto.PostDTO;
import tn.esprit.forums_service.dto.PostListProjection;
import tn.esprit.forums_service.entity.*;
import tn.esprit.forums_service.exception.ResourceNotFoundException;
import tn.esprit.forums_service.repository.*;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostEngagementService {

    private final UserDisplayClient userDisplayClient;
    private final PostRepository postRepository;
    private final PostViewRepository postViewRepository;
    private final PostReactionRepository postReactionRepository;
    private final PostRatingRepository postRatingRepository;
    private final PostInteractionService postInteractionService;
    private final PostMediaRepository postMediaRepository;
    private final PostFollowRepository postFollowRepository;
    private final ForumNotificationPublisher forumNotificationPublisher;
    private final ForumBanService forumBanService;

    @Transactional
    public void recordView(Long postId, Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Valid userId required");
        }
        if (!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Post not found with id: " + postId);
        }
        if (postViewRepository.existsByPost_IdAndUserId(postId, userId)) {
            return;
        }
        Post post = postRepository.getReferenceById(postId);
        postViewRepository.save(PostView.builder().post(post).userId(userId).build());
        postInteractionService.touch(postId);
    }

    @Transactional
    public void setReaction(Long postId, Long userId, ReactionType type) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Valid userId required");
        }
        if (forumBanService.isBanned(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "FORUM_BANNED");
        }
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));
        postReactionRepository.findByPost_IdAndUserId(postId, userId).ifPresentOrElse(
                r -> {
                    r.setReactionType(type);
                    postReactionRepository.save(r);
                },
                () -> postReactionRepository.save(
                        PostReaction.builder().post(post).userId(userId).reactionType(type).build())
        );
        postInteractionService.touch(postId);
        notifyAuthorAboutReaction(post, userId, type);
    }

    @Transactional
    public void clearReaction(Long postId, Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Valid userId required");
        }
        if (forumBanService.isBanned(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "FORUM_BANNED");
        }
        postReactionRepository.deleteByPost_IdAndUserId(postId, userId);
        postInteractionService.touch(postId);
    }

    @Transactional
    public void setRating(Long postId, Long userId, int value) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Valid userId required");
        }
        if (forumBanService.isBanned(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "FORUM_BANNED");
        }
        if (value < 1 || value > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));
        postRatingRepository.findByPost_IdAndUserId(postId, userId).ifPresentOrElse(
                r -> {
                    r.setStars(value);
                    postRatingRepository.save(r);
                },
                () -> postRatingRepository.save(
                        PostRating.builder().post(post).userId(userId).stars(value).build())
        );
        postInteractionService.touch(postId);
        notifyAuthorAboutRating(post, userId, value);
    }

    /** Post author only; never notify yourself for your own reaction (Instagram-style). */
    private void notifyAuthorAboutReaction(Post post, Long actorId, ReactionType type) {
        Long authorId = post.getUserId();
        Long postId = post.getId();
        if (authorId == null || authorId <= 0 || authorId.equals(actorId)) {
            return;
        }
        String actor = userDisplayClient.resolveDisplayNames(Set.of(actorId)).getOrDefault(actorId, "A member");
        String shortTitle = shortenPostTitle(post.getTitle());
        forumNotificationPublisher.notifyEnriched(
                authorId,
                actor + " reacted to your post \"" + shortTitle + "\" (" + reactionParen(type) + ") " + reactionEmoji(type),
                "INFO",
                postId,
                "FORUM_REACTION",
                actor,
                shortTitle,
                null,
                actorId);
    }

    /** Post author only; never notify yourself for your own rating. */
    private void notifyAuthorAboutRating(Post post, Long actorId, int stars) {
        Long authorId = post.getUserId();
        Long postId = post.getId();
        if (authorId == null || authorId <= 0 || authorId.equals(actorId)) {
            return;
        }
        String actor = userDisplayClient.resolveDisplayNames(Set.of(actorId)).getOrDefault(actorId, "A member");
        String shortTitle = shortenPostTitle(post.getTitle());
        forumNotificationPublisher.notifyEnriched(
                authorId,
                actor + " rated your post \"" + shortTitle + "\" · " + stars + "/5 stars",
                "INFO",
                postId,
                "FORUM_RATING",
                actor,
                shortTitle,
                stars + "/5",
                actorId);
    }

    /** Lowercase label inside parentheses, e.g. (haha) */
    private static String reactionParen(ReactionType type) {
        if (type == null) {
            return "?";
        }
        return switch (type) {
            case LIKE -> "like";
            case LOVE -> "love";
            case CARE -> "care";
            case HAHA -> "haha";
            case WOW -> "wow";
            case SAD -> "sad";
            case ANGRY -> "angry";
            case DISLIKE -> "dislike";
        };
    }

    private static String reactionEmoji(ReactionType type) {
        if (type == null) {
            return "";
        }
        return switch (type) {
            case LIKE -> "👍";
            case LOVE -> "❤️";
            case CARE -> "🤗";
            case HAHA -> "😄";
            case WOW -> "😮";
            case SAD -> "😢";
            case ANGRY -> "😠";
            case DISLIKE -> "👎";
        };
    }

    private static String shortenPostTitle(String title) {
        if (title == null || title.isBlank()) {
            return "Post";
        }
        String t = title.trim();
        if (t.length() <= 60) {
            return t;
        }
        return t.substring(0, 57) + "…";
    }

    public PostDTO toDto(
            Post post,
            long commentCount,
            Long userIdOptional
    ) {
        return toDtos(List.of(post), Map.of(post.getId(), commentCount), userIdOptional).get(0);
    }

    /**
     * Back-office table: view + comment counts only — skips reactions/ratings DB work and omits body.
     */
    public List<PostDTO> buildStaffListingDtos(
            List<PostListProjection> rows,
            Map<Long, Long> commentCountByPostId
    ) {
        if (rows.isEmpty()) {
            return List.of();
        }
        List<Long> ids = rows.stream().map(PostListProjection::getId).toList();
        Map<Long, Long> viewCounts = loadViewCounts(ids);
        Map<Long, Boolean> hasImg = loadHasImages(ids);
        Map<String, Long> emptyRc = emptyReactionMap();
        Set<Long> authorIds = rows.stream().map(PostListProjection::getUserId).collect(Collectors.toSet());
        Map<Long, String> namesByUser = userDisplayClient.resolveDisplayNames(authorIds);

        List<PostDTO> out = new ArrayList<>();
        for (PostListProjection r : rows) {
            Long pid = r.getId();
            long cc = commentCountByPostId.getOrDefault(pid, 0L);
            Boolean inact = r.getInactive();
            Long uid = r.getUserId();
            String rowStatus = r.getStatus();
            if (rowStatus == null || rowStatus.isBlank()) {
                rowStatus = "PUBLISHED";
            } else {
                rowStatus = rowStatus.trim().toUpperCase();
            }
            out.add(PostDTO.builder()
                    .id(pid)
                    .title(r.getTitle())
                    .content("")
                    .userId(uid)
                    .author(namesByUser.getOrDefault(uid, "User " + uid))
                    .createdAt(r.getCreatedAt())
                    .categoryId(r.getCategoryId())
                    .categoryName(r.getCategoryName() != null ? r.getCategoryName() : "General")
                    .status(rowStatus)
                    .commentCount(cc)
                    .viewCount(viewCounts.getOrDefault(pid, 0L))
                    .reactionCounts(new LinkedHashMap<>(emptyRc))
                    .myReaction(null)
                    .averageRating(null)
                    .ratingCount(0L)
                    .myRating(null)
                    .inactive(Boolean.TRUE.equals(inact))
                    .inactiveSince(r.getInactiveSince())
                    .hasImages(hasImg.getOrDefault(pid, false))
                    .build());
        }
        return out;
    }

    public List<PostDTO> toDtos(
            List<Post> posts,
            Map<Long, Long> commentCountByPostId,
            Long userIdOptional
    ) {
        if (posts.isEmpty()) {
            return List.of();
        }
        List<Long> ids = posts.stream().map(Post::getId).toList();
        Map<Long, Long> viewCounts = loadViewCounts(ids);
        Map<Long, Boolean> hasImg = loadHasImages(ids);
        Map<Long, Map<String, Long>> reactionCounts = loadReactionCounts(ids);
        Map<Long, double[]> ratingStats = loadRatingStats(ids);
        Map<Long, ReactionType> myReactions = (userIdOptional != null && userIdOptional > 0)
                ? loadMyReactions(userIdOptional, ids)
                : Map.of();
        Map<Long, Integer> myRatings = (userIdOptional != null && userIdOptional > 0)
                ? loadMyRatings(userIdOptional, ids)
                : Map.of();

        Set<Long> followedIds = (userIdOptional != null && userIdOptional > 0)
                ? loadFollowedPostIds(userIdOptional, ids)
                : Set.of();

        Set<Long> authorIds = posts.stream().map(Post::getUserId).collect(Collectors.toSet());
        Map<Long, String> namesByUser = userDisplayClient.resolveDisplayNames(authorIds);

        List<PostDTO> out = new ArrayList<>();
        for (Post post : posts) {
            Long pid = post.getId();
            long cc = commentCountByPostId.getOrDefault(pid, 0L);
            Map<String, Long> rc = reactionCounts.getOrDefault(pid, emptyReactionMap());
            double[] rs = ratingStats.get(pid);
            Double avg = rs != null ? rs[0] : null;
            long rcount = rs != null ? (long) rs[1] : 0L;

            ReactionType mine = myReactions.get(pid);
            Integer myR = myRatings.get(pid);

            Long uid = post.getUserId();
            out.add(PostDTO.builder()
                    .id(pid)
                    .title(post.getTitle())
                    .content(post.getContent())
                    .userId(uid)
                    .author(namesByUser.getOrDefault(uid, "User " + uid))
                    .createdAt(post.getCreatedAt())
                    .categoryId(post.getCategory() != null ? post.getCategory().getId() : null)
                    .categoryName(post.getCategory() != null ? post.getCategory().getName() : "General")
                    .status(post.getStatus() != null ? post.getStatus() : "PUBLISHED")
                    .commentCount(cc)
                    .viewCount(viewCounts.getOrDefault(pid, 0L))
                    .reactionCounts(rc)
                    .myReaction(mine != null ? mine.name() : null)
                    .averageRating(avg)
                    .ratingCount(rcount)
                    .myRating(myR)
                    .inactive(post.isInactive())
                    .inactiveSince(post.getInactiveSince())
                    .hasImages(hasImg.getOrDefault(pid, false))
                    .following(userIdOptional != null && userIdOptional > 0 ? followedIds.contains(pid) : null)
                    .build());
        }
        return out;
    }

    private Set<Long> loadFollowedPostIds(Long userId, List<Long> postIds) {
        if (postIds.isEmpty()) {
            return Set.of();
        }
        Set<Long> out = new HashSet<>();
        for (PostFollow f : postFollowRepository.findByUserIdAndPost_IdIn(userId, postIds)) {
            if (f.getPost() != null && f.getPost().getId() != null) {
                out.add(f.getPost().getId());
            }
        }
        return out;
    }

    private Map<Long, Boolean> loadHasImages(List<Long> postIds) {
        if (postIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Boolean> map = postIds.stream().collect(Collectors.toMap(id -> id, id -> false, (a, b) -> a, LinkedHashMap::new));
        List<Object[]> rows = postMediaRepository.countByPostIdIn(postIds);
        for (Object[] row : rows) {
            Long pid = (Long) row[0];
            long cnt = ((Number) row[1]).longValue();
            map.put(pid, cnt > 0);
        }
        return map;
    }

    private Map<String, Long> emptyReactionMap() {
        Map<String, Long> m = new LinkedHashMap<>();
        for (ReactionType t : ReactionType.values()) {
            m.put(t.name(), 0L);
        }
        return m;
    }

    private Map<Long, Long> loadViewCounts(List<Long> postIds) {
        Map<Long, Long> map = postIds.stream().collect(Collectors.toMap(id -> id, id -> 0L, (a, b) -> a, LinkedHashMap::new));
        List<Object[]> rows = postViewRepository.countByPostIdIn(postIds);
        for (Object[] row : rows) {
            map.put((Long) row[0], (Long) row[1]);
        }
        return map;
    }

    private Map<Long, Map<String, Long>> loadReactionCounts(List<Long> postIds) {
        Map<Long, Map<String, Long>> out = new HashMap<>();
        for (Long pid : postIds) {
            out.put(pid, new LinkedHashMap<>(emptyReactionMap()));
        }
        List<Object[]> rows = postReactionRepository.countGroupedByPostIds(postIds);
        for (Object[] row : rows) {
            Long pid = (Long) row[0];
            ReactionType t = (ReactionType) row[1];
            Long c = (Long) row[2];
            out.computeIfAbsent(pid, k -> new LinkedHashMap<>(emptyReactionMap())).put(t.name(), c);
        }
        return out;
    }

    /** postId -> [avg, count] */
    private Map<Long, double[]> loadRatingStats(List<Long> postIds) {
        Map<Long, double[]> out = new HashMap<>();
        List<Object[]> rows = postRatingRepository.avgAndCountByPostIds(postIds);
        for (Object[] row : rows) {
            Long pid = (Long) row[0];
            Double avg = row[1] != null ? ((Number) row[1]).doubleValue() : null;
            long cnt = ((Number) row[2]).longValue();
            if (avg != null) {
                out.put(pid, new double[]{avg, cnt});
            }
        }
        return out;
    }

    private Map<Long, ReactionType> loadMyReactions(Long userId, List<Long> postIds) {
        List<Object[]> rows = postReactionRepository.findReactionsForUser(postIds, userId);
        Map<Long, ReactionType> m = new HashMap<>();
        for (Object[] row : rows) {
            m.put((Long) row[0], (ReactionType) row[1]);
        }
        return m;
    }

    private Map<Long, Integer> loadMyRatings(Long userId, List<Long> postIds) {
        List<Object[]> rows = postRatingRepository.findRatingsForUser(postIds, userId);
        Map<Long, Integer> m = new HashMap<>();
        for (Object[] row : rows) {
            m.put((Long) row[0], ((Number) row[1]).intValue());
        }
        return m;
    }
}
