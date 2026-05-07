package tn.esprit.recommendation_service.dto.medicalevent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.esprit.recommendation_service.enums.ParticipantType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipantRankingResponse {

    private Long participantId;
    private ParticipantType participantType;
    private Long totalScore;
    private Long participations;
    private Integer rank;
}
