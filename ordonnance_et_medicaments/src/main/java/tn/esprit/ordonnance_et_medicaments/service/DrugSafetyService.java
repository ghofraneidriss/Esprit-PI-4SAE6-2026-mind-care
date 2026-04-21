package tn.esprit.ordonnance_et_medicaments.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.ordonnance_et_medicaments.dto.DrugSafetyAlertDTO;
import tn.esprit.ordonnance_et_medicaments.dto.DrugSafetyAlertDTO.AlertType;
import tn.esprit.ordonnance_et_medicaments.entities.Medicine;
import tn.esprit.ordonnance_et_medicaments.entities.PrescriptionLine;
import tn.esprit.ordonnance_et_medicaments.repository.MedicineRepository;
import tn.esprit.ordonnance_et_medicaments.repository.PrescriptionLineRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Drug Safety Service — pre-prescription conflict detection.
 *
 * Before a doctor finalizes a new prescription line, this service runs
 * a series of JPQL-based checks to detect potential drug conflicts:
 *
 *  1. SAME_MEDICINE   – Exact same medicine already active for this patient.
 *  2. SAME_INN        – Different brand but identical active ingredient (INN).
 *  3. SAME_FAMILY     – Same therapeutic family already prescribed (overdose risk).
 *  4. CONTRAINDICATION – The new medicine has a registered contraindication.
 *
 * The doctor receives a structured list of alerts and can choose to
 * acknowledge and proceed, or cancel the prescription.
 */
@Service
@RequiredArgsConstructor
public class DrugSafetyService {

    private final PrescriptionLineRepository prescriptionLineRepository;
    private final MedicineRepository medicineRepository;

    /**
     * Main entry point — runs all four drug safety checks for a given patient/medicine/dates.
     *
     * @param patientId             ID of the patient receiving the prescription.
     * @param medicineId            ID of the medicine the doctor wants to prescribe.
     * @param startDate             Planned start date of the new prescription line.
     * @param endDate               Planned end date of the new prescription line.
     * @param currentPrescriptionId ID of the prescription being created (0 if brand new).
     * @return List of {@link DrugSafetyAlertDTO} — empty list means no conflicts.
     */
    public List<DrugSafetyAlertDTO> checkDrugSafety(Long patientId,
                                                      Long medicineId,
                                                      LocalDate startDate,
                                                      LocalDate endDate,
                                                      Long currentPrescriptionId) {

        List<DrugSafetyAlertDTO> alerts = new ArrayList<>();

        // Resolve the "exclude self" sentinel value:
        // If no existing prescription ID is provided (new prescription), use Long.MAX_VALUE
        // so the JPQL <> clause never accidentally matches any real prescription.
        Long excludePrescriptionId = (currentPrescriptionId != null && currentPrescriptionId > 0)
                ? currentPrescriptionId
                : Long.MAX_VALUE;

        // Fetch the medicine being prescribed — needed to run checks 2, 3, and 4
        Optional<Medicine> medicineOpt = medicineRepository.findById(medicineId);
        if (medicineOpt.isEmpty()) {
            // If the medicine ID is unknown, we cannot check further — return no alerts
            return alerts;
        }
        Medicine newMedicine = medicineOpt.get();

        // ── Check 1: Exact same medicine already active ───────────────────────
        runSameMedicineCheck(alerts, patientId, medicineId, startDate, endDate, excludePrescriptionId);

        // ── Check 2: Same INN (active ingredient) under a different brand ─────
        runSameInnCheck(alerts, patientId, newMedicine, startDate, endDate, excludePrescriptionId);

        // ── Check 3: Same therapeutic family (pharmacological class) ──────────
        runSameFamilyCheck(alerts, patientId, newMedicine, startDate, endDate, excludePrescriptionId);

        // ── Check 4: Contraindication flag on the new medicine ────────────────
        runContraindicationCheck(alerts, newMedicine);

        return alerts;
    }

    // ── Private check methods ─────────────────────────────────────────────────

    /**
     * CHECK 1 — SAME_MEDICINE
     *
     * Detects whether the exact same medicine (by ID) is already prescribed
     * and active for this patient during the requested date range.
     *
     * JPQL: findOverlappingMedicinePrescriptions
     */
    private void runSameMedicineCheck(List<DrugSafetyAlertDTO> alerts,
                                       Long patientId,
                                       Long medicineId,
                                       LocalDate startDate,
                                       LocalDate endDate,
                                       Long excludePrescriptionId) {

        List<PrescriptionLine> conflicts = prescriptionLineRepository
                .findOverlappingMedicinePrescriptions(patientId, medicineId, startDate, endDate, excludePrescriptionId);

        for (PrescriptionLine pl : conflicts) {
            alerts.add(new DrugSafetyAlertDTO(
                    AlertType.SAME_MEDICINE,
                    "⚠️ OVERDOSE RISK — This exact medicine is already active in another prescription.",
                    pl.getPrescription().getId(),
                    pl.getPrescription().getStatus(),
                    pl.getMedicine().getCommercialName(),
                    pl.getMedicine().getInn(),
                    pl.getMedicine().getTherapeuticFamily(),
                    pl.getStartDate(),
                    pl.getEndDate(),
                    pl.getDosage(),
                    null // No contraindication detail for this alert type
            ));
        }
    }

