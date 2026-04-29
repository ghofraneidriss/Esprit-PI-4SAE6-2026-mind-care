package tn.esprit.lost_item_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.lost_item_service.service.PatientIntelligenceService;

import java.util.Map;

@RestController
@RequestMapping("/api/lost-items")
@RequiredArgsConstructor
public class PatientIntelligenceController {

    private final PatientIntelligenceService intelligenceService;

    /**
     * GET /api/lost-items/patients/{patientId}/intelligence
     *
     * Runs the full behavioral intelligence analysis for a patient.
     * Collects 90 days of lost-item data, computes risk/trend statistics,
     * and calls the Groq LLM (via Spring AI) for a clinical narrative.
     */
    @GetMapping("/patients/{patientId}/intelligence")
    public ResponseEntity<Map<String, Object>> getPatientIntelligence(
            @PathVariable Long patientId) {
        return ResponseEntity.ok(intelligenceService.analyzePatient(patientId));
    }
}
