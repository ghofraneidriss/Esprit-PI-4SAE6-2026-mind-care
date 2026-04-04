package tn.esprit.traitement_et_consultation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.traitement_et_consultation.entity.Consultation;
import tn.esprit.traitement_et_consultation.service.ConsultationService;

import java.util.List;

/**
 * Contrôleur REST exposant les endpoints HTTP pour la gestion des consultations
 * médicales.
 * Préfixe de base : /api/consultations
 *
 * Règle métier enforced : un rendez-vous (appointmentId) ne peut être associé
 * qu'à UNE SEULE consultation. Toute violation retourne HTTP 400 Bad Request.
 */
@RestController
@RequestMapping("/api/consultations")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ConsultationController {

    private final ConsultationService consultationService;

    /**
     * Crée une nouvelle consultation.
     * Retourne HTTP 400 si le rendez-vous est déjà lié à une autre consultation.
     *
     * POST /api/consultations
     *
     * @param consultation données de la consultation à créer
     * @return la consultation créée (201) ou un message d'erreur (400)
     */
    @PostMapping
    public ResponseEntity<?> createConsultation(@RequestBody Consultation consultation) {
        try {
            return ResponseEntity.ok(consultationService.saveConsultation(consultation));
        } catch (IllegalStateException e) {
            // Violation de la règle "un rendez-vous → une consultation" → 400 Bad Request
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Récupère la liste complète de toutes les consultations.
     *
     * GET /api/consultations
     *
     * @return liste de toutes les consultations
     */
    @GetMapping
    public ResponseEntity<List<Consultation>> getAll() {
        return ResponseEntity.ok(consultationService.getAllConsultations());
    }

    /**
     * COMMENTAIRE POUR LE REPERAGE (Demande utilisateur) :
     * Point d'entrée pour récupérer les consultations filtrées depuis le backend.
     * L'application (frontend) appelle cette route avec les paramètres pour
     * déléguer la recherche à Spring Boot.
     * 
     * GET /api/consultations/filter
     */
    @GetMapping("/filter")
    public ResponseEntity<List<Consultation>> getFilteredConsultations(
            @RequestParam(name = "stage", required = false) String stage,
            @RequestParam(name = "searchTerm", required = false) String searchTerm) {
        return ResponseEntity.ok(consultationService.getFilteredConsultations(stage, searchTerm));
    }

    /**
     * Récupère une consultation par son identifiant.
     *
     * GET /api/consultations/{id}
     *
     * @param id identifiant de la consultation
     * @return la consultation trouvée, ou 404 si inexistante
     */
    @GetMapping("/{id}")
    public ResponseEntity<Consultation> getById(@PathVariable(name = "id") Long id) {
        return consultationService.getConsultationById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Met à jour une consultation existante.
     * Retourne HTTP 400 si le nouvel appointmentId est déjà utilisé par une autre
     * consultation.
     *
     * PUT /api/consultations/{id}
     *
     * @param id           identifiant de la consultation à modifier
     * @param consultation nouvelles données
     * @return la consultation mise à jour (200) ou un message d'erreur (400)
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable(name = "id") Long id, @RequestBody Consultation consultation) {
        try {
            return ResponseEntity.ok(consultationService.updateConsultation(id, consultation));
        } catch (IllegalStateException e) {
            // Violation de la règle d'unicité → 400 Bad Request
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Supprime une consultation par son identifiant.
     * Une fois supprimée, le rendez-vous associé peut à nouveau être utilisé pour
     * une nouvelle consultation.
     *
     * DELETE /api/consultations/{id}
     *
     * @param id identifiant de la consultation à supprimer
     * @return réponse 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable(name = "id") Long id) {
        consultationService.deleteConsultation(id);
        return ResponseEntity.noContent().build();
    }
}
