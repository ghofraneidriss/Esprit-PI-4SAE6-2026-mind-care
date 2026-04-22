package tn.esprit.recommendation_service.dto.medicalevent;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.esprit.recommendation_service.enums.ParticipantType;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JoinMedicalEventRequest {

    @NotNull
    private Long participantId;

    @NotNull
    private ParticipantType participantType;

    @Builder.Default
    private LocalDate participationDate = LocalDate.now();

    @Min(0)
    @Builder.Default
    private Integer score = 0;
}
