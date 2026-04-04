package tn.esprit.lost_item_service.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.lost_item_service.Entity.AlertLevel;
import tn.esprit.lost_item_service.Entity.AlertStatus;
import tn.esprit.lost_item_service.Entity.ItemAlert;
import tn.esprit.lost_item_service.Service.ItemAlertService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/item-alerts")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class ItemAlertController {

    private final ItemAlertService itemAlertService;

    @PostMapping
    public ResponseEntity<ItemAlert> createAlert(@Valid @RequestBody ItemAlert alert) {
        return new ResponseEntity<>(itemAlertService.createAlert(alert), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ItemAlert>> getAllAlerts() {
        return ResponseEntity.ok(itemAlertService.getAllAlerts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemAlert> getAlertById(@PathVariable Long id) {
        return ResponseEntity.ok(itemAlertService.getAlertById(id));
    }

    @GetMapping("/lost-item/{lostItemId}")
    public ResponseEntity<List<ItemAlert>> getAlertsByLostItemId(@PathVariable Long lostItemId) {
        return ResponseEntity.ok(itemAlertService.getAlertsByLostItemId(lostItemId));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<ItemAlert>> getAlertsByPatientId(@PathVariable Long patientId) {
        return ResponseEntity.ok(itemAlertService.getAlertsByPatientId(patientId));
    }

    @GetMapping("/level/{level}")
    public ResponseEntity<List<ItemAlert>> getAlertsByLevel(@PathVariable AlertLevel level) {
        return ResponseEntity.ok(itemAlertService.getAlertsByLevel(level));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<ItemAlert>> getAlertsByStatus(@PathVariable AlertStatus status) {
        return ResponseEntity.ok(itemAlertService.getAlertsByStatus(status));
    }

    @GetMapping("/critical/new")
    public ResponseEntity<List<ItemAlert>> getCriticalNewAlerts() {
        return ResponseEntity.ok(itemAlertService.getCriticalNewAlerts());
    }

    @GetMapping("/caregiver/{caregiverId}")
    public ResponseEntity<List<ItemAlert>> getAlertsByCaregiverId(@PathVariable Long caregiverId) {
        return ResponseEntity.ok(itemAlertService.getAlertsByCaregiverId(caregiverId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ItemAlert> updateAlert(@PathVariable Long id, @Valid @RequestBody ItemAlert alert) {
        return ResponseEntity.ok(itemAlertService.updateAlert(id, alert));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlert(@PathVariable Long id) {
        itemAlertService.deleteAlert(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/view")
    public ResponseEntity<ItemAlert> markAsViewed(@PathVariable Long id) {
        return ResponseEntity.ok(itemAlertService.markAsViewed(id));
    }

    @PatchMapping("/{id}/resolve")
    public ResponseEntity<ItemAlert> resolveAlert(@PathVariable Long id) {
        return ResponseEntity.ok(itemAlertService.resolveAlert(id));
    }

    @PatchMapping("/{id}/escalate")
    public ResponseEntity<ItemAlert> escalateAlert(@PathVariable Long id) {
        return ResponseEntity.ok(itemAlertService.escalateAlert(id));
    }

    @PatchMapping("/lost-item/{lostItemId}/resolve-all")
    public ResponseEntity<Map<String, Object>> resolveAllByLostItem(@PathVariable Long lostItemId) {
        int resolved = itemAlertService.resolveAllByLostItem(lostItemId);
        return ResponseEntity.ok(Map.of("resolved", resolved, "lostItemId", lostItemId));
    }

    @PatchMapping("/patient/{patientId}/resolve-all")
    public ResponseEntity<Map<String, Object>> resolveAllByPatient(@PathVariable Long patientId) {
        int resolved = itemAlertService.resolveAllByPatient(patientId);
        return ResponseEntity.ok(Map.of("resolved", resolved, "patientId", patientId));
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(itemAlertService.getStatistics());
    }
}
