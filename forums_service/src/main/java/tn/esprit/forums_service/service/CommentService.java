package tn.esprit.forums_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.forums_service.client.UserDisplayClient;
import tn.esprit.forums_service.entity.Comment;
import tn.esprit.forums_service.entity.Post;
import tn.esprit.forums_service.exception.ResourceNotFoundException;
import tn.esprit.forums_service.repository.CommentDislikeRepository;
import tn.esprit.forums_service.repository.CommentLikeRepository;
import tn.esprit.forums_service.repository.CommentRepository;
import tn.esprit.forums_service.repository.PostRepository;

import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final UserDisplayClient userDisplayClient;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final CommentDislikeRepository commentDislikeRepository;
    private final PostRepository postRepository;
    private final PostInteractionService postInteractionService;
    private final PostFollowService postFollowService;
    private final ForumNotificationPublisher forumNotificationPublisher;
    private final ForumBanService forumBanService;
    private final CommentToxicityModerationService commentToxicityModerationService;

    public Comment addComment(Long postId, Comment comment) {
        if (forumBanService.isBanned(comment.getUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "FORUM_BANNED");
        }
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found with id: " + postId));
        if (comment.getContent() != null) {
            comment.setContent(commentToxicityModerationService.moderateIfNeeded(comment.getContent()));
        }
        comment.setPost(post);
        Comment saved = commentRepository.save(comment);
        postInteractionService.touch(postId);

        /*
         * Notification policy (Instagram-style):
         * - Thread followers get a "following" alert (excluding the commenter — never notify yourself).
         * - Post author gets an "on your post" alert only if they are not already covered as a follower
         *   (avoids duplicate). Author never gets notified when they comment on their own post (actor == author).
         */
        Long authorId = post.getUserId();
        Long actorId = comment.getUserId();
        String excerpt = saved.getContent() == null ? "" : saved.getContent().trim();
        if (excerpt.length() > 120) {
            excerpt = excerpt.substring(0, 117) + "…";
        }
        String titleShort = post.getTitle() == null ? "Post" : post.getTitle();
        if (titleShort.length() > 60) {
            titleShort = titleShort.substring(0, 57) + "…";
        }

        Map<Long, String> names = userDisplayClient.resolveDisplayNames(Set.of(actorId));
        String actorName = names.getOrDefault(actorId, "A member");

        Set<Long> followerIds = new HashSet<>(postFollowService.followerUserIds(postId));
        for (Long followerId : followerIds) {
            if (followerId.equals(actorId)) {
                continue;
            }
            forumNotificationPublisher.notifyEnriched(
                    followerId,
                    actorName + " left a comment on a thread you follow — \"" + titleShort + "\"",
                    "INFO",
                    postId,
                    "FORUM_COMMENT_FOLLOW",
                    actorName,
                    titleShort,
                    excerpt,
                    actorId);
        }
        if (authorId != null
                && !authorId.equals(actorId)
                && !followerIds.contains(authorId)) {
            forumNotificationPublisher.notifyEnriched(
                    authorId,
                    actorName + " left a new comment on your post — \"" + titleShort + "\"",
                    "INFO",
                    postId,
                    "FORUM_COMMENT_AUTHOR",
                    actorName,
                    titleShort,
                    excerpt,
                    actorId);
        }
        attachAuthorName(saved);
        saved.setLikedByMe(false);
        saved.setDislikedByMe(false);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Comment> getCommentsByPostId(Long postId, Long viewerUserId) {
        List<Comment> list = commentRepository.findByPost_Id(postId);
        attachAuthorNames(list);
        for (Comment c : list) {
            c.setContent(commentToxicityModerationService.maskContentForDisplay(c.getContent()));
        }
        if (viewerUserId != null && viewerUserId > 0) {
            for (Comment c : list) {
                c.setLikedByMe(commentLikeRepository.existsByUserIdAndComment_Id(viewerUserId, c.getId()));
                c.setDislikedByMe(commentDislikeRepository.existsByUserIdAndComment_Id(viewerUserId, c.getId()));
            }
        }
        return list;
    }

    @Transactional(readOnly = true)
    public List<Comment> getAllComments() {
        List<Comment> list = commentRepository.findAll();
        attachAuthorNames(list);
        for (Comment c : list) {
            c.setContent(commentToxicityModerationService.maskContentForDisplay(c.getContent()));
        }
        return list;
    }

    /** Comments on posts whose author is {@code authorUserId} (threads owned by that doctor). */
    @Transactional(readOnly = true)
    public List<Comment> getCommentsByPostAuthor(Long authorUserId) {
        List<Comment> list = commentRepository.findByPostAuthorUserId(authorUserId);
        attachAuthorNames(list);
        for (Comment c : list) {
            c.setContent(commentToxicityModerationService.maskContentForDisplay(c.getContent()));
        }
        return list;
    }

    public Comment updateComment(Long id, String newContent) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + id));
        String next = newContent;
        comment.setContent(next != null ? commentToxicityModerationService.moderateIfNeeded(next) : null);
        Comment saved = commentRepository.save(comment);
        attachAuthorName(saved);
        return saved;
    }

    public void deleteComment(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + id));
        commentRepository.delete(comment);
    }

    private void attachAuthorNames(List<Comment> comments) {
        if (comments == null || comments.isEmpty()) {
            return;
        }
        Set<Long> ids = comments.stream().map(Comment::getUserId).collect(Collectors.toSet());
        Map<Long, String> names = userDisplayClient.resolveDisplayNames(ids);
        for (Comment c : comments) {
            c.setAuthorName(names.getOrDefault(c.getUserId(), "User " + c.getUserId()));
        }
    }

    private void attachAuthorName(Comment c) {
        if (c == null || c.getUserId() == null) {
            return;
        }
        Map<Long, String> names = userDisplayClient.resolveDisplayNames(List.of(c.getUserId()));
        c.setAuthorName(names.getOrDefault(c.getUserId(), "User " + c.getUserId()));
    }
}
