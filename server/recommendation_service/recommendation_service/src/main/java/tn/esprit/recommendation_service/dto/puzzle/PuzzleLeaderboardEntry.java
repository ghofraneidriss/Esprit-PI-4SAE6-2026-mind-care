package tn.esprit.recommendation_service.dto.puzzle;

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
public class PuzzleLeaderboardEntry {

    private Integer rank;
    private Long patientId;
    private Integer bestScore;
    private Integer bestDurationSeconds;
    private Integer completedSessions;
    private LocalDateTime lastCompletedAt;
}
