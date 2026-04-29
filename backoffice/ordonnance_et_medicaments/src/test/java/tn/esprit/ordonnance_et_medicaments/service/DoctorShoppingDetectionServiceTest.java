package tn.esprit.ordonnance_et_medicaments.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.ordonnance_et_medicaments.dto.DoctorShoppingAlertDTO;
import tn.esprit.ordonnance_et_medicaments.entities.Medicine;
import tn.esprit.ordonnance_et_medicaments.entities.Prescription;
import tn.esprit.ordonnance_et_medicaments.entities.PrescriptionLine;
import tn.esprit.ordonnance_et_medicaments.repository.PrescriptionLineRepository;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorShoppingDetectionServiceTest {

    @Mock
    private PrescriptionLineRepository prescriptionLineRepository;

    @InjectMocks
    private DoctorShoppingDetectionService doctorShoppingDetectionService;

    private PrescriptionLine conflictingLine;

    @BeforeEach
    void setUp() {
        Medicine medicine = new Medicine();
        medicine.setCommercialName("Xanax");
        medicine.setInn("Alprazolam");

        Prescription prescription = new Prescription();
        prescription.setId(500L);
        prescription.setDoctorId(2L); // Different doctor
        prescription.setStatus("SIGNED");

        conflictingLine = new PrescriptionLine();
        conflictingLine.setMedicine(medicine);
        conflictingLine.setPrescription(prescription);
        conflictingLine.setStartDate(LocalDate.now().minusDays(5));
        conflictingLine.setEndDate(LocalDate.now().plusDays(10));
        conflictingLine.setDosage("1 pill a day");
    }

    @Test
    void testDetectDoctorShopping_NoAlert() {
        when(prescriptionLineRepository.findActivePrescriptionsByOtherDoctors(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        List<DoctorShoppingAlertDTO> alerts = doctorShoppingDetectionService.detectDoctorShopping(1L, 10L, 1L);

        assertTrue(alerts.isEmpty());
    }

    @Test
    void testDetectDoctorShopping_WithAlert() {
        when(prescriptionLineRepository.findActivePrescriptionsByOtherDoctors(eq(1L), eq(10L), eq(1L), any(LocalDate.class)))
                .thenReturn(List.of(conflictingLine));

        List<DoctorShoppingAlertDTO> alerts = doctorShoppingDetectionService.detectDoctorShopping(1L, 10L, 1L);

        assertEquals(1, alerts.size());
        assertEquals("Xanax", alerts.get(0).getMedicineName());
        assertEquals(2L, alerts.get(0).getPrescribingDoctorId());
    }
}
