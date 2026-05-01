package tn.esprit.followup_alert_service.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.followup_alert_service.dto.FollowUpRequestDTO;
import tn.esprit.followup_alert_service.dto.FollowUpResponseDTO;
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
    public ResponseEntity<FollowUpResponseDTO> createFollowUp(@Valid @RequestBody FollowUpRequestDTO requestDTO) {
        FollowUp followUp = new FollowUp();
        followUp.setPatientId(requestDTO.getPatientId());
        followUp.setCaregiverId(requestDTO.getCaregiverId());
        followUp.setFollowUpDate(requestDTO.getFollowUpDate());
        followUp.setCognitiveScore(requestDTO.getCognitiveScore());
        followUp.setMood(requestDTO.getMood());
        followUp.setAgitationObserved(requestDTO.getAgitationObserved());
        followUp.setConfusionObserved(requestDTO.getConfusionObserved());
        followUp.setEating(requestDTO.getEating());
        followUp.setDressing(requestDTO.getDressing());
        followUp.setMobility(requestDTO.getMobility());
        followUp.setHoursSlept(requestDTO.getHoursSlept());
        followUp.setSleepQuality(requestDTO.getSleepQuality());
        followUp.setNotes(requestDTO.getNotes());
        followUp.setVitalSigns(requestDTO.getVitalSigns());
        FollowUp created = followUpService.createFollowUp(followUp);
        return new ResponseEntity<>(FollowUpResponseDTO.fromEntity(created), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<FollowUpResponseDTO>> getAllFollowUps() {
        return ResponseEntity.ok(followUpService.getAllFollowUps().stream()
                .map(FollowUpResponseDTO::fromEntity)
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FollowUpResponseDTO> getFollowUpById(@PathVariable Long id) {
        return ResponseEntity.ok(FollowUpResponseDTO.fromEntity(followUpService.getFollowUpById(id)));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<FollowUpResponseDTO>> getFollowUpsByPatientId(@PathVariable Long patientId) {
        return ResponseEntity.ok(followUpService.getFollowUpsByPatientId(patientId).stream()
                .map(FollowUpResponseDTO::fromEntity)
                .toList());
    }

    @GetMapping("/caregiver/{caregiverId}")
    public ResponseEntity<List<FollowUpResponseDTO>> getFollowUpsByCaregiverId(@PathVariable Long caregiverId) {
        return ResponseEntity.ok(followUpService.getFollowUpsByCaregiverId(caregiverId).stream()
                .map(FollowUpResponseDTO::fromEntity)
                .toList());
    }

    @PutMapping("/{id}")
    public ResponseEntity<FollowUpResponseDTO> updateFollowUp(@PathVariable Long id, @Valid @RequestBody FollowUpRequestDTO requestDTO) {
        FollowUp followUp = new FollowUp();
        followUp.setPatientId(requestDTO.getPatientId());
        followUp.setCaregiverId(requestDTO.getCaregiverId());
        followUp.setFollowUpDate(requestDTO.getFollowUpDate());
        followUp.setCognitiveScore(requestDTO.getCognitiveScore());
        followUp.setMood(requestDTO.getMood());
        followUp.setAgitationObserved(requestDTO.getAgitationObserved());
        followUp.setConfusionObserved(requestDTO.getConfusionObserved());
        followUp.setEating(requestDTO.getEating());
        followUp.setDressing(requestDTO.getDressing());
        followUp.setMobility(requestDTO.getMobility());
        followUp.setHoursSlept(requestDTO.getHoursSlept());
        followUp.setSleepQuality(requestDTO.getSleepQuality());
        followUp.setNotes(requestDTO.getNotes());
        followUp.setVitalSigns(requestDTO.getVitalSigns());
        return ResponseEntity.ok(FollowUpResponseDTO.fromEntity(followUpService.updateFollowUp(id, followUp)));
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