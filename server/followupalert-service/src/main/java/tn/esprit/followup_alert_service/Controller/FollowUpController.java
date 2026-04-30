package tn.esprit.followup_alert_service.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.followup_alert_service.Entity.FollowUp;
import tn.esprit.followup_alert_service.Service.FollowUpService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/followups")
@RequiredArgsConstructor
public class FollowUpController {

    private final FollowUpService followUpService;

    // ==================== EXISTING CRUD ====================

    @PostMapping
    public ResponseEntity<FollowUp> createFollowUp(@Valid @RequestBody FollowUp followUp) {
        FollowUp created = followUpService.createFollowUp(followUp);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<FollowUp>> getAllFollowUps() {
        return ResponseEntity.ok(followUpService.getAllFollowUps());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FollowUp> getFollowUpById(@PathVariable Long id) {
        return ResponseEntity.ok(followUpService.getFollowUpById(id));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<FollowUp>> getFollowUpsByPatientId(@PathVariable Long patientId) {
        return ResponseEntity.ok(followUpService.getFollowUpsByPatientId(patientId));
    }

    @GetMapping("/caregiver/{caregiverId}")
    public ResponseEntity<List<FollowUp>> getFollowUpsByCaregiverId(@PathVariable Long caregiverId) {
        return ResponseEntity.ok(followUpService.getFollowUpsByCaregiverId(caregiverId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FollowUp> updateFollowUp(@PathVariable Long id, @Valid @RequestBody FollowUp followUp) {
        return ResponseEntity.ok(followUpService.updateFollowUp(id, followUp));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFollowUp(@PathVariable Long id) {
        followUpService.deleteFollowUp(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== FONCTIONNALITES AVANCEES ====================

    /** Detect if a patient has a cognitive decline trend */
    @GetMapping("/patient/{patientId}/cognitive-decline")
    public ResponseEntity<Map<String, Object>> detectCognitiveDecline(@PathVariable Long patientId) {
        boolean declining = followUpService.detectCognitiveDecline(patientId);
        return ResponseEntity.ok(Map.of("patientId", patientId, "cognitiveDecline", declining));
    }

    /** Calculate a patient's overall risk score (0-100) with risk factors */
    @GetMapping("/patient/{patientId}/risk")
    public ResponseEntity<Map<String, Object>> getPatientRisk(@PathVariable Long patientId) {
        return ResponseEntity.ok(followUpService.calculatePatientRisk(patientId));
    }

    /** Global follow-up statistics dashboard */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(followUpService.getStatistics());
    }

    /** Per-patient follow-up statistics */
    @GetMapping("/statistics/patient/{patientId}")
    public ResponseEntity<Map<String, Object>> getStatisticsByPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(followUpService.getStatisticsByPatient(patientId));
    }
}