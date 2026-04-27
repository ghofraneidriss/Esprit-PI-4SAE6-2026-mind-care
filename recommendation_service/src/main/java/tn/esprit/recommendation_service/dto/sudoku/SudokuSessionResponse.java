package tn.esprit.recommendation_service.dto.sudoku;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SudokuSessionResponse {
    private Long id;
    private Long gameId;
    private Long patientId;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Integer durationSeconds;
    private Integer errorsCount;
    private Integer hintsUsed;
    private Integer completionPercent;
    private Integer score;
    private Boolean completed;
    private Boolean abandoned;
    private LocalDateTime createdAt;
}
