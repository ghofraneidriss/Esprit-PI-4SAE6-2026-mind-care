package tn.esprit.recommendation_service.dto.medicalevent;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.esprit.recommendation_service.enums.DifficultyLevel;
import tn.esprit.recommendation_service.enums.MedicalEventStatus;
import tn.esprit.recommendation_service.enums.MedicalEventType;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalEventUpdateRequest {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private MedicalEventType type;

    @NotNull
    private DifficultyLevel difficulty;

    @NotNull
    private MedicalEventStatus status;

    @NotNull
    private Long patientId;

    private Long familyId;

    @NotNull
    private LocalDateTime startDate;

    @NotNull
    private LocalDateTime endDate;
}
