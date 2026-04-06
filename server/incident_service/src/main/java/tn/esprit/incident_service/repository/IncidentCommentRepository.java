package tn.esprit.incident_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.incident_service.entity.IncidentComment;

import java.util.List;

@Repository
public interface IncidentCommentRepository extends JpaRepository<IncidentComment, Long> {

    List<IncidentComment> findByIncidentIdOrderByCreatedAtAsc(Long incidentId);

    void deleteByIncidentId(Long incidentId);
}
