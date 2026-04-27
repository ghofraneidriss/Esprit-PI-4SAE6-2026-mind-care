package tn.esprit.recommendation_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.recommendation_service.dto.recommendation.AutoRecommendationGenerateRequest;
import tn.esprit.recommendation_service.dto.recommendation.ClinicalEscalationAlertResponse;
import tn.esprit.recommendation_service.dto.recommendation.RecommendationCreateRequest;
import tn.esprit.recommendation_service.dto.recommendation.RecommendationResponse;
import tn.esprit.recommendation_service.dto.recommendation.RecommendationStatsResponse;
import tn.esprit.recommendation_service.dto.recommendation.RecommendationStatusUpdateRequest;
import tn.esprit.recommendation_service.dto.recommendation.RecommendationUpdateRequest;
import tn.esprit.recommendation_service.service.RecommendationService;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @PostMapping
    public ResponseEntity<RecommendationResponse> create(@Valid @RequestBody RecommendationCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recommendationService.createRecommendation(request));
    }

    @GetMapping
    public ResponseEntity<List<RecommendationResponse>> getAll() {
        return ResponseEntity.ok(recommendationService.getAllRecommendations());
    }

    @GetMapping("/search")
    public ResponseEntity<List<RecommendationResponse>> search(@RequestParam String query) {
        return ResponseEntity.ok(recommendationService.searchRecommendations(query));
    }

    @GetMapping("/{recommendationId}")
    public ResponseEntity<RecommendationResponse> getById(@PathVariable Long recommendationId) {
        return ResponseEntity.ok(recommendationService.getRecommendationById(recommendationId));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<RecommendationResponse>> getByPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(recommendationService.getRecommendationsByPatient(patientId));
    }

    @PostMapping("/auto-generate")
    public ResponseEntity<List<RecommendationResponse>> autoGenerate(
            @Valid @RequestBody AutoRecommendationGenerateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recommendationService.generateAutomaticRecommendations(request));
    }

    @GetMapping("/patient/{patientId}/active")
    public ResponseEntity<List<RecommendationResponse>> getActiveByPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(recommendationService.getActiveRecommendationsByPatient(patientId));
    }

    @GetMapping("/patient/{patientId}/sorted")
    public ResponseEntity<List<RecommendationResponse>> getSortedByPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(recommendationService.getRecommendationsSortedByPriorityAndCreatedAt(patientId));
    }

    @GetMapping("/patient/{patientId}/stats")
    public ResponseEntity<RecommendationStatsResponse> getStatsByPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(recommendationService.countAcceptedVsRejectedByPatient(patientId));
    }

    @GetMapping("/doctor/{doctorId}/alerts")
    public ResponseEntity<List<ClinicalEscalationAlertResponse>> getDoctorAlerts(@PathVariable Long doctorId) {
        return ResponseEntity.ok(recommendationService.getAlertsByDoctor(doctorId));
    }

    @PatchMapping("/alerts/{alertId}/resolve")
    public ResponseEntity<ClinicalEscalationAlertResponse> resolveAlert(@PathVariable Long alertId) {
        return ResponseEntity.ok(recommendationService.resolveAlert(alertId));
    }

    @PutMapping("/{recommendationId}")
    public ResponseEntity<RecommendationResponse> updateStatus(
            @PathVariable Long recommendationId,
            @Valid @RequestBody RecommendationStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(recommendationService.updateRecommendationStatus(recommendationId, request));
    }

    @PutMapping("/{recommendationId}/details")
    public ResponseEntity<RecommendationResponse> updateDetails(
            @PathVariable Long recommendationId,
            @Valid @RequestBody RecommendationUpdateRequest request
    ) {
        return ResponseEntity.ok(recommendationService.updateRecommendationDetails(recommendationId, request));
    }

    @PatchMapping("/{recommendationId}/status")
    public ResponseEntity<RecommendationResponse> patchStatus(
            @PathVariable Long recommendationId,
            @Valid @RequestBody RecommendationStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(recommendationService.updateRecommendationStatus(recommendationId, request));
    }

    @PatchMapping("/{recommendationId}/approve")
    public ResponseEntity<RecommendationResponse> approve(@PathVariable Long recommendationId) {
        return ResponseEntity.ok(recommendationService.acceptRecommendation(recommendationId));
    }

    @PostMapping("/{recommendationId}/accept")
    public ResponseEntity<RecommendationResponse> accept(@PathVariable Long recommendationId) {
        return ResponseEntity.ok(recommendationService.acceptRecommendation(recommendationId));
    }

    @PostMapping("/{recommendationId}/dismiss")
    public ResponseEntity<RecommendationResponse> dismiss(@PathVariable Long recommendationId) {
        return ResponseEntity.ok(recommendationService.dismissRecommendation(recommendationId));
    }

    @PostMapping("/archive-expired")
    public ResponseEntity<Integer> archiveExpired() {
        return ResponseEntity.ok(recommendationService.archiveExpiredRecommendations());
    }

    @DeleteMapping("/{recommendationId}")
    public ResponseEntity<Void> delete(@PathVariable Long recommendationId) {
        recommendationService.deleteRecommendation(recommendationId);
        return ResponseEntity.noContent().build();
    }
}
