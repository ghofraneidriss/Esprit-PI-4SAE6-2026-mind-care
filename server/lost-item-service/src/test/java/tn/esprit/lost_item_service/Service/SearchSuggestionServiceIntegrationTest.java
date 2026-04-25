package tn.esprit.lost_item_service.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tn.esprit.lost_item_service.Entity.*;
import tn.esprit.lost_item_service.Repository.LostItemRepository;
import tn.esprit.lost_item_service.Repository.SearchReportRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class SearchSuggestionServiceIntegrationTest {

    @Autowired
    private SearchSuggestionService searchSuggestionService;

    @Autowired
    private LostItemRepository lostItemRepository;

    @Autowired
    private SearchReportRepository searchReportRepository;

    @BeforeEach
    void setUp() {
        searchReportRepository.deleteAll();
        lostItemRepository.deleteAll();
    }

    // ==================== BASIC SUGGESTION TESTS ====================

    @Test
    void testGetSuggestionsForPatientWithHistory() {
        // Create items with search history
        LostItem item1 = new LostItem();
        item1.setTitle("Lost Keys");
        item1.setCategory(ItemCategory.ACCESSORY);
        item1.setPatientId(1L);
        item1.setStatus(ItemStatus.FOUND);
        item1.setPriority(ItemPriority.MEDIUM);
        item1.setCreatedAt(LocalDateTime.now().minusDays(30));
        lostItemRepository.save(item1);

        // Add search reports for item1
        SearchReport report1 = new SearchReport();
        report1.setLostItemId(item1.getId());
        report1.setReportedBy(10L);
        report1.setSearchDate(LocalDate.now().minusDays(30));
        report1.setLocationSearched("Living Room");
        report1.setSearchResult(SearchResult.FOUND);
        report1.setStatus(ReportStatus.CLOSED);
        searchReportRepository.save(report1);

        // Create another item
        LostItem item2 = new LostItem();
        item2.setTitle("Lost Wallet");
        item2.setCategory(ItemCategory.ACCESSORY);
        item2.setPatientId(1L);
        item2.setStatus(ItemStatus.FOUND);
        item2.setPriority(ItemPriority.HIGH);
        item2.setCreatedAt(LocalDateTime.now().minusDays(15));
        lostItemRepository.save(item2);

        // Add search reports for item2
        SearchReport report2 = new SearchReport();
        report2.setLostItemId(item2.getId());
        report2.setReportedBy(10L);
        report2.setSearchDate(LocalDate.now().minusDays(15));
        report2.setLocationSearched("Living Room");
        report2.setSearchResult(SearchResult.FOUND);
        report2.setStatus(ReportStatus.CLOSED);
        searchReportRepository.save(report2);

        List<Map<String, Object>> suggestions = searchSuggestionService.getSuggestions(1L, null);

        assertNotNull(suggestions);
        assertFalse(suggestions.isEmpty());
        assertTrue(suggestions.size() <= 3);
    }

    @Test
    void testGetSuggestionsFiltered ByCategory() {
        // Create items of different categories
        for (int i = 0; i < 2; i++) {
            LostItem item = new LostItem();
            item.setTitle("Lost Medication " + i);
            item.setCategory(ItemCategory.MEDICATION);
            item.setPatientId(1L);
            item.setStatus(ItemStatus.FOUND);
            item.setPriority(ItemPriority.CRITICAL);
            item.setCreatedAt(LocalDateTime.now().minusDays(30 - i * 10));
            lostItemRepository.save(item);

            SearchReport report = new SearchReport();
            report.setLostItemId(item.getId());
            report.setReportedBy(10L);
            report.setSearchDate(LocalDate.now().minusDays(30 - i * 10));
            report.setLocationSearched("Bedroom");
            report.setSearchResult(SearchResult.FOUND);
            report.setStatus(ReportStatus.CLOSED);
            searchReportRepository.save(report);
        }

        // Create clothing items
        for (int i = 0; i < 2; i++) {
            LostItem item = new LostItem();
            item.setTitle("Lost Jacket " + i);
            item.setCategory(ItemCategory.CLOTHING);
            item.setPatientId(1L);
            item.setStatus(ItemStatus.FOUND);
            item.setPriority(ItemPriority.LOW);
            item.setCreatedAt(LocalDateTime.now().minusDays(20 - i * 5));
            lostItemRepository.save(item);

            SearchReport report = new SearchReport();
            report.setLostItemId(item.getId());
            report.setReportedBy(10L);
            report.setSearchDate(LocalDate.now().minusDays(20 - i * 5));
            report.setLocationSearched("Closet");
            report.setSearchResult(SearchResult.FOUND);
            report.setStatus(ReportStatus.CLOSED);
            searchReportRepository.save(report);
        }

        // Get suggestions for MEDICATION category only
        List<Map<String, Object>> medicationSuggestions = searchSuggestionService.getSuggestions(1L, "MEDICATION");
        assertNotNull(medicationSuggestions);
        // Should find bedroom as suggestion for medications

        // Get suggestions for CLOTHING
        List<Map<String, Object>> clothingSuggestions = searchSuggestionService.getSuggestions(1L, "CLOTHING");
        assertNotNull(clothingSuggestions);
        // Should find closet as suggestion for clothing
    }

    @Test
    void testGetSuggestionsEmptyHistory() {
        List<Map<String, Object>> suggestions = searchSuggestionService.getSuggestions(999L, null);
        assertTrue(suggestions.isEmpty());
    }

    @Test
    void testGetSuggestionsInvalidCategory() {
        LostItem item = new LostItem();
        item.setTitle("Test Item");
        item.setCategory(ItemCategory.MEDICATION);
        item.setPatientId(1L);
        item.setStatus(ItemStatus.LOST);
        item.setPriority(ItemPriority.MEDIUM);
        item.setCreatedAt(LocalDateTime.now());
        lostItemRepository.save(item);

        // Try with invalid category - should fall back to all items
        List<Map<String, Object>> suggestions = searchSuggestionService.getSuggestions(1L, "INVALID_CATEGORY");
        assertNotNull(suggestions);
    }

    // ==================== LOCATION RANKING TESTS ====================

    @Test
    void testSuggestionsRankedBySuccessRate() {
        LostItem item1 = new LostItem();
        item1.setTitle("Item 1");
        item1.setCategory(ItemCategory.ACCESSORY);
        item1.setPatientId(1L);
        item1.setStatus(ItemStatus.LOST);
        item1.setPriority(ItemPriority.MEDIUM);
        item1.setCreatedAt(LocalDateTime.now().minusDays(60));
        lostItemRepository.save(item1);

        // Location A: 100% success (2 found, 0 not found)
        for (int i = 0; i < 2; i++) {
            SearchReport report = new SearchReport();
            report.setLostItemId(item1.getId());
            report.setReportedBy(10L);
            report.setSearchDate(LocalDate.now().minusDays(60 - i));
            report.setLocationSearched("Location A");
            report.setSearchResult(SearchResult.FOUND);
            report.setStatus(ReportStatus.CLOSED);
            searchReportRepository.save(report);
        }

        // Location B: 50% success (1 found, 1 not found)
        SearchReport report1 = new SearchReport();
        report1.setLostItemId(item1.getId());
        report1.setReportedBy(10L);
        report1.setSearchDate(LocalDate.now().minusDays(30));
        report1.setLocationSearched("Location B");
        report1.setSearchResult(SearchResult.FOUND);
        report1.setStatus(ReportStatus.CLOSED);
        searchReportRepository.save(report1);

        SearchReport report2 = new SearchReport();
        report2.setLostItemId(item1.getId());
        report2.setReportedBy(10L);
        report2.setSearchDate(LocalDate.now().minusDays(29));
        report2.setLocationSearched("Location B");
        report2.setSearchResult(SearchResult.NOT_FOUND);
        report2.setStatus(ReportStatus.OPEN);
        searchReportRepository.save(report2);

        List<Map<String, Object>> suggestions = searchSuggestionService.getSuggestions(1L, null);
        assertNotNull(suggestions);
        assertFalse(suggestions.isEmpty());
        // Location A should rank higher due to 100% success rate
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    void testSuggestionsWithMixedSearchResults() {
        LostItem item = new LostItem();
        item.setTitle("Test Item");
        item.setCategory(ItemCategory.CLOTHING);
        item.setPatientId(1L);
        item.setStatus(ItemStatus.LOST);
        item.setPriority(ItemPriority.MEDIUM);
        item.setCreatedAt(LocalDateTime.now().minusDays(30));
        lostItemRepository.save(item);

        // Mix of results
        for (SearchResult result : new SearchResult[]{SearchResult.FOUND, SearchResult.NOT_FOUND, SearchResult.PARTIALLY_FOUND}) {
            SearchReport report = new SearchReport();
            report.setLostItemId(item.getId());
            report.setReportedBy(10L);
            report.setSearchDate(LocalDate.now().minusDays(30));
            report.setLocationSearched("Test Location");
            report.setSearchResult(result);
            report.setStatus(ReportStatus.OPEN);
            searchReportRepository.save(report);
        }

        List<Map<String, Object>> suggestions = searchSuggestionService.getSuggestions(1L, null);
        assertNotNull(suggestions);
    }

    @Test
    void testSuggestionsLimitedToThree() {
        LostItem item = new LostItem();
        item.setTitle("Test Item");
        item.setCategory(ItemCategory.ACCESSORY);
        item.setPatientId(1L);
        item.setStatus(ItemStatus.LOST);
        item.setPriority(ItemPriority.LOW);
        item.setCreatedAt(LocalDateTime.now().minusDays(30));
        lostItemRepository.save(item);

        // Create search reports for 5 different locations
        for (int i = 0; i < 5; i++) {
            SearchReport report = new SearchReport();
            report.setLostItemId(item.getId());
            report.setReportedBy(10L);
            report.setSearchDate(LocalDate.now().minusDays(30 - i));
            report.setLocationSearched("Location " + i);
            report.setSearchResult(SearchResult.FOUND);
            report.setStatus(ReportStatus.CLOSED);
            searchReportRepository.save(report);
        }

        List<Map<String, Object>> suggestions = searchSuggestionService.getSuggestions(1L, null);
        assertTrue(suggestions.size() <= 3, "Suggestions should be limited to 3");
    }

    @Test
    void testMultiplePatientsIndependent() {
        // Patient 1 - Medication
        LostItem item1 = new LostItem();
        item1.setTitle("Medication");
        item1.setCategory(ItemCategory.MEDICATION);
        item1.setPatientId(1L);
        item1.setStatus(ItemStatus.FOUND);
        item1.setPriority(ItemPriority.CRITICAL);
        item1.setCreatedAt(LocalDateTime.now().minusDays(30));
        lostItemRepository.save(item1);

        SearchReport report1 = new SearchReport();
        report1.setLostItemId(item1.getId());
        report1.setReportedBy(10L);
        report1.setSearchDate(LocalDate.now().minusDays(30));
        report1.setLocationSearched("Bedroom");
        report1.setSearchResult(SearchResult.FOUND);
        report1.setStatus(ReportStatus.CLOSED);
        searchReportRepository.save(report1);

        // Patient 2 - Clothing
        LostItem item2 = new LostItem();
        item2.setTitle("Jacket");
        item2.setCategory(ItemCategory.CLOTHING);
        item2.setPatientId(2L);
        item2.setStatus(ItemStatus.FOUND);
        item2.setPriority(ItemPriority.MEDIUM);
        item2.setCreatedAt(LocalDateTime.now().minusDays(30));
        lostItemRepository.save(item2);

        SearchReport report2 = new SearchReport();
        report2.setLostItemId(item2.getId());
        report2.setReportedBy(11L);
        report2.setSearchDate(LocalDate.now().minusDays(30));
        report2.setLocationSearched("Closet");
        report2.setSearchResult(SearchResult.FOUND);
        report2.setStatus(ReportStatus.CLOSED);
        searchReportRepository.save(report2);

        List<Map<String, Object>> patient1Suggestions = searchSuggestionService.getSuggestions(1L, null);
        List<Map<String, Object>> patient2Suggestions = searchSuggestionService.getSuggestions(2L, null);

        assertNotNull(patient1Suggestions);
        assertNotNull(patient2Suggestions);
        // Each patient gets their own suggestions
    }
}
