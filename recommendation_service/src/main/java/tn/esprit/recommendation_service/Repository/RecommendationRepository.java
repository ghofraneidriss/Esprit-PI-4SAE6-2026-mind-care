package tn.esprit.recommendation_service.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import tn.esprit.recommendation_service.Entities.Recommendation;
import tn.esprit.recommendation_service.Entities.RecommendationStatus;
import tn.esprit.recommendation_service.Entities.RecommendationType;

import java.util.List;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {
    List<Recommendation> findByType(RecommendationType type);

    List<Recommendation> findByStatus(RecommendationStatus status);
}
