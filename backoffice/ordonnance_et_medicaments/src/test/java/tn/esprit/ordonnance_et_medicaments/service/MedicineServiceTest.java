package tn.esprit.ordonnance_et_medicaments.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.ordonnance_et_medicaments.entities.Medicine;
import tn.esprit.ordonnance_et_medicaments.repository.MedicineRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicineServiceTest {

    @Mock
    private MedicineRepository medicineRepository;

    @InjectMocks
    private MedicineService medicineService;

    private Medicine medicine;

    @BeforeEach
    void setUp() {
        medicine = new Medicine();
        medicine.setId(1L);
        medicine.setCommercialName("Doliprane");
        medicine.setInn("Paracetamol");
    }

    @Test
    void testSaveMedicine_Success() {
        when(medicineRepository.countByNameAndInnJPQL("Doliprane", "Paracetamol"))
                .thenReturn(0L);

        String result = medicineService.saveMedicine(medicine);

        assertNotNull(result);
        assertTrue(result.contains("successfully added"));
        verify(medicineRepository, times(1)).save(medicine);
    }

    @Test
    void testSaveMedicine_ReturnsAlreadyExistsWhenDuplicate() {
        when(medicineRepository.countByNameAndInnJPQL("Doliprane", "Paracetamol"))
                .thenReturn(1L);

        String result = medicineService.saveMedicine(medicine);

        assertTrue(result.contains("already exists"));
        verify(medicineRepository, never()).save(any(Medicine.class));
    }

    @Test
    void testGetAllMedicines() {
        when(medicineRepository.findAll()).thenReturn(List.of(medicine));

        List<Medicine> medicines = medicineService.getAllMedicines();

        assertEquals(1, medicines.size());
    }

    @Test
    void testGetById() {
        when(medicineRepository.findById(1L)).thenReturn(Optional.of(medicine));

        Medicine foundInfo = medicineService.getById(1L);

        assertNotNull(foundInfo);
        assertEquals(1L, foundInfo.getId());
    }
}
