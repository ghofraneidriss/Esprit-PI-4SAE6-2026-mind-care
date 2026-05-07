package tn.esprit.recommendation_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import tn.esprit.recommendation_service.enums.DifficultyLevel;

import java.time.LocalDateTime;

@Entity
@Table(name = "sudoku_games")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SudokuGame {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "medical_event_id", nullable = false)
    private MedicalEvent medicalEvent;

    @Column(nullable = false)
    private Long patientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DifficultyLevel difficulty; // EASY=4x4, HARD=9x9

    @Column(nullable = false, length = 500)
    private String puzzle;   // JSON array: "[[5,3,0,...],...]" 0=empty

    @Column(nullable = false, length = 500)
    private String solution; // JSON array: full solution

    @Column(nullable = false)
    @Builder.Default
    private Integer gridSize = 9; // 4 or 9

    @Column(nullable = false)
    @Builder.Default
    private Integer timeLimitSeconds = 600;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = Boolean.TRUE;

    @Column
    private Integer bestScore;

    @Column
    @Builder.Default
    private Integer completedSessions = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
