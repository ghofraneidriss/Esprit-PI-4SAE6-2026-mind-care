package tn.esprit.forums_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.forums_service.entity.Post;
import tn.esprit.forums_service.entity.PostFollow;
import tn.esprit.forums_service.exception.ResourceNotFoundException;
import tn.esprit.forums_service.client.UserDisplayClient;
import tn.esprit.forums_service.repository.PostFollowRepository;
import tn.esprit.forums_service.repository.PostRepository;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostFollowService {

    private final PostFollowRepository postFollowRepository;
    private final PostRepository postRepository;
    private final PostInteractionService postInteractionService;
    private final ForumNotificationPublisher forumNotificationPublisher;
    private final UserDisplayClient userDisplayClient;

    @Transactional
    public void follow(Long userId, Long postId) {
        if (userId == null || userId <= 0 || postId == null) {
            throw new IllegalArgumentException("userId and postId required");
        }
        if (postFollowRepository.existsByUserIdAndPost_Id(userId, postId)) {
            return;
        }
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));
        postFollowRepository.save(PostFollow.builder().userId(userId).post(post).build());
        postInteractionService.touch(postId);

        /* Author only; never notify yourself for following your own thread. */
        Long authorId = post.getUserId();
        if (authorId != null && authorId > 0 && !authorId.equals(userId)) {
            String actor = userDisplayClient.resolveDisplayNames(Set.of(userId)).getOrDefault(userId, "A member");
            String shortTitle = shortenPostTitle(post.getTitle());
            forumNotificationPublisher.notifyEnriched(
                    authorId,
                    actor + " followed your post \"" + shortTitle + "\"",
                    "INFO",
                    postId,
                    "FORUM_THREAD_FOLLOW",
                    actor,
                    shortTitle,
                    null,
                    userId);
        }
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

    @Transactional
    public void unfollow(Long userId, Long postId) {
        if (userId == null || postId == null) {
            return;
        }
        postFollowRepository.deleteByUserIdAndPost_Id(userId, postId);
        /* No touch(): unsubscribing must not bump lastInteractionAt or “activity” KPIs on the thread. */
    }

    public boolean isFollowing(Long userId, Long postId) {
        if (userId == null || userId <= 0 || postId == null) {
            return false;
        }
        return postFollowRepository.existsByUserIdAndPost_Id(userId, postId);
    }

    public List<Long> followerUserIds(Long postId) {
        return postFollowRepository.findByPost_Id(postId).stream()
                .map(PostFollow::getUserId)
                .collect(Collectors.toList());
    }
}
