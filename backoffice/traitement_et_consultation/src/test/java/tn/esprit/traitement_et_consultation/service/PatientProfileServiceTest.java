package tn.esprit.traitement_et_consultation.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.traitement_et_consultation.entity.PatientProfile;
import tn.esprit.traitement_et_consultation.repository.PatientProfileRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientProfileServiceTest {

    @Mock
    private PatientProfileRepository patientProfileRepository;

    @InjectMocks
    private PatientProfileService patientProfileService;

    @Test
    void saveProfileRejectsUnderagePatients() {
        PatientProfile profile = PatientProfile.builder()
                .dateOfBirth(LocalDate.now().minusYears(10))
                .build();

        assertThrows(IllegalStateException.class, () -> patientProfileService.saveProfile(profile));
        verify(patientProfileRepository, never()).save(profile);
    }

    @Test
    void saveProfileRejectsDuplicateEmail() {
        PatientProfile profile = PatientProfile.builder()
                .email("patient@example.com")
                .dateOfBirth(LocalDate.now().minusYears(30))
                .build();

        when(patientProfileRepository.findByEmail("patient@example.com"))
                .thenReturn(Optional.of(new PatientProfile()));

        assertThrows(IllegalStateException.class, () -> patientProfileService.saveProfile(profile));
    }

    @Test
    void saveProfileRejectsDuplicateUserId() {
        PatientProfile profile = PatientProfile.builder()
                .userId(9L)
                .dateOfBirth(LocalDate.now().minusYears(30))
                .build();

        when(patientProfileRepository.findByUserId(9L))
                .thenReturn(Optional.of(new PatientProfile()));

        assertThrows(IllegalStateException.class, () -> patientProfileService.saveProfile(profile));
    }

    @Test
    void saveProfilePersistsValidProfile() {
        PatientProfile profile = PatientProfile.builder()
                .userId(15L)
                .email("ok@example.com")
                .dateOfBirth(LocalDate.now().minusYears(40))
                .build();

        when(patientProfileRepository.findByEmail("ok@example.com")).thenReturn(Optional.empty());
        when(patientProfileRepository.findByUserId(15L)).thenReturn(Optional.empty());
        when(patientProfileRepository.save(profile)).thenReturn(profile);

        PatientProfile saved = patientProfileService.saveProfile(profile);

        assertSame(profile, saved);
    }

    @Test
    void updateProfileCopiesFieldsAndSavesManagedEntity() {
        PatientProfile existing = PatientProfile.builder()
                .id(1L)
                .userId(100L)
                .dateOfBirth(LocalDate.now().minusYears(45))
                .build();
        PatientProfile updates = PatientProfile.builder()
                .bloodGroup("A+")
                .heightCm(170.0)
                .weightKg(68.0)
                .educationLevel("College")
                .caregiverEmergencyNumber("123")
                .isSmoker(true)
                .drinksAlcohol(false)
                .physicalActivity(true)
                .familyHistoryAlzheimer(true)
                .hypertension(true)
                .type2Diabetes(false)
                .hypercholesterolemia(true)
                .sleepDisorders(false)
                .medications("Donepezil")
                .externalCognitiveScore(19.0)
                .allergies(List.of("Penicillin"))
                .amedicaments(List.of("MedA"))
                .dateOfBirth(LocalDate.now().minusYears(46))
                .build();

        when(patientProfileRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(patientProfileRepository.save(existing)).thenReturn(existing);

        PatientProfile updated = patientProfileService.updateProfile(1L, updates);

        assertEquals("A+", updated.getBloodGroup());
        assertEquals(170.0, updated.getHeightCm());
        assertEquals(68.0, updated.getWeightKg());
        assertEquals("Donepezil", updated.getMedications());
        assertEquals(19.0, updated.getExternalCognitiveScore());
        assertEquals(List.of("Penicillin"), updated.getAllergies());
    }

    @Test
    void updateProfileThrowsWhenMissing() {
        when(patientProfileRepository.findById(77L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> patientProfileService.updateProfile(77L, PatientProfile.builder().build()));
    }

    @Test
    void checkAllergyForMedicineNormalizesNullInputs() {
        when(patientProfileRepository.findMatchingAllergies(4L, "", ""))
                .thenReturn(List.of("Aspirin"));

        List<String> matches = patientProfileService.checkAllergyForMedicine(4L, null, null);

        assertEquals(List.of("Aspirin"), matches);
    }

    @Test
    void delegationMethodsReturnRepositoryResults() {
        PatientProfile profile = PatientProfile.builder().userId(5L).email("a@b.com").build();
        when(patientProfileRepository.findByUserId(5L)).thenReturn(Optional.of(profile));
        when(patientProfileRepository.findByEmail("a@b.com")).thenReturn(Optional.of(profile));
        when(patientProfileRepository.findAll()).thenReturn(List.of(profile));
        when(patientProfileRepository.findPatientsWithRapidDegradation("therapy", 2)).thenReturn(List.of(profile));
        when(patientProfileRepository.findSeverePatientsWithoutFollowUp(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(profile));

        assertEquals(Optional.of(profile), patientProfileService.getProfileByUserId(5L));
        assertEquals(Optional.of(profile), patientProfileService.getProfileByEmail("a@b.com"));
        assertEquals(1, patientProfileService.getAllProfiles().size());
        assertEquals(1, patientProfileService.getPatientsWithRapidDegradation("therapy", 2).size());
        assertEquals(1, patientProfileService.getSeverePatientsWithoutFollowUp(3).size());

        patientProfileService.deleteProfile(9L);
        verify(patientProfileRepository).deleteById(9L);
    }
}
