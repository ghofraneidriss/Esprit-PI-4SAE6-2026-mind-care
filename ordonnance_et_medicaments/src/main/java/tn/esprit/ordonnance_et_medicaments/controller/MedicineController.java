package tn.esprit.ordonnance_et_medicaments.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.ordonnance_et_medicaments.service.MedicineService;

import java.util.List;

@RestController
@RequestMapping("/api/medicaments")
@RequiredArgsConstructor
@CrossOrigin("*")
public class MedicineController {

    private final MedicineService medicineService;

    @GetMapping("/suggest-names")
    public ResponseEntity<List<String>> suggestNames(@RequestParam("query") String query) {
        return ResponseEntity.ok(medicineService.suggestCommercialNames(query));
    }

    @GetMapping("/suggest-categories")
    public ResponseEntity<List<String>> suggestCategories(@RequestParam("query") String query) {
        return ResponseEntity.ok(medicineService.suggestTherapeuticFamilies(query));
    }
}
