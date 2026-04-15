package tn.esprit.ordonnance_et_medicaments.dto;

import java.time.LocalDate;

/**
 * DTO retourné quand le système détecte un comportement de "doctor shopping".
 * Un patient tente d'obtenir la même prescription auprès de plusieurs médecins
 * différents, alors qu'une prescription active existe déjà.
 *
 * Utilisé par le endpoint GET /api/doctor/prescriptions/check-doctor-shopping
 */
public class DoctorShoppingAlertDTO {

    /** ID de la prescription active existante (prescrite par un autre médecin) */
    private Long existingPrescriptionId;

    /** ID du médecin qui a établi la prescription active */
    private Long prescribingDoctorId;

    /** Nom commercial du médicament en conflit */
    private String medicineName;

    /** DCI (Dénomination Commune Internationale) du médicament */
    private String medicineInn;

    /** Date de début du traitement déjà prescrit */
    private LocalDate activeStartDate;

    /** Date de fin du traitement déjà prescrit (non encore atteinte) */
    private LocalDate activeEndDate;

    /** Posologie définie dans la prescription existante */
    private String existingDosage;

    /** Statut de la prescription existante (SIGNED, PENDING…) */
    private String existingStatus;

    // Constructeur complet utilisé par le service
    public DoctorShoppingAlertDTO(Long existingPrescriptionId,
                                   Long prescribingDoctorId,
                                   String medicineName,
                                   String medicineInn,
                                   LocalDate activeStartDate,
                                   LocalDate activeEndDate,
                                   String existingDosage,
                                   String existingStatus) {
        this.existingPrescriptionId = existingPrescriptionId;
        this.prescribingDoctorId    = prescribingDoctorId;
        this.medicineName           = medicineName;
        this.medicineInn            = medicineInn;
        this.activeStartDate        = activeStartDate;
        this.activeEndDate          = activeEndDate;
        this.existingDosage         = existingDosage;
        this.existingStatus         = existingStatus;
    }

    // Getters (pas de setters — DTO en lecture seule)
    public Long   getExistingPrescriptionId() { return existingPrescriptionId; }
    public Long   getPrescribingDoctorId()     { return prescribingDoctorId; }
    public String getMedicineName()            { return medicineName; }
    public String getMedicineInn()             { return medicineInn; }
    public LocalDate getActiveStartDate()      { return activeStartDate; }
    public LocalDate getActiveEndDate()        { return activeEndDate; }
    public String getExistingDosage()          { return existingDosage; }
    public String getExistingStatus()          { return existingStatus; }
}
