package tn.esprit.ordonnance_et_medicaments.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.ordonnance_et_medicaments.service.MedicineService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicineControllerTest {

    @Mock
    private MedicineService medicineService;

    @InjectMocks
    private MedicineController medicineController;

    @Test
    void suggestionEndpointsReturnListsFromService() {
        when(medicineService.suggestCommercialNames("dol")).thenReturn(List.of("Doliprane"));
        when(medicineService.suggestTherapeuticFamilies("ana")).thenReturn(List.of("Analgesic"));

        assertEquals(List.of("Doliprane"), medicineController.suggestNames("dol").getBody());
        assertEquals(List.of("Analgesic"), medicineController.suggestCategories("ana").getBody());
    }
}
