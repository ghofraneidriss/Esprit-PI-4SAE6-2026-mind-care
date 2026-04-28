package tn.esprit.lost_item_service.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.lost_item_service.dto.CreateLostItemAlertRequest;
import tn.esprit.lost_item_service.dto.DTOMapper;
import tn.esprit.lost_item_service.dto.LostItemAlertDTO;
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
    public ResponseEntity<LostItemAlertDTO> createAlert(@Valid @RequestBody CreateLostItemAlertRequest request) {
        LostItemAlert alert = DTOMapper.toLostItemAlert(request);
        LostItemAlert created = lostItemAlertService.createAlert(alert);
        return new ResponseEntity<>(DTOMapper.toLostItemAlertDTO(created), HttpStatus.CREATED);
    }

    /**
     * List all alerts — scoped by role via headers.
     * ADMIN/DOCTOR: all alerts.
     * CAREGIVER: only their alerts.
     * PATIENT: only their alerts.
     */
    @GetMapping
    public ResponseEntity<List<LostItemAlertDTO>> getAllAlerts(
            @RequestHeader(value = "X-User-Id",   required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        String role = userRole != null ? userRole.toUpperCase() : "ADMIN";

        if ("CAREGIVER".equals(role) && userId != null) {
            return ResponseEntity.ok(DTOMapper.toLostItemAlertDTOList(lostItemAlertService.getAlertsByCaregiverId(userId)));
        }
        if ("PATIENT".equals(role) && userId != null) {
            return ResponseEntity.ok(DTOMapper.toLostItemAlertDTOList(lostItemAlertService.getAlertsByPatientId(userId)));
        }
        return ResponseEntity.ok(DTOMapper.toLostItemAlertDTOList(lostItemAlertService.getAllAlerts()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LostItemAlertDTO> getAlertById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id",   required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        LostItemAlert alert = authorizationService.checkAlertAccess(id, userId, userRole);
        return ResponseEntity.ok(DTOMapper.toLostItemAlertDTO(alert));
    }

    @GetMapping("/lost-item/{lostItemId}")
    public ResponseEntity<List<LostItemAlertDTO>> getAlertsByLostItemId(
            @PathVariable Long lostItemId,
            @RequestHeader(value = "X-User-Id",   required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        authorizationService.checkItemIdAccess(lostItemId, userId, userRole);
        return ResponseEntity.ok(DTOMapper.toLostItemAlertDTOList(lostItemAlertService.getAlertsByLostItemId(lostItemId)));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<LostItemAlertDTO>> getAlertsByPatientId(
            @PathVariable Long patientId,
            @RequestHeader(value = "X-User-Id",   required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        authorizationService.checkPatientAlertAccess(patientId, userId, userRole);
        return ResponseEntity.ok(DTOMapper.toLostItemAlertDTOList(lostItemAlertService.getAlertsByPatientId(patientId)));
    }

    @GetMapping("/level/{level}")
    public ResponseEntity<List<LostItemAlertDTO>> getAlertsByLevel(@PathVariable AlertLevel level) {
        return ResponseEntity.ok(DTOMapper.toLostItemAlertDTOList(lostItemAlertService.getAlertsByLevel(level)));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<LostItemAlertDTO>> getAlertsByStatus(@PathVariable AlertStatus status) {
        return ResponseEntity.ok(DTOMapper.toLostItemAlertDTOList(lostItemAlertService.getAlertsByStatus(status)));
    }

    @GetMapping("/critical/new")
    public ResponseEntity<List<LostItemAlertDTO>> getCriticalNewAlerts() {
        return ResponseEntity.ok(DTOMapper.toLostItemAlertDTOList(lostItemAlertService.getCriticalNewAlerts()));
    }

    @GetMapping("/caregiver/{caregiverId}")
    public ResponseEntity<List<LostItemAlertDTO>> getAlertsByCaregiverId(@PathVariable Long caregiverId) {
        return ResponseEntity.ok(DTOMapper.toLostItemAlertDTOList(lostItemAlertService.getAlertsByCaregiverId(caregiverId)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LostItemAlertDTO> updateAlert(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLostItemAlertRequest request,
            @RequestHeader(value = "X-User-Id",   required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        authorizationService.checkAlertAccess(id, userId, userRole);
        LostItemAlert alert = DTOMapper.toLostItemAlert(request);
        LostItemAlert updated = lostItemAlertService.updateAlert(id, alert);
        return ResponseEntity.ok(DTOMapper.toLostItemAlertDTO(updated));
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
