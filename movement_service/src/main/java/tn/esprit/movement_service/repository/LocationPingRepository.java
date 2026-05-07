package tn.esprit.movement_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tn.esprit.movement_service.entity.LocationPing;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LocationPingRepository extends JpaRepository<LocationPing, Long> {

    Optional<LocationPing> findTopByPatientIdOrderByRecordedAtDesc(Long patientId);

    List<LocationPing> findByPatientIdAndRecordedAtAfterOrderByRecordedAtAsc(Long patientId, LocalDateTime start);

    List<LocationPing> findTop200ByPatientIdOrderByRecordedAtDesc(Long patientId);

    @Query("select distinct lp.patientId from LocationPing lp")
    List<Long> findDistinctPatientIds();
}
