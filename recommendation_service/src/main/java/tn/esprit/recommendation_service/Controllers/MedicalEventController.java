package tn.esprit.recommendation_service.Controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.recommendation_service.Entities.MedicalEvent;
import tn.esprit.recommendation_service.Services.MedicalEventService;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MedicalEventController {

    private final MedicalEventService service;

    @PostMapping
    public ResponseEntity<MedicalEvent> create(@RequestBody MedicalEvent event) {
        return new ResponseEntity<>(service.addEvent(event), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<MedicalEvent>> getAll() {
        return ResponseEntity.ok(service.getAllEvents());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicalEvent> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getEventById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }
}
