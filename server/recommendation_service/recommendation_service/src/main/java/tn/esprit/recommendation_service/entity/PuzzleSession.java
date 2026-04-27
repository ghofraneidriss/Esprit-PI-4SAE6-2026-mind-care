package tn.esprit.recommendation_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "puzzle_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PuzzleSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "puzzle_id", nullable = false)
    private SouvenirPuzzle puzzle;

    @Column(nullable = false)
    private Long patientId;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    private Integer durationSeconds;

    @Column(nullable = false)
    @Builder.Default
    private Integer movesCount = 0;

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
