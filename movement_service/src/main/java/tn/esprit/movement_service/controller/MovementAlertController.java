package tn.esprit.movement_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.movement_service.entity.MovementAlert;
import tn.esprit.movement_service.service.MovementMonitoringService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class MovementAlertController {

    private final MovementMonitoringService movementMonitoringService;

    @GetMapping
    public ResponseEntity<List<MovementAlert>> getAlerts(
            @RequestParam(defaultValue = "false") boolean unacknowledgedOnly
    ) {
        return ResponseEntity.ok(movementMonitoringService.getRecentAlerts(unacknowledgedOnly));
    }

    /**
     * Aggregated safe-zone exit counts (persisted alerts only). Avoids relying on {@code GET /alerts},
     * which returns only the 200 most recent alerts globally.
     */
    @GetMapping("/stats/out-of-safe-zone-by-patient")
    public ResponseEntity<Map<Long, Long>> getOutOfSafeZoneExitCountsByPatient() {
        return ResponseEntity.ok(movementMonitoringService.getOutOfSafeZoneExitCountsByPatient());
    }

    /**
     * Total movement alerts per patient (all {@link tn.esprit.movement_service.entity.AlertType} values).
     * Same rows as {@code GET /api/alerts/patient/{id}} — for critical lists, history, and PDF.
     */
    @GetMapping("/stats/total-by-patient")
    public ResponseEntity<Map<Long, Long>> getTotalMovementAlertCountsByPatient() {
        return ResponseEntity.ok(movementMonitoringService.getTotalMovementAlertCountsByPatient());
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<MovementAlert>> getPatientAlerts(@PathVariable Long patientId) {
        return ResponseEntity.ok(movementMonitoringService.getPatientAlerts(patientId));
    }

    @PutMapping("/{alertId}/ack")
    public ResponseEntity<MovementAlert> acknowledge(@PathVariable Long alertId) {
        return ResponseEntity.ok(movementMonitoringService.acknowledgeAlert(alertId));
    }

    @DeleteMapping("/{alertId}")
    public ResponseEntity<Void> deleteAlert(@PathVariable Long alertId) {
        movementMonitoringService.deleteAlert(alertId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/patient/{patientId}/all")
    public ResponseEntity<Void> deleteAllForPatient(@PathVariable Long patientId) {
        movementMonitoringService.deleteAllAlertsForPatient(patientId);
        return ResponseEntity.noContent().build();
    }

    /** Global clear (e.g. doctor dashboard). Path must not be `/all` alone — would match `/{alertId}`. */
    @DeleteMapping("/bulk/all")
    public ResponseEntity<Void> deleteAllAlerts() {
        movementMonitoringService.deleteAllAlerts();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/checks/no-gps")
    public ResponseEntity<String> triggerNoGpsCheck() {
        movementMonitoringService.checkGpsSilence();
        return ResponseEntity.ok("No-GPS check executed");
    }
}
