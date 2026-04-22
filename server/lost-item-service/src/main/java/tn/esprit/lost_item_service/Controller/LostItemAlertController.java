package tn.esprit.lost_item_service.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.lost_item_service.Entity.AlertLevel;
import tn.esprit.lost_item_service.Entity.AlertStatus;
import tn.esprit.lost_item_service.Entity.LostItemAlert;
import tn.esprit.lost_item_service.Service.AuthorizationService;
import tn.esprit.lost_item_service.Service.LostItemAlertService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/item-alerts")
@RequiredArgsConstructor
public class LostItemAlertController {

    private final LostItemAlertService lostItemAlertService;
    private final AuthorizationService authorizationService;

    @PostMapping
    public ResponseEntity<LostItemAlert> createAlert(@Valid @RequestBody LostItemAlert alert) {
        return new ResponseEntity<>(lostItemAlertService.createAlert(alert), HttpStatus.CREATED);
    }

    /**
     * List all alerts — scoped by role via headers.
     * ADMIN/DOCTOR: all alerts.
     * CAREGIVER: only their alerts.
     * PATIENT: only their alerts.
     */
    @GetMapping
    public ResponseEntity<List<LostItemAlert>> getAllAlerts(
            @RequestHeader(value = "X-User-Id",   required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        String role = userRole != null ? userRole.toUpperCase() : "ADMIN";

        if ("CAREGIVER".equals(role) && userId != null) {
            return ResponseEntity.ok(lostItemAlertService.getAlertsByCaregiverId(userId));
        }
        if ("PATIENT".equals(role) && userId != null) {
            return ResponseEntity.ok(lostItemAlertService.getAlertsByPatientId(userId));
        }
        return ResponseEntity.ok(lostItemAlertService.getAllAlerts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<LostItemAlert> getAlertById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id",   required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        LostItemAlert alert = authorizationService.checkAlertAccess(id, userId, userRole);
        return ResponseEntity.ok(alert);
    }

    @GetMapping("/lost-item/{lostItemId}")
    public ResponseEntity<List<LostItemAlert>> getAlertsByLostItemId(
            @PathVariable Long lostItemId,
            @RequestHeader(value = "X-User-Id",   required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        authorizationService.checkItemIdAccess(lostItemId, userId, userRole);
        return ResponseEntity.ok(lostItemAlertService.getAlertsByLostItemId(lostItemId));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<LostItemAlert>> getAlertsByPatientId(
            @PathVariable Long patientId,
            @RequestHeader(value = "X-User-Id",   required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        authorizationService.checkPatientAlertAccess(patientId, userId, userRole);
        return ResponseEntity.ok(lostItemAlertService.getAlertsByPatientId(patientId));
    }

    @GetMapping("/level/{level}")
    public ResponseEntity<List<LostItemAlert>> getAlertsByLevel(@PathVariable AlertLevel level) {
        return ResponseEntity.ok(lostItemAlertService.getAlertsByLevel(level));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<LostItemAlert>> getAlertsByStatus(@PathVariable AlertStatus status) {
        return ResponseEntity.ok(lostItemAlertService.getAlertsByStatus(status));
    }

    @GetMapping("/critical/new")
    public ResponseEntity<List<LostItemAlert>> getCriticalNewAlerts() {
        return ResponseEntity.ok(lostItemAlertService.getCriticalNewAlerts());
    }

    @GetMapping("/caregiver/{caregiverId}")
    public ResponseEntity<List<LostItemAlert>> getAlertsByCaregiverId(@PathVariable Long caregiverId) {
        return ResponseEntity.ok(lostItemAlertService.getAlertsByCaregiverId(caregiverId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LostItemAlert> updateAlert(
            @PathVariable Long id,
            @Valid @RequestBody LostItemAlert alert,
            @RequestHeader(value = "X-User-Id",   required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        authorizationService.checkAlertAccess(id, userId, userRole);
        return ResponseEntity.ok(lostItemAlertService.updateAlert(id, alert));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlert(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id",   required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        authorizationService.checkAlertAccess(id, userId, userRole);
        lostItemAlertService.deleteAlert(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/view")
    public ResponseEntity<LostItemAlert> markAsViewed(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id",   required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        authorizationService.checkAlertAccess(id, userId, userRole);
        return ResponseEntity.ok(lostItemAlertService.markAsViewed(id));
    }

    @PatchMapping("/{id}/resolve")
    public ResponseEntity<LostItemAlert> resolveAlert(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id",   required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        authorizationService.checkAlertAccess(id, userId, userRole);
        return ResponseEntity.ok(lostItemAlertService.resolveAlert(id));
    }

    @PatchMapping("/{id}/escalate")
    public ResponseEntity<LostItemAlert> escalateAlert(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id",   required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        authorizationService.checkAlertAccess(id, userId, userRole);
        return ResponseEntity.ok(lostItemAlertService.escalateAlert(id));
    }

    @PatchMapping("/lost-item/{lostItemId}/resolve-all")
    public ResponseEntity<Map<String, Object>> resolveAllByLostItem(
            @PathVariable Long lostItemId,
            @RequestHeader(value = "X-User-Id",   required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        authorizationService.checkItemIdAccess(lostItemId, userId, userRole);
        int resolved = lostItemAlertService.resolveAllByLostItem(lostItemId);
        return ResponseEntity.ok(Map.of("resolved", resolved, "lostItemId", lostItemId));
    }

    @PatchMapping("/patient/{patientId}/resolve-all")
    public ResponseEntity<Map<String, Object>> resolveAllByPatient(
            @PathVariable Long patientId,
            @RequestHeader(value = "X-User-Id",   required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        authorizationService.checkPatientAlertAccess(patientId, userId, userRole);
        int resolved = lostItemAlertService.resolveAllByPatient(patientId);
        return ResponseEntity.ok(Map.of("resolved", resolved, "patientId", patientId));
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(lostItemAlertService.getStatistics());
    }
}
