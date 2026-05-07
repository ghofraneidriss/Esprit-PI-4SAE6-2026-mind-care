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
public class PuzzleSessionStartResponse {

    private Long sessionId;
    private Long puzzleId;
    private Long patientId;
    private LocalDateTime startedAt;
}
