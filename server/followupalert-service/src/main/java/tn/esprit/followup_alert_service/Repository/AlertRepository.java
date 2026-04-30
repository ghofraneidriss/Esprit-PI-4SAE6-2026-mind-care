package tn.esprit.followup_alert_service.Repository;

import tn.esprit.followup_alert_service.Entity.Alert;
import tn.esprit.followup_alert_service.Entity.AlertLevel;
import tn.esprit.followup_alert_service.Entity.AlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    // Trouver toutes les alertes d'un patient
    List<Alert> findByPatientId(Long patientId);

    // Trouver les alertes par niveau (LOW, MEDIUM, HIGH, CRITICAL)
    List<Alert> findByLevel(AlertLevel level);

    // Trouver les alertes par statut (NEW, VIEWED, RESOLVED)
    List<Alert> findByStatus(AlertStatus status);

    // Trouver les alertes critiques non vues (combinaison)
    List<Alert> findByLevelAndStatus(AlertLevel level, AlertStatus status);
}