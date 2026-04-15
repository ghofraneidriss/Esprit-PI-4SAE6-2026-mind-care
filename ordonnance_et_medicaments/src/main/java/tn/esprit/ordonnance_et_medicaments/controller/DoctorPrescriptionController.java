package tn.esprit.ordonnance_et_medicaments.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.ordonnance_et_medicaments.dto.DrugSafetyAlertDTO;
import tn.esprit.ordonnance_et_medicaments.dto.DoctorShoppingAlertDTO;
import tn.esprit.ordonnance_et_medicaments.dto.OverlapConflictDTO;
import tn.esprit.ordonnance_et_medicaments.entities.Prescription;
import tn.esprit.ordonnance_et_medicaments.entities.Medicine;
import tn.esprit.ordonnance_et_medicaments.service.DrugSafetyService;
import tn.esprit.ordonnance_et_medicaments.service.DoctorShoppingDetectionService;
import tn.esprit.ordonnance_et_medicaments.service.PrescriptionService;
import tn.esprit.ordonnance_et_medicaments.service.MedicineService;
import tn.esprit.ordonnance_et_medicaments.service.PrescriptionOverlapService;

import java.time.LocalDate;
import java.util.List;

/**
 * Contrôleur destiné aux médecins (DOCTOR) pour la gestion des prescriptions.
 */
@RestController
@RequestMapping("/api/doctor/prescriptions")
@RequiredArgsConstructor
@CrossOrigin("*")
public class DoctorPrescriptionController {

    private final PrescriptionService prescriptionService;
    private final MedicineService medicineService;
    private final PrescriptionOverlapService overlapService;
    private final DrugSafetyService drugSafetyService;

    /** Service dédié à la détection du comportement de "doctor shopping" */
    private final DoctorShoppingDetectionService doctorShoppingService;

    // Création d'une prescription complète
    @PostMapping
    public ResponseEntity<Prescription> create(@RequestBody Prescription prescription) {
        return ResponseEntity.ok(prescriptionService.createPrescription(prescription));
    }

    // Sauvegarde en tant que BROUILLON (DRAFT)
    // Permet au médecin de continuer après l'ajout d'un nouveau médicament
    @PostMapping("/draft")
    public ResponseEntity<Prescription> createDraft(@RequestBody Prescription prescription) {
        return ResponseEntity.ok(prescriptionService.saveAsDraft(prescription));
    }

    // Peut consulter l'historique par patient (/patient/{id})
    @GetMapping("/patient/{id}")
    public ResponseEntity<List<Prescription>> getHistory(@PathVariable(name = "id") Long id) {
        return ResponseEntity.ok(prescriptionService.getHistoryByPatientId(id));
    }

    // Peut modifier une prescription
    @PutMapping("/{id}")
    public ResponseEntity<Prescription> update(@PathVariable(name = "id") Long id, @RequestBody Prescription prescription) {
        return ResponseEntity.ok(prescriptionService.updatePrescription(id, prescription));
    }

    // Peut supprimer une prescription
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable(name = "id") Long id) {
        prescriptionService.deletePrescription(id);
        return ResponseEntity.noContent().build();
    }

    // Consulter le détail d'une prescription unitaire
    @GetMapping("/{id}")
    public ResponseEntity<Prescription> getById(@PathVariable(name = "id") Long id) {
        return ResponseEntity.ok(prescriptionService.getPrescriptionById(id));
    }

    // Peut rechercher des médicaments pour la prescription
    @GetMapping("/search-medicines")
    public ResponseEntity<List<Medicine>> searchMedicines(@RequestParam(name = "query") String query) {
        return ResponseEntity.ok(medicineService.searchMedicines(query));
    }

    /**
     * JPQL — Detects simple date-overlap for the exact same medicine.
     * Kept for backward compatibility.
     *
     * GET /api/doctor/prescriptions/check-overlap
     *   ?patientId=5&medicineId=12&startDate=2024-01-01&endDate=2024-02-01&currentPrescriptionId=0
     *
     * @return List of overlap conflicts (empty = no conflict)
     */
    @GetMapping("/check-overlap")
    public ResponseEntity<List<OverlapConflictDTO>> checkOverlap(
            @RequestParam(name = "patientId") Long patientId,
            @RequestParam(name = "medicineId") Long medicineId,
            @RequestParam(name = "startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "currentPrescriptionId", defaultValue = "0") Long currentPrescriptionId) {

        List<OverlapConflictDTO> conflicts = overlapService.detectOverlaps(
                patientId, medicineId, startDate, endDate, currentPrescriptionId);

        return ResponseEntity.ok(conflicts);
    }

    /**
     * JPQL — Comprehensive drug safety check before prescribing a new medicine.
     *
     * Runs four parallel checks and returns a consolidated list of alerts:
     *  - SAME_MEDICINE   : Exact same medicine already active for this patient.
     *  - SAME_INN        : Different brand, same active ingredient (INN) already active.
     *  - SAME_FAMILY     : Same therapeutic family already prescribed (overdose risk).
     *  - CONTRAINDICATION: The new medicine has a registered contraindication.
     *
     * GET /api/doctor/prescriptions/check-drug-safety
     *   ?patientId=5
     *   &medicineId=12
     *   &startDate=2024-01-01
     *   &endDate=2024-02-01
     *   &currentPrescriptionId=0   (0 = brand new prescription)
     *
     * Response:
     *  - 200 OK with empty array  → No conflicts detected, safe to prescribe.
     *  - 200 OK with alert list   → One or more conflicts — doctor must review.
     *
     * @return List of {@link DrugSafetyAlertDTO} describing each conflict found.
     */
    @GetMapping("/check-drug-safety")
    public ResponseEntity<List<DrugSafetyAlertDTO>> checkDrugSafety(
            @RequestParam(name = "patientId") Long patientId,
            @RequestParam(name = "medicineId") Long medicineId,
            @RequestParam(name = "startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "currentPrescriptionId", defaultValue = "0") Long currentPrescriptionId) {

        List<DrugSafetyAlertDTO> alerts = drugSafetyService.checkDrugSafety(
                patientId, medicineId, startDate, endDate, currentPrescriptionId);

        return ResponseEntity.ok(alerts);
    }

    /**
     * JPQL — Détecte le comportement de "Doctor Shopping".
     *
     * Vérifie si ce patient possède déjà une prescription ACTIVE (non expirée)
     * pour le même médicament, prescrite par un médecin DIFFÉRENT.
     * Si la prescription est terminée (endDate < today), aucune alerte n'est levée.
     *
     * GET /api/doctor/prescriptions/check-doctor-shopping
     *   ?patientId=5
     *   &medicineId=12
     *   &currentDoctorId=3
     *
     * Response:
     *  - 200 OK avec tableau vide  → Aucun comportement suspect détecté.
     *  - 200 OK avec liste d'alertes → Le patient a déjà une prescription active
     *                                   du même médicament chez un autre médecin.
     *
     * @return Liste de {@link DoctorShoppingAlertDTO} (vide = aucune alerte).
     */
    @GetMapping("/check-doctor-shopping")
    public ResponseEntity<List<DoctorShoppingAlertDTO>> checkDoctorShopping(
            @RequestParam(name = "patientId")       Long patientId,
            @RequestParam(name = "medicineId")      Long medicineId,
            @RequestParam(name = "currentDoctorId") Long currentDoctorId) {

        List<DoctorShoppingAlertDTO> alerts = doctorShoppingService
                .detectDoctorShopping(patientId, medicineId, currentDoctorId);

        return ResponseEntity.ok(alerts);
    }
}
