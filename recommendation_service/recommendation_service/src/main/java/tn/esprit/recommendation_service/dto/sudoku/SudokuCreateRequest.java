package tn.esprit.recommendation_service.dto.sudoku;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import tn.esprit.recommendation_service.enums.DifficultyLevel;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SudokuCreateRequest {
    @NotNull private Long patientId;
    @NotNull private DifficultyLevel difficulty; // EASY=4x4, HARD=9x9
    private Integer timeLimitSeconds;
    private String title;
    private String description;
}
