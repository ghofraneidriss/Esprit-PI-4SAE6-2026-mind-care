package tn.esprit.forums_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.forums_service.entity.Post;
import tn.esprit.forums_service.repository.PostRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PostInteractionService {

    private final PostRepository postRepository;

    @Transactional
    public void touch(Long postId) {
        if (postId == null) {
            return;
        }
        postRepository.findById(postId).ifPresent(p -> {
            p.setLastInteractionAt(LocalDateTime.now());
            postRepository.save(p);
        });
    }

    @Transactional
    public void touch(Post post) {
        if (post == null || post.getId() == null) {
            return;
        }
        post.setLastInteractionAt(LocalDateTime.now());
        postRepository.save(post);
    }
}
