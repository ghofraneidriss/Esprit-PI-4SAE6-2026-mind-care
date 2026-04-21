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
public class StreakResponse {

    private Long medicalEventId;
    private Long participantId;
    private Integer streakDays;
}
