package tn.esprit.ordonnance_et_medicaments.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.ordonnance_et_medicaments.dto.DrugSafetyAlertDTO;
import tn.esprit.ordonnance_et_medicaments.entities.Medicine;
import tn.esprit.ordonnance_et_medicaments.entities.Prescription;
import tn.esprit.ordonnance_et_medicaments.entities.PrescriptionLine;
import tn.esprit.ordonnance_et_medicaments.repository.MedicineRepository;
import tn.esprit.ordonnance_et_medicaments.repository.PrescriptionLineRepository;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DrugSafetyServiceTest {

    @Mock
    private PrescriptionLineRepository prescriptionLineRepository;

    @Mock
    private MedicineRepository medicineRepository;

    @InjectMocks
    private DrugSafetyService drugSafetyService;

    private Medicine medicine;
    private PrescriptionLine overlappingLine;
    private Prescription prescription;

    @BeforeEach
    void setUp() {
        medicine = new Medicine();
        medicine.setId(1L);
        medicine.setCommercialName("Doliprane");
        medicine.setInn("Paracetamol");
        medicine.setTherapeuticFamily("Analgesic");

        prescription = new Prescription();
        prescription.setId(100L);
        prescription.setStatus("ACTIVE");

        overlappingLine = new PrescriptionLine();
        overlappingLine.setMedicine(medicine);
        overlappingLine.setPrescription(prescription);
        overlappingLine.setStartDate(LocalDate.now().minusDays(2));
        overlappingLine.setEndDate(LocalDate.now().plusDays(5));
    }

    @Test
    void testCheckDrugSafety_NoConflicts() {
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(medicine));
        when(prescriptionLineRepository.findOverlappingMedicinePrescriptions(any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(prescriptionLineRepository.findOverlappingByInn(any(), any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(prescriptionLineRepository.findOverlappingByTherapeuticFamily(any(), any(), any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        List<DrugSafetyAlertDTO> alerts = drugSafetyService.checkDrugSafety(1L, 1L, LocalDate.now(), LocalDate.now().plusDays(5), null);

        assertTrue(alerts.isEmpty());
    }

    @Test
    void testCheckDrugSafety_SameMedicineConflict() {
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(medicine));
        when(prescriptionLineRepository.findOverlappingMedicinePrescriptions(eq(1L), eq(1L), any(LocalDate.class), any(LocalDate.class), any()))
                .thenReturn(List.of(overlappingLine));

        List<DrugSafetyAlertDTO> alerts = drugSafetyService.checkDrugSafety(1L, 1L, LocalDate.now(), LocalDate.now().plusDays(5), null);

        assertEquals(1, alerts.size());
        assertEquals(DrugSafetyAlertDTO.AlertType.SAME_MEDICINE, alerts.get(0).getAlertType());
    }

    @Test
    void testCheckDrugSafety_Contraindication() {
        medicine.setContraindications("Severe allergy");
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(medicine));
        
        List<DrugSafetyAlertDTO> alerts = drugSafetyService.checkDrugSafety(1L, 1L, LocalDate.now(), LocalDate.now().plusDays(5), null);

        assertEquals(1, alerts.size());
        assertEquals(DrugSafetyAlertDTO.AlertType.CONTRAINDICATION, alerts.get(0).getAlertType());
    }
}
