package tn.esprit.ordonnance_et_medicaments.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.ordonnance_et_medicaments.entities.Medicine;
import tn.esprit.ordonnance_et_medicaments.entities.Prescription;
import tn.esprit.ordonnance_et_medicaments.entities.PrescriptionLine;
import tn.esprit.ordonnance_et_medicaments.repository.MedicineRepository;
import tn.esprit.ordonnance_et_medicaments.repository.PrescriptionRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceTest {

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private PrescriptionMailService mailService;

    @InjectMocks
    private PrescriptionService prescriptionService;

    @Test
    void createPrescriptionReusesExistingMedicineWhenExactMatchExists() {
        Medicine newMedicine = Medicine.builder().commercialName("Doliprane").inn("Paracetamol").build();
        Medicine existingMedicine = Medicine.builder().id(3L).commercialName("Doliprane").inn("Paracetamol").build();
        Prescription prescription = buildPrescription(newMedicine);

        when(medicineRepository.findExactMatchJPQL("Doliprane", "Paracetamol"))
                .thenReturn(Optional.of(existingMedicine));
        when(prescriptionRepository.save(prescription)).thenReturn(prescription);

        Prescription saved = prescriptionService.createPrescription(prescription);

        assertSame(existingMedicine, saved.getPrescriptionLines().get(0).getMedicine());
        assertSame(saved, saved.getPrescriptionLines().get(0).getPrescription());
        verify(medicineRepository, never()).save(any(Medicine.class));
    }

    @Test
    void createPrescriptionSavesNewMedicineWhenNoExactMatchExists() {
        Medicine medicine = Medicine.builder().commercialName("NewMed").inn("Mol").build();
        Prescription prescription = buildPrescription(medicine);

        when(medicineRepository.findExactMatchJPQL("NewMed", "Mol")).thenReturn(Optional.empty());
        when(prescriptionRepository.save(prescription)).thenReturn(prescription);

        prescriptionService.createPrescription(prescription);

        verify(medicineRepository).save(medicine);
    }

    @Test
    void saveAsDraftSetsStatusBeforeDelegating() {
        Prescription prescription = buildPrescription(Medicine.builder().id(1L).build());
        when(prescriptionRepository.save(prescription)).thenReturn(prescription);

        Prescription saved = prescriptionService.saveAsDraft(prescription);

        assertEquals("DRAFT", saved.getStatus());
    }

    @Test
    void updatePrescriptionReplacesLinesAndSendsMailWhenSigned() {
        Prescription existing = Prescription.builder()
                .id(5L)
                .status("DRAFT")
                .doctorSignature("old")
                .prescriptionLines(new java.util.ArrayList<>(List.of(buildLine(Medicine.builder().id(1L).build()))))
                .build();
        Prescription details = Prescription.builder()
                .status("SIGNED")
                .doctorSignature("new-signature")
                .prescriptionLines(List.of(buildLine(Medicine.builder().id(9L).build())))
                .build();

        when(prescriptionRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(prescriptionRepository.save(existing)).thenReturn(existing);

        Prescription saved = prescriptionService.updatePrescription(5L, details);

        assertEquals("SIGNED", saved.getStatus());
        assertEquals("new-signature", saved.getDoctorSignature());
        assertEquals(1, saved.getPrescriptionLines().size());
        assertSame(saved, saved.getPrescriptionLines().get(0).getPrescription());
        verify(mailService).sendSignedPrescriptionEmail(saved);
    }

    @Test
    void updatePrescriptionDoesNotSendMailForUnsignedStatuses() {
        Prescription existing = Prescription.builder()
                .id(5L)
                .prescriptionLines(new java.util.ArrayList<>())
                .build();
        Prescription details = Prescription.builder()
                .status("PENDING")
                .prescriptionLines(List.of())
                .build();

        when(prescriptionRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(prescriptionRepository.save(existing)).thenReturn(existing);

        prescriptionService.updatePrescription(5L, details);

        verify(mailService, never()).sendSignedPrescriptionEmail(any(Prescription.class));
    }

    @Test
    void updatePrescriptionThrowsWhenMissing() {
        when(prescriptionRepository.findById(55L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> prescriptionService.updatePrescription(55L, Prescription.builder().status("SIGNED").build()));
    }

    @Test
    void repositoryDelegationMethodsReturnExpectedValues() {
        Prescription prescription = Prescription.builder().id(4L).patientId(7L).build();
        when(prescriptionRepository.findByPatientIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(prescription));
        when(prescriptionRepository.findById(4L)).thenReturn(Optional.of(prescription));

        assertEquals(1, prescriptionService.getHistoryByPatientId(7L).size());
        assertSame(prescription, prescriptionService.getPrescriptionById(4L));

        prescriptionService.deletePrescription(4L);
        verify(prescriptionRepository).deleteById(4L);
    }

    private Prescription buildPrescription(Medicine medicine) {
        PrescriptionLine line = buildLine(medicine);
        Prescription prescription = Prescription.builder()
                .patientId(1L)
                .prescriptionLines(new java.util.ArrayList<>(List.of(line)))
                .build();
        line.setPrescription(prescription);
        return prescription;
    }

    private PrescriptionLine buildLine(Medicine medicine) {
        return PrescriptionLine.builder()
                .medicine(medicine)
                .dosage("1/day")
                .startDate(LocalDate.of(2026, 5, 6))
                .endDate(LocalDate.of(2026, 5, 12))
                .build();
    }
}
