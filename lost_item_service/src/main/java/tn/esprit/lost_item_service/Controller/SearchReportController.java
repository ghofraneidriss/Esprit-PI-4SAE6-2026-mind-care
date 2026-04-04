package tn.esprit.lost_item_service.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.lost_item_service.Entity.ReportStatus;
import tn.esprit.lost_item_service.Entity.SearchReport;
import tn.esprit.lost_item_service.Entity.SearchResult;
import tn.esprit.lost_item_service.Service.SearchReportService;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search-reports")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class SearchReportController {

    private final SearchReportService searchReportService;

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<SearchReport> createSearchReport(@Valid @RequestBody SearchReport report) {
        return new ResponseEntity<>(searchReportService.createSearchReport(report), HttpStatus.CREATED);
    }

    @GetMapping("/lost-item/{lostItemId}")
    public ResponseEntity<List<SearchReport>> getSearchReportsByLostItemId(@PathVariable Long lostItemId) {
        return ResponseEntity.ok(searchReportService.getSearchReportsByLostItemId(lostItemId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SearchReport> getSearchReportById(@PathVariable Long id) {
        return ResponseEntity.ok(searchReportService.getSearchReportById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SearchReport> updateSearchReport(@PathVariable Long id, @Valid @RequestBody SearchReport report) {
        return ResponseEntity.ok(searchReportService.updateSearchReport(id, report));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteSearchReport(@PathVariable Long id) {
        searchReportService.deleteSearchReport(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Search report id=" + id + " deleted.");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/lost-item/{lostItemId}/open-count")
    public ResponseEntity<Map<String, Object>> getOpenReportsCount(@PathVariable Long lostItemId) {
        long count = searchReportService.getOpenReportsCount(lostItemId);
        Map<String, Object> response = new HashMap<>();
        response.put("lostItemId", lostItemId);
        response.put("openCount", count);
        return ResponseEntity.ok(response);
    }

    // ── Advanced Search Log ───────────────────────────────────────────────────

    /**
     * Advanced filter: all params optional.
     * GET /api/search-reports/search?lostItemId=1&reportedBy=5&searchResult=NOT_FOUND
     *   &status=OPEN&locationKeyword=kitchen&from=2025-01-01&to=2025-12-31
     */
    @GetMapping("/search")
    public ResponseEntity<List<SearchReport>> advancedSearch(
            @RequestParam(required = false) Long lostItemId,
            @RequestParam(required = false) Long reportedBy,
            @RequestParam(required = false) SearchResult searchResult,
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) String locationKeyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(searchReportService.advancedSearch(
                lostItemId, reportedBy, searchResult, status, locationKeyword, from, to
        ));
    }

    /**
     * Full search timeline for a lost item: per-day breakdown, location heatmap, success rate.
     * GET /api/search-reports/lost-item/{lostItemId}/timeline
     */
    @GetMapping("/lost-item/{lostItemId}/timeline")
    public ResponseEntity<Map<String, Object>> getSearchTimeline(@PathVariable Long lostItemId) {
        return ResponseEntity.ok(searchReportService.getSearchTimeline(lostItemId));
    }

    /**
     * Global search log statistics across all items.
     * GET /api/search-reports/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getGlobalSearchLogStats() {
        return ResponseEntity.ok(searchReportService.getGlobalSearchLogStats());
    }

    /**
     * All reports submitted by a specific user (caregiver / staff member).
     * GET /api/search-reports/reporter/{reportedBy}
     */
    @GetMapping("/reporter/{reportedBy}")
    public ResponseEntity<List<SearchReport>> getReportsByReporter(@PathVariable Long reportedBy) {
        return ResponseEntity.ok(searchReportService.getReportsByReporter(reportedBy));
    }

    /**
     * Scoped: returns only search reports for items owned by a given patient.
     * The frontend calls this for PATIENT-role users (my reports only).
     * GET /api/search-reports/patient/{patientId}
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<SearchReport>> getReportsByPatient(@PathVariable Long patientId) {
        // Collect all lost items for this patient, then return their reports
        return ResponseEntity.ok(searchReportService.getReportsByPatient(patientId));
    }
}
