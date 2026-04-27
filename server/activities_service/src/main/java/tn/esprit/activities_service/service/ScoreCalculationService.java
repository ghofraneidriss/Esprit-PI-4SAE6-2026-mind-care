package tn.esprit.activities_service.service;

import org.springframework.stereotype.Service;
import tn.esprit.activities_service.entity.GameResult;

/**
 * Service de calcul de score intelligent.
 * 
 * Le score pondéré (0-100) est basé sur :
 * - Précision (correctAnswers / totalQuestions) : 50% du poids
 * - Difficulté (multiplicateur) : 25% du poids
 * - Temps de réponse (rapidité) : 25% du poids
 */
@Service
public class ScoreCalculationService {

    // Temps de référence par question selon la difficulté (en secondes)
    private static final double EASY_EXPECTED_TIME = 30.0;
    private static final double MEDIUM_EXPECTED_TIME = 45.0;
    private static final double HARD_EXPECTED_TIME = 60.0;

    /**
     * Calcule le score pondéré intelligent pour un résultat de jeu.
     * Met à jour le GameResult avec le weightedScore et avgResponseTime.
     */
    public GameResult calculateWeightedScore(GameResult result) {
        if (result.getTotalQuestions() == null || result.getTotalQuestions() == 0) {
            result.setWeightedScore(0.0);
            result.setAvgResponseTime(0.0);
            return result;
        }

        // 1. Score de précision (0-100)
        double accuracy = (double) result.getCorrectAnswers() / result.getTotalQuestions() * 100.0;

        // 2. Multiplicateur de difficulté
        double difficultyMultiplier = getDifficultyMultiplier(result.getDifficulty());

        // 3. Score de temps de réponse (0-100)
        double avgTime = (double) result.getTimeSpentSeconds() / result.getTotalQuestions();
        result.setAvgResponseTime(Math.round(avgTime * 100.0) / 100.0);
        double expectedTime = getExpectedTime(result.getDifficulty());
        double timeScore = calculateTimeScore(avgTime, expectedTime);

        // Score pondéré final
        // 50% précision + 25% difficulté bonus + 25% rapidité
        double weightedScore = (accuracy * 0.50)
                + (accuracy * difficultyMultiplier * 0.25)
                + (timeScore * 0.25);

        // Limiter entre 0 et 100
        weightedScore = Math.max(0, Math.min(100, weightedScore));
        result.setWeightedScore(Math.round(weightedScore * 100.0) / 100.0);

        return result;
    }

    /**
     * Retourne le multiplicateur selon la difficulté.
     * Plus la difficulté est élevée, plus le bonus est important.
     */
    private double getDifficultyMultiplier(String difficulty) {
        if (difficulty == null) return 1.0;
        return switch (difficulty.toUpperCase()) {
            case "EASY" -> 0.8;
            case "MEDIUM" -> 1.0;
            case "HARD" -> 1.3;
            default -> 1.0;
        };
    }

    /**
     * Retourne le temps attendu par question selon la difficulté.
     */
    private double getExpectedTime(String difficulty) {
        if (difficulty == null) return MEDIUM_EXPECTED_TIME;
        return switch (difficulty.toUpperCase()) {
            case "EASY" -> EASY_EXPECTED_TIME;
            case "MEDIUM" -> MEDIUM_EXPECTED_TIME;
            case "HARD" -> HARD_EXPECTED_TIME;
            default -> MEDIUM_EXPECTED_TIME;
        };
    }

    /**
     * Calcule un score de temps (0-100).
     * Si le temps moyen est inférieur ou égal au temps attendu → score max.
     * Si le temps dépasse 3x le temps attendu → score 0.
     */
    private double calculateTimeScore(double avgTime, double expectedTime) {
        if (avgTime <= expectedTime) {
            return 100.0;
        }
        double maxTime = expectedTime * 3.0;
        if (avgTime >= maxTime) {
            return 0.0;
        }
        // Décroissance linéaire entre expectedTime et maxTime
        return 100.0 * (1.0 - (avgTime - expectedTime) / (maxTime - expectedTime));
    }
}
