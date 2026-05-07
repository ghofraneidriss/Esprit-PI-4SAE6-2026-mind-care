package tn.esprit.activities_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.activities_service.entity.QuizLimit;
import tn.esprit.activities_service.service.QuizLimitService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quiz-limits")
@Tag(name = "Quiz Limits", description = "API pour gérer les limites de quiz par patient")
public class QuizLimitController {

    @Autowired
    private QuizLimitService quizLimitService;

    @Operation(summary = "Obtenir toutes les limites", description = "Retourne toutes les limites de quiz configurées")
    @GetMapping
    public ResponseEntity<List<QuizLimit>> getAllLimits() {
        return ResponseEntity.ok(quizLimitService.getAllLimits());
    }

    @Operation(summary = "Obtenir la limite d'un patient", description = "Retourne la limite de quiz pour un patient spécifique")
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<Object> getLimitForPatient(
            @Parameter(description = "ID du patient") @PathVariable("patientId") Long patientId) {
        return ResponseEntity.ok(quizLimitService.getLimitForPatient(patientId)
                .map(l -> (Object) l)
                .orElse(Map.of("message", "Aucune limite définie", "patientId", patientId)));
    }

    @Operation(summary = "Obtenir le statut de limite d'un patient",
               description = "Retourne le statut complet: limite, quiz complétés, restants, peut jouer")
    @GetMapping("/patient/{patientId}/status")
    public ResponseEntity<QuizLimitService.QuizLimitStatus> getStatus(
            @Parameter(description = "ID du patient") @PathVariable("patientId") Long patientId) {
        return ResponseEntity.ok(quizLimitService.getStatus(patientId));
    }

    @Operation(summary = "Définir la limite de quiz pour un patient",
               description = "Crée ou met à jour la limite maximale de quiz pour un patient")
    @PostMapping
    public ResponseEntity<QuizLimit> setLimit(@RequestBody Map<String, Object> body) {
        Long patientId = ((Number) body.get("patientId")).longValue();
        Integer maxQuizzes = ((Number) body.get("maxQuizzes")).intValue();
        Long setBy = body.get("setBy") != null ? ((Number) body.get("setBy")).longValue() : null;
        String setByName = (String) body.getOrDefault("setByName", "");

        QuizLimit limit = quizLimitService.setLimit(patientId, maxQuizzes, setBy, setByName);
        return ResponseEntity.ok(limit);
    }

    @Operation(summary = "Supprimer la limite d'un patient",
               description = "Supprime la limite de quiz pour un patient (revient à illimité)")
    @DeleteMapping("/patient/{patientId}")
    public ResponseEntity<Object> removeLimit(
            @Parameter(description = "ID du patient") @PathVariable("patientId") Long patientId) {
        quizLimitService.removeLimit(patientId);
        return ResponseEntity.ok(Map.of("message", "Limite supprimée", "patientId", patientId));
    }
}
