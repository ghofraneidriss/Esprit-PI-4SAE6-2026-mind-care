package tn.esprit.recommendation_service.dto.sudoku;

import lombok.*;
import tn.esprit.recommendation_service.enums.DifficultyLevel;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SudokuResponse {
    private Long id;
    private Long medicalEventId;
    private Long patientId;
    private DifficultyLevel difficulty;
    private Integer gridSize;
    private String puzzle;   // JSON grid with 0s for empty cells
    private Integer timeLimitSeconds;
    private Boolean active;
    private Integer bestScore;
    private Integer completedSessions;
    private LocalDateTime createdAt;
}
