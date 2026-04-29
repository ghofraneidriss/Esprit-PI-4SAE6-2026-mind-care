package tn.esprit.lost_item_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tn.esprit.lost_item_service.entity.*;
import tn.esprit.lost_item_service.repository.LostItemRepository;
import tn.esprit.lost_item_service.repository.SearchReportRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class RecoveryStrategyServiceIntegrationTest {

    @Autowired
    private RecoveryStrategyService recoveryStrategyService;

    @Autowired
    private LostItemRepository lostItemRepository;

    @Autowired
    private SearchReportRepository searchReportRepository;

    private LostItem testItem;

    @BeforeEach
    void setUp() {
        searchReportRepository.deleteAll();
        lostItemRepository.deleteAll();

        testItem = new LostItem();
        testItem.setTitle("Test Item");
        testItem.setCategory(ItemCategory.MEDICATION);
        testItem.setPatientId(1L);
        testItem.setStatus(ItemStatus.LOST);
        testItem.setPriority(ItemPriority.CRITICAL);
        testItem.setLastSeenLocation("Living Room");
        testItem.setLastSeenDate(LocalDate.now().minusDays(5));
        testItem.setCreatedAt(LocalDateTime.now().minusDays(5));
        lostItemRepository.save(testItem);
    }

    @Test
    void testGenerateRecoveryStrategy() {
        Map<String, Object> strategy = recoveryStrategyService.getRecoveryStrategy(testItem.getId());
        assertNotNull(strategy);
        assertNotNull(strategy.get("itemId"));
    }

    @Test
    void testGenerateStrategyWithSearchHistory() {
        // Add search reports
        for (int i = 0; i < 3; i++) {
            SearchReport report = new SearchReport();
            report.setLostItemId(testItem.getId());
            report.setReportedBy(10L);
            report.setSearchDate(LocalDate.now().minusDays(3 - i));
            report.setLocationSearched("Location " + i);
            report.setSearchResult(SearchResult.NOT_FOUND);
            report.setStatus(ReportStatus.OPEN);
            searchReportRepository.save(report);
        }

        Map<String, Object> strategy = recoveryStrategyService.getRecoveryStrategy(testItem.getId());
        assertNotNull(strategy);
        assertTrue(strategy.size() > 0);
    }

    @Test
    void testStrategyForDifferentCategories() {
        for (ItemCategory category : new ItemCategory[]{ItemCategory.CLOTHING, ItemCategory.DOCUMENT, ItemCategory.ACCESSORY}) {
            LostItem item = new LostItem();
            item.setTitle("Item " + category);
            item.setCategory(category);
            item.setPatientId(2L);
            item.setStatus(ItemStatus.LOST);
            item.setPriority(ItemPriority.MEDIUM);
            item.setLastSeenLocation("Unknown");
            item.setCreatedAt(LocalDateTime.now());
            LostItem saved = lostItemRepository.save(item);

            Map<String, Object> strategy = recoveryStrategyService.getRecoveryStrategy(saved.getId());
            assertNotNull(strategy);
        }
    }
}
