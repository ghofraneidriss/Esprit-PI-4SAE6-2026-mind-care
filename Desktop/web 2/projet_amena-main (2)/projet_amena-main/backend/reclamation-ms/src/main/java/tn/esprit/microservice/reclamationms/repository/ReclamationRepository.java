package tn.esprit.microservice.reclamationms.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.microservice.reclamationms.entity.ReclamationClassificationLevel;
import tn.esprit.microservice.reclamationms.entity.Reclamation;

public interface ReclamationRepository extends JpaRepository<Reclamation, Long> {

	List<Reclamation> findByClassificationLevelOrderByCreatedAtDesc(ReclamationClassificationLevel classificationLevel);
}
