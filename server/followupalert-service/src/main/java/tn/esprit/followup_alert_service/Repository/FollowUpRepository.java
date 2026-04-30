package tn.esprit.followup_alert_service.Repository;

import tn.esprit.followup_alert_service.Entity.FollowUp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FollowUpRepository extends JpaRepository<FollowUp, Long> {

    // Trouver tous les follow-ups d'un patient
    List<FollowUp> findByPatientId(Long patientId);

    // Trouver un follow-up par patient et date (pour validation 1 par jour)
    Optional<FollowUp> findByPatientIdAndFollowUpDate(Long patientId, LocalDate date);

    // Trouver tous les follow-ups d'un caregiver
    List<FollowUp> findByCaregiverId(Long caregiverId);
}