package tn.esprit.lost_item_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tn.esprit.lost_item_service.entity.*;
import tn.esprit.lost_item_service.exception.DuplicateReportException;
import tn.esprit.lost_item_service.repository.LostItemRepository;
import tn.esprit.lost_item_service.repository.SearchReportRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class SearchReportServiceIntegrationTest {

    @Autowired
    private SearchReportService searchReportService;

    @Autowired
    private SearchReportRepository searchReportRepository;

    @Autowired
    private LostItemRepository lostItemRepository;

    private SearchReport testReport;
    private LostItem testItem;

    @BeforeEach
    void setUp() {
        searchReportRepository.deleteAll();
        lostItemRepository.deleteAll();

        // Create a lost item first
        testItem = new LostItem();
        testItem.setTitle("Test Item");
        testItem.setCategory(ItemCategory.MEDICATION);
        testItem.setPatientId(1L);
        testItem.setStatus(ItemStatus.LOST);
        testItem.setPriority(ItemPriority.CRITICAL);
        testItem.setCreatedAt(LocalDateTime.now());
        testItem.setUpdatedAt(LocalDateTime.now());
        lostItemRepository.save(testItem);

        testReport = new SearchReport();
        testReport.setLostItemId(testItem.getId());
        testReport.setReportedBy(10L);
        testReport.setSearchDate(LocalDate.now());
        testReport.setLocationSearched("Living Room");
        testReport.setSearchResult(SearchResult.NOT_FOUND);
        testReport.setStatus(ReportStatus.OPEN);
        testReport.setCreatedAt(LocalDateTime.now());
    }

    // ==================== BASIC CRUD TESTS ====================

    @Test
    void testCreateSearchReport() {
        SearchReport created = searchReportService.createSearchReport(testReport);

        assertNotNull(created.getId());
        assertEquals("Living Room", created.getLocationSearched());
        assertEquals(SearchResult.NOT_FOUND, created.getSearchResult());
        assertEquals(ReportStatus.OPEN, created.getStatus());
    }

    @Test
    void testCreateDuplicateSearchReportThrowsException() {
        searchReportService.createSearchReport(testReport);

        SearchReport duplicate = new SearchReport();
        duplicate.setLostItemId(testItem.getId());
        duplicate.setReportedBy(10L);
        duplicate.setSearchDate(LocalDate.now());
        duplicate.setLocationSearched("Kitchen");
        duplicate.setSearchResult(SearchResult.NOT_FOUND);
        duplicate.setStatus(ReportStatus.OPEN);

        assertThrows(DuplicateReportException.class, () -> searchReportService.createSearchReport(duplicate));
    }

    @Test
    void testGetSearchReportById() {
        SearchReport created = searchReportService.createSearchReport(testReport);

        SearchReport retrieved = searchReportService.getSearchReportById(created.getId());
        assertNotNull(retrieved);
        assertEquals(created.getId(), retrieved.getId());
        assertEquals("Living Room", retrieved.getLocationSearched());
    }

    @Test
    void testGetSearchReportByIdNotFound() {
        assertThrows(RuntimeException.class, () -> searchReportService.getSearchReportById(999L));
    }

    @Test
    void testUpdateSearchReport() {
        SearchReport created = searchReportService.createSearchReport(testReport);

        SearchReport updated = new SearchReport();
        updated.setLostItemId(testItem.getId());
        updated.setReportedBy(10L);
        updated.setSearchDate(LocalDate.now());
        updated.setLocationSearched("Bedroom");
        updated.setSearchResult(SearchResult.FOUND);
        updated.setStatus(ReportStatus.OPEN);

        SearchReport result = searchReportService.updateSearchReport(created.getId(), updated);
        assertEquals("Bedroom", result.getLocationSearched());
        assertEquals(SearchResult.FOUND, result.getSearchResult());
    }

    @Test
    void testDeleteSearchReport() {
        SearchReport created = searchReportService.createSearchReport(testReport);
        Long id = created.getId();

        searchReportService.deleteSearchReport(id);

        assertThrows(RuntimeException.class, () -> searchReportService.getSearchReportById(id));
    }

    @Test
    void testDeleteNonExistentSearchReport() {
        assertThrows(RuntimeException.class, () -> searchReportService.deleteSearchReport(999L));
    }

    // ==================== SEARCH TESTS ====================

    @Test
    void testAdvancedSearchBySearchResult() {
        // Create multiple reports with different results
        SearchReport found = new SearchReport();
        found.setLostItemId(testItem.getId());
        found.setReportedBy(10L);
        found.setSearchDate(LocalDate.now().minusDays(1));
        found.setLocationSearched("Kitchen");
        found.setSearchResult(SearchResult.FOUND);
        found.setStatus(ReportStatus.OPEN);
        searchReportService.createSearchReport(found);

        searchReportService.createSearchReport(testReport); // NOT_FOUND

        List<SearchReport> results = searchReportService.advancedSearch(
                testItem.getId(), null, SearchResult.FOUND, null, null, null, null);

        assertEquals(1, results.size());
        assertEquals(SearchResult.FOUND, results.get(0).getSearchResult());
    }

    @Test
    void testAdvancedSearchByLocation() {
        searchReportService.createSearchReport(testReport); // Living Room

        SearchReport kitchen = new SearchReport();
        kitchen.setLostItemId(testItem.getId());
        kitchen.setReportedBy(10L);
        kitchen.setSearchDate(LocalDate.now().minusDays(1));
        kitchen.setLocationSearched("Kitchen");
        kitchen.setSearchResult(SearchResult.NOT_FOUND);
        kitchen.setStatus(ReportStatus.OPEN);
        searchReportService.createSearchReport(kitchen);

        List<SearchReport> results = searchReportService.advancedSearch(
                testItem.getId(), null, null, null, "living", null, null);

        assertTrue(results.stream().allMatch(r -> r.getLocationSearched().toLowerCase().contains("living")));
    }

    @Test
    void testAdvancedSearchByStatus() {
        searchReportService.createSearchReport(testReport); // OPEN

        SearchReport closed = new SearchReport();
        closed.setLostItemId(testItem.getId());
        closed.setReportedBy(10L);
        closed.setSearchDate(LocalDate.now().minusDays(1));
        closed.setLocationSearched("Basement");
        closed.setSearchResult(SearchResult.FOUND);
        closed.setStatus(ReportStatus.CLOSED);
        searchReportService.createSearchReport(closed);

        List<SearchReport> openReports = searchReportService.advancedSearch(
                testItem.getId(), null, null, ReportStatus.OPEN, null, null, null);

        assertTrue(openReports.stream().allMatch(r -> r.getStatus() == ReportStatus.OPEN));
    }

    @Test
    void testAdvancedSearchNoFilters() {
        searchReportService.createSearchReport(testReport);

        SearchReport report2 = new SearchReport();
        report2.setLostItemId(testItem.getId());
        report2.setReportedBy(11L);
        report2.setSearchDate(LocalDate.now().minusDays(1));
        report2.setLocationSearched("Kitchen");
        report2.setSearchResult(SearchResult.NOT_FOUND);
        report2.setStatus(ReportStatus.OPEN);
        searchReportService.createSearchReport(report2);

        List<SearchReport> all = searchReportService.advancedSearch(
                testItem.getId(), null, null, null, null, null, null);

        assertTrue(all.size() >= 2);
    }

    // ==================== TIMELINE AND STATISTICS TESTS ====================

    @Test
    void testGetSearchTimeline() {
        // Create multiple reports
        for (int i = 0; i < 3; i++) {
            SearchReport report = new SearchReport();
            report.setLostItemId(testItem.getId());
            report.setReportedBy(10L);
            report.setSearchDate(LocalDate.now().minusDays(i));
            report.setLocationSearched("Location " + i);
            report.setSearchResult(i == 0 ? SearchResult.FOUND : SearchResult.NOT_FOUND);
            report.setStatus(i == 0 ? ReportStatus.CLOSED : ReportStatus.OPEN);
            searchReportService.createSearchReport(report);
        }

        Map<String, Object> timeline = searchReportService.getSearchTimeline(testItem.getId());

        assertEquals(3L, timeline.get("totalSearches"));
        assertTrue((Double) timeline.get("successRate") > 0);
        assertNotNull(timeline.get("timeline"));
        assertNotNull(timeline.get("locationFrequency"));
    }

    @Test
    void testGetSearchTimelineEmpty() {
        Map<String, Object> timeline = searchReportService.getSearchTimeline(testItem.getId());

        assertEquals(0L, timeline.get("totalSearches"));
        assertEquals(0.0, timeline.get("successRate"));
        assertTrue(((List<?>) timeline.get("timeline")).isEmpty());
    }

    @Test
    void testGetSearchTimelineAllFound() {
        for (int i = 0; i < 3; i++) {
            SearchReport report = new SearchReport();
            report.setLostItemId(testItem.getId());
            report.setReportedBy(10L);
            report.setSearchDate(LocalDate.now().minusDays(i));
            report.setLocationSearched("Found Location " + i);
            report.setSearchResult(SearchResult.FOUND);
            report.setStatus(ReportStatus.CLOSED);
            searchReportService.createSearchReport(report);
        }

        Map<String, Object> timeline = searchReportService.getSearchTimeline(testItem.getId());

        assertEquals(3L, timeline.get("foundCount"));
        assertTrue((Double) timeline.get("successRate") >= 100.0);
    }

    @Test
    void testGetSearchTimelineLocationFrequency() {
        // Search same location multiple times
        for (int i = 0; i < 3; i++) {
            SearchReport report = new SearchReport();
            report.setLostItemId(testItem.getId());
            report.setReportedBy(10L);
            report.setSearchDate(LocalDate.now().minusDays(i));
            report.setLocationSearched("Bedroom");
            report.setSearchResult(SearchResult.NOT_FOUND);
            report.setStatus(ReportStatus.OPEN);
            searchReportService.createSearchReport(report);
        }

        Map<String, Object> timeline = searchReportService.getSearchTimeline(testItem.getId());

        @SuppressWarnings("unchecked")
        Map<String, Long> freq = (Map<String, Long>) timeline.get("locationFrequency");
        assertEquals(3L, freq.get("Bedroom"));
    }

    // ==================== PATIENT REPORTS TESTS ====================

    @Test
    void testGetReportsByPatient() {
        searchReportService.createSearchReport(testReport);

        List<SearchReport> reports = searchReportService.getReportsByPatient(1L);
        assertTrue(reports.size() > 0);
        assertTrue(reports.stream().allMatch(r -> r.getLostItemId().equals(testItem.getId())));
    }

    @Test
    void testGetReportsByPatientNoItems() {
        List<SearchReport> reports = searchReportService.getReportsByPatient(999L);
        assertTrue(reports.isEmpty());
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    void testCreateMultipleReportsForSameItem() {
        for (int i = 0; i < 5; i++) {
            SearchReport report = new SearchReport();
            report.setLostItemId(testItem.getId());
            report.setReportedBy(10L + i);
            report.setSearchDate(LocalDate.now().minusDays(i));
            report.setLocationSearched("Location " + i);
            report.setSearchResult(SearchResult.values()[i % SearchResult.values().length]);
            report.setStatus(ReportStatus.OPEN);
            searchReportService.createSearchReport(report);
        }

        Map<String, Object> timeline = searchReportService.getSearchTimeline(testItem.getId());
        assertEquals(5L, timeline.get("totalSearches"));
    }

    @Test
    void testReportStatusTransition() {
        SearchReport created = searchReportService.createSearchReport(testReport);
        assertEquals(ReportStatus.OPEN, created.getStatus());

        SearchReport updated = new SearchReport();
        updated.setLostItemId(created.getLostItemId());
        updated.setReportedBy(created.getReportedBy());
        updated.setSearchDate(created.getSearchDate());
        updated.setLocationSearched(created.getLocationSearched());
        updated.setSearchResult(SearchResult.FOUND);
        updated.setStatus(ReportStatus.CLOSED);

        SearchReport result = searchReportService.updateSearchReport(created.getId(), updated);
        assertEquals(ReportStatus.CLOSED, result.getStatus());
    }
}
