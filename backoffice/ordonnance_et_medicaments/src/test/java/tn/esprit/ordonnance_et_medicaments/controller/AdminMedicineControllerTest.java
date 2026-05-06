package tn.esprit.ordonnance_et_medicaments.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.ordonnance_et_medicaments.entities.Medicine;
import tn.esprit.ordonnance_et_medicaments.service.MedicineService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminMedicineControllerTest {

    @Mock
    private MedicineService medicineService;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private AdminMedicineController adminMedicineController;

    @Test
    void adminEndpointsDelegateToMedicineService() {
        Medicine medicine = Medicine.builder().id(1L).commercialName("Doliprane").build();
        when(medicineService.getAllMedicines()).thenReturn(List.of(medicine));
        when(medicineService.getById(1L)).thenReturn(medicine);
        when(medicineService.saveMedicine(medicine)).thenReturn("saved");
        when(medicineService.updateMedicine(1L, medicine)).thenReturn(medicine);
        when(medicineService.importMedicines(multipartFile)).thenReturn("imported");

        assertEquals(1, adminMedicineController.getAll().getBody().size());
        assertEquals(HttpStatus.OK, adminMedicineController.getById(1L).getStatusCode());
        assertEquals("saved", adminMedicineController.create(medicine).getBody().get("message"));
        assertEquals(HttpStatus.OK, adminMedicineController.update(1L, medicine).getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, adminMedicineController.delete(1L).getStatusCode());
        verify(medicineService).deleteMedicine(1L);
        assertEquals("imported", adminMedicineController.importMedicines(multipartFile).getBody());
    }
}
