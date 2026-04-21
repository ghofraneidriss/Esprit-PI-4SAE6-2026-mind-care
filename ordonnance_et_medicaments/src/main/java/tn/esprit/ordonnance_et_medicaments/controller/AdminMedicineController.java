package tn.esprit.ordonnance_et_medicaments.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.esprit.ordonnance_et_medicaments.entities.Medicine;
import tn.esprit.ordonnance_et_medicaments.service.MedicineService;

import java.util.List;

/**
 * Contrôleur destiné à l'administrateur pour la gestion du référentiel des médicaments.
 */
@RestController
@RequestMapping("/api/admin/medicines")
@RequiredArgsConstructor
@CrossOrigin("*")
public class AdminMedicineController {

    private final MedicineService medicineService;

    // Récupérer tout le catalogue
    @GetMapping
    public ResponseEntity<List<Medicine>> getAll() {
        return ResponseEntity.ok(medicineService.getAllMedicines());
    }

    // Récupérer un médicament par son identifiant
    @GetMapping("/{id}")
    public ResponseEntity<Medicine> getById(@PathVariable(name = "id") Long id) {
        return ResponseEntity.ok(medicineService.getById(id));
    }

    // Création d'un nouveau médicament avec message JSON de statut en retour
    @PostMapping
    public ResponseEntity<java.util.Map<String, String>> create(@RequestBody Medicine medicine) {
        String result = medicineService.saveMedicine(medicine);
        return ResponseEntity.ok(java.util.Collections.singletonMap("message", result));
    }

    // Mise à jour d'un médicament existant
    @PutMapping("/{id}")
    public ResponseEntity<Medicine> update(@PathVariable(name = "id") Long id, @RequestBody Medicine medicine) {
        return ResponseEntity.ok(medicineService.updateMedicine(id, medicine));
    }

    // Suppression d'un médicament
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable(name = "id") Long id) {
        medicineService.deleteMedicine(id);
        return ResponseEntity.noContent().build();
    }

    // Importation massive de médicaments depuis Excel ou CSV
    @PostMapping("/import")
    public ResponseEntity<String> importMedicines(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(medicineService.importMedicines(file));
    }
}
