package tn.esprit.recommendation_service.dto.puzzle;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.esprit.recommendation_service.enums.DifficultyLevel;
import tn.esprit.recommendation_service.enums.PuzzleStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PuzzleUpdateRequest {

    @NotBlank
    @Size(max = 255)
    private String title;

    @Size(max = 4000)
    private String description;

    @NotNull
    private DifficultyLevel difficulty;

    @Min(30)
    @Max(3600)
    private Integer timeLimitSeconds;

    @Min(0)
    @Max(10)
    private Integer maxHints;

    @NotNull
    private PuzzleStatus status;
}
