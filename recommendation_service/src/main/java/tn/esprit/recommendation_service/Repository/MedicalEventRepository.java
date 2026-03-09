package tn.esprit.recommendation_service.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.recommendation_service.Entities.MedicalEvent;
import java.util.List;

public interface MedicalEventRepository extends JpaRepository<MedicalEvent, Long> {
    List<MedicalEvent> findAllByOrderByIdDesc();

    List<MedicalEvent> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrderByTitleAsc(
            String title, String description);
}
