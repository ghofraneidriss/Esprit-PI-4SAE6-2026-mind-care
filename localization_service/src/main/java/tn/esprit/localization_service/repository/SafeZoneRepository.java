package tn.esprit.localization_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tn.esprit.localization_service.entity.SafeZone;

import java.util.List;

@Repository
public interface SafeZoneRepository extends JpaRepository<SafeZone, Long> {
    List<SafeZone> findByPatientId(Long patientId);

    @Query("select distinct sz.patientId from SafeZone sz where sz.patientId is not null")
    List<Long> findDistinctPatientIds();
}
