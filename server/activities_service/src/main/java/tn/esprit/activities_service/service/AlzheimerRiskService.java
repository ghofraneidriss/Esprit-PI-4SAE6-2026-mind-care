package tn.esprit.activities_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tn.esprit.activities_service.entity.GameResult;

import java.util.List;

/**
 * Service de détection automatique du risque d'Alzheimer.
 * 
 * Seuils de risque basés sur le score pondéré (weightedScore 0-100) :
 * - CRITICAL : score < 35 → Risque très élevé, alerte immédiate
 * - HIGH     : score < 50 → Risque élevé, suivi recommandé
 * - MEDIUM   : score < 70 → Risque modéré, surveillance
 * - LOW      : score >= 70 → Risque faible, normal
 */
@Service
public class AlzheimerRiskService {

    @Value("${alzheimer.risk.threshold.high:50}")
    private int highThreshold;

    @Value("${alzheimer.risk.threshold.medium:70}")
    private int mediumThreshold;

    private static final int CRITICAL_THRESHOLD = 35;

    /**
     * Détecte le niveau de risque d'Alzheimer à partir du score pondéré.
     */
    public String detectRiskLevel(double weightedScore) {
        if (weightedScore < CRITICAL_THRESHOLD) {
            return "CRITICAL";
        } else if (weightedScore < highThreshold) {
            return "HIGH";
        } else if (weightedScore < mediumThreshold) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    /**
     * Détecte le risque et met à jour le GameResult.
     */
    public GameResult evaluateRisk(GameResult result) {
        if (result.getWeightedScore() != null) {
            String riskLevel = detectRiskLevel(result.getWeightedScore());
            result.setRiskLevel(riskLevel);
        }
        return result;
    }

    /**
     * Analyse la tendance des résultats d'un patient.
     * Si les 3 derniers résultats montrent une baisse constante → risque aggravé.
     */
    public String analyzeTrend(List<GameResult> recentResults) {
        if (recentResults == null || recentResults.size() < 3) {
            return "INSUFFICIENT_DATA";
        }

        // Prendre les 3 derniers résultats (les plus récents)
        List<GameResult> lastThree = recentResults.subList(0, Math.min(3, recentResults.size()));

        boolean declining = true;
        for (int i = 0; i < lastThree.size() - 1; i++) {
            Double current = lastThree.get(i).getWeightedScore();
            Double next = lastThree.get(i + 1).getWeightedScore();
            if (current == null || next == null || current >= next) {
                declining = false;
                break;
            }
        }

        if (declining) {
            return "DECLINING";
        }

        // Calculer la moyenne des scores récents
        double avgScore = lastThree.stream()
                .filter(r -> r.getWeightedScore() != null)
                .mapToDouble(GameResult::getWeightedScore)
                .average()
                .orElse(50.0);

        return detectRiskLevel(avgScore);
    }

    /**
     * Détermine si une alerte email doit être envoyée.
     * Envoie si le risque est HIGH ou CRITICAL.
     */
    public boolean shouldSendAlert(GameResult result) {
        String risk = result.getRiskLevel();
        return "HIGH".equals(risk) || "CRITICAL".equals(risk);
    }

    /**
     * Génère un message descriptif du risque.
     */
    public String getRiskMessage(GameResult result) {
        String risk = result.getRiskLevel();
        if (risk == null) return "Risque non évalué.";

        return switch (risk) {
            case "CRITICAL" -> String.format(
                    "⚠️ ALERTE CRITIQUE - Score: %.1f/100. " +
                    "Le patient montre des signes très préoccupants de déclin cognitif. " +
                    "Consultation médicale urgente recommandée.",
                    result.getWeightedScore());
            case "HIGH" -> String.format(
                    "🔴 RISQUE ÉLEVÉ - Score: %.1f/100. " +
                    "Performance significativement en dessous de la normale. " +
                    "Un suivi médical est fortement recommandé.",
                    result.getWeightedScore());
            case "MEDIUM" -> String.format(
                    "🟡 RISQUE MODÉRÉ - Score: %.1f/100. " +
                    "Quelques signes de difficulté cognitive détectés. " +
                    "Surveillance régulière conseillée.",
                    result.getWeightedScore());
            case "LOW" -> String.format(
                    "🟢 RISQUE FAIBLE - Score: %.1f/100. " +
                    "Performance dans la norme. Continuer les activités régulièrement.",
                    result.getWeightedScore());
            default -> "Risque non déterminé.";
        };
    }
}
