package tn.esprit.recommendation_service.dto.sudoku;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SudokuSessionSubmitRequest {
    @NotNull private Long patientId;
    private Integer durationSeconds;
    private Integer errorsCount;
    private Integer hintsUsed;
    private Integer completionPercent;
    private Boolean completed;
    private Boolean abandoned;
}
