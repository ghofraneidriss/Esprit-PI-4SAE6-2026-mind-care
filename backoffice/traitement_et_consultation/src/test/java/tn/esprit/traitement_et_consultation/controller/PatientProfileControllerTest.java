package tn.esprit.traitement_et_consultation.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tn.esprit.traitement_et_consultation.entity.PatientProfile;
import tn.esprit.traitement_et_consultation.service.PatientProfileService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientProfileControllerTest {

    @Mock
    private PatientProfileService patientProfileService;

    @InjectMocks
    private PatientProfileController patientProfileController;

    @Test
    void createProfileReturnsOkOrConflict() {
        PatientProfile profile = PatientProfile.builder().userId(1L).build();
        when(patientProfileService.saveProfile(profile)).thenReturn(profile);

        assertEquals(HttpStatus.OK, patientProfileController.createProfile(profile).getStatusCode());

        when(patientProfileService.saveProfile(profile)).thenThrow(new IllegalStateException("duplicate"));
        assertEquals(HttpStatus.CONFLICT, patientProfileController.createProfile(profile).getStatusCode());
    }

    @Test
    void lookupEndpointsReturnExpectedStatuses() {
        PatientProfile profile = PatientProfile.builder().userId(7L).email("a@b.com").build();
        when(patientProfileService.getProfileByUserId(7L)).thenReturn(Optional.of(profile));
        when(patientProfileService.getProfileByUserId(9L)).thenReturn(Optional.empty());
        when(patientProfileService.getProfileByEmail("a@b.com")).thenReturn(Optional.of(profile));
        when(patientProfileService.getProfileByEmail("x@y.com")).thenReturn(Optional.empty());
        when(patientProfileService.getAllProfiles()).thenReturn(List.of(profile));

        assertEquals(HttpStatus.OK, patientProfileController.getProfileByUserId(7L).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, patientProfileController.getProfileByUserId(9L).getStatusCode());
        assertEquals(HttpStatus.OK, patientProfileController.getProfileByEmail("a@b.com").getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, patientProfileController.getProfileByEmail("x@y.com").getStatusCode());
        assertEquals(1, patientProfileController.getAllProfiles().getBody().size());
    }

    @Test
    void updateAndDeleteMapRuntimeExceptionsToNotFound() {
        PatientProfile profile = PatientProfile.builder().build();
        when(patientProfileService.updateProfile(1L, profile)).thenReturn(profile);

        assertEquals(HttpStatus.OK, patientProfileController.updateProfile(1L, profile).getStatusCode());

        when(patientProfileService.updateProfile(2L, profile)).thenThrow(new RuntimeException("missing"));
        assertEquals(HttpStatus.NOT_FOUND, patientProfileController.updateProfile(2L, profile).getStatusCode());

        assertEquals(HttpStatus.NO_CONTENT, patientProfileController.deleteProfile(1L).getStatusCode());

        doThrow(new RuntimeException("missing")).when(patientProfileService).deleteProfile(2L);
        assertEquals(HttpStatus.NOT_FOUND, patientProfileController.deleteProfile(2L).getStatusCode());
    }

    @Test
    void listEndpointsReturnBodyFromService() {
        PatientProfile profile = PatientProfile.builder().userId(3L).build();
        when(patientProfileService.checkAllergyForMedicine(3L, "Med", "Family")).thenReturn(List.of("allergy"));
        when(patientProfileService.getPatientsWithRapidDegradation("therapy", 2)).thenReturn(List.of(profile));
        when(patientProfileService.getSeverePatientsWithoutFollowUp(4)).thenReturn(List.of(profile));

        ResponseEntity<List<String>> allergy = patientProfileController.checkAllergy(3L, "Med", "Family");
        assertEquals(List.of("allergy"), allergy.getBody());
        assertEquals(1, patientProfileController.getPatientsWithRapidDegradation("therapy", 2).getBody().size());
        assertEquals(1, patientProfileController.getSeverePatientsWithoutFollowUp(4).getBody().size());
        verify(patientProfileService).getSeverePatientsWithoutFollowUp(4);
    }
}
