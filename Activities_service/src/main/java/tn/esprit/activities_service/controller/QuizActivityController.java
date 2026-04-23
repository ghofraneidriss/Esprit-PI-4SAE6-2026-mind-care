package tn.esprit.activities_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.activities_service.entity.QuizActivity;
import tn.esprit.activities_service.service.QuizActivityService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/quiz")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
@Tag(name = "Quiz Activities Management", description = "API pour la gestion des activités quiz")
public class QuizActivityController {

    @Autowired
    private QuizActivityService quizActivityService;

    @Operation(summary = "Récupérer tous les quiz", description = "Retourne la liste de tous les quiz disponibles")
    @GetMapping
    public ResponseEntity<List<QuizActivity>> getAllQuizActivities() {
        List<QuizActivity> quizzes = quizActivityService.getAllQuizActivities();
        return ResponseEntity.ok(quizzes);
    }

    @Operation(summary = "Récupérer un quiz par ID", description = "Retourne un quiz spécifique basé sur son ID")
    @GetMapping("/{id}")
    public ResponseEntity<QuizActivity> getQuizActivityById(
            @Parameter(description = "ID du quiz à récupérer") @PathVariable Long id) {
        Optional<QuizActivity> quiz = quizActivityService.getQuizActivityById(id);
        return quiz.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Créer un nouveau quiz", description = "Crée une nouvelle activité quiz")
    @PostMapping
    public ResponseEntity<QuizActivity> createQuizActivity(@RequestBody QuizActivity quizActivity) {
        QuizActivity createdQuiz = quizActivityService.createQuizActivity(quizActivity);
        return ResponseEntity.ok(createdQuiz);
    }

    @Operation(summary = "Mettre à jour un quiz", description = "Met à jour un quiz existant")
    @PutMapping("/{id}")
    public ResponseEntity<QuizActivity> updateQuizActivity(
            @Parameter(description = "ID du quiz à mettre à jour") @PathVariable Long id,
            @RequestBody QuizActivity quizActivity) {
        QuizActivity updatedQuiz = quizActivityService.updateQuizActivity(id, quizActivity);
        if (updatedQuiz != null) {
            return ResponseEntity.ok(updatedQuiz);
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(summary = "Supprimer un quiz", description = "Supprime un quiz existant")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuizActivity(
            @Parameter(description = "ID du quiz à supprimer") @PathVariable Long id) {
        boolean deleted = quizActivityService.deleteQuizActivity(id);
        if (deleted) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(summary = "Rechercher des quiz par thème", description = "Retourne les quiz d'un thème spécifique")
    @GetMapping("/theme/{theme}")
    public ResponseEntity<List<QuizActivity>> getQuizActivitiesByTheme(
            @Parameter(description = "Thème des quiz") @PathVariable String theme) {
        List<QuizActivity> quizzes = quizActivityService.getQuizActivitiesByTheme(theme);
        return ResponseEntity.ok(quizzes);
    }

    @Operation(summary = "Rechercher des quiz par difficulté", description = "Retourne les quiz d'une difficulté spécifique")
    @GetMapping("/difficulty/{difficulty}")
    public ResponseEntity<List<QuizActivity>> getQuizActivitiesByDifficulty(
            @Parameter(description = "Difficulté des quiz") @PathVariable String difficulty) {
        List<QuizActivity> quizzes = quizActivityService.getQuizActivitiesByDifficulty(difficulty);
        return ResponseEntity.ok(quizzes);
    }

    @Operation(summary = "Rechercher des quiz par titre", description = "Recherche des quiz contenant le titre spécifié")
    @GetMapping("/search")
    public ResponseEntity<List<QuizActivity>> searchQuizActivities(
            @Parameter(description = "Terme de recherche") @RequestParam String title) {
        List<QuizActivity> quizzes = quizActivityService.searchQuizActivities(title);
        return ResponseEntity.ok(quizzes);
    }

    @Operation(summary = "Endpoint de test", description = "Test simple")
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Backend fonctionne !");
    }
}
