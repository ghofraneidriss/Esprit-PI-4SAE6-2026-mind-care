package tn.esprit.recommendation_service.Services;

import tn.esprit.recommendation_service.Entities.Recommendation;
import java.util.List;

public interface RecommendationService {
    Recommendation create(Recommendation recommendation);

    List<Recommendation> getAll();

    Recommendation getById(Long id);

    List<Recommendation> getByType(String type);

    List<Recommendation> getByStatus(String status);

    Recommendation update(Long id, Recommendation recommendation);

    void delete(Long id);

    Recommendation approve(Long id);

    List<Recommendation> search(String keyword);
}
