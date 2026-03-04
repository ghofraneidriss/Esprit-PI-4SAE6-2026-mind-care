package tn.esprit.recommendation_service.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.recommendation_service.Entities.MedicalEvent;

public interface MedicalEventRepository extends JpaRepository<MedicalEvent, Long> {
}
