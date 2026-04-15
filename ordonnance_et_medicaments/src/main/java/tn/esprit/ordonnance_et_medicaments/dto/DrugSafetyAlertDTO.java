package tn.esprit.ordonnance_et_medicaments.dto;

import java.time.LocalDate;

/**
 * DTO representing a drug safety alert returned to the doctor
 * when a potential conflict is detected during prescription.
 *
 * Alert types:
 * - SAME_MEDICINE     : Exact same medicine already active for this patient
 * - SAME_INN          : Different brand but identical active ingredient (INN) already active
 * - SAME_FAMILY       : Same therapeutic family already prescribed — overdose risk
 * - CONTRAINDICATION  : The new medicine has a registered contraindication
 */
public class DrugSafetyAlertDTO {

    /**
     * Severity / type of the detected alert.
     */
    public enum AlertType {
        SAME_MEDICINE,
        SAME_INN,
        SAME_FAMILY,
        CONTRAINDICATION
    }

    // ── Alert classification ──────────────────────────────────────────────────

    /** Type of conflict detected */
    private AlertType alertType;

    /** Human-readable severity label sent to the UI */
    private String alertLabel;

    // ── Conflicting prescription context ──────────────────────────────────────

    /** ID of the existing prescription that causes the conflict */
    private Long conflictingPrescriptionId;

    /** Status of the conflicting prescription (e.g. SIGNED, PENDING) */
    private String conflictingPrescriptionStatus;

    // ── Conflicting medicine details ───────────────────────────────────────────

    /** Commercial name of the already-prescribed medicine */
    private String conflictingMedicineName;

    /** INN (International Non-proprietary Name) of the already-prescribed medicine */
    private String conflictingMedicineInn;

    /** Therapeutic family of the already-prescribed medicine */
    private String conflictingTherapeuticFamily;

    /** Active period start of the conflicting prescription line */
    private LocalDate conflictStartDate;

    /** Active period end of the conflicting prescription line */
    private LocalDate conflictEndDate;

    /** Dosage in the conflicting prescription line */
    private String conflictDosage;

    // ── Contraindication detail (only populated when alertType = CONTRAINDICATION) ──

    /** Registered contraindications of the new medicine being prescribed */
    private String contraindicationDetail;

    // ── Constructor ───────────────────────────────────────────────────────────

    public DrugSafetyAlertDTO(AlertType alertType,
                               String alertLabel,
                               Long conflictingPrescriptionId,
                               String conflictingPrescriptionStatus,
                               String conflictingMedicineName,
                               String conflictingMedicineInn,
                               String conflictingTherapeuticFamily,
                               LocalDate conflictStartDate,
                               LocalDate conflictEndDate,
                               String conflictDosage,
                               String contraindicationDetail) {
        this.alertType = alertType;
        this.alertLabel = alertLabel;
        this.conflictingPrescriptionId = conflictingPrescriptionId;
        this.conflictingPrescriptionStatus = conflictingPrescriptionStatus;
        this.conflictingMedicineName = conflictingMedicineName;
        this.conflictingMedicineInn = conflictingMedicineInn;
        this.conflictingTherapeuticFamily = conflictingTherapeuticFamily;
        this.conflictStartDate = conflictStartDate;
        this.conflictEndDate = conflictEndDate;
        this.conflictDosage = conflictDosage;
        this.contraindicationDetail = contraindicationDetail;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public AlertType getAlertType()                    { return alertType; }
    public String getAlertLabel()                      { return alertLabel; }
    public Long getConflictingPrescriptionId()         { return conflictingPrescriptionId; }
    public String getConflictingPrescriptionStatus()   { return conflictingPrescriptionStatus; }
    public String getConflictingMedicineName()         { return conflictingMedicineName; }
    public String getConflictingMedicineInn()          { return conflictingMedicineInn; }
    public String getConflictingTherapeuticFamily()    { return conflictingTherapeuticFamily; }
    public LocalDate getConflictStartDate()            { return conflictStartDate; }
    public LocalDate getConflictEndDate()              { return conflictEndDate; }
    public String getConflictDosage()                  { return conflictDosage; }
    public String getContraindicationDetail()          { return contraindicationDetail; }
}
