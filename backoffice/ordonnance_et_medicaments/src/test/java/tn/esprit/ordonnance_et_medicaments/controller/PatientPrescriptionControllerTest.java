package tn.esprit.ordonnance_et_medicaments.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import tn.esprit.ordonnance_et_medicaments.entities.Prescription;
import tn.esprit.ordonnance_et_medicaments.service.PrescriptionService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientPrescriptionControllerTest {

    @Mock
    private PrescriptionService prescriptionService;

    @InjectMocks
    private PatientPrescriptionController patientPrescriptionController;

    @Test
    void patientEndpointsReturnHistoryAndDetails() {
        Prescription prescription = Prescription.builder().id(1L).build();
        when(prescriptionService.getHistoryByPatientId(4L)).thenReturn(List.of(prescription));
        when(prescriptionService.getPrescriptionById(1L)).thenReturn(prescription);

        assertEquals(HttpStatus.OK, patientPrescriptionController.getMyHistory(4L).getStatusCode());
        assertEquals(1, patientPrescriptionController.getMyHistory(4L).getBody().size());
        assertEquals(HttpStatus.OK, patientPrescriptionController.getById(1L).getStatusCode());
    }
}
