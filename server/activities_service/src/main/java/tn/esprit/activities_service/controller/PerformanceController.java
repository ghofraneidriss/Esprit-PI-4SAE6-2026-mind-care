package tn.esprit.activities_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.activities_service.service.PerformanceAnalysisService;

import java.util.List;

@RestController
@RequestMapping("/api/performance")
@Tag(name = "Performance Analysis", description = "API pour l'analyse de performance cognitive par thème")
public class PerformanceController {

    @Autowired
    private PerformanceAnalysisService performanceService;

    @Operation(summary = "Analyse de performance d'un patient",
               description = "Retourne la performance par thème cognitif avec recommandations personnalisées")
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<PerformanceAnalysisService.PatientPerformance> getPatientPerformance(
            @Parameter(description = "ID du patient") @PathVariable("patientId") Long patientId) {
        return ResponseEntity.ok(performanceService.analyzePatient(patientId));
    }

    @Operation(summary = "Analyse de performance de tous les patients",
               description = "Retourne la performance de tous les patients (vue admin/docteur)")
    @GetMapping("/all")
    public ResponseEntity<List<PerformanceAnalysisService.PatientPerformance>> getAllPerformances() {
        return ResponseEntity.ok(performanceService.analyzeAllPatients());
    }
}
