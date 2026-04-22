package tn.esprit.recommendation_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.recommendation_service.dto.stats.PatientStatsResponse;
import tn.esprit.recommendation_service.service.PatientStatsService;

@RestController
@RequestMapping("/api/patient-stats")
@RequiredArgsConstructor
public class PatientStatsController {

    private final PatientStatsService patientStatsService;

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<PatientStatsResponse> getByPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(patientStatsService.getByPatient(patientId));
    }
}

