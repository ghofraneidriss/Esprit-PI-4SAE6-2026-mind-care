package tn.esprit.activities_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.activities_service.entity.GameResult;
import tn.esprit.activities_service.entity.QuizActivity;
import tn.esprit.activities_service.repository.GameResultRepository;
import tn.esprit.activities_service.repository.QuizActivityRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PerformanceAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(PerformanceAnalysisService.class);

    private static final int MAX_SUGGESTED_QUIZZES_PER_THEME = 5;

    @Autowired
    private GameResultRepository gameResultRepository;

    @Autowired
    private QuizActivityRepository quizActivityRepository;

    private static final Map<String, String> THEME_LABELS = new LinkedHashMap<>() {{
        put("MEMORY", "Mémoire");
        put("CONCENTRATION", "Concentration");
        put("LOGIC", "Logique");
        put("LANGUAGE", "Langage");
        put("ORIENTATION", "Orientation");
        put("VISUAL", "Reconnaissance Visuelle");
        put("CALCULATION", "Calcul");
        put("ATTENTION", "Attention");
    }};

    private static final Map<String, String> THEME_ICONS = new LinkedHashMap<>() {{
        put("MEMORY", "🧠");
        put("CONCENTRATION", "🎯");
        put("LOGIC", "🧩");
        put("LANGUAGE", "📝");
        put("ORIENTATION", "🧭");
        put("VISUAL", "👁️");
        put("CALCULATION", "🔢");
        put("ATTENTION", "👀");
    }};

    /**
     * Analyse performance for a single patient across all themes.
     */
    public PatientPerformance analyzePatient(Long patientId) {
        List<GameResult> results = gameResultRepository.findByPatientIdOrderByCompletedAtDesc(patientId);
        Map<Long, QuizActivity> quizById = loadQuizCacheForResults(results);
        return analyzePatientFromResults(patientId, results, quizById);
    }

    /**
     * Analyse performance for ALL patients (admin/doctor view).
     * Optimisé : un seul chargement des résultats, un seul cache quiz (IDs référencés),
     * pas de re-requête patient ni de findAll() sur quiz_activity.
     */
    public List<PatientPerformance> analyzeAllPatients() {
        List<GameResult> allResults = gameResultRepository.findAll();
        if (allResults.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, QuizActivity> quizById = loadQuizCacheForResults(allResults);

        Map<Long, List<GameResult>> byPatient = allResults.stream()
                .collect(Collectors.groupingBy(GameResult::getPatientId));

        List<PatientPerformance> performances = new ArrayList<>(byPatient.size());
        for (Map.Entry<Long, List<GameResult>> e : byPatient.entrySet()) {
            List<GameResult> patientResults = e.getValue();
            patientResults.sort((a, b) -> {
                if (a.getCompletedAt() == null || b.getCompletedAt() == null) {
                    return 0;
                }
                return b.getCompletedAt().compareTo(a.getCompletedAt());
            });
            performances.add(analyzePatientFromResults(e.getKey(), patientResults, quizById));
        }

        performances.sort(Comparator.comparingDouble(p -> p.globalScore));
        return performances;
    }

    private PatientPerformance analyzePatientFromResults(Long patientId,
                                                         List<GameResult> results,
                                                         Map<Long, QuizActivity> quizCache) {
        PatientPerformance perf = new PatientPerformance();
        perf.patientId = patientId;
        perf.totalQuizzes = results.size();

        if (results.isEmpty()) {
            perf.patientName = "";
            perf.themeScores = Collections.emptyList();
            perf.recommendations = Collections.emptyList();
            perf.globalScore = 0;
            return perf;
        }

        perf.patientName = results.get(0).getPatientName();

        Map<String, List<GameResult>> byTheme = new LinkedHashMap<>();
        for (GameResult r : results) {
            String theme = resolveTheme(r, quizCache);
            byTheme.computeIfAbsent(theme, k -> new ArrayList<>()).add(r);
        }

        List<ThemeScore> themeScores = new ArrayList<>();
        double totalWeighted = 0;
        int totalCount = 0;

        for (Map.Entry<String, List<GameResult>> entry : byTheme.entrySet()) {
            String theme = entry.getKey();
            List<GameResult> themeResults = entry.getValue();

            ThemeScore ts = new ThemeScore();
            ts.theme = theme;
            ts.label = THEME_LABELS.getOrDefault(theme, theme);
            ts.icon = THEME_ICONS.getOrDefault(theme, "📊");
            ts.quizCount = themeResults.size();

            double avgPercent = themeResults.stream()
                    .filter(r -> r.getTotalQuestions() != null && r.getTotalQuestions() > 0)
                    .mapToDouble(r -> (double) r.getCorrectAnswers() / r.getTotalQuestions() * 100)
                    .average()
                    .orElse(0);

            ts.avgScore = Math.round(avgPercent * 10) / 10.0;

            ts.avgWeightedScore = themeResults.stream()
                    .filter(r -> r.getWeightedScore() != null)
                    .mapToDouble(GameResult::getWeightedScore)
                    .average()
                    .orElse(0);
            ts.avgWeightedScore = Math.round(ts.avgWeightedScore * 10) / 10.0;

            ts.avgResponseTime = themeResults.stream()
                    .filter(r -> r.getAvgResponseTime() != null && r.getAvgResponseTime() > 0)
                    .mapToDouble(GameResult::getAvgResponseTime)
                    .average()
                    .orElse(0);
            ts.avgResponseTime = Math.round(ts.avgResponseTime * 10) / 10.0;

            if (themeResults.size() >= 4) {
                double recentAvg = themeResults.subList(0, Math.min(3, themeResults.size())).stream()
                        .filter(r -> r.getTotalQuestions() != null && r.getTotalQuestions() > 0)
                        .mapToDouble(r -> (double) r.getCorrectAnswers() / r.getTotalQuestions() * 100)
                        .average().orElse(avgPercent);
                double olderAvg = themeResults.subList(Math.max(0, themeResults.size() - 3), themeResults.size()).stream()
                        .filter(r -> r.getTotalQuestions() != null && r.getTotalQuestions() > 0)
                        .mapToDouble(r -> (double) r.getCorrectAnswers() / r.getTotalQuestions() * 100)
                        .average().orElse(avgPercent);
                double diff = recentAvg - olderAvg;
                ts.trend = diff > 5 ? "UP" : (diff < -5 ? "DOWN" : "STABLE");
            } else {
                ts.trend = "INSUFFICIENT";
            }

            ts.level = classifyLevel(ts.avgScore);

            themeScores.add(ts);
            totalWeighted += ts.avgScore * ts.quizCount;
            totalCount += ts.quizCount;
        }

        themeScores.sort(Comparator.comparingDouble(a -> a.avgScore));
        perf.themeScores = themeScores;
        perf.globalScore = totalCount > 0 ? Math.round(totalWeighted / totalCount * 10) / 10.0 : 0;

        perf.recommendations = generateRecommendations(perf.patientName, themeScores);

        return perf;
    }

    private String resolveTheme(GameResult result, Map<Long, QuizActivity> quizCache) {
        if ("PHOTO".equalsIgnoreCase(result.getActivityType()) ||
            "IMAGE_RECOGNITION".equalsIgnoreCase(result.getActivityType())) {
            return "VISUAL";
        }

        QuizActivity quiz = quizCache.get(result.getActivityId());
        if (quiz != null && quiz.getTheme() != null && !quiz.getTheme().isEmpty()) {
            return quiz.getTheme().toUpperCase();
        }

        String title = result.getActivityTitle() != null ? result.getActivityTitle().toLowerCase() : "";
        if (title.contains("mémoire") || title.contains("memoire") || title.contains("memory")) return "MEMORY";
        if (title.contains("concentration")) return "CONCENTRATION";
        if (title.contains("logique") || title.contains("logic")) return "LOGIC";
        if (title.contains("langage") || title.contains("language")) return "LANGUAGE";
        if (title.contains("orientation")) return "ORIENTATION";
        if (title.contains("calcul") || title.contains("math")) return "CALCULATION";
        if (title.contains("attention")) return "ATTENTION";

        return "MEMORY";
    }

    /**
     * Charge uniquement les quiz référencés par les résultats (pas tout le catalogue).
     */
    private Map<Long, QuizActivity> loadQuizCacheForResults(List<GameResult> results) {
        if (results == null || results.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> ids = results.stream()
                .filter(r -> !isPhotoLikeActivity(r))
                .map(GameResult::getActivityId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        List<QuizActivity> quizzes = quizActivityRepository.findAllById(ids);
        if (log.isDebugEnabled()) {
            log.debug("Performance: loaded {} quiz activities for {} distinct activity ids", quizzes.size(), ids.size());
        }
        return quizzes.stream().collect(Collectors.toMap(QuizActivity::getId, q -> q, (a, b) -> a));
    }

    private boolean isPhotoLikeActivity(GameResult r) {
        String t = r.getActivityType();
        return t != null && (t.equalsIgnoreCase("PHOTO") || t.equalsIgnoreCase("IMAGE_RECOGNITION"));
    }

    private String classifyLevel(double avgScore) {
        if (avgScore >= 80) return "EXCELLENT";
        if (avgScore >= 60) return "BON";
        if (avgScore >= 40) return "MOYEN";
        if (avgScore >= 20) return "FAIBLE";
        return "CRITIQUE";
    }

    private List<Recommendation> generateRecommendations(String patientName, List<ThemeScore> themeScores) {
        List<Recommendation> recommendations = new ArrayList<>();

        List<ThemeScore> weakThemes = themeScores.stream()
                .filter(ts -> ts.avgScore < 60)
                .collect(Collectors.toList());

        if (weakThemes.isEmpty() && !themeScores.isEmpty()) {
            Recommendation rec = new Recommendation();
            rec.type = "MAINTAIN";
            rec.priority = "LOW";
            rec.message = "Continuez vos activités actuelles pour maintenir vos bonnes performances.";
            rec.suggestedQuizIds = Collections.emptyList();
            rec.suggestedQuizTitles = Collections.emptyList();
            rec.theme = "";
            rec.themeLabel = "Général";
            recommendations.add(rec);
            return recommendations;
        }

        for (ThemeScore weakTheme : weakThemes) {
            Recommendation rec = new Recommendation();
            rec.theme = weakTheme.theme;
            rec.themeLabel = weakTheme.label;
            rec.currentScore = weakTheme.avgScore;

            if (weakTheme.avgScore < 20) {
                rec.type = "CRITICAL";
                rec.priority = "CRITICAL";
                rec.message = String.format(
                        "Votre score en %s est très faible (%.0f%%). Nous recommandons fortement de pratiquer des exercices de %s régulièrement.",
                        weakTheme.label, weakTheme.avgScore, weakTheme.label.toLowerCase());
            } else if (weakTheme.avgScore < 40) {
                rec.type = "IMPROVE";
                rec.priority = "HIGH";
                rec.message = String.format(
                        "Votre score en %s nécessite une amélioration (%.0f%%). Pratiquez davantage les quiz de %s.",
                        weakTheme.label, weakTheme.avgScore, weakTheme.label.toLowerCase());
            } else {
                rec.type = "PRACTICE";
                rec.priority = "MEDIUM";
                rec.message = String.format(
                        "Votre score en %s est moyen (%.0f%%). Quelques exercices supplémentaires vous aideront à progresser.",
                        weakTheme.label, weakTheme.avgScore);
            }

            List<QuizActivity> suggested = quizActivityRepository.findByThemeIgnoreCaseOrderByIdAsc(
                    weakTheme.theme,
                    PageRequest.of(0, MAX_SUGGESTED_QUIZZES_PER_THEME));

            List<Long> quizIds = new ArrayList<>();
            List<String> quizTitles = new ArrayList<>();
            for (QuizActivity quiz : suggested) {
                quizIds.add(quiz.getId());
                quizTitles.add(quiz.getTitle());
            }

            if ("VISUAL".equals(weakTheme.theme) && quizIds.isEmpty()) {
                rec.message += " Essayez les activités de reconnaissance photo.";
            }

            rec.suggestedQuizIds = quizIds;
            rec.suggestedQuizTitles = quizTitles;
            recommendations.add(rec);
        }

        return recommendations;
    }

    public static class PatientPerformance {
        public Long patientId;
        public String patientName;
        public int totalQuizzes;
        public double globalScore;
        public List<ThemeScore> themeScores;
        public List<Recommendation> recommendations;
    }

    public static class ThemeScore {
        public String theme;
        public String label;
        public String icon;
        public int quizCount;
        public double avgScore;
        public double avgWeightedScore;
        public double avgResponseTime;
        public String trend;
        public String level;
    }

    public static class Recommendation {
        public String type;
        public String priority;
        public String theme;
        public String themeLabel;
        public double currentScore;
        public String message;
        public List<Long> suggestedQuizIds;
        public List<String> suggestedQuizTitles;
    }
}
