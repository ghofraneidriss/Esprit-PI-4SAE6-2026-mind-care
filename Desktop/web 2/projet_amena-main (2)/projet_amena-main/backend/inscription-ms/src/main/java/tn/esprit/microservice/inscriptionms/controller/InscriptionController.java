package tn.esprit.microservice.inscriptionms.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.microservice.inscriptionms.dto.InscriptionDetails;
import tn.esprit.microservice.inscriptionms.entity.Inscription;
import tn.esprit.microservice.inscriptionms.service.InscriptionService;

@RestController
@RequestMapping("/inscriptions")
public class InscriptionController {

    private final InscriptionService inscriptionService;

    public InscriptionController(InscriptionService inscriptionService) {
        this.inscriptionService = inscriptionService;
    }

    @GetMapping
    public List<Inscription> getAllInscriptions() {
        return inscriptionService.getAllInscriptions();
    }

    @GetMapping("/{id}")
    public Inscription getInscriptionById(@PathVariable Long id) {
        return inscriptionService.getInscriptionById(id);
    }

    @GetMapping("/{id}/details")
    public InscriptionDetails getInscriptionDetails(@PathVariable Long id) {
        return inscriptionService.getInscriptionDetails(id);
    }

    @PostMapping
    public ResponseEntity<Inscription> createInscription(@RequestBody Inscription inscription) {
        return ResponseEntity.ok(inscriptionService.createInscription(inscription));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Inscription> updateInscription(@PathVariable Long id, @RequestBody Inscription inscription) {
        return ResponseEntity.ok(inscriptionService.updateInscription(id, inscription));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInscription(@PathVariable Long id) {
        inscriptionService.deleteInscription(id);
        return ResponseEntity.noContent().build();
    }
}
