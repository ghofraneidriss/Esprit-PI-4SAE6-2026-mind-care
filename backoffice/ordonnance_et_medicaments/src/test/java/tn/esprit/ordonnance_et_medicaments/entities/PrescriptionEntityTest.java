package tn.esprit.ordonnance_et_medicaments.entities;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tn.esprit.ordonnance_et_medicaments.dto.OverlapConflictDTO;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PrescriptionEntityTest {

    @Test
    void onCreateInitializesTimestamp() {
        Prescription prescription = new Prescription();

        ReflectionTestUtils.invokeMethod(prescription, "onCreate");

        assertNotNull(prescription.getCreatedAt());
    }

    @Test
    void overlapConflictDtoExposesConstructorValues() {
        OverlapConflictDTO dto = new OverlapConflictDTO(1L, "Doliprane", "Paracetamol", "Analgesic",
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 2), "2/day", "SIGNED");

        assertEquals(1L, dto.getConflictingPrescriptionId());
        assertEquals("Doliprane", dto.getMedicineName());
        assertEquals("Paracetamol", dto.getMedicineInn());
        assertEquals("Analgesic", dto.getTherapeuticFamily());
        assertEquals("2/day", dto.getConflictDosage());
        assertEquals("SIGNED", dto.getPrescriptionStatus());
    }
}
