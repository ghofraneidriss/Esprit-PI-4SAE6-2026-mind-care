package tn.esprit.followup_alert_service.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.followup_alert_service.Entity.Alert;
import tn.esprit.followup_alert_service.Entity.AlertLevel;
import tn.esprit.followup_alert_service.Entity.AlertStatus;
import tn.esprit.followup_alert_service.Service.AlertService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    // ==================== EXISTING CRUD ====================

    @PostMapping
    public ResponseEntity<Alert> createAlert(@Valid @RequestBody Alert alert) {
        Alert created = alertService.createAlert(alert);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Alert>> getAllAlerts() {
        return ResponseEntity.ok(alertService.getAllAlerts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Alert> getAlertById(@PathVariable Long id) {
        return ResponseEntity.ok(alertService.getAlertById(id));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<Alert>> getAlertsByPatientId(@PathVariable Long patientId) {
        return ResponseEntity.ok(alertService.getAlertsByPatientId(patientId));
    }

    @GetMapping("/level/{level}")
    public ResponseEntity<List<Alert>> getAlertsByLevel(@PathVariable AlertLevel level) {
        return ResponseEntity.ok(alertService.getAlertsByLevel(level));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Alert>> getAlertsByStatus(@PathVariable AlertStatus status) {
        return ResponseEntity.ok(alertService.getAlertsByStatus(status));
    }

    @GetMapping("/critical/new")
    public ResponseEntity<List<Alert>> getCriticalNewAlerts() {
        return ResponseEntity.ok(alertService.getCriticalNewAlerts());
    }

    @PatchMapping("/{id}/view")
    public ResponseEntity<Alert> markAsViewed(@PathVariable Long id) {
        return ResponseEntity.ok(alertService.markAsViewed(id));
    }

    @PatchMapping("/{id}/resolve")
    public ResponseEntity<Alert> resolveAlert(@PathVariable Long id) {
        return ResponseEntity.ok(alertService.resolveAlert(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Alert> updateAlert(@PathVariable Long id, @Valid @RequestBody Alert alert) {
        return ResponseEntity.ok(alertService.updateAlert(id, alert));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlert(@PathVariable Long id) {
        alertService.deleteAlert(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== FONCTIONNALITES AVANCEES ====================

    /** Escalate alert level: LOW -> MEDIUM -> HIGH -> CRITICAL */
    @PatchMapping("/{id}/escalate")
    public ResponseEntity<Alert> escalateAlert(@PathVariable Long id) {
        return ResponseEntity.ok(alertService.escalateAlert(id));
    }

    /** Bulk resolve all alerts for a patient */
    @PatchMapping("/patient/{patientId}/resolve-all")
    public ResponseEntity<Map<String, Object>> resolveAllByPatient(@PathVariable Long patientId) {
        int count = alertService.resolveAllByPatient(patientId);
        return ResponseEntity.ok(Map.of("resolved", count, "patientId", patientId));
    }

    /** Alert statistics dashboard */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(alertService.getStatistics());
    }
}