package tn.esprit.ordonnance_et_medicaments.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.ordonnance_et_medicaments.dto.OverlapConflictDTO;
import tn.esprit.ordonnance_et_medicaments.entities.Medicine;
import tn.esprit.ordonnance_et_medicaments.entities.Prescription;
import tn.esprit.ordonnance_et_medicaments.entities.PrescriptionLine;
import tn.esprit.ordonnance_et_medicaments.repository.PrescriptionLineRepository;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrescriptionOverlapServiceTest {

    @Mock
    private PrescriptionLineRepository prescriptionLineRepository;

    @InjectMocks
    private PrescriptionOverlapService prescriptionOverlapService;

    @Test
    void detectOverlapsMapsRepositoryLinesToDtoAndUsesMaxValueForNewPrescription() {
        PrescriptionLine conflict = PrescriptionLine.builder()
                .medicine(Medicine.builder()
                        .commercialName("Doliprane")
                        .inn("Paracetamol")
                        .therapeuticFamily("Analgesic")
                        .build())
                .prescription(Prescription.builder().id(5L).status("SIGNED").build())
                .startDate(LocalDate.of(2026, 5, 1))
                .endDate(LocalDate.of(2026, 5, 5))
                .dosage("2/day")
                .build();

        when(prescriptionLineRepository.findOverlappingMedicinePrescriptions(
                9L, 3L, LocalDate.of(2026, 5, 3), LocalDate.of(2026, 5, 7), Long.MAX_VALUE))
                .thenReturn(List.of(conflict));

        List<OverlapConflictDTO> results = prescriptionOverlapService.detectOverlaps(
                9L, 3L, LocalDate.of(2026, 5, 3), LocalDate.of(2026, 5, 7), 0L);

        assertEquals(1, results.size());
        assertEquals(5L, results.get(0).getConflictingPrescriptionId());
        assertEquals("Doliprane", results.get(0).getMedicineName());
        assertEquals("SIGNED", results.get(0).getPrescriptionStatus());
    }

    @Test
    void detectOverlapsUsesExistingPrescriptionIdWhenProvided() {
        when(prescriptionLineRepository.findOverlappingMedicinePrescriptions(
                9L, 3L, LocalDate.of(2026, 5, 3), LocalDate.of(2026, 5, 7), 44L))
                .thenReturn(List.of());

        prescriptionOverlapService.detectOverlaps(
                9L, 3L, LocalDate.of(2026, 5, 3), LocalDate.of(2026, 5, 7), 44L);

        verify(prescriptionLineRepository).findOverlappingMedicinePrescriptions(
                9L, 3L, LocalDate.of(2026, 5, 3), LocalDate.of(2026, 5, 7), 44L);
    }
}
