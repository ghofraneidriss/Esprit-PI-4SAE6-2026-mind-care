package tn.esprit.activities_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/quizzes")
@Tag(name = "Quiz Management", description = "API pour la gestion des quiz cognitifs")
public class QuizController {

    private final Map<Long, Map<String, Object>> quizzes = new HashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    @Operation(summary = "Récupérer tous les quiz", description = "Retourne la liste de tous les quiz disponibles")
    @GetMapping
    public List<Map<String, Object>> getAllQuizzes() {
        return new ArrayList<>(quizzes.values());
    }

    @Operation(summary = "Récupérer un quiz par ID", description = "Retourne un quiz spécifique basé sur son ID")
    @GetMapping("/{id}")
    public Map<String, Object> getQuizById(
            @Parameter(description = "ID du quiz à récupérer") @PathVariable("id") Long id) {
        Map<String, Object> quiz = quizzes.get(id);
        if (quiz != null) {
            return quiz;
        }
        return Map.of("error", "Quiz non trouvé");
    }

    @Operation(summary = "Créer un nouveau quiz", description = "Crée un nouveau quiz cognitif")
    @PostMapping
    public Map<String, Object> createQuiz(@RequestBody Map<String, Object> quizData) {
        Long id = idCounter.getAndIncrement();
        Map<String, Object> quiz = new HashMap<>(quizData);
        quiz.put("id", id);
        quiz.put("createdAt", System.currentTimeMillis());
        quizzes.put(id, quiz);
        return quiz;
    }

    @Operation(summary = "Mettre à jour un quiz", description = "Met à jour un quiz existant")
    @PutMapping("/{id}")
    public Map<String, Object> updateQuiz(
            @Parameter(description = "ID du quiz à mettre à jour") @PathVariable("id") Long id,
            @RequestBody Map<String, Object> quizData) {
        Map<String, Object> existingQuiz = quizzes.get(id);
        if (existingQuiz != null) {
            existingQuiz.putAll(quizData);
            existingQuiz.put("id", id);
            return existingQuiz;
        }
        return Map.of("error", "Quiz non trouvé");
    }

    @Operation(summary = "Supprimer un quiz", description = "Supprime un quiz existant")
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteQuiz(
            @Parameter(description = "ID du quiz à supprimer") @PathVariable("id") Long id) {
        Map<String, Object> removed = quizzes.remove(id);
        if (removed != null) {
            return Map.of("message", "Quiz supprimé avec succès");
        }
        return Map.of("error", "Quiz non trouvé");
    }
}
