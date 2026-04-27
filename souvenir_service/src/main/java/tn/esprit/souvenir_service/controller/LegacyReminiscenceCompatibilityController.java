package tn.esprit.souvenir_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reminiscence-activities")
@CrossOrigin(origins = "*")
public class LegacyReminiscenceCompatibilityController {

    @GetMapping("/entree/{entreeId}")
    public ResponseEntity<List<Object>> getByEntree(@PathVariable Long entreeId) {
        return ResponseEntity.ok(Collections.emptyList());
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<Object>> getByPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(Collections.emptyList());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Object>> getByStatus(@PathVariable String status) {
        return ResponseEntity.ok(Collections.emptyList());
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<Object>> getByType(@PathVariable String type) {
        return ResponseEntity.ok(Collections.emptyList());
    }

    @GetMapping("/{activityId}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long activityId) {
        return ResponseEntity.ok(Map.of(
                "id", activityId,
                "message", "Legacy endpoint kept for compatibility. Activity feature is removed."
        ));
    }

    @GetMapping("/patient/{patientId}/average-score")
    public ResponseEntity<Map<String, Object>> getAverageScore(@PathVariable Long patientId) {
        return ResponseEntity.ok(Map.of("patientId", patientId, "averageScore", 0));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody(required = false) Map<String, Object> payload) {
        return ResponseEntity.ok(Map.of(
                "message", "Legacy endpoint kept for compatibility. Activity feature is removed."
        ));
    }

    @PostMapping("/generate/{entreeId}")
    public ResponseEntity<List<Object>> generate(@PathVariable Long entreeId) {
        return ResponseEntity.ok(Collections.emptyList());
    }

    @PatchMapping("/{activityId}/complete")
    public ResponseEntity<Map<String, Object>> complete(
            @PathVariable Long activityId,
            @RequestBody(required = false) Map<String, Object> payload
    ) {
        return ResponseEntity.ok(Map.of(
                "id", activityId,
                "message", "Legacy endpoint kept for compatibility. Activity feature is removed."
        ));
    }

    @DeleteMapping("/{activityId}")
    public ResponseEntity<Void> delete(@PathVariable Long activityId) {
        return ResponseEntity.noContent().build();
    }
}
