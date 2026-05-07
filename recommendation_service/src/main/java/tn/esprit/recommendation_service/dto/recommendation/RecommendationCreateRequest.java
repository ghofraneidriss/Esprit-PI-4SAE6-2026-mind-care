package tn.esprit.recommendation_service.dto.recommendation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.esprit.recommendation_service.enums.RecommendationType;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationCreateRequest {

    @NotBlank
    private String content;

    @NotNull
    private RecommendationType type;

    @NotNull
    private Long doctorId;

    @NotNull
    private Long patientId;

    @Min(0)
    @Max(10)
    @Builder.Default
    private Integer priority = 0;

    private LocalDate expirationDate;

    private Long generatedMedicalEventId;
}
