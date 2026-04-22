package tn.esprit.recommendation_service.dto.recommendation;

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
public class RecommendationStatsResponse {

    private Long patientId;
    private Long acceptedCount;
    private Long rejectedCount;
}
