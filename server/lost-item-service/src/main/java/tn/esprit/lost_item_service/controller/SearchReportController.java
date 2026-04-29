package tn.esprit.lost_item_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.lost_item_service.dto.DTOMapper;
import tn.esprit.lost_item_service.dto.SearchReportDTO;
import tn.esprit.lost_item_service.dto.CreateSearchReportRequest;
import tn.esprit.lost_item_service.dto.UpdateSearchReportRequest;
import tn.esprit.lost_item_service.entity.LostItem;
import tn.esprit.lost_item_service.entity.ReportStatus;
import tn.esprit.lost_item_service.entity.SearchReport;
import tn.esprit.lost_item_service.entity.SearchResult;
import tn.esprit.lost_item_service.entity.ItemStatus;
import tn.esprit.lost_item_service.service.AuthorizationService;
import tn.esprit.lost_item_service.service.LostItemService;
import tn.esprit.lost_item_service.service.RecoveryStrategyService;
import tn.esprit.lost_item_service.service.SearchReportService;
import tn.esprit.lost_item_service.service.SearchSuggestionService;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search-reports")
@RequiredArgsConstructor
public class SearchReportController {

    private final SearchReportService searchReportService;
    private final AuthorizationService authorizationService;
    private final LostItemService lostItemService;
    private final RecoveryStrategyService recoveryStrategyService;
    private final SearchSuggestionService searchSuggestionService;

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<Map<String, Object>> createSearchReport(
            @RequestBody CreateSearchReportRequest request,
            @RequestHeader(value = "X-User-Id",   required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        authorizationService.checkItemIdAccess(request.getLostItemId(), userId, userRole);

        SearchReport report = DTOMapper.toSearchReport(request);
        SearchReport saved = searchReportService.createSearchReport(report);

        // If the search result is FOUND, cascade: update item status, close open reports, resolve alerts
        if (saved.getSearchResult() == SearchResult.FOUND) {
            lostItemService.markAsFound(saved.getLostItemId());
        }

        LostItem item = lostItemService.getLostItemById(saved.getLostItemId());
        String category = item.getCategory() != null ? item.getCategory().name() : null;

        Map<String, Object> recoveryStrategy = recoveryStrategyService.getRecoveryStrategy(saved.getLostItemId());
        List<Map<String, Object>> suggestions = searchSuggestionService.getSuggestions(item.getPatientId(), category);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("report", DTOMapper.toSearchReportDTO(saved));
        response.put("recoveryStrategy", recoveryStrategy);
        response.put("searchSuggestions", suggestions);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/lost-item/{lostItemId}")
    public ResponseEntity<List<SearchReportDTO>> getSearchReportsByLostItemId(
            @PathVariable Long lostItemId,
            @RequestHeader(value = "X-User-Id",   required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        authorizationService.checkReportListAccess(lostItemId, userId, userRole);
        return ResponseEntity.ok(DTOMapper.toSearchReportDTOList(searchReportService.getSearchReportsByLostItemId(lostItemId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SearchReportDTO> getSearchReportById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id",   required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        SearchReport report = authorizationService.checkReportAccess(id, userId, userRole);
        return ResponseEntity.ok(DTOMapper.toSearchReportDTO(report));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SearchReportDTO> updateSearchReport(
            @PathVariable Long id,
            @RequestBody UpdateSearchReportRequest request,
            @RequestHeader(value = "X-User-Id",   required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        authorizationService.checkReportAccess(id, userId, userRole);
        SearchReport report = DTOMapper.toSearchReportForUpdate(request);
        SearchReport updated = searchReportService.updateSearchReport(id, report);
        return ResponseEntity.ok(DTOMapper.toSearchReportDTO(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteSearchReport(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id",   required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        authorizationService.checkReportAccess(id, userId, userRole);
        searchReportService.deleteSearchReport(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Search report id=" + id + " deleted.");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/lost-item/{lostItemId}/open-count")
    public ResponseEntity<Map<String, Object>> getOpenReportsCount(
            @PathVariable Long lostItemId,
            @RequestHeader(value = "X-User-Id",   required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        authorizationService.checkReportListAccess(lostItemId, userId, userRole);
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
    public ResponseEntity<List<SearchReportDTO>> advancedSearch(
            @RequestParam(required = false) Long lostItemId,
            @RequestParam(required = false) Long reportedBy,
            @RequestParam(required = false) SearchResult searchResult,
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) String locationKeyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(DTOMapper.toSearchReportDTOList(searchReportService.advancedSearch(
                lostItemId, reportedBy, searchResult, status, locationKeyword, from, to
        )));
    }

    /**
     * Full search timeline for a lost item: per-day breakdown, location heatmap, success rate.
     * GET /api/search-reports/lost-item/{lostItemId}/timeline
     */
    @GetMapping("/lost-item/{lostItemId}/timeline")
    public ResponseEntity<Map<String, Object>> getSearchTimeline(
            @PathVariable Long lostItemId,
            @RequestHeader(value = "X-User-Id",   required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        authorizationService.checkReportListAccess(lostItemId, userId, userRole);
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
    public ResponseEntity<List<SearchReportDTO>> getReportsByReporter(@PathVariable Long reportedBy) {
        return ResponseEntity.ok(DTOMapper.toSearchReportDTOList(searchReportService.getReportsByReporter(reportedBy)));
    }

    /**
     * Scoped: returns only search reports for items owned by a given patient.
     * The frontend calls this for PATIENT-role users (my reports only).
     * GET /api/search-reports/patient/{patientId}
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<SearchReportDTO>> getReportsByPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(DTOMapper.toSearchReportDTOList(searchReportService.getReportsByPatient(patientId)));
    }
}
