package tn.esprit.forums_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.forums_service.dto.PostDTO;
import tn.esprit.forums_service.entity.Post;
import tn.esprit.forums_service.repository.CommentRepository;
import tn.esprit.forums_service.service.PostService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200") // Allow Angular Frontend
public class PostController {

    private final PostService postService;
    private final CommentRepository commentRepository;

    @PostMapping("/category/{categoryId}")
    public ResponseEntity<Post> createPost(@Valid @RequestBody Post post, @PathVariable Long categoryId) {
        return new ResponseEntity<>(postService.createPost(post, categoryId), HttpStatus.CREATED);
    }

    @GetMapping
    public List<PostDTO> getAllPosts() {
        System.out.println(">>> Request: getAllPosts");
        List<Post> posts = postService.getAllPosts();
        System.out.println(">>> Found " + posts.size() + " posts");
        
        List<PostDTO> dtos = posts.stream()
            .map(post -> {
                long count = 0;
                try {
                    count = commentRepository.countByPost_Id(post.getId());
                } catch (Exception e) {
                    System.err.println(">>> Error counting comments for post " + post.getId() + ": " + e.getMessage());
                }
                
                return PostDTO.builder()
                    .id(post.getId())
                    .title(post.getTitle())
                    .content(post.getContent())
                    .userId(post.getUserId())
                    .author("User " + post.getUserId())
                    .createdAt(post.getCreatedAt())
                    .categoryId(post.getCategory() != null ? post.getCategory().getId() : null)
                    .categoryName(post.getCategory() != null ? post.getCategory().getName() : "General")
                    .status(post.getStatus() != null ? post.getStatus() : "PUBLISHED")
                    .commentCount(count)
                    .build();
            })
            .collect(Collectors.toList());
        
        System.out.println(">>> Returning " + dtos.size() + " DTOs for dashboard");
        return dtos;
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostDTO> getPostById(@PathVariable Long id) {
        System.out.println(">>> Request: getPostById - ID: " + id);
        Post post = postService.getPostById(id);
        System.out.println(">>> Found post: " + (post != null ? post.getTitle() : "NULL"));
        
        long count = 0;
        try {
            count = commentRepository.countByPost_Id(id);
        } catch (Exception e) {
            System.err.println(">>> Error counting comments: " + e.getMessage());
        }

        PostDTO dto = PostDTO.builder()
            .id(post.getId())
            .title(post.getTitle())
            .content(post.getContent())
            .userId(post.getUserId())
            .author("User " + post.getUserId())
            .createdAt(post.getCreatedAt())
            .categoryId(post.getCategory() != null ? post.getCategory().getId() : null)
            .categoryName(post.getCategory() != null ? post.getCategory().getName() : "General")
            .status(post.getStatus() != null ? post.getStatus() : "PUBLISHED")
            .commentCount(count)
            .build();
        
        System.out.println(">>> Returning DTO for post " + id);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/category/{categoryId}")
    public List<PostDTO> getPostsByCategoryId(@PathVariable Long categoryId) {
        return postService.getPostsByCategoryId(categoryId).stream()
            .map(post -> PostDTO.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .userId(post.getUserId())
                .author("User " + post.getUserId())
                .createdAt(post.getCreatedAt())
                .categoryId(post.getCategory() != null ? post.getCategory().getId() : null)
                .categoryName(post.getCategory() != null ? post.getCategory().getName() : "General")
                .status(post.getStatus() != null ? post.getStatus() : "PUBLISHED")
                .commentCount(commentRepository.countByPost_Id(post.getId()))
                .build())
            .collect(Collectors.toList());
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
}
