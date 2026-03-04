package tn.esprit.recommendation_service.Controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.recommendation_service.Entities.Recommendation;
import tn.esprit.recommendation_service.Services.RecommendationService;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RecommendationController {

    private final RecommendationService service;

    @PostMapping
    public ResponseEntity<Recommendation> create(@RequestBody Recommendation recommendation) {
        return new ResponseEntity<>(service.create(recommendation), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Recommendation>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Recommendation> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<Recommendation>> getByType(@PathVariable String type) {
        return ResponseEntity.ok(service.getByType(type));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Recommendation>> getByStatus(@PathVariable String status) {
        return ResponseEntity.ok(service.getByStatus(status));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Recommendation> update(@PathVariable Long id, @RequestBody Recommendation recommendation) {
        return ResponseEntity.ok(service.update(id, recommendation));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<Recommendation> approve(@PathVariable Long id) {
        return ResponseEntity.ok(service.approve(id));
    }
}
