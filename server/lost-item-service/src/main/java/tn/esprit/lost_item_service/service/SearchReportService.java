package tn.esprit.lost_item_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.lost_item_service.entity.ReportStatus;
import tn.esprit.lost_item_service.entity.SearchReport;
import tn.esprit.lost_item_service.entity.SearchResult;
import tn.esprit.lost_item_service.exception.DuplicateReportException;
import tn.esprit.lost_item_service.repository.LostItemRepository;
import tn.esprit.lost_item_service.repository.SearchReportRepository;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchReportService {

    private static final String UNKNOWN_STATUS = "UNKNOWN";
    private static final String OPEN_STATUS = "OPEN";

    private final SearchReportRepository searchReportRepository;
    private final LostItemRepository lostItemRepository;

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public SearchReport createSearchReport(SearchReport report) {
        if (searchReportRepository.existsByLostItemIdAndSearchDate(report.getLostItemId(), report.getSearchDate())) {
            throw new DuplicateReportException(
                "A search report already exists for lost item id=" + report.getLostItemId()
                + " on " + report.getSearchDate() + ". Only one report per item per day is allowed."
            );
        }
        log.info("Creating search report for lost item id={}", report.getLostItemId());
        return searchReportRepository.save(report);
    }

    public List<SearchReport> getSearchReportsByLostItemId(Long lostItemId) {
        return searchReportRepository.findByLostItemIdOrderBySearchDateDesc(lostItemId);
    }

    public SearchReport getSearchReportById(Long id) {
        return searchReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Search report not found with id: " + id));
    }

    public SearchReport updateSearchReport(Long id, SearchReport updated) {
        SearchReport existing = getSearchReportById(id);
        existing.setLostItemId(updated.getLostItemId());
        existing.setReportedBy(updated.getReportedBy());
        existing.setSearchDate(updated.getSearchDate());
        existing.setLocationSearched(updated.getLocationSearched());
        existing.setSearchResult(updated.getSearchResult());
        existing.setNotes(updated.getNotes());
        existing.setStatus(updated.getStatus());
        return searchReportRepository.save(existing);
    }

    public void deleteSearchReport(Long id) {
        if (!searchReportRepository.existsById(id)) {
            throw new RuntimeException("Search report not found with id: " + id);
        }
        searchReportRepository.deleteById(id);
    }

    public long getOpenReportsCount(Long lostItemId) {
        return searchReportRepository.countByLostItemIdAndStatus(lostItemId, ReportStatus.OPEN);
    }

    // ── Advanced Search Log ───────────────────────────────────────────────────

    /**
     * Advanced search log filter: returns reports matching any combination of
     * lostItemId, reportedBy, searchResult, status, locationKeyword, fromDate, toDate.
     * All parameters are optional (null = skip that filter).
     */
    public List<SearchReport> advancedSearch(
            Long lostItemId,
            Long reportedBy,
            SearchResult searchResult,
            ReportStatus status,
            String locationKeyword,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        List<SearchReport> base;

        // Start with most specific filter available
        if (lostItemId != null && fromDate != null && toDate != null) {
            base = searchReportRepository.findByLostItemIdAndDateRange(lostItemId, fromDate, toDate);
        } else if (lostItemId != null) {
            base = searchReportRepository.findByLostItemIdOrderBySearchDateDesc(lostItemId);
        } else if (fromDate != null && toDate != null) {
            base = searchReportRepository.findByDateRange(fromDate, toDate);
        } else if (reportedBy != null) {
            base = searchReportRepository.findByReportedByOrderBySearchDateDesc(reportedBy);
        } else {
            base = searchReportRepository.findAll();
            base.sort(Comparator.comparing(SearchReport::getSearchDate, Comparator.nullsLast(Comparator.reverseOrder())));
        }

        // Apply remaining in-memory filters
        return base.stream()
                .filter(r -> reportedBy == null || reportedBy.equals(r.getReportedBy()))
                .filter(r -> searchResult == null || searchResult == r.getSearchResult())
                .filter(r -> status == null || status == r.getStatus())
                .filter(r -> locationKeyword == null || locationKeyword.isBlank()
                        || (r.getLocationSearched() != null
                            && r.getLocationSearched().toLowerCase().contains(locationKeyword.toLowerCase())))
                .toList();
    }

    /**
     * Returns a chronological timeline of all search reports for a given lost item,
     * enriched with per-date statistics.
     */
    public Map<String, Object> getSearchTimeline(Long lostItemId) {
        List<SearchReport> reports = searchReportRepository.findByLostItemIdOrderBySearchDateDesc(lostItemId);

        long totalSearches = reports.size();
        long foundCount    = reports.stream().filter(r -> r.getSearchResult() == SearchResult.FOUND).count();
        long partialCount  = reports.stream().filter(r -> r.getSearchResult() == SearchResult.PARTIALLY_FOUND).count();
        long notFoundCount = reports.stream().filter(r -> r.getSearchResult() == SearchResult.NOT_FOUND).count();
        long openCount     = reports.stream().filter(r -> r.getStatus() == ReportStatus.OPEN).count();

        double successRate = totalSearches == 0 ? 0.0
                : Math.round(((foundCount + partialCount) * 100.0 / totalSearches) * 10.0) / 10.0;

        // Group by date (day-level summary)
        Map<LocalDate, List<SearchReport>> byDate = reports.stream()
                .filter(r -> r.getSearchDate() != null)
                .collect(Collectors.groupingBy(SearchReport::getSearchDate));

        List<Map<String, Object>> timeline = byDate.entrySet().stream()
                .sorted(Map.Entry.<LocalDate, List<SearchReport>>comparingByKey().reversed())
                .map(entry -> {
                    Map<String, Object> day = new LinkedHashMap<>();
                    day.put("date", entry.getKey().toString());
                    day.put("reportCount", entry.getValue().size());
                    day.put("results", entry.getValue().stream()
                            .map(r -> Map.of(
                                    "id", r.getId(),
                                    "result", r.getSearchResult() != null ? r.getSearchResult().name() : UNKNOWN_STATUS,
                                    "location", r.getLocationSearched() != null ? r.getLocationSearched() : "",
                                    "status", r.getStatus() != null ? r.getStatus().name() : OPEN_STATUS
                            ))
                            .toList());
                    return day;
                })
                .toList();

        // Location heatmap: most searched locations
        Map<String, Long> locationFrequency = reports.stream()
                .filter(r -> r.getLocationSearched() != null && !r.getLocationSearched().isBlank())
                .collect(Collectors.groupingBy(SearchReport::getLocationSearched, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new
                ));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("lostItemId", lostItemId);
        result.put("totalSearches", totalSearches);
        result.put("foundCount", foundCount);
        result.put("partiallyFoundCount", partialCount);
        result.put("notFoundCount", notFoundCount);
        result.put("openReports", openCount);
        result.put("successRate", successRate);
        result.put("locationFrequency", locationFrequency);
        result.put("timeline", timeline);
        return result;
    }

    /**
     * Returns global search log statistics across all lost items.
     */
    public Map<String, Object> getGlobalSearchLogStats() {
        long total = searchReportRepository.count();

        Map<String, Long> byResult = new LinkedHashMap<>();
        for (Object[] row : searchReportRepository.countGroupedByResult()) {
            byResult.put(row[0] != null ? row[0].toString() : UNKNOWN_STATUS, (Long) row[1]);
        }

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (Object[] row : searchReportRepository.countGroupedByStatus()) {
            byStatus.put(row[0] != null ? row[0].toString() : UNKNOWN_STATUS, (Long) row[1]);
        }

        // Top searched items (most search reports)
        List<Map<String, Object>> topSearchedItems = new ArrayList<>();
        for (Object[] row : searchReportRepository.countGroupedByLostItem()) {
            Long itemId = (Long) row[0];
            Long count  = (Long) row[1];
            String itemTitle = lostItemRepository.findById(itemId)
                    .map(i -> i.getTitle())
                    .orElse("Item #" + itemId);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("lostItemId", itemId);
            entry.put("itemTitle", itemTitle);
            entry.put("searchCount", count);
            topSearchedItems.add(entry);
            if (topSearchedItems.size() >= 10) break;
        }

        // Top reporters
        List<Map<String, Object>> topReporters = new ArrayList<>();
        for (Object[] row : searchReportRepository.countGroupedByReporter()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("reportedBy", row[0]);
            entry.put("reportCount", row[1]);
            topReporters.add(entry);
            if (topReporters.size() >= 10) break;
        }

        long foundTotal = searchReportRepository.countBySearchResult(SearchResult.FOUND);
        long partialTotal = searchReportRepository.countBySearchResult(SearchResult.PARTIALLY_FOUND);
        double globalSuccessRate = total == 0 ? 0.0
                : Math.round(((foundTotal + partialTotal) * 100.0 / total) * 10.0) / 10.0;

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalReports", total);
        stats.put("resultDistribution", byResult);
        stats.put("statusDistribution", byStatus);
        stats.put("globalSuccessRate", globalSuccessRate);
        stats.put("topSearchedItems", topSearchedItems);
        stats.put("topReporters", topReporters);
        return stats;
    }

    /**
     * Returns all reports submitted by a specific user (caregiver or staff).
     */
    public List<SearchReport> getReportsByReporter(Long reportedBy) {
        return searchReportRepository.findByReportedByOrderBySearchDateDesc(reportedBy);
    }

    /**
     * Returns all search reports for items belonging to a patient.
     * Used for PATIENT-scoped view.
     */
    public List<SearchReport> getReportsByPatient(Long patientId) {
        List<Long> itemIds = lostItemRepository.findByPatientId(patientId)
                .stream().map(i -> i.getId()).toList();
        if (itemIds.isEmpty()) return List.of();
        return searchReportRepository.findAll().stream()
                .filter(r -> itemIds.contains(r.getLostItemId()))
                .sorted(Comparator.comparing(SearchReport::getSearchDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }
}
