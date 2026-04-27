package tn.esprit.recommendation_service.dto.recommendation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.esprit.recommendation_service.enums.AlertStatus;
import tn.esprit.recommendation_service.enums.RecommendationType;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClinicalEscalationAlertResponse {

    private Long id;
    private Long recommendationId;
    private Long doctorId;
    private Long patientId;
    private RecommendationType recommendationType;
    private Integer rejectionCount;
    private String message;
    private AlertStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
