package tn.esprit.activities_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.activities_service.entity.GameResult;
import tn.esprit.activities_service.service.GameResultService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/game-results")
@Tag(name = "Game Results Management", description = "API pour la gestion des résultats des jeux")
public class GameResultController {

    @Autowired
    private GameResultService gameResultService;

    @Operation(summary = "Récupérer tous les résultats", description = "Retourne la liste de tous les résultats des jeux")
    @GetMapping
    public ResponseEntity<List<GameResult>> getAllGameResults() {
        List<GameResult> results = gameResultService.getAllGameResults();
        return ResponseEntity.ok(results);
    }

    @Operation(summary = "Récupérer un résultat par ID", description = "Retourne un résultat spécifique basé sur son ID")
    @GetMapping("/{id}")
    public ResponseEntity<GameResult> getGameResultById(
            @Parameter(description = "ID du résultat à récupérer") @PathVariable("id") Long id) {
        Optional<GameResult> result = gameResultService.getGameResultById(id);
        return result.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Enregistrer un nouveau résultat", description = "Enregistre le résultat d'un jeu terminé")
    @PostMapping
    public ResponseEntity<?> createGameResult(@RequestBody GameResult gameResult) {
        try {
            GameResult createdResult = gameResultService.createGameResult(gameResult);
            return ResponseEntity.ok(createdResult);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("QUIZ_LIMIT_REACHED")) {
                return ResponseEntity.status(403).body(Map.of(
                    "error", "QUIZ_LIMIT_REACHED",
                    "message", "Le patient a atteint sa limite de quiz autorisés."
                ));
            }
            if (e.getMessage() != null && e.getMessage().startsWith("FIRST_ATTEMPT_EXISTS")) {
                return ResponseEntity.status(409).body(Map.of(
                    "error", "FIRST_ATTEMPT_EXISTS",
                    "message", "Un résultat existe déjà pour cette activité ; la première tentative est conservée pour le suivi médical."
                ));
            }
            throw e;
        }
    }

    @Operation(summary = "Mettre à jour un résultat", description = "Met à jour un résultat existant")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateGameResult(
            @Parameter(description = "ID du résultat à mettre à jour") @PathVariable("id") Long id,
            @RequestBody GameResult gameResult) {
        try {
            GameResult updatedResult = gameResultService.updateGameResult(id, gameResult);
            if (updatedResult != null) {
                return ResponseEntity.ok(updatedResult);
            }
            return ResponseEntity.notFound().build();
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("GAME_RESULT_IMMUTABLE")) {
                return ResponseEntity.status(403).body(Map.of(
                    "error", "GAME_RESULT_IMMUTABLE",
                    "message", "Les résultats ne peuvent pas être modifiés (première impression conservée)."
                ));
            }
            throw e;
        }
    }

    @Operation(summary = "Supprimer un résultat", description = "Supprime un résultat existant")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGameResult(
            @Parameter(description = "ID du résultat à supprimer") @PathVariable("id") Long id) {
        boolean deleted = gameResultService.deleteGameResult(id);
        if (deleted) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(summary = "Récupérer les résultats d'un patient", description = "Retourne tous les résultats d'un patient spécifique")
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<GameResult>> getResultsByPatientId(
            @Parameter(description = "ID du patient") @PathVariable("patientId") Long patientId) {
        List<GameResult> results = gameResultService.getResultsByPatientId(patientId);
        return ResponseEntity.ok(results);
    }

    @Operation(summary = "Récupérer les résultats d'une activité", description = "Retourne tous les résultats pour une activité spécifique")
    @GetMapping("/activity/{activityType}/{activityId}")
    public ResponseEntity<List<GameResult>> getResultsByActivity(
            @Parameter(description = "Type d'activité") @PathVariable("activityType") String activityType,
            @Parameter(description = "ID de l'activité") @PathVariable("activityId") Long activityId) {
        List<GameResult> results = gameResultService.getResultsByActivity(activityType, activityId);
        return ResponseEntity.ok(results);
    }

    @Operation(summary = "Récupérer les statistiques d'un patient", description = "Retourne les statistiques d'un patient pour une activité")
    @GetMapping("/patient/{patientId}/activity/{activityType}/stats")
    public ResponseEntity<Object> getPatientStats(
            @Parameter(description = "ID du patient") @PathVariable("patientId") Long patientId,
            @Parameter(description = "Type d'activité") @PathVariable("activityType") String activityType) {
        Long count = gameResultService.getResultsCountByPatientAndActivity(patientId, activityType);
        Double averageScore = gameResultService.getAverageScoreByPatientAndActivity(patientId, activityType);

        return ResponseEntity.ok(Map.of(
                "totalGames", count,
                "averageScore", averageScore != null ? averageScore : 0.0));
    }

    @Operation(summary = "Analyser la tendance d'un patient", description = "Analyse les derniers résultats pour détecter une tendance de risque Alzheimer")
    @GetMapping("/patient/{patientId}/risk-analysis")
    public ResponseEntity<Object> analyzePatientRisk(
            @Parameter(description = "ID du patient") @PathVariable("patientId") Long patientId) {
        String trend = gameResultService.analyzePatientTrend(patientId);
        List<GameResult> recentResults = gameResultService.getResultsByPatientId(patientId);

        return ResponseEntity.ok(Map.of(
                "patientId", patientId,
                "trend", trend,
                "totalResults", recentResults.size(),
                "results", recentResults
        ));
    }

    @Operation(summary = "Envoyer une alerte email manuellement", description = "Envoie un email d'alerte pour un résultat spécifique, quel que soit le niveau de risque")
    @PostMapping("/{id}/send-alert")
    public ResponseEntity<Object> sendAlertEmail(
            @Parameter(description = "ID du résultat") @PathVariable("id") Long id) {
        Optional<GameResult> result = gameResultService.getGameResultById(id);
        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        boolean sent = gameResultService.sendManualAlert(result.get());
        return ResponseEntity.ok(Map.of(
                "sent", sent,
                "message", sent ? "Email envoyé avec succès" : "Erreur lors de l'envoi de l'email"
        ));
    }
}
