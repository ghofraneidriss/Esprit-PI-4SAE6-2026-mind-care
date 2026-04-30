package tn.esprit.lost_item_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.lost_item_service.entity.ItemCategory;
import tn.esprit.lost_item_service.entity.LostItem;
import tn.esprit.lost_item_service.entity.SearchReport;
import tn.esprit.lost_item_service.entity.SearchResult;
import tn.esprit.lost_item_service.repository.LostItemRepository;
import tn.esprit.lost_item_service.repository.SearchReportRepository;

import java.util.*;

/**
 * Real-time Smart Search Suggestion Engine.
 *
 * Given a patientId and optional category, analyses historical search reports
 * across all previous items of that patient/category combination and returns
 * up to 3 location suggestions ranked by historical success rate with
 * contextual tips.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchSuggestionService {

    private static final String MEDICATION_CATEGORY = "MEDICATION";
    private static final String DOCUMENT_CATEGORY = "DOCUMENT";
    private static final String LOCATION_KEY = "location";
    private static final int MAX_SUGGESTIONS = 3;
    private static final double HIGH_SCORE_THRESHOLD = 0.7;
    private static final double MEDIUM_SCORE_THRESHOLD = 0.4;
    private static final int MIN_SEARCHES_FOR_FREQUENCY = 3;

    private final LostItemRepository lostItemRepository;
    private final SearchReportRepository searchReportRepository;

    /**
     * Returns smart search suggestions for where to look for an item.
     *
     * @param patientId  the patient whose history to mine
     * @param category   optional category filter (null = all categories)
     * @return list of up to 3 location suggestions with confidence scores
     */
    public List<Map<String, Object>> getSuggestions(Long patientId, String category) {
        log.info("[Suggestions] patientId={}, category={}", patientId, category);

        // 1. Find all previous items for this patient (optionally filter by category)
        List<LostItem> items = lostItemRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
        if (category != null && !category.isBlank()) {
            try {
                ItemCategory cat = ItemCategory.valueOf(category.toUpperCase());
                items = items.stream()
                        .filter(i -> cat.equals(i.getCategory()))
                        .toList();
            } catch (IllegalArgumentException ignored) {
                // unknown category — use all items
            }
        }

        if (items.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. Fetch all search reports for those items in one query
        List<Long> itemIds = items.stream().map(LostItem::getId).toList();
        List<SearchReport> reports = searchReportRepository.findByLostItemIdIn(itemIds);

        // 3. Build per-location statistics
        Map<String, int[]> locationStats = new LinkedHashMap<>();
        for (SearchReport r : reports) {
            String loc = r.getLocationSearched();
            if (loc == null || loc.isBlank()) continue;
            loc = loc.trim();
            locationStats.computeIfAbsent(loc, k -> new int[]{0, 0, 0});
            int[] stats = locationStats.get(loc);
            stats[0]++; // total
            if (r.getSearchResult() == SearchResult.FOUND)           stats[1]++;
            else if (r.getSearchResult() == SearchResult.PARTIALLY_FOUND) stats[2]++;
        }

        // 4. Score and sort: primary = found rate, secondary = (found + 0.5 * partial) / total
        List<Map.Entry<String, int[]>> ranked = new ArrayList<>(locationStats.entrySet());
        ranked.sort((a, b) -> {
            double scoreA = computeScore(a.getValue());
            double scoreB = computeScore(b.getValue());
            return Double.compare(scoreB, scoreA);
        });

        // 5. Build result DTOs (top 3)
        List<Map<String, Object>> suggestions = new ArrayList<>();
        for (int i = 0; i < Math.min(MAX_SUGGESTIONS, ranked.size()); i++) {
            Map.Entry<String, int[]> entry = ranked.get(i);
            String loc = entry.getKey();
            int[] stats = entry.getValue();
            int total    = stats[0];
            int found    = stats[1];
            int partial  = stats[2];
            double score = computeScore(stats);

            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put(LOCATION_KEY, loc);
            dto.put("totalSearches", total);
            dto.put("foundCount", found);
            dto.put("partialCount", partial);
            dto.put("confidenceScore", Math.round(score * 1000.0) / 10.0); // 0-100 %
            dto.put("rank", i + 1);
            dto.put("tip", buildTip(loc, stats, category, i));
            suggestions.add(dto);
        }

        log.info("[Suggestions] Returning {} suggestions for patientId={}", suggestions.size(), patientId);
        return suggestions;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Score = (found + 0.5 * partial) / total, clamped to [0, 1].
     * Locations with no data score 0.
     */
    private double computeScore(int[] stats) {
        int total   = stats[0];
        int found   = stats[1];
        int partial = stats[2];
        if (total == 0) return 0.0;
        return Math.min(1.0, (found + 0.5 * partial) / total);
    }

    /** Generates a context-aware tip sentence for each suggestion. */
    private String buildTip(String location, int[] stats, String category, int rank) {
        int total = stats[0];
        int found = stats[1];
        double score = computeScore(stats);

        boolean isMedication = MEDICATION_CATEGORY.equalsIgnoreCase(category);
        boolean isDocument   = DOCUMENT_CATEGORY.equalsIgnoreCase(category);

        if (score >= HIGH_SCORE_THRESHOLD) {
            if (isMedication) {
                return "High success rate — check " + location + " immediately. Medication retrieval is time-critical.";
            }
            return "Best bet — " + location + " has a " + Math.round(score * 100) + "% success rate based on " + total + " searches.";
        } else if (score >= MEDIUM_SCORE_THRESHOLD) {
            if (isDocument) {
                return "Moderate success at " + location + ". Check drawers and flat surfaces carefully.";
            }
            return "Decent chance at " + location + " — found " + found + " out of " + total + " searches here before.";
        } else if (total >= MIN_SEARCHES_FOR_FREQUENCY) {
            return "Less successful location (" + location + "), but frequently searched — worth a quick check.";
        } else {
            return rank == 0
                ? "Limited data — " + location + " is the most promising based on available history."
                : "Try " + location + " as a secondary search area.";
        }
    }
}
