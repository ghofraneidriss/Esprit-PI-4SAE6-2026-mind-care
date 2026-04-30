package tn.esprit.lost_item_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.lost_item_service.entity.*;
import tn.esprit.lost_item_service.repository.LostItemAlertRepository;
import tn.esprit.lost_item_service.repository.LostItemRepository;
import tn.esprit.lost_item_service.repository.SearchReportRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Smart Recovery Intelligence Engine.
 *
 * For a given lost item, analyzes all historical search report data across
 * all items of the same category to produce:
 *  - A ranked list of recommended search locations (by success rate)
 *  - A recovery probability score (0–100%)
 *  - Estimated days to recovery
 *  - Category-level insights
 *  - Patient behavioral profile
 *  - Actionable strategy tips
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecoveryStrategyService {

    private final LostItemRepository lostItemRepository;
    private final SearchReportRepository searchReportRepository;
    private final LostItemAlertRepository lostItemAlertRepository;

    // Minimum number of historical searches for a location to be ranked
    private static final int MIN_SEARCHES_TO_QUALIFY = 1;

    // String constants to avoid duplication
    private static final String FOUND = "FOUND";
    private static final String PARTIALLY_FOUND = "PARTIALLY_FOUND";
    private static final String LOCATION = "location";
    private static final String SUCCESS_RATE = "successRate";
    private static final String ALREADY_SEARCHED = "alreadySearched";
    private static final String TOTAL_SEARCHES = "totalSearches";

    // Data holder for location analysis results
    private record LocationAnalysis(
            List<Map<String, Object>> recommendedLocations,
            List<Map<String, Object>> alreadySearchedLocations,
            String topLocation
    ) {}

    @Transactional(readOnly = true, timeout = 15)
    public Map<String, Object> getRecoveryStrategy(Long itemId) {
        log.info("[RecoveryStrategy] Computing strategy for lostItem id={}", itemId);

        LostItem item = lostItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Lost item not found: " + itemId));

        long daysElapsed = item.getCreatedAt() != null
                ? ChronoUnit.DAYS.between(item.getCreatedAt(), LocalDateTime.now())
                : 0;

        if (item.getStatus() == ItemStatus.FOUND) {
            return buildFoundItemResult(itemId, item, daysElapsed);
        }

        // ── 2. Load all search reports and build a category map ───────────────
        List<SearchReport> allReports = searchReportRepository.findAll();
        Map<Long, ItemCategory> itemCategoryMap = lostItemRepository.findAll().stream()
                .filter(i -> i.getCategory() != null)
                .collect(Collectors.toMap(LostItem::getId, LostItem::getCategory));

        // Reports already done for THIS item
        List<SearchReport> itemReports = allReports.stream()
                .filter(r -> itemId.equals(r.getLostItemId()))
                .toList();

        Set<String> alreadySearched = itemReports.stream()
                .filter(r -> r.getLocationSearched() != null && !r.getLocationSearched().isBlank())
                .map(r -> normalize(r.getLocationSearched()))
                .collect(Collectors.toSet());

        // Best result per location for THIS item (FOUND > PARTIALLY_FOUND > NOT_FOUND)
        Map<String, String> itemLocationResults = itemReports.stream()
                .filter(r -> r.getLocationSearched() != null && !r.getLocationSearched().isBlank()
                        && r.getSearchResult() != null)
                .collect(Collectors.toMap(
                        r -> normalize(r.getLocationSearched()),
                        r -> r.getSearchResult().name(),
                        (a, b) -> {
                            if (FOUND.equals(a) || FOUND.equals(b)) return FOUND;
                            if (PARTIALLY_FOUND.equals(a) || PARTIALLY_FOUND.equals(b)) return PARTIALLY_FOUND;
                            return a;
                        }
                ));

        // Reports for same CATEGORY (historical intelligence base)
        List<SearchReport> categoryReports = allReports.stream()
                .filter(r -> item.getCategory().equals(itemCategoryMap.get(r.getLostItemId())))
                .toList();

        // ── 3. Location success rate analysis ────────────────────────────────
        LocationAnalysis locationAnalysis = analyzeLocations(
                categoryReports, alreadySearched, itemLocationResults);
        List<Map<String, Object>> recommended = locationAnalysis.recommendedLocations();
        List<Map<String, Object>> alreadySearchedDisplay = locationAnalysis.alreadySearchedLocations();
        String topLocation = locationAnalysis.topLocation();

        // ── 4. Category-level insights ────────────────────────────────────────
        List<LostItem> categoryItems = lostItemRepository.findAll().stream()
                .filter(i -> item.getCategory().equals(i.getCategory()))
                .toList();

        long categoryFound = categoryItems.stream()
                .filter(i -> i.getStatus() == ItemStatus.FOUND).count();
        double categoryRecoveryRate = categoryItems.isEmpty() ? 0.0
                : Math.round((categoryFound * 100.0 / categoryItems.size()) * 10.0) / 10.0;

        OptionalDouble avgDaysToFind = categoryItems.stream()
                .filter(i -> i.getStatus() == ItemStatus.FOUND
                        && i.getCreatedAt() != null && i.getUpdatedAt() != null)
                .mapToLong(i -> ChronoUnit.DAYS.between(i.getCreatedAt(), i.getUpdatedAt()))
                .average();
        double estimatedDays = avgDaysToFind.isPresent()
                ? Math.round(avgDaysToFind.getAsDouble() * 10.0) / 10.0 : -1;

        // ── 5. Patient behavioral profile ─────────────────────────────────────
        List<LostItem> patientItems = lostItemRepository.findByPatientId(item.getPatientId());
        long patientFound = patientItems.stream()
                .filter(i -> i.getStatus() == ItemStatus.FOUND).count();
        double patientRecoveryRate = patientItems.isEmpty() ? 0.0
                : Math.round((patientFound * 100.0 / patientItems.size()) * 10.0) / 10.0;

        // Frequency trend: compare last 30 vs 31–60 days
        LocalDateTime now = LocalDateTime.now();
        long recentCount = lostItemRepository
                .findByPatientIdAndCreatedAtBetween(item.getPatientId(), now.minusDays(30), now).size();
        long previousCount = lostItemRepository
                .findByPatientIdAndCreatedAtBetween(item.getPatientId(), now.minusDays(60), now.minusDays(30)).size();
        boolean isFrequentLoser = recentCount >= 3 && recentCount > previousCount;

        // ── 6. Recovery probability score ─────────────────────────────────────
        double probability = computeRecoveryProbability(
                item, daysElapsed, itemReports,
                categoryRecoveryRate, isFrequentLoser);

        String probabilityLevel = determineProbabilityLevel(probability);

        // ── 7. Actionable strategy tips ───────────────────────────────────────
        List<String> tips = buildStrategyTips(item, daysElapsed, itemReports,
                recommended, isFrequentLoser, categoryRecoveryRate, estimatedDays);

        // ── 8. Assemble response ──────────────────────────────────────────────
        Map<String, Object> categoryInsights = new LinkedHashMap<>();
        categoryInsights.put("totalItems", categoryItems.size());
        categoryInsights.put("foundCount", categoryFound);
        categoryInsights.put("recoveryRate", categoryRecoveryRate);
        categoryInsights.put("avgDaysToFind", estimatedDays >= 0 ? estimatedDays : null);
        categoryInsights.put("topSuccessLocation", topLocation);
        categoryInsights.put("totalSearchReportsAnalyzed", categoryReports.size());

        Map<String, Object> patientProfile = new LinkedHashMap<>();
        patientProfile.put("patientId", item.getPatientId());
        patientProfile.put("totalItemsLost", patientItems.size());
        patientProfile.put("foundItems", patientFound);
        patientProfile.put("recoveryRate", patientRecoveryRate);
        patientProfile.put("isFrequentLoser", isFrequentLoser);
        patientProfile.put("recentMonthLostCount", recentCount);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("itemId", itemId);
        result.put("itemTitle", item.getTitle());
        result.put("category", item.getCategory().name());
        result.put("status", item.getStatus().name());
        result.put("priority", item.getPriority() != null ? item.getPriority().name() : null);
        result.put("daysElapsed", daysElapsed);
        result.put("searchAttemptsCount", itemReports.size());
        result.put("recoveryProbability", probability);
        result.put("probabilityLevel", probabilityLevel);
        result.put("estimatedDaysToRecover", estimatedDays >= 0 ? estimatedDays : null);
        result.put("recommendedLocations", recommended);
        result.put("alreadySearchedLocations", alreadySearchedDisplay);
        result.put("categoryInsights", categoryInsights);
        result.put("patientProfile", patientProfile);
        result.put("strategyTips", tips);

        log.info("[RecoveryStrategy] Done for itemId={}: probability={}% level={} recommendedLocations={}",
                itemId, probability, probabilityLevel, recommended.size());
        return result;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Map<String, Object> buildFoundItemResult(Long itemId, LostItem item, long daysElapsed) {
        List<SearchReport> doneReports = searchReportRepository.findAll().stream()
                .filter(r -> itemId.equals(r.getLostItemId()))
                .toList();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("itemId", itemId);
        result.put("itemTitle", item.getTitle());
        result.put("category", item.getCategory() != null ? item.getCategory().name() : null);
        result.put("status", FOUND);
        result.put("message", "Item has been found. No active recovery strategy needed.");
        result.put("daysElapsed", daysElapsed);
        result.put("searchAttemptsCount", doneReports.size());
        return result;
    }

    private LocationAnalysis analyzeLocations(
            List<SearchReport> categoryReports,
            Set<String> alreadySearched, Map<String, String> itemLocationResults) {

        Map<String, List<SearchReport>> byLocation = categoryReports.stream()
                .filter(r -> r.getLocationSearched() != null && !r.getLocationSearched().isBlank())
                .collect(Collectors.groupingBy(r -> normalize(r.getLocationSearched())));

        List<Map<String, Object>> allLocationRanks = new ArrayList<>();
        for (Map.Entry<String, List<SearchReport>> entry : byLocation.entrySet()) {
            String location = entry.getKey();
            List<SearchReport> searches = entry.getValue();
            int total = searches.size();
            if (total < MIN_SEARCHES_TO_QUALIFY) continue;

            double foundScore = searches.stream().mapToDouble(r -> {
                if (r.getSearchResult() == null) return 0.0;
                return switch (r.getSearchResult()) {
                    case FOUND            -> 1.0;
                    case PARTIALLY_FOUND  -> 0.5;
                    default               -> 0.0;
                };
            }).sum();

            double successRate = Math.round((foundScore / total) * 1000.0) / 10.0;

            Map<String, Object> loc = new LinkedHashMap<>();
            loc.put(LOCATION, capitalize(location));
            loc.put(SUCCESS_RATE, successRate);
            loc.put(TOTAL_SEARCHES, total);
            loc.put(ALREADY_SEARCHED, alreadySearched.contains(location));
            allLocationRanks.add(loc);
        }

        allLocationRanks.sort((a, b) ->
                Double.compare((double) b.get(SUCCESS_RATE), (double) a.get(SUCCESS_RATE)));

        List<Map<String, Object>> recommended = new ArrayList<>();
        List<Map<String, Object>> alreadySearchedDisplay = new ArrayList<>();
        int rank = 1;
        for (Map<String, Object> loc : allLocationRanks) {
            if (Boolean.TRUE.equals(loc.get(ALREADY_SEARCHED))) {
                String normalizedLoc = normalize(loc.get(LOCATION).toString());
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put(LOCATION, loc.get(LOCATION));
                entry.put("result", itemLocationResults.getOrDefault(normalizedLoc, "UNKNOWN"));
                entry.put("categorySuccessRate", loc.get(SUCCESS_RATE));
                alreadySearchedDisplay.add(entry);
            } else {
                loc.put("rank", rank++);
                recommended.add(loc);
            }
        }

        Set<String> coveredLocs = alreadySearchedDisplay.stream()
                .map(e -> normalize(e.get(LOCATION).toString()))
                .collect(Collectors.toSet());
        for (Map.Entry<String, String> e : itemLocationResults.entrySet()) {
            if (!coveredLocs.contains(e.getKey())) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put(LOCATION, capitalize(e.getKey()));
                entry.put("result", e.getValue());
                entry.put("categorySuccessRate", null);
                alreadySearchedDisplay.add(entry);
            }
        }

        String topLocation = allLocationRanks.isEmpty() ? "—"
                : allLocationRanks.get(0).get(LOCATION).toString();

        return new LocationAnalysis(recommended, alreadySearchedDisplay, topLocation);
    }

    /** Determine probability level from recovery probability score. */
    private String determineProbabilityLevel(double probability) {
        if (probability >= 75) return "HIGH";
        if (probability >= 45) return "MODERATE";
        if (probability >= 20) return "LOW";
        return "CRITICAL";
    }

    /**
     * Computes recovery probability 0–100 using weighted factors.
     * Search results are split: NOT_FOUND penalises, PARTIALLY_FOUND/FOUND boost.
     */
    private double computeRecoveryProbability(
            LostItem item, long daysElapsed, List<SearchReport> itemReports,
            double categoryRecoveryRate, boolean isFrequentLoser) {

        double score = categoryRecoveryRate > 0 ? categoryRecoveryRate : 50.0;
        score += computePriorityBoost(item);
        score += computeCaregiverBoost(item);
        score -= computeDaysPenalty(daysElapsed);
        score += computeReportAdjustment(itemReports);
        if (isFrequentLoser) score -= 8;
        score += computeItemCategoryBoost(item);

        return Math.max(5.0, Math.min(95.0, Math.round(score * 10.0) / 10.0));
    }

    private double computePriorityBoost(LostItem item) {
        if (item.getPriority() == ItemPriority.CRITICAL) return 5;
        if (item.getPriority() == ItemPriority.HIGH) return 3;
        return 0;
    }

    private double computeCaregiverBoost(LostItem item) {
        return item.getCaregiverId() != null ? 5 : 0;
    }

    private double computeDaysPenalty(long daysElapsed) {
        return Math.min(daysElapsed * 2.5, 35);
    }

    private double computeReportAdjustment(List<SearchReport> itemReports) {
        double adjustment = 0;
        for (SearchReport r : itemReports) {
            if (r.getSearchResult() == null) continue;
            adjustment += switch (r.getSearchResult()) {
                case NOT_FOUND       -> -4;
                case PARTIALLY_FOUND -> 6;
                case FOUND           -> 15;
            };
        }
        return Math.max(-28, Math.min(30, adjustment));
    }

    private double computeItemCategoryBoost(LostItem item) {
        return item.getCategory() == ItemCategory.MEDICATION ? 5 : 0;
    }

    /**
     * Generates human-readable actionable tips based on the strategy data.
     */
    private List<String> buildStrategyTips(
            LostItem item, long daysElapsed, List<SearchReport> itemReports,
            List<Map<String, Object>> recommended,
            boolean isFrequentLoser, double categoryRecoveryRate, double estimatedDays) {

        List<String> tips = new ArrayList<>();

        long notFoundCount      = itemReports.stream().filter(r -> r.getSearchResult() == SearchResult.NOT_FOUND).count();
        long partialFoundCount  = itemReports.stream().filter(r -> r.getSearchResult() == SearchResult.PARTIALLY_FOUND).count();

        // Partial finds → strong directional hint
        if (partialFoundCount > 0) {
            tips.add("A partial find was reported — concentrate the next search in the same area and check hidden spots (under/behind furniture, inside bags).");
        }

        if (!recommended.isEmpty()) {
            Map<String, Object> top = recommended.get(0);
            tips.add(String.format("Next recommended location: \"%s\" — %.1f%% historical success rate for %s items.",
                    top.get(LOCATION), top.get(SUCCESS_RATE), item.getCategory().name().toLowerCase()));
        }

        if (item.getCategory() == ItemCategory.MEDICATION) {
            tips.add("Medication items must be found urgently. Alert the attending doctor if not found within 24 hours.");
        }

        if (daysElapsed >= 7) {
            tips.add("Item has been missing for " + daysElapsed + " days — consider filing an escalation report.");
        } else if (daysElapsed >= 3) {
            tips.add("Item has been lost for " + daysElapsed + " days. Expand the search to less obvious locations.");
        }

        if (notFoundCount >= 5) {
            tips.add("After " + notFoundCount + " unsuccessful searches, try retracing the patient's full daily routine from the last known date.");
        }

        if (isFrequentLoser) {
            tips.add("This patient frequently loses items. Consider a daily item checklist routine with the caregiver.");
        }

        if (categoryRecoveryRate > 65) {
            tips.add(String.format("Good news: %.1f%% of %s items are eventually found — keep searching systematically.",
                    categoryRecoveryRate, item.getCategory().name().toLowerCase()));
        } else if (categoryRecoveryRate < 40) {
            tips.add(String.format("Recovery rate for %s items is %.1f%% — document all attempts for insurance/administrative purposes.",
                    item.getCategory().name().toLowerCase(), categoryRecoveryRate));
        }

        if (estimatedDays > 0) {
            tips.add(String.format("Similar items are typically found within %.0f day(s). Stay consistent with daily searches.",
                    Math.ceil(estimatedDays)));
        }

        if (item.getLastSeenLocation() != null && !item.getLastSeenLocation().isBlank()) {
            tips.add("Last seen location was \"" + item.getLastSeenLocation() + "\" — check all adjacent areas thoroughly.");
        }

        return tips;
    }

    /** Normalize location string for grouping (lowercase + trim). */
    private String normalize(String location) {
        return location.trim().toLowerCase();
    }

    /** Capitalize for display. */
    private String capitalize(String s) {
        if (s == null || s.isBlank()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
