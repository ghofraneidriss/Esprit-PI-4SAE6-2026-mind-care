package tn.esprit.movement_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.movement_service.entity.SafeZone;
import tn.esprit.movement_service.service.SafeZoneService;

import java.util.List;

@RestController
@RequestMapping("/safezones")
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
