package tn.esprit.recommendation_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "patient_stats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientStats {

    @Id
    private Long patientId;

    @Column(nullable = false)
    @Builder.Default
    private Integer totalSessions = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer completedSessions = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer completedScoreSum = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer completedDurationSum = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer totalMoves = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer totalErrors = 0;

    private Integer bestScore;

    private LocalDateTime lastSessionAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

