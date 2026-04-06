package tn.esprit.forums_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    private String title;

    @Lob
    @NotBlank(message = "Content is required")
    private String content;

    private String status; // PUBLISHED, DRAFT

    private LocalDateTime createdAt;

    @NotNull(message = "User ID is required")
    private Long userId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    @JsonIgnoreProperties({"posts"}) // Prevent serialization of category.posts list
    private Category category;

    @OneToMany(mappedBy = "post", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore // Prevent serialization of comments entirely
    @JsonIgnoreProperties({"post"}) // Prevent bi-directional loop if accessed
    private List<Comment> comments;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
