package tn.esprit.traitement_et_consultation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.traitement_et_consultation.entity.PatientProfile;
import tn.esprit.traitement_et_consultation.repository.PatientProfileRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PatientProfileService {

    private final PatientProfileRepository patientProfileRepository;

    /**
     * Valide que le patient a au moins 15 ans.
     * Cette règle métier assure que la plateforme est utilisée en conformité avec
     * l'éthique médicale.
     */
    private void validateAgeOver15(java.time.LocalDate dob) {
        if (dob != null) {
            int age = java.time.Period.between(dob, java.time.LocalDate.now()).getYears();
            if (age < 15) {
                throw new IllegalStateException("Le patient doit avoir au moins 15 ans.");
            }
        }
    }

    /**
     * [API] Sauvegarde un nouveau profil patient.
     * Vérifie l'unicité de l'email et de l'ID utilisateur pour éviter les doublons
     * en base.
     */
    public PatientProfile saveProfile(PatientProfile profile) {
        validateAgeOver15(profile.getDateOfBirth());

        if (profile.getEmail() != null && patientProfileRepository.findByEmail(profile.getEmail()).isPresent()) {
            throw new IllegalStateException("Un profil existe déjà avec cet email: " + profile.getEmail());
        }
        if (profile.getUserId() != null && patientProfileRepository.findByUserId(profile.getUserId()).isPresent()) {
            throw new IllegalStateException(
                    "Un profil existe déjà pour cet utilisateur (userId=" + profile.getUserId() + ")");
        }
        return patientProfileRepository.save(profile);
    }

    public Optional<PatientProfile> getProfileByUserId(Long userId) {
        return patientProfileRepository.findByUserId(userId);
    }

    public Optional<PatientProfile> getProfileByEmail(String email) {
        return patientProfileRepository.findByEmail(email);
    }

    public List<PatientProfile> getAllProfiles() {
        return patientProfileRepository.findAll();
    }

    public PatientProfile updateProfile(Long id, PatientProfile updatedProfile) {
        return patientProfileRepository.findById(id)
                .map(existing -> {
                    // Copy fields onto the managed entity to avoid detachment / unique constraint
                    // issues
                    existing.setBloodGroup(updatedProfile.getBloodGroup());
                    existing.setHeightCm(updatedProfile.getHeightCm());
                    existing.setWeightKg(updatedProfile.getWeightKg());
                    existing.setEducationLevel(updatedProfile.getEducationLevel());
                    existing.setCaregiverEmergencyNumber(updatedProfile.getCaregiverEmergencyNumber());
                    existing.setIsSmoker(updatedProfile.getIsSmoker());
                    existing.setDrinksAlcohol(updatedProfile.getDrinksAlcohol());
                    existing.setPhysicalActivity(updatedProfile.getPhysicalActivity());
                    existing.setFamilyHistoryAlzheimer(updatedProfile.getFamilyHistoryAlzheimer());
                    existing.setHypertension(updatedProfile.getHypertension());
                    existing.setType2Diabetes(updatedProfile.getType2Diabetes());
                    existing.setHypercholesterolemia(updatedProfile.getHypercholesterolemia());
                    existing.setSleepDisorders(updatedProfile.getSleepDisorders());
                    existing.setMedications(updatedProfile.getMedications());
                    existing.setExternalCognitiveScore(updatedProfile.getExternalCognitiveScore());
                    existing.setAllergies(updatedProfile.getAllergies());
                    existing.setAmedicaments(updatedProfile.getAmedicaments());

                    validateAgeOver15(updatedProfile.getDateOfBirth());
                    existing.setDateOfBirth(updatedProfile.getDateOfBirth());

                    return patientProfileRepository.save(existing);
                }).orElseThrow(() -> new RuntimeException("Profile not found with id: " + id));
    }

    public void deleteProfile(Long id) {
        patientProfileRepository.deleteById(id);
    }

    /**
     * [ALGORITHME] Identifie les patients subissant une dégradation cognitive
     * rapide.
     * Cette logique est liée au tableau de bord "Status Tracking" du Frontend
     * Angular.
     * Le calcul est déporté sur le Backend pour optimiser les performances.
     */
    public List<PatientProfile> getPatientsWithRapidDegradation(String treatment, Integer degradationThreshold) {
        return patientProfileRepository.findPatientsWithRapidDegradation(treatment, degradationThreshold);
    }

    /**
     * [ALGORITHME] Détecte les patients sévères n'ayant pas eu de suivi récent.
     * Analyse les rendez-vous passés par rapport à un seuil temporel (monthsAgo).
     */
    /**
     * [JPQL] Vérifie si un patient a une allergie correspondant
     * au nom ou à la famille thérapeutique d'un médicament donné.
     *
     * @param userId            ID du patient
     * @param medicineName      Nom commercial du médicament
     * @param therapeuticFamily Famille thérapeutique du médicament
     * @return Liste des allergies correspondantes (vide = aucune allergie détectée)
     */
    public List<String> checkAllergyForMedicine(Long userId, String medicineName, String therapeuticFamily) {
        return patientProfileRepository.findMatchingAllergies(
                userId,
                medicineName != null ? medicineName : "",
                therapeuticFamily != null ? therapeuticFamily : ""
        );
    }

    public List<PatientProfile> getSeverePatientsWithoutFollowUp(int monthsAgo) {
        java.time.LocalDateTime thresholdDate = java.time.LocalDateTime.now().minusMonths(monthsAgo);
        return patientProfileRepository.findSeverePatientsWithoutFollowUp(thresholdDate);
    }
}
