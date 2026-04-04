package tn.esprit.lost_item_service.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.lost_item_service.Entity.SearchReport;
import tn.esprit.lost_item_service.Service.SearchReportService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search-reports")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class SearchReportController {

    private final SearchReportService searchReportService;

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
}
