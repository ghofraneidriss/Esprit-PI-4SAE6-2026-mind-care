package tn.esprit.forums_service.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.forums_service.entity.Post;
import tn.esprit.forums_service.repository.PostRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostService postService;

    @Test
    void testGetAllPosts() {
        Post post = new Post();
        post.setId(1L);
        post.setTitle("Test Title");

        // Utilisation de Collections.singletonList pour éviter le warning
        when(postRepository.findAll()).thenReturn(Collections.singletonList(post));

        List<Post> result = postService.getAllPosts();

        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(postRepository, times(1)).findAll();
    }

    @Test
    void testGetPostById() {
        Post post = new Post();
        post.setId(1L);

        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        Post result = postService.getPostById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }
}