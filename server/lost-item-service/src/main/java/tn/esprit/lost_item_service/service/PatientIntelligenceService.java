package tn.esprit.lost_item_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import tn.esprit.lost_item_service.entity.ItemCategory;
import tn.esprit.lost_item_service.entity.ItemStatus;
import tn.esprit.lost_item_service.entity.LostItem;
import tn.esprit.lost_item_service.entity.SearchReport;
import tn.esprit.lost_item_service.entity.SearchResult;
import tn.esprit.lost_item_service.repository.LostItemRepository;
import tn.esprit.lost_item_service.repository.SearchReportRepository;

import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Patient Behavioral Intelligence Engine.
 *
 * Collects 90 days of lost-item data for a patient, computes statistical indicators
 * (loss velocity, category risk, danger zones, cognitive trend), then sends a
 * structured clinical prompt to the Groq LLM via Spring AI and returns both the
 * AI-generated analysis and the supporting raw statistics.
 */
@Service
@Slf4j
public class PatientIntelligenceService {

    private final ChatClient chatClient;
    private final LostItemRepository lostItemRepository;

    private static final String COUNT = "count";
    private static final String CRITICAL_PRIORITY = "CRITICAL";
    private static final String MEDICATION_CATEGORY = "MEDICATION";
    private static final String RISK_LEVEL_HIGH = "HIGH";
    private static final String RISK_LEVEL_MEDIUM = "MEDIUM";
    private static final String RISK_LEVEL_LOW = "LOW";
    private static final String RISK_LEVEL_CRITICAL = "CRITICAL";
    private static final String STABLE_TREND = "STABLE";
    private static final String INCREASING_TREND = "INCREASING";
    private static final String DECREASING_TREND = "DECREASING";
    private static final int CATEGORY_MEDIUM_THRESHOLD = 2;
    private static final int CATEGORY_RISK_THRESHOLD = 3;
    private static final int INCREASING_SCORE = 2;
    private static final int CRITICAL_ALERT_HIGH_SCORE = 3;
    private static final int CRITICAL_ALERT_ONE_SCORE = 2;
    private static final int RECENT_HIGH_SCORE = 2;
    private static final int RECENT_MEDIUM_SCORE = 1;
    private static final int RECENT_ITEMS_THRESHOLD = 5;
    private static final int RECENT_ITEMS_MEDIUM_THRESHOLD = 3;
    private static final double RECOVERY_LOW_THRESHOLD = 20.0;
    private static final double RECOVERY_MEDIUM_THRESHOLD = 40.0;
    private static final int RECOVERY_LOW_SCORE = 2;
    private static final int RECOVERY_MEDIUM_SCORE = 1;
    private static final int CRITICAL_RISK_THRESHOLD = 6;
    private static final int HIGH_RISK_THRESHOLD = 4;
    private static final int MODERATE_RISK_THRESHOLD = 2;
    private static final int TREND_DIFFERENCE_THRESHOLD = 1;
    private static final double TREND_MULTIPLIER_DEFAULT = 99.0;

    public PatientIntelligenceService(
            ChatClient.Builder chatClientBuilder,
            LostItemRepository lostItemRepository) {
        this.chatClient = chatClientBuilder.build();
        this.lostItemRepository = lostItemRepository;
    }

    public Map<String, Object> analyzePatient(Long patientId) {
        log.info("[Intelligence] Analyzing patient id={}", patientId);

        LocalDateTime now = LocalDateTime.now();

        // ── 1. Load all patient items ─────────────────────────────────────────
        List<LostItem> allItems = lostItemRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        List<LostItem> last90 = allItems.stream()
                .filter(i -> i.getCreatedAt() != null && i.getCreatedAt().isAfter(now.minusDays(90)))
                .toList();

        long totalLost = allItems.size();
        long totalFound = allItems.stream().filter(i -> i.getStatus() == ItemStatus.FOUND).count();
        double recoveryRate = totalLost > 0 ? Math.round((totalFound * 100.0 / totalLost) * 10.0) / 10.0 : 0.0;

        // ── 2. Monthly trend (last 6 months) ──────────────────────────────────
        List<Map<String, Object>> monthlyTrend = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDateTime from = now.minusMonths((long)i + 1).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime to   = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            long count = lostItemRepository.findByPatientIdAndCreatedAtBetween(patientId, from, to).size();
            String month = from.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                         + " " + from.getYear();
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("month", month);
            entry.put(COUNT, count);
            monthlyTrend.add(entry);
        }

        // Trend direction: compare last 30 days vs previous 30 days
        long recentCount   = lostItemRepository.findByPatientIdAndCreatedAtBetween(patientId, now.minusDays(30), now).size();
        long previousCount = lostItemRepository.findByPatientIdAndCreatedAtBetween(patientId, now.minusDays(60), now.minusDays(30)).size();
        String trendDir = determineTrendDirection(recentCount, previousCount);
        double trendMultiplier = calculateTrendMultiplier(recentCount, previousCount);

        // ── 3. Category risk distribution ─────────────────────────────────────
        Map<String, Long> categoryDist = last90.stream()
                .filter(i -> i.getCategory() != null)
                .collect(Collectors.groupingBy(i -> i.getCategory().name(), Collectors.counting()));
        List<Map<String, Object>> categoryRisk = categoryDist.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("category", e.getKey());
                    m.put(COUNT, e.getValue());
                    String risk = determineCategoryRiskLevel(e.getKey(), e.getValue());
                    m.put("riskLevel", risk);
                    return m;
                }).toList();

        // ── 4. Danger zones (loss locations) ─────────────────────────────────
        Map<String, Long> locationFreq = last90.stream()
                .filter(i -> i.getLastSeenLocation() != null && !i.getLastSeenLocation().isBlank())
                .collect(Collectors.groupingBy(i -> i.getLastSeenLocation().trim(), Collectors.counting()));
        List<Map<String, Object>> dangerZones = locationFreq.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("location", e.getKey());
                    m.put("lossCount", e.getValue());
                    return m;
                }).toList();

        // ── 5. Unresolved critical items ──────────────────────────────────────
        long unresolvedCritical = allItems.stream()
                .filter(i -> i.getStatus() == ItemStatus.LOST || i.getStatus() == ItemStatus.SEARCHING)
                .filter(i -> CRITICAL_PRIORITY.equals(i.getPriority() != null ? i.getPriority().name() : ""))
                .count();

        Optional<LostItem> longestUnresolved = allItems.stream()
                .filter(i -> i.getStatus() == ItemStatus.LOST || i.getStatus() == ItemStatus.SEARCHING)
                .filter(i -> i.getCreatedAt() != null)
                .min(Comparator.comparing(LostItem::getCreatedAt));

        long longestDays = longestUnresolved.map(i ->
                java.time.temporal.ChronoUnit.DAYS.between(i.getCreatedAt(), now)).orElse(0L);

        // ── 6. Build LLM prompt ───────────────────────────────────────────────
        String prompt = buildPrompt(
                patientId, totalLost, totalFound, recoveryRate,
                recentCount, previousCount, trendDir, trendMultiplier,
                monthlyTrend, categoryRisk, dangerZones,
                unresolvedCritical, longestDays,
                longestUnresolved.map(i -> i.getCategory() != null ? i.getCategory().name() : "UNKNOWN").orElse("N/A")
        );

        // ── 7. Call Groq via Spring AI ────────────────────────────────────────
        String aiAnalysis;
        String aiError = null;
        try {
            aiAnalysis = chatClient.prompt()
                    .system("You are a clinical AI assistant for an Alzheimer patient care facility. " +
                            "Be professional, concise, and medically accurate.")
                    .user(prompt)
                    .call()
                    .content();
            log.info("[Intelligence] AI analysis complete for patientId={}", patientId);
        } catch (Exception ex) {
            log.error("[Intelligence] LLM call failed: {}", ex.getMessage());
            aiAnalysis = null;
            aiError = "AI analysis temporarily unavailable: " + ex.getMessage();
        }

        // ── 8. Overall risk level (computed, not AI) ──────────────────────────
        String overallRisk = computeRiskLevel(recentCount, trendDir,
                unresolvedCritical, totalLost, totalFound);

        // ── 9. Assemble response ──────────────────────────────────────────────
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("patientId", patientId);
        result.put("totalItemsLost", totalLost);
        result.put("totalFound", totalFound);
        result.put("recoveryRate", recoveryRate);
        result.put("recentMonthCount", recentCount);
        result.put("previousMonthCount", previousCount);
        result.put("trendDirection", trendDir);
        result.put("trendMultiplier", trendMultiplier);
        result.put("monthlyTrend", monthlyTrend);
        result.put("categoryRisk", categoryRisk);
        result.put("dangerZones", dangerZones);
        result.put("unresolvedCritical", unresolvedCritical);
        result.put("longestUnresolvedDays", longestDays);
        result.put("overallRiskLevel", overallRisk);
        result.put("aiAnalysis", aiAnalysis);
        if (aiError != null) result.put("aiError", aiError);

        return result;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String buildPrompt(Long patientId, long totalLost, long totalFound,
            double recoveryRate, long recentCount, long previousCount,
            String trendDir, double multiplier,
            List<Map<String, Object>> monthlyTrend,
            List<Map<String, Object>> categoryRisk,
            List<Map<String, Object>> dangerZones,
            long unresolvedCritical, long longestDays, String longestCategory) {

        StringBuilder sb = new StringBuilder();
        sb.append("Analyze the following behavioral data for an Alzheimer patient (ID: ").append(patientId).append("):\n\n");

        sb.append("LOST ITEM OVERVIEW (last 90 days):\n");
        sb.append("- Total items lost: ").append(totalLost).append("\n");
        sb.append("- Items recovered: ").append(totalFound)
          .append(" (").append(recoveryRate).append("% recovery rate)\n");
        sb.append("- Unresolved critical items: ").append(unresolvedCritical).append("\n");
        sb.append("- Longest unresolved item: ").append(longestDays)
          .append(" days (category: ").append(longestCategory).append(")\n\n");

        sb.append("MONTHLY LOSS TREND (last 6 months):\n");
        for (Map<String, Object> m : monthlyTrend) {
            sb.append("  ").append(m.get("month")).append(": ").append(m.get(COUNT)).append(" items\n");
        }
        sb.append("- Trend: ").append(trendDir);
        if (!STABLE_TREND.equals(trendDir) && multiplier != 1.0 && multiplier != TREND_MULTIPLIER_DEFAULT) {
            sb.append(" (").append(multiplier).append("x vs previous period)");
        }
        sb.append("\n- Last 30 days: ").append(recentCount)
          .append(" vs previous 30 days: ").append(previousCount).append("\n\n");

        if (!categoryRisk.isEmpty()) {
            sb.append("CATEGORY RISK:\n");
            for (Map<String, Object> c : categoryRisk) {
                sb.append("  ").append(c.get("category")).append(": ").append(c.get(COUNT))
                  .append(" items (risk: ").append(c.get("riskLevel")).append(")\n");
            }
            sb.append("\n");
        }

        if (!dangerZones.isEmpty()) {
            sb.append("FREQUENT LOSS LOCATIONS (danger zones):\n");
            for (Map<String, Object> z : dangerZones) {
                sb.append("  ").append(z.get("location")).append(": ")
                  .append(z.get("lossCount")).append(" losses\n");
            }
            sb.append("\n");
        }

        sb.append("Please provide a structured response with EXACTLY these sections:\n");
        sb.append("CLINICAL ASSESSMENT: (2-3 sentence clinical interpretation)\n");
        sb.append("RISK LEVEL: (one of LOW / MODERATE / HIGH / CRITICAL with 1 sentence justification)\n");
        sb.append("KEY PATTERNS: (2-3 bullet points of behavioral patterns observed)\n");
        sb.append("CARE TEAM RECOMMENDATIONS: (2-3 specific actionable recommendations)\n");
        sb.append("COGNITIVE INDICATOR: (brief statement on whether data suggests cognitive decline progression)\n");

        return sb.toString();
    }

    /** Determine trend direction based on recent vs previous counts. */
    private String determineTrendDirection(long recentCount, long previousCount) {
        if (recentCount > previousCount + TREND_DIFFERENCE_THRESHOLD) return INCREASING_TREND;
        if (recentCount < previousCount - TREND_DIFFERENCE_THRESHOLD) return DECREASING_TREND;
        return STABLE_TREND;
    }

    /** Calculate trend multiplier showing change ratio. */
    private double calculateTrendMultiplier(long recentCount, long previousCount) {
        if (previousCount > 0) {
            return Math.round((recentCount * 10.0 / previousCount)) / 10.0;
        }
        return recentCount > 0 ? TREND_MULTIPLIER_DEFAULT : 1.0;
    }

    private String determineCategoryRiskLevel(String category, long count) {
        if (MEDICATION_CATEGORY.equals(category)) return RISK_LEVEL_HIGH;
        if (count >= CATEGORY_RISK_THRESHOLD) return RISK_LEVEL_HIGH;
        if (count == CATEGORY_MEDIUM_THRESHOLD) return RISK_LEVEL_MEDIUM;
        return RISK_LEVEL_LOW;
    }

    private String computeRiskLevel(long recentCount, String trendDir,
                                     long unresolvedCritical, long totalLost, long totalFound) {
        int score = 0;
        if (INCREASING_TREND.equals(trendDir)) score += INCREASING_SCORE;
        if (unresolvedCritical >= 2) score += CRITICAL_ALERT_HIGH_SCORE;
        else if (unresolvedCritical == 1) score += CRITICAL_ALERT_ONE_SCORE;
        if (recentCount >= RECENT_ITEMS_THRESHOLD) score += RECENT_HIGH_SCORE;
        else if (recentCount >= RECENT_ITEMS_MEDIUM_THRESHOLD) score += RECENT_MEDIUM_SCORE;
        double recoveryRate = totalLost > 0 ? (totalFound * 100.0 / totalLost) : 100.0;
        if (recoveryRate < RECOVERY_LOW_THRESHOLD) score += RECOVERY_LOW_SCORE;
        else if (recoveryRate < RECOVERY_MEDIUM_THRESHOLD) score += RECOVERY_MEDIUM_SCORE;

        if (score >= CRITICAL_RISK_THRESHOLD) return RISK_LEVEL_CRITICAL;
        if (score >= HIGH_RISK_THRESHOLD) return RISK_LEVEL_HIGH;
        if (score >= MODERATE_RISK_THRESHOLD) return "MODERATE";
        return RISK_LEVEL_LOW;
    }
}
