package tn.esprit.activities_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.activities_service.entity.GameResult;
import tn.esprit.activities_service.repository.GameResultRepository;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class GameResultService {

    private static final Logger log = LoggerFactory.getLogger(GameResultService.class);

    @Autowired
    private GameResultRepository gameResultRepository;

    @Autowired
    private ScoreCalculationService scoreCalculationService;

    @Autowired
    private AlzheimerRiskService riskService;

    @Autowired
    private EmailAlertService emailAlertService;

    @Autowired
    private QuizLimitService quizLimitService;

    public List<GameResult> getAllGameResults() {
        return gameResultRepository.findAll();
    }

    public Optional<GameResult> getGameResultById(Long id) {
        return gameResultRepository.findById(id);
    }

    /**
     * Crée un résultat avec calcul intelligent du score et détection de risque.
     * Envoie automatiquement un email d'alerte si le risque est élevé.
     * Vérifie la limite de quiz du patient avant de créer le résultat.
     */
    public GameResult createGameResult(GameResult gameResult) {
        String type = gameResult.getActivityType() != null ? gameResult.getActivityType().trim().toUpperCase() : "";
        gameResult.setActivityType(type.isEmpty() ? gameResult.getActivityType() : type);

        // 0. Quiz / photo : une seule tentative enregistrée par activité (première impression pour le médecin)
        if ("QUIZ".equals(type) || "PHOTO".equals(type)) {
            if (gameResultRepository.existsByPatientAndActivityCaseInsensitive(
                    gameResult.getPatientId(), type, gameResult.getActivityId())) {
                throw new RuntimeException(
                        "FIRST_ATTEMPT_EXISTS: Un résultat existe déjà pour cette activité ; la première tentative ne peut pas être remplacée.");
            }
        }

        // 1. Vérifier la limite de quiz du patient
        if (!quizLimitService.canPlay(gameResult.getPatientId())) {
            throw new RuntimeException("QUIZ_LIMIT_REACHED: Le patient a atteint sa limite de quiz autorisés.");
        }

        // 2. Calcul du score pondéré intelligent
        scoreCalculationService.calculateWeightedScore(gameResult);

        // 3. Détection du risque d'Alzheimer
        riskService.evaluateRisk(gameResult);

        // 4. Sauvegarder d'abord
        GameResult saved = gameResultRepository.save(gameResult);

        // 5. Envoi d'alerte email si nécessaire (async-safe)
        try {
            emailAlertService.sendAlertIfNeeded(saved);
            if (Boolean.TRUE.equals(saved.getAlertSent())) {
                gameResultRepository.save(saved); // Mise à jour du flag alertSent
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi d'alerte pour le résultat {}: {}", saved.getId(), e.getMessage());
        }

        log.info("Résultat créé: patient={}, score={}/{}, weighted={}, risk={}",
                saved.getPatientId(), saved.getScore(), saved.getMaxScore(),
                saved.getWeightedScore(), saved.getRiskLevel());

        return saved;
    }

    public GameResult updateGameResult(Long id, GameResult gameResult) {
        throw new RuntimeException("GAME_RESULT_IMMUTABLE: Les résultats d'activité ne peuvent pas être modifiés (première impression conservée).");
    }

    public boolean deleteGameResult(Long id) {
        if (gameResultRepository.existsById(id)) {
            gameResultRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<GameResult> getResultsByPatientId(Long patientId) {
        return gameResultRepository.findByPatientId(patientId);
    }

    public List<GameResult> getResultsByActivity(String activityType, Long activityId) {
        return gameResultRepository.findByActivityTypeAndActivityId(activityType, activityId);
    }

    public List<GameResult> getResultsByPatientAndActivity(Long patientId, String activityType) {
        return gameResultRepository.findByPatientIdAndActivityType(patientId, activityType);
    }

    public Long getResultsCountByPatientAndActivity(Long patientId, String activityType) {
        return gameResultRepository.countByPatientIdAndActivityType(patientId, activityType);
    }

    public Double getAverageScoreByPatientAndActivity(Long patientId, String activityType) {
        return gameResultRepository.getAverageScoreByPatientAndActivityType(patientId, activityType);
    }

    /**
     * Analyse la tendance d'un patient et retourne le risque global.
     */
    public String analyzePatientTrend(Long patientId) {
        List<GameResult> results = gameResultRepository.findByPatientIdOrderByCompletedAtDesc(patientId);
        return riskService.analyzeTrend(results);
    }

    /**
     * Envoie manuellement une alerte email pour un résultat.
     */
    public boolean sendManualAlert(GameResult result) {
        try {
            boolean sent = emailAlertService.sendAlertForResult(result);
            if (sent) {
                gameResultRepository.save(result);
            }
            return sent;
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi manuel d'alerte pour le résultat {}: {}", result.getId(), e.getMessage());
            return false;
        }
    }
}
