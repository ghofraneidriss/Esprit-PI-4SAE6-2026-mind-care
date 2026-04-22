package tn.esprit.recommendation_service.dto.recommendation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.esprit.recommendation_service.enums.PatientLevel;
import tn.esprit.recommendation_service.enums.RecommendationType;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutoRecommendationGenerateRequest {

    @NotNull
    private Long doctorId;

    @NotNull
    private Long patientId;

    private Long preferredMedicalEventId;

    @NotNull
    @Min(1)
    @Max(120)
    private Integer age;

    @NotNull
    private PatientLevel level;

    @Min(0)
    @Builder.Default
    private Integer weeklyFrequency = 0;

    @Builder.Default
    private Integer acceptedCount = 0;

    @Builder.Default
    private Integer rejectedCount = 0;

    @Builder.Default
    private Boolean medicationAdherenceIssue = Boolean.FALSE;

    @Builder.Default
    private Boolean lowPhysicalActivity = Boolean.FALSE;

    @Builder.Default
    private Boolean cognitiveDropObserved = Boolean.FALSE;

    @Builder.Default
    private List<RecommendationType> recentRecommendationTypes = List.of();
}
