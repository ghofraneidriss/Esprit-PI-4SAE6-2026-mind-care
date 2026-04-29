package tn.esprit.ordonnance_et_medicaments.dto;

import java.time.LocalDate;

/**
 * DTO retourné par le endpoint de détection de chevauchement.
 * Contient toutes les informations nécessaires pour alerter le médecin dans l'UI.
 */
public class OverlapConflictDTO {

    private Long conflictingPrescriptionId;   // ID de l'ordonnance en conflit
    private String medicineName;               // Nom commercial du médicament
    private String medicineInn;                // DCI du médicament
    private String therapeuticFamily;          // Famille thérapeutique
    private LocalDate conflictStartDate;       // Début du traitement en conflit
    private LocalDate conflictEndDate;         // Fin du traitement en conflit
    private String conflictDosage;             // Posologie dans l'autre ordonnance
    private String prescriptionStatus;         // Statut de l'ordonnance en conflit

    // Constructeur complet
    public OverlapConflictDTO(Long conflictingPrescriptionId,
                               String medicineName,
                               String medicineInn,
                               String therapeuticFamily,
                               LocalDate conflictStartDate,
                               LocalDate conflictEndDate,
                               String conflictDosage,
                               String prescriptionStatus) {
        this.conflictingPrescriptionId = conflictingPrescriptionId;
        this.medicineName = medicineName;
        this.medicineInn = medicineInn;
        this.therapeuticFamily = therapeuticFamily;
        this.conflictStartDate = conflictStartDate;
        this.conflictEndDate = conflictEndDate;
        this.conflictDosage = conflictDosage;
        this.prescriptionStatus = prescriptionStatus;
    }

    public Long getConflictingPrescriptionId() { return conflictingPrescriptionId; }
    public String getMedicineName() { return medicineName; }
    public String getMedicineInn() { return medicineInn; }
    public String getTherapeuticFamily() { return therapeuticFamily; }
    public LocalDate getConflictStartDate() { return conflictStartDate; }
    public LocalDate getConflictEndDate() { return conflictEndDate; }
    public String getConflictDosage() { return conflictDosage; }
    public String getPrescriptionStatus() { return prescriptionStatus; }
}