    /**
     * CHECK 2 — SAME_INN
     *
     * Detects whether a different brand containing the same active ingredient
     * (INN) is already active for this patient during the requested date range.
     *
     * Example: Prescribing "Doliprane" when the patient already takes "Efferalgan"
     *          (both contain Paracetamol / same INN).
     *
     * JPQL: findOverlappingByInn
     */
    private void runSameInnCheck(List<DrugSafetyAlertDTO> alerts,
                                  Long patientId,
                                  Medicine newMedicine,
                                  LocalDate startDate,
                                  LocalDate endDate,
                                  Long excludePrescriptionId) {

        // Only run if the new medicine has a known INN
        if (newMedicine.getInn() == null || newMedicine.getInn().isBlank()) {
            return;
        }

        List<PrescriptionLine> conflicts = prescriptionLineRepository
                .findOverlappingByInn(patientId, newMedicine.getInn(), newMedicine.getId(),
                        startDate, endDate, excludePrescriptionId);

        for (PrescriptionLine pl : conflicts) {
            alerts.add(new DrugSafetyAlertDTO(
                    AlertType.SAME_INN,
                    "⚠️ OVERDOSE RISK — A medicine with the same active ingredient ("
                            + newMedicine.getInn() + ") is already active in another prescription.",
                    pl.getPrescription().getId(),
                    pl.getPrescription().getStatus(),
                    pl.getMedicine().getCommercialName(),
                    pl.getMedicine().getInn(),
                    pl.getMedicine().getTherapeuticFamily(),
                    pl.getStartDate(),
                    pl.getEndDate(),
                    pl.getDosage(),
                    null
            ));
        }
    }

    /**
     * CHECK 3 — SAME_FAMILY
     *
     * Detects whether another medicine from the same therapeutic family
     * is already active for this patient during the requested date range.
     *
     * Example: Prescribing a second SSRI antidepressant when the patient already
     *          takes one — serotonin syndrome risk.
     *
     * JPQL: findOverlappingByTherapeuticFamily
     */
    private void runSameFamilyCheck(List<DrugSafetyAlertDTO> alerts,
                                     Long patientId,
                                     Medicine newMedicine,
                                     LocalDate startDate,
                                     LocalDate endDate,
                                     Long excludePrescriptionId) {

        // Only run if the new medicine has a known therapeutic family
        if (newMedicine.getTherapeuticFamily() == null || newMedicine.getTherapeuticFamily().isBlank()) {
            return;
        }

        List<PrescriptionLine> conflicts = prescriptionLineRepository
                .findOverlappingByTherapeuticFamily(patientId, newMedicine.getTherapeuticFamily(),
                        newMedicine.getId(), startDate, endDate, excludePrescriptionId);

        for (PrescriptionLine pl : conflicts) {
            alerts.add(new DrugSafetyAlertDTO(
                    AlertType.SAME_FAMILY,
                    "⚠️ OVERDOSE RISK — Another medicine from the same therapeutic family ("
                            + newMedicine.getTherapeuticFamily() + ") is already active.",
                    pl.getPrescription().getId(),
                    pl.getPrescription().getStatus(),
                    pl.getMedicine().getCommercialName(),
                    pl.getMedicine().getInn(),
                    pl.getMedicine().getTherapeuticFamily(),
                    pl.getStartDate(),
                    pl.getEndDate(),
                    pl.getDosage(),
                    null
            ));
        }
    }

    /**
     * CHECK 4 — CONTRAINDICATION
     *
     * Checks whether the new medicine itself has a non-empty contraindication
     * list registered in the system. If so, a warning is generated regardless
     * of any other active prescriptions, alerting the doctor to review it.
     *
     * Note: This check does NOT require a JPQL join — it reads directly from
     *       the Medicine entity already fetched in step 0.
     */
    private void runContraindicationCheck(List<DrugSafetyAlertDTO> alerts,
                                           Medicine newMedicine) {

        if (newMedicine.getContraindications() != null
                && !newMedicine.getContraindications().isBlank()) {

            alerts.add(new DrugSafetyAlertDTO(
                    AlertType.CONTRAINDICATION,
                    "⛔ CONTRAINDICATION — This medicine has registered contraindications. Please review before prescribing.",
                    null,  // No conflicting prescription — this is about the medicine itself
                    null,
                    newMedicine.getCommercialName(),
                    newMedicine.getInn(),
                    newMedicine.getTherapeuticFamily(),
                    null,
                    null,
                    null,
                    newMedicine.getContraindications()
            ));
        }
    }
}
