package tn.esprit.ordonnance_et_medicaments.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import tn.esprit.ordonnance_et_medicaments.dto.DoctorShoppingAlertDTO;
import tn.esprit.ordonnance_et_medicaments.dto.DrugSafetyAlertDTO;
import tn.esprit.ordonnance_et_medicaments.dto.OverlapConflictDTO;
import tn.esprit.ordonnance_et_medicaments.entities.Medicine;
import tn.esprit.ordonnance_et_medicaments.entities.Prescription;
import tn.esprit.ordonnance_et_medicaments.service.DoctorShoppingDetectionService;
import tn.esprit.ordonnance_et_medicaments.service.DrugSafetyService;
import tn.esprit.ordonnance_et_medicaments.service.MedicineService;
import tn.esprit.ordonnance_et_medicaments.service.PrescriptionOverlapService;
import tn.esprit.ordonnance_et_medicaments.service.PrescriptionService;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorPrescriptionControllerTest {

    @Mock
    private PrescriptionService prescriptionService;

    @Mock
    private MedicineService medicineService;

    @Mock
    private PrescriptionOverlapService overlapService;

    @Mock
    private DrugSafetyService drugSafetyService;

    @Mock
    private DoctorShoppingDetectionService doctorShoppingService;

    @InjectMocks
    private DoctorPrescriptionController doctorPrescriptionController;

    @Test
    void doctorEndpointsDelegateToUnderlyingServices() {
        Prescription prescription = Prescription.builder().id(1L).build();
        Medicine medicine = Medicine.builder().id(7L).commercialName("Doliprane").build();
        OverlapConflictDTO overlap = new OverlapConflictDTO(4L, "Doliprane", "Paracetamol",
                "Analgesic", LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 4), "1/day", "SIGNED");
        DrugSafetyAlertDTO safetyAlert = new DrugSafetyAlertDTO(
                DrugSafetyAlertDTO.AlertType.SAME_MEDICINE, "Same medicine", 7L, "SIGNED",
                "Doliprane", "Paracetamol", "Analgesic",
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 4), "1/day", null);
        DoctorShoppingAlertDTO shoppingAlert = new DoctorShoppingAlertDTO(
                4L, 99L, "Doliprane", "Paracetamol", LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 4), "1/day", "SIGNED");

        when(prescriptionService.createPrescription(prescription)).thenReturn(prescription);
        when(prescriptionService.saveAsDraft(prescription)).thenReturn(prescription);
        when(prescriptionService.getHistoryByPatientId(6L)).thenReturn(List.of(prescription));
        when(prescriptionService.updatePrescription(1L, prescription)).thenReturn(prescription);
        when(prescriptionService.getPrescriptionById(1L)).thenReturn(prescription);
        when(medicineService.searchMedicines("dol")).thenReturn(List.of(medicine));
        when(overlapService.detectOverlaps(6L, 7L, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 4), 0L))
                .thenReturn(List.of(overlap));
        when(drugSafetyService.checkDrugSafety(6L, 7L, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 4), 0L))
                .thenReturn(List.of(safetyAlert));
        when(doctorShoppingService.detectDoctorShopping(6L, 7L, 8L)).thenReturn(List.of(shoppingAlert));

        assertEquals(HttpStatus.OK, doctorPrescriptionController.create(prescription).getStatusCode());
        assertEquals(HttpStatus.OK, doctorPrescriptionController.createDraft(prescription).getStatusCode());
        assertEquals(1, doctorPrescriptionController.getHistory(6L).getBody().size());
        assertEquals(HttpStatus.OK, doctorPrescriptionController.update(1L, prescription).getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, doctorPrescriptionController.delete(1L).getStatusCode());
        verify(prescriptionService).deletePrescription(1L);
        assertEquals(HttpStatus.OK, doctorPrescriptionController.getById(1L).getStatusCode());
        assertEquals(1, doctorPrescriptionController.searchMedicines("dol").getBody().size());
        assertEquals(1, doctorPrescriptionController.checkOverlap(
                6L, 7L, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 4), 0L).getBody().size());
        assertEquals(1, doctorPrescriptionController.checkDrugSafety(
                6L, 7L, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 4), 0L).getBody().size());
        assertEquals(1, doctorPrescriptionController.checkDoctorShopping(6L, 7L, 8L).getBody().size());
    }
}
