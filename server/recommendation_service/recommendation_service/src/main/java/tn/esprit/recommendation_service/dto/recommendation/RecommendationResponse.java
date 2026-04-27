package tn.esprit.recommendation_service.dto.recommendation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.esprit.recommendation_service.enums.RecommendationStatus;
import tn.esprit.recommendation_service.enums.RecommendationType;
import tn.esprit.recommendation_service.enums.MedicalEventType;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationResponse {

    private Long id;
    private String content;
    private RecommendationType type;
    private RecommendationStatus status;
    private Boolean dismissed;
    private Integer priority;
    private Long doctorId;
    private Long patientId;
    private Integer rejectionCount;
    private LocalDate expirationDate;
    private LocalDateTime acceptedAt;
    private Long generatedMedicalEventId;
    private String generatedMedicalEventTitle;
    private MedicalEventType generatedMedicalEventType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
