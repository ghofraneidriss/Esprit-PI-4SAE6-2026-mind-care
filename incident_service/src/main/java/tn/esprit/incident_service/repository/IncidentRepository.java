package tn.esprit.incident_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import tn.esprit.incident_service.entity.Incident;
import tn.esprit.incident_service.enums.IncidentStatus;
import tn.esprit.incident_service.enums.SeverityLevel;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    // FIND ACTIVE (deleted = false) - Par défaut
    @Query("SELECT i FROM Incident i WHERE i.deleted = false ORDER BY i.incidentDate DESC")
    List<Incident> findAllActive();

    // FIND BY PATIENT (Active only)
    @Query("SELECT i FROM Incident i WHERE i.patientId = ?1 AND i.deleted = false ORDER BY i.incidentDate DESC")
    List<Incident> findByPatientIdActive(Long patientId);

    // FIND BY PATIENT HISTORY (All, active AND deleted)
    @Query("SELECT i FROM Incident i WHERE i.patientId = ?1 ORDER BY i.incidentDate DESC")
    List<Incident> findByPatientIdAll(Long patientId);

    // FIND BY CAREGIVER (Active only)
    @Query("SELECT i FROM Incident i WHERE i.caregiverId = ?1 AND i.deleted = false ORDER BY i.incidentDate DESC")
    List<Incident> findByCaregiverIdActive(Long caregiverId);

    @Query("SELECT i FROM Incident i WHERE i.volunteerId = ?1 AND i.deleted = false ORDER BY i.incidentDate DESC")
    List<Incident> findByVolunteerIdActive(Long volunteerId);

    // FIND HISTORY (All, active AND deleted) - Pour l'historique Admin
    @Query("SELECT i FROM Incident i ORDER BY i.incidentDate DESC")
    List<Incident> findAllIncludingHistory();

    // FIND SOFT DELETED ONLY (Archive)
    @Query("SELECT i FROM Incident i WHERE i.deleted = true ORDER BY i.incidentDate DESC")
    List<Incident> findDeletedOnly();

    // FIND BY SOURCE (Active only)
    @Query("SELECT i FROM Incident i WHERE i.source = ?1 AND i.deleted = false ORDER BY i.incidentDate DESC")
    List<Incident> findBySourceActive(String source);

    @Query("SELECT i FROM Incident i WHERE i.source = ?1 AND i.reporterUserId = ?2 AND i.deleted = false ORDER BY i.incidentDate DESC")
    List<Incident> findBySourceAndReporterUserIdActive(String source, Long reporterUserId);

    // Override delete behavior (Soft Delete handled by @SQLDelete on Entity)

    // --- STATS ---
    @Query("SELECT COUNT(i) FROM Incident i WHERE i.deleted = false AND i.severityLevel = ?1")
    long countBySeverityActive(SeverityLevel level);

    @Query("SELECT COUNT(i) FROM Incident i WHERE i.deleted = false AND i.status = ?1")
    long countByStatusActive(IncidentStatus status);

    @Query("SELECT COUNT(i) FROM Incident i WHERE i.deleted = false")
    long countAllActive();

    @Query("SELECT COUNT(i) FROM Incident i")
    long countAllTotal();

    @Query("SELECT COUNT(i) FROM Incident i WHERE i.deleted = false AND i.incidentDate >= ?1 AND i.incidentDate < ?2")
    long countByMonthRange(LocalDateTime start, LocalDateTime end);

    // For recurrence scoring: count patient incidents in the last N days
    @Query("SELECT COUNT(i) FROM Incident i WHERE i.patientId = ?1 AND i.deleted = false AND i.incidentDate >= ?2")
    long countRecentByPatient(Long patientId, LocalDateTime since);
}
