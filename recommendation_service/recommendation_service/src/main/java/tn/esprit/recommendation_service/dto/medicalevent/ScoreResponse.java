package tn.esprit.recommendation_service.dto.medicalevent;

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
public class ScoreResponse {

    private Long medicalEventId;
    private Long participantId;
    private Integer totalScore;
}
