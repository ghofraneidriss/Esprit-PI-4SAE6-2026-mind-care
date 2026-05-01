package tn.esprit.followup_alert_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.followup_alert_service.dto.AlertRequestDTO;
import tn.esprit.followup_alert_service.dto.AlertResponseDTO;
import tn.esprit.followup_alert_service.entity.Alert;
import tn.esprit.followup_alert_service.entity.AlertLevel;
import tn.esprit.followup_alert_service.entity.AlertStatus;
import tn.esprit.followup_alert_service.service.AlertService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    // ==================== EXISTING CRUD ====================

    @PostMapping
    public ResponseEntity<AlertResponseDTO> createAlert(@Valid @RequestBody AlertRequestDTO requestDTO) {
        Alert alert = new Alert();
        alert.setPatientId(requestDTO.getPatientId());
        alert.setTitle(requestDTO.getTitle());
        alert.setDescription(requestDTO.getDescription());
        alert.setLevel(requestDTO.getLevel());
        Alert created = alertService.createAlert(alert);
        return new ResponseEntity<>(AlertResponseDTO.fromEntity(created), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<AlertResponseDTO>> getAllAlerts() {
        return ResponseEntity.ok(alertService.getAllAlerts().stream()
                .map(AlertResponseDTO::fromEntity)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlertResponseDTO> getAlertById(@PathVariable Long id) {
        return ResponseEntity.ok(AlertResponseDTO.fromEntity(alertService.getAlertById(id)));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<AlertResponseDTO>> getAlertsByPatientId(@PathVariable Long patientId) {
        return ResponseEntity.ok(alertService.getAlertsByPatientId(patientId).stream()
                .map(AlertResponseDTO::fromEntity)
                .toList());
    }

    @GetMapping("/level/{level}")
    public ResponseEntity<List<AlertResponseDTO>> getAlertsByLevel(@PathVariable AlertLevel level) {
        return ResponseEntity.ok(alertService.getAlertsByLevel(level).stream()
                .map(AlertResponseDTO::fromEntity)
                .toList());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<AlertResponseDTO>> getAlertsByStatus(@PathVariable AlertStatus status) {
        return ResponseEntity.ok(alertService.getAlertsByStatus(status).stream()
                .map(AlertResponseDTO::fromEntity)
                .toList());
    }

    @GetMapping("/critical/new")
    public ResponseEntity<List<AlertResponseDTO>> getCriticalNewAlerts() {
        return ResponseEntity.ok(alertService.getCriticalNewAlerts().stream()
                .map(AlertResponseDTO::fromEntity)
                .toList());
    }

    @PatchMapping("/{id}/view")
    public ResponseEntity<AlertResponseDTO> markAsViewed(@PathVariable Long id) {
        return ResponseEntity.ok(AlertResponseDTO.fromEntity(alertService.markAsViewed(id)));
    }

    @PatchMapping("/{id}/resolve")
    public ResponseEntity<AlertResponseDTO> resolveAlert(@PathVariable Long id) {
        return ResponseEntity.ok(AlertResponseDTO.fromEntity(alertService.resolveAlert(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AlertResponseDTO> updateAlert(@PathVariable Long id, @Valid @RequestBody AlertRequestDTO requestDTO) {
        Alert alert = new Alert();
        alert.setPatientId(requestDTO.getPatientId());
        alert.setTitle(requestDTO.getTitle());
        alert.setDescription(requestDTO.getDescription());
        alert.setLevel(requestDTO.getLevel());
        return ResponseEntity.ok(AlertResponseDTO.fromEntity(alertService.updateAlert(id, alert)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlert(@PathVariable Long id) {
        alertService.deleteAlert(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== FONCTIONNALITES AVANCEES ====================

    /** Escalate alert level: LOW -> MEDIUM -> HIGH -> CRITICAL */
    @PatchMapping("/{id}/escalate")
    public ResponseEntity<AlertResponseDTO> escalateAlert(@PathVariable Long id) {
        return ResponseEntity.ok(AlertResponseDTO.fromEntity(alertService.escalateAlert(id)));
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