package tn.esprit.localization_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.localization_service.entity.SafeZone;
import tn.esprit.localization_service.service.SafeZoneService;

import java.util.List;

@RestController
@RequestMapping("/api/safezones")
@RequiredArgsConstructor
public class SafeZoneController {

    private final SafeZoneService safeZoneService;

    @PostMapping
    public SafeZone createSafeZone(@RequestBody SafeZone safeZone) {
        return safeZoneService.createSafeZone(safeZone);
    }

    @GetMapping
    public List<SafeZone> getAllSafeZones() {
        return safeZoneService.getAllSafeZones();
    }

    @GetMapping("/{id}")
    public ResponseEntity<SafeZone> getSafeZoneById(@PathVariable Long id) {
        return safeZoneService.getSafeZoneById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/patient/{patientId}")
    public List<SafeZone> getSafeZonesByPatientId(@PathVariable Long patientId) {
        return safeZoneService.getSafeZonesByPatientId(patientId);
    }

    @GetMapping("/patient-ids")
    public List<Long> getPatientIdsWithSafeZones() {
        return safeZoneService.getPatientIdsWithSafeZones();
    }

    @PutMapping("/{id}")
    public ResponseEntity<SafeZone> updateSafeZone(@PathVariable Long id, @RequestBody SafeZone safeZoneDetails) {
        try {
            return ResponseEntity.ok(safeZoneService.updateSafeZone(id, safeZoneDetails));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSafeZone(@PathVariable Long id) {
        safeZoneService.deleteSafeZone(id);
        return ResponseEntity.noContent().build();
    }
}
