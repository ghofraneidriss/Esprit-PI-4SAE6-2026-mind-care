package tn.esprit.recommendation_service.dto.puzzle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.esprit.recommendation_service.enums.DifficultyLevel;
import tn.esprit.recommendation_service.enums.PuzzleStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PuzzleResponse {

    private Long id;
    private Long medicalEventId;
    private Long souvenirEntryId;
    private Long patientId;
    private String title;
    private String description;
    private DifficultyLevel difficulty;
    private Integer gridSize;
    private Integer timeLimitSeconds;
    private Integer maxHints;
    private PuzzleStatus status;
    private Integer bestScore;
    private Integer completedSessions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private SouvenirSourceSummary souvenir;
}
