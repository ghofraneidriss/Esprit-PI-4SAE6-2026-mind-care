package tn.esprit.recommendation_service.dto.medicalevent;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.esprit.recommendation_service.enums.ParticipantType;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalEventParticipationResponse {

    private Long id;
    private Long medicalEventId;
    private Long participantId;
    private ParticipantType participantType;
    private LocalDate participationDate;
    private Integer score;
    private LocalDateTime createdAt;
}
