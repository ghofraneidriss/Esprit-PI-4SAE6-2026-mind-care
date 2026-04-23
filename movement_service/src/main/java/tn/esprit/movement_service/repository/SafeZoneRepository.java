package tn.esprit.movement_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.movement_service.entity.SafeZone;

import java.util.List;

@Repository
public interface SafeZoneRepository extends JpaRepository<SafeZone, Long> {
    List<SafeZone> findByPatientId(Long patientId);
}
