package tn.esprit.traitement_et_consultation.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private Long userId; // Link to user in users-service

    @Column(unique = true)
    private String email; // Link via email

    private java.time.LocalDate dateOfBirth;

    private String bloodGroup;
    private Double heightCm;
    private Double weightKg;
    private String educationLevel;

    private String caregiverEmergencyNumber;

    private Boolean isSmoker;
    private Boolean drinksAlcohol;
    private Boolean physicalActivity;
    private Boolean familyHistoryAlzheimer;

    private Boolean hypertension;
    private Boolean type2Diabetes;
    private Boolean hypercholesterolemia;
    private Boolean sleepDisorders;

    @Column(columnDefinition = "TEXT")
    private String medications;

    @ElementCollection
    @CollectionTable(name = "patient_profile_amedicaments")
    private java.util.List<String> amedicaments; // Liste des médicaments actuels

    @ElementCollection
    @CollectionTable(name = "patient_profile_allergies")
    private java.util.List<String> allergies; // Liste des allergies (saisies par le patient)

    private Double externalCognitiveScore;

    /**
     * Nom complet du patient (pour l'affichage sans jointure complexe).
     */
    private String patientName;

    /**
     * Note explicative générée par le système pour le Status Tracking.
     */
    @Transient // On ne le stocke pas forcément, ou on l'utilise pour le DTO
    private String clinicalAnomalyNote;
}
