package tn.esprit.recommendation_service.dto.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientStatsResponse {

    private Long patientId;

    private Integer totalSessions;
    private Integer completedSessions;

    private Double avgScore;
    private Double avgDurationSeconds;

    private Integer bestScore;
    private Integer totalMoves;
    private Integer totalErrors;

    private LocalDateTime lastSessionAt;
    private LocalDateTime updatedAt;
}

