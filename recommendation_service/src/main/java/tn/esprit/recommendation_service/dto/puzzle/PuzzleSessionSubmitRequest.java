package tn.esprit.recommendation_service.dto.puzzle;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PuzzleSessionSubmitRequest {

    @NotNull
    private Long patientId;

    @Min(0)
    private Integer durationSeconds;

    @Min(0)
    private Integer movesCount;

    @Min(0)
    private Integer errorsCount;

    @Min(0)
    private Integer hintsUsed;

    @Min(0)
    @Max(100)
    private Integer completionPercent;

    @NotNull
    private Boolean completed;

    @NotNull
    private Boolean abandoned;
}
