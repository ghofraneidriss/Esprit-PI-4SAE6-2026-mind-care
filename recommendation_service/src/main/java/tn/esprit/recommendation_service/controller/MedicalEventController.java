package tn.esprit.recommendation_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.recommendation_service.dto.medicalevent.JoinMedicalEventRequest;
import tn.esprit.recommendation_service.dto.medicalevent.MedicalEventCreateRequest;
import tn.esprit.recommendation_service.dto.medicalevent.MedicalEventParticipationResponse;
import tn.esprit.recommendation_service.dto.medicalevent.MedicalEventResponse;
import tn.esprit.recommendation_service.dto.medicalevent.MedicalEventUpdateRequest;
import tn.esprit.recommendation_service.dto.medicalevent.ParticipantRankingResponse;
import tn.esprit.recommendation_service.dto.medicalevent.ScoreResponse;
import tn.esprit.recommendation_service.dto.medicalevent.StreakResponse;
import tn.esprit.recommendation_service.enums.MedicalEventType;
import tn.esprit.recommendation_service.enums.ParticipantType;
import tn.esprit.recommendation_service.service.MedicalEventService;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MedicalEventController {

    private final MedicalEventService medicalEventService;

    @PostMapping
    public ResponseEntity<MedicalEventResponse> create(@Valid @RequestBody MedicalEventCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(medicalEventService.createMedicalEvent(request));
    }

    @GetMapping
    public ResponseEntity<List<MedicalEventResponse>> getAll() {
        return ResponseEntity.ok(medicalEventService.getAllMedicalEvents());
    }

    @GetMapping("/search")
    public ResponseEntity<List<MedicalEventResponse>> search(@RequestParam String query) {
        return ResponseEntity.ok(medicalEventService.searchMedicalEvents(query));
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<MedicalEventResponse> getById(@PathVariable Long eventId) {
        return ResponseEntity.ok(medicalEventService.getMedicalEventById(eventId));
    }

    @GetMapping("/patient/{patientId}/active")
    public ResponseEntity<List<MedicalEventResponse>> getActiveEventsByPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(medicalEventService.getActiveMedicalEventsByPatient(patientId));
    }

    @GetMapping("/patient/{patientId}/completed")
    public ResponseEntity<List<MedicalEventResponse>> getCompletedEventsByPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(medicalEventService.getCompletedMedicalEvents(patientId));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<MedicalEventResponse>> getByType(@PathVariable MedicalEventType type) {
        return ResponseEntity.ok(medicalEventService.getMedicalEventsByType(type));
    }

    @PutMapping("/{eventId}")
    public ResponseEntity<MedicalEventResponse> update(
            @PathVariable Long eventId,
            @Valid @RequestBody MedicalEventUpdateRequest request
    ) {
        return ResponseEntity.ok(medicalEventService.updateMedicalEvent(eventId, request));
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> delete(@PathVariable Long eventId) {
        medicalEventService.deleteMedicalEvent(eventId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{eventId}/join")
    public ResponseEntity<MedicalEventParticipationResponse> join(
            @PathVariable Long eventId,
            @Valid @RequestBody JoinMedicalEventRequest request
    ) {
        return ResponseEntity.ok(medicalEventService.joinMedicalEvent(eventId, request));
    }

    @GetMapping("/{eventId}/streak")
    public ResponseEntity<StreakResponse> getStreak(
            @PathVariable Long eventId,
            @RequestParam Long participantId,
            @RequestParam ParticipantType participantType
    ) {
        return ResponseEntity.ok(medicalEventService.calculateStreak(eventId, participantId, participantType));
    }

    @GetMapping("/{eventId}/score")
    public ResponseEntity<ScoreResponse> getScore(
            @PathVariable Long eventId,
            @RequestParam Long participantId,
            @RequestParam ParticipantType participantType
    ) {
        return ResponseEntity.ok(medicalEventService.calculateTotalScore(eventId, participantId, participantType));
    }

    @GetMapping("/{eventId}/ranking")
    public ResponseEntity<List<ParticipantRankingResponse>> getRanking(@PathVariable Long eventId) {
        return ResponseEntity.ok(medicalEventService.getParticipantRanking(eventId));
    }

    @GetMapping("/{eventId}/joined")
    public ResponseEntity<Boolean> hasJoined(
            @PathVariable Long eventId,
            @RequestParam Long participantId,
            @RequestParam ParticipantType participantType
    ) {
        return ResponseEntity.ok(medicalEventService.hasUserJoined(eventId, participantId, participantType));
    }

    @PostMapping("/complete-expired")
    public ResponseEntity<Integer> completeExpired() {
        return ResponseEntity.ok(medicalEventService.completeExpiredMedicalEvents());
    }
}
