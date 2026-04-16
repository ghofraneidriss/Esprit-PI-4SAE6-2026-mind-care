package tn.esprit.activities_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import tn.esprit.activities_service.entity.Question;
import java.util.List;
import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "quiz_activity")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuizActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Question> questions = new ArrayList<>();

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String theme;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "status")
    private String status;

    @Column(nullable = false)
    private String difficulty;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String level;

    @Column(name = "created_at", updatable = false)
    private java.util.Date createdAt;

    @Column(name = "updated_at")
    private java.util.Date updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = new java.util.Date();
        updatedAt = new java.util.Date();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new java.util.Date();
    }
}
