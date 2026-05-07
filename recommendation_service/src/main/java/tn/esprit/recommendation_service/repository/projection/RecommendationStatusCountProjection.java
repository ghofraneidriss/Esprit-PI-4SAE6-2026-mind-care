package tn.esprit.recommendation_service.repository.projection;

public interface RecommendationStatusCountProjection {
    Long getAcceptedCount();
    Long getRejectedCount();
}
