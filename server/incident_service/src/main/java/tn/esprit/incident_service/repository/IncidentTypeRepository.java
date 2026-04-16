package tn.esprit.incident_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.incident_service.entity.IncidentType;

@Repository
public interface IncidentTypeRepository extends JpaRepository<IncidentType, Long> {
}
