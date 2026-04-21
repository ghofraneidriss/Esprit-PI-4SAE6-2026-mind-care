package tn.esprit.movement_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.movement_service.dto.PatientLocationStatusDto;
import tn.esprit.movement_service.dto.PatientTrackingDashboardDto;
import tn.esprit.movement_service.service.PatientTrackingFacadeService;

/**
 * API métier avancée : agrège localisation (zones) + mouvement (GPS, alertes).
 * <p>
 * Exemple : {@code GET /api/tracking/patient/42/dashboard}
 */
@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
public class PatientTrackingController {

    private final PatientTrackingFacadeService patientTrackingFacadeService;

    /**
     * Vue complète pour un patient : zones, dernière position, conformité, alertes récentes.
     */
    @GetMapping("/patient/{patientId}/dashboard")
    public ResponseEntity<PatientTrackingDashboardDto> dashboard(@PathVariable Long patientId) {
        return ResponseEntity.ok(patientTrackingFacadeService.getDashboard(patientId));
    }

    /**
     * Synthèse légère : dans une zone oui/non, fraîcheur du GPS.
     */
    @GetMapping("/patient/{patientId}/status")
    public ResponseEntity<PatientLocationStatusDto> status(@PathVariable Long patientId) {
        return ResponseEntity.ok(patientTrackingFacadeService.getLocationStatus(patientId));
    }
}
