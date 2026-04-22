package tn.esprit.recommendation_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sudoku_sessions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SudokuSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sudoku_game_id", nullable = false)
    private SudokuGame sudokuGame;

    @Column(nullable = false)
    private Long patientId;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    @Column(nullable = false)
    @Builder.Default
    private Integer durationSeconds = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer errorsCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer hintsUsed = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer completionPercent = 0;

    private Integer score;

    @Column(nullable = false)
    @Builder.Default
    private Boolean completed = Boolean.FALSE;

    @Column(nullable = false)
    @Builder.Default
    private Boolean abandoned = Boolean.FALSE;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
