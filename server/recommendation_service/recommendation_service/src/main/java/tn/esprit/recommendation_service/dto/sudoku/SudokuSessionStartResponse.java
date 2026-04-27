package tn.esprit.recommendation_service.dto.sudoku;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SudokuSessionStartResponse {
    private Long sessionId;
    private Long gameId;
    private Long patientId;
    private LocalDateTime startedAt;
}
