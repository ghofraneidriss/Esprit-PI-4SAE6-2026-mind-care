package tn.esprit.forums_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.forums_service.entity.Comment;
import tn.esprit.forums_service.entity.CommentDislike;
import tn.esprit.forums_service.entity.CommentLike;
import tn.esprit.forums_service.entity.Post;
import tn.esprit.forums_service.client.UserDisplayClient;
import tn.esprit.forums_service.exception.ResourceNotFoundException;
import tn.esprit.forums_service.repository.CommentDislikeRepository;
import tn.esprit.forums_service.repository.CommentLikeRepository;
import tn.esprit.forums_service.repository.CommentRepository;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CommentLikeService {

    public static final int BEST_ANSWER_LIKE_THRESHOLD = 7;

    private final CommentLikeRepository commentLikeRepository;
    private final CommentDislikeRepository commentDislikeRepository;
    private final CommentRepository commentRepository;
    private final ForumNotificationPublisher forumNotificationPublisher;
    private final PostInteractionService postInteractionService;
    private final UserDisplayClient userDisplayClient;
    private final ForumBanService forumBanService;

    @Transactional
    public Comment toggleLike(Long commentId, Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Valid userId required");
        }
        if (forumBanService.isBanned(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "FORUM_BANNED");
        }
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));
        Post post = comment.getPost();
        Long postId = post.getId();
        boolean wasBestAnswer = comment.isBestAnswer();
        boolean addedLike = false;

        if (commentLikeRepository.existsByUserIdAndComment_Id(userId, commentId)) {
            commentLikeRepository.deleteByUserIdAndComment_Id(userId, commentId);
            comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
        } else {
            if (commentDislikeRepository.existsByUserIdAndComment_Id(userId, commentId)) {
                commentDislikeRepository.deleteByUserIdAndComment_Id(userId, commentId);
                comment.setDislikeCount(Math.max(0, comment.getDislikeCount() - 1));
            }
            commentLikeRepository.save(
                    CommentLike.builder().userId(userId).comment(comment).build());
            comment.setLikeCount(comment.getLikeCount() + 1);
            addedLike = true;
        }

        applyBestAnswerRules(comment, post);
        postInteractionService.touch(postId);
        Comment saved = commentRepository.save(comment);

        if (addedLike) {
            Long commentAuthorId = saved.getUserId();
            if (commentAuthorId != null && !commentAuthorId.equals(userId)) {
                String actor = userDisplayClient.resolveDisplayNames(Set.of(userId)).getOrDefault(userId, "A member");
                String shortTitle = shortenPostTitle(post.getTitle());
                forumNotificationPublisher.notifyEnriched(
                        commentAuthorId,
                        actor + " liked your comment on \"" + shortTitle + "\"",
                        "INFO",
                        postId,
                        "FORUM_LIKE",
                        actor,
                        shortTitle,
                        null,
                        userId);
            }
        }

        if (saved.isBestAnswer() && !wasBestAnswer && saved.getLikeCount() >= BEST_ANSWER_LIKE_THRESHOLD) {
            String shortTitle = shortenPostTitle(post.getTitle());
            forumNotificationPublisher.notifyEnriched(
                    post.getUserId(),
                    "A comment on \"" + shortTitle + "\" was marked as best answer ("
                            + BEST_ANSWER_LIKE_THRESHOLD + "+ likes).",
                    "INFO",
                    postId,
                    "FORUM_BEST_ANSWER",
                    null,
                    shortTitle,
                    null,
                    null);
        }
        return saved;
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
    public Comment toggleDislike(Long commentId, Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("Valid userId required");
        }
        if (forumBanService.isBanned(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "FORUM_BANNED");
        }
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + commentId));
        Post post = comment.getPost();
        Long postId = post.getId();

        if (commentDislikeRepository.existsByUserIdAndComment_Id(userId, commentId)) {
            commentDislikeRepository.deleteByUserIdAndComment_Id(userId, commentId);
            comment.setDislikeCount(Math.max(0, comment.getDislikeCount() - 1));
        } else {
            if (commentLikeRepository.existsByUserIdAndComment_Id(userId, commentId)) {
                commentLikeRepository.deleteByUserIdAndComment_Id(userId, commentId);
                comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
            }
            commentDislikeRepository.save(
                    CommentDislike.builder().userId(userId).comment(comment).build());
            comment.setDislikeCount(comment.getDislikeCount() + 1);
        }

        applyBestAnswerRules(comment, post);
        postInteractionService.touch(postId);
        return commentRepository.save(comment);
    }

    private void applyBestAnswerRules(Comment comment, Post post) {
        if (comment.getLikeCount() >= BEST_ANSWER_LIKE_THRESHOLD) {
            List<Comment> siblings = commentRepository.findByPost_Id(post.getId());
            for (Comment c : siblings) {
                if (!c.getId().equals(comment.getId()) && c.isBestAnswer()) {
                    c.setBestAnswer(false);
                    commentRepository.save(c);
                }
            }
            comment.setBestAnswer(true);
        } else {
            if (comment.isBestAnswer()) {
                comment.setBestAnswer(false);
            }
        }
    }
}
