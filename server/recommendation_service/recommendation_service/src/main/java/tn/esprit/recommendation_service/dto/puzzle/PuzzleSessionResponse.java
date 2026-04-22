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
public class PuzzleSessionResponse {

    private Long id;
    private Long puzzleId;
    private Long patientId;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Integer durationSeconds;
    private Integer movesCount;
    private Integer errorsCount;
    private Integer hintsUsed;
    private Integer completionPercent;
    private Integer score;
    private Boolean completed;
    private Boolean abandoned;
    private LocalDateTime createdAt;
}
