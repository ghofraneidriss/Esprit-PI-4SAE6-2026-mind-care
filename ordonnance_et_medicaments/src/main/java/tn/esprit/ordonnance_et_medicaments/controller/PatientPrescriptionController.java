package tn.esprit.ordonnance_et_medicaments.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.ordonnance_et_medicaments.entities.Prescription;
import tn.esprit.ordonnance_et_medicaments.service.PrescriptionService;

import java.util.List;

/**
 * Contrôleur destiné aux patients (PATIENT) pour consulter leurs prescriptions personnelles.
 */
@RestController
@RequestMapping("/api/patient/prescriptions")
@RequiredArgsConstructor
@CrossOrigin("*")
public class PatientPrescriptionController {

    private final PrescriptionService prescriptionService;

    /**
     * Accès en lecture seule à ses propres prescriptions uniquement (filtrées par son ID).
     */
    @GetMapping("/my-history/{patientId}")
    public ResponseEntity<List<Prescription>> getMyHistory(@PathVariable(name = "patientId") Long patientId) {
        return ResponseEntity.ok(prescriptionService.getHistoryByPatientId(patientId));
    }

    /**
     * Consulter le détail d'une prescription unitaire.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Prescription> getById(@PathVariable(name = "id") Long id) {
        return ResponseEntity.ok(prescriptionService.getPrescriptionById(id));
    }
}
