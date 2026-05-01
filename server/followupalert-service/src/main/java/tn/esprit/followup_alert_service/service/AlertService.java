package tn.esprit.followup_alert_service.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.followup_alert_service.Entity.Alert;
import tn.esprit.followup_alert_service.Entity.AlertLevel;
import tn.esprit.followup_alert_service.Entity.AlertStatus;
import tn.esprit.followup_alert_service.Repository.AlertRepository;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final AlertRepository alertRepository;

    // ==================== EXISTING CRUD (unchanged) ====================

    public Alert createAlert(Alert alert) {
        return alertRepository.save(alert);
    }

    public List<Alert> getAllAlerts() {
        return alertRepository.findAll();
    }

    public Alert getAlertById(Long id) {
        return alertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alert not found with id: " + id));
    }

    public List<Alert> getAlertsByPatientId(Long patientId) {
        return alertRepository.findByPatientId(patientId);
    }

    public List<Alert> getAlertsByLevel(AlertLevel level) {
        return alertRepository.findByLevel(level);
    }

    public List<Alert> getAlertsByStatus(AlertStatus status) {
        return alertRepository.findByStatus(status);
    }

    public List<Alert> getCriticalNewAlerts() {
        return alertRepository.findByLevelAndStatus(AlertLevel.CRITICAL, AlertStatus.NEW);
    }

    public Alert markAsViewed(Long id) {
        Alert alert = getAlertById(id);
        alert.setStatus(AlertStatus.VIEWED);
        alert.setViewedAt(LocalDateTime.now());
        return alertRepository.save(alert);
    }

    public Alert resolveAlert(Long id) {
        Alert alert = getAlertById(id);
        alert.setStatus(AlertStatus.RESOLVED);
        return alertRepository.save(alert);
    }

    public Alert updateAlert(Long id, Alert updatedAlert) {
        Alert existing = getAlertById(id);
        existing.setPatientId(updatedAlert.getPatientId());
        existing.setTitle(updatedAlert.getTitle());
        existing.setDescription(updatedAlert.getDescription());
        existing.setLevel(updatedAlert.getLevel());
        return alertRepository.save(existing);
    }

    public void deleteAlert(Long id) {
        if (!alertRepository.existsById(id)) {
            throw new RuntimeException("Alert not found with id: " + id);
        }
        alertRepository.deleteById(id);
    }

    // ==================== FONCTIONNALITE AVANCEE 1: Escalate Alert ====================

    public Alert escalateAlert(Long id) {
        Alert alert = getAlertById(id);
        switch (alert.getLevel()) {
            case LOW -> alert.setLevel(AlertLevel.MEDIUM);
            case MEDIUM -> alert.setLevel(AlertLevel.HIGH);
            case HIGH -> alert.setLevel(AlertLevel.CRITICAL);
            case CRITICAL -> log.info("Alert {} is already at CRITICAL level", id);
        }
        alert.setStatus(AlertStatus.NEW);
        return alertRepository.save(alert);
    }

    // ==================== FONCTIONNALITE AVANCEE 2: Bulk Resolve by Patient ====================

    public int resolveAllByPatient(Long patientId) {
        List<Alert> alerts = alertRepository.findByPatientId(patientId);
        int count = 0;
        for (Alert alert : alerts) {
            if (alert.getStatus() != AlertStatus.RESOLVED) {
                alert.setStatus(AlertStatus.RESOLVED);
                alertRepository.save(alert);
                count++;
            }
        }
        log.info("Bulk resolved {} alerts for patient {}", count, patientId);
        return count;
    }

    // ==================== FONCTIONNALITE AVANCEE 3: Statistics ====================

    public Map<String, Object> getStatistics() {
        List<Alert> all = alertRepository.findAll();

        long newCount = all.stream().filter(a -> a.getStatus() == AlertStatus.NEW).count();
        long viewedCount = all.stream().filter(a -> a.getStatus() == AlertStatus.VIEWED).count();
        long resolvedCount = all.stream().filter(a -> a.getStatus() == AlertStatus.RESOLVED).count();

        Map<String, Long> levelDist = all.stream()
                .collect(Collectors.groupingBy(a -> a.getLevel().name(), Collectors.counting()));

        Map<String, Long> alertsPerPatient = all.stream()
                .filter(a -> a.getPatientId() != null)
                .collect(Collectors.groupingBy(a -> String.valueOf(a.getPatientId()), Collectors.counting()));

        long criticalUnresolved = all.stream()
                .filter(a -> a.getLevel() == AlertLevel.CRITICAL && a.getStatus() != AlertStatus.RESOLVED)
                .count();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalAlerts", all.size());
        stats.put("newAlerts", newCount);
        stats.put("viewedAlerts", viewedCount);
        stats.put("resolvedAlerts", resolvedCount);
        stats.put("levelDistribution", levelDist);
        stats.put("alertsPerPatient", alertsPerPatient);
        stats.put("criticalUnresolvedCount", criticalUnresolved);
        return stats;
    }
}