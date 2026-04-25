package tn.esprit.lost_item_service.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import tn.esprit.lost_item_service.Entity.*;
import tn.esprit.lost_item_service.Repository.LostItemAlertRepository;
import tn.esprit.lost_item_service.Repository.LostItemRepository;
import tn.esprit.lost_item_service.Repository.SearchReportRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class LostItemServiceIntegrationTest {

    @Autowired
    private LostItemService lostItemService;

    @Autowired
    private LostItemRepository lostItemRepository;

    @Autowired
    private SearchReportRepository searchReportRepository;

    @Autowired
    private LostItemAlertRepository alertRepository;

    private LostItem testItem;

    @BeforeEach
    void setUp() {
        lostItemRepository.deleteAll();
        searchReportRepository.deleteAll();
        alertRepository.deleteAll();

        testItem = new LostItem();
        testItem.setTitle("Test Medication");
        testItem.setDescription("Lost medication");
        testItem.setCategory(ItemCategory.MEDICATION);
        testItem.setPatientId(1L);
        testItem.setCaregiverId(10L);
        testItem.setStatus(ItemStatus.LOST);
        testItem.setPriority(ItemPriority.CRITICAL);
        testItem.setLastSeenLocation("Living room");
        testItem.setLastSeenDate(LocalDate.now().minusDays(1));
        testItem.setCreatedAt(LocalDateTime.now());
        testItem.setUpdatedAt(LocalDateTime.now());
    }

    // ==================== BASIC CRUD TESTS ====================

    @Test
    void testCreateLostItem() {
        LostItem created = lostItemService.createLostItem(testItem);

        assertNotNull(created.getId());
        assertEquals("Test Medication", created.getTitle());
        assertEquals(ItemStatus.LOST, created.getStatus());
        assertEquals(ItemCategory.MEDICATION, created.getCategory());
    }

    @Test
    void testGetAllLostItems() {
        lostItemService.createLostItem(testItem);

        LostItem item2 = new LostItem();
        item2.setTitle("Test Jacket");
        item2.setCategory(ItemCategory.CLOTHING);
        item2.setPatientId(1L);
        item2.setStatus(ItemStatus.LOST);
        item2.setPriority(ItemPriority.MEDIUM);
        item2.setCreatedAt(LocalDateTime.now());
        lostItemService.createLostItem(item2);

        List<LostItem> items = lostItemService.getAllLostItems();
        assertTrue(items.size() >= 2);
    }

    @Test
    void testGetLostItemById() {
        LostItem created = lostItemService.createLostItem(testItem);

        LostItem retrieved = lostItemService.getLostItemById(created.getId());
        assertNotNull(retrieved);
        assertEquals(created.getId(), retrieved.getId());
        assertEquals("Test Medication", retrieved.getTitle());
    }

    @Test
    void testGetLostItemByIdNotFound() {
        assertThrows(RuntimeException.class, () -> lostItemService.getLostItemById(999L));
    }

    @Test
    void testUpdateLostItem() {
        LostItem created = lostItemService.createLostItem(testItem);

        LostItem updated = new LostItem();
        updated.setTitle("Updated Title");
        updated.setDescription("Updated description");
        updated.setCategory(ItemCategory.DOCUMENT);
        updated.setPatientId(2L);
        updated.setCaregiverId(20L);
        updated.setLastSeenLocation("Kitchen");
        updated.setLastSeenDate(LocalDate.now());
        updated.setStatus(ItemStatus.SEARCHING);
        updated.setPriority(ItemPriority.HIGH);

        LostItem result = lostItemService.updateLostItem(created.getId(), updated);
        assertEquals("Updated Title", result.getTitle());
        assertEquals(ItemCategory.DOCUMENT, result.getCategory());
        assertEquals(ItemStatus.SEARCHING, result.getStatus());
    }

    @Test
    void testMarkAsFound() {
        LostItem created = lostItemService.createLostItem(testItem);

        LostItem found = lostItemService.markAsFound(created.getId());
        assertEquals(ItemStatus.FOUND, found.getStatus());
    }

    @Test
    void testDeleteLostItem() {
        LostItem created = lostItemService.createLostItem(testItem);
        Long id = created.getId();

        lostItemService.deleteLostItem(id);

        LostItem deleted = lostItemService.getLostItemById(id);
        assertEquals(ItemStatus.CLOSED, deleted.getStatus());
    }

    // ==================== PAGINATION TESTS ====================

    @Test
    void testGetPatientLostItemsWithPagination() {
        // Create multiple items
        for (int i = 0; i < 5; i++) {
            LostItem item = new LostItem();
            item.setTitle("Item " + i);
            item.setCategory(ItemCategory.MEDICATION);
            item.setPatientId(1L);
            item.setStatus(ItemStatus.LOST);
            item.setPriority(ItemPriority.MEDIUM);
            item.setCreatedAt(LocalDateTime.now().minusDays(i));
            lostItemService.createLostItem(item);
        }

        Page<LostItem> page = lostItemService.getPatientLostItems(1L, null, null, 0, 3);
        assertEquals(3, page.getContent().size());
        assertTrue(page.getTotalElements() >= 5);
    }

    @Test
    void testGetPatientLostItemsByStatus() {
        lostItemService.createLostItem(testItem);

        LostItem item2 = new LostItem();
        item2.setTitle("Found Item");
        item2.setCategory(ItemCategory.CLOTHING);
        item2.setPatientId(1L);
        item2.setStatus(ItemStatus.FOUND);
        item2.setPriority(ItemPriority.LOW);
        item2.setCreatedAt(LocalDateTime.now());
        lostItemService.createLostItem(item2);

        Page<LostItem> lostItems = lostItemService.getPatientLostItems(1L, ItemStatus.LOST, null, 0, 10);
        assertTrue(lostItems.getContent().stream().allMatch(i -> i.getStatus() == ItemStatus.LOST));
    }

    @Test
    void testGetPatientLostItemsByCategory() {
        lostItemService.createLostItem(testItem);

        LostItem item2 = new LostItem();
        item2.setTitle("Lost Jacket");
        item2.setCategory(ItemCategory.CLOTHING);
        item2.setPatientId(1L);
        item2.setStatus(ItemStatus.LOST);
        item2.setPriority(ItemPriority.MEDIUM);
        item2.setCreatedAt(LocalDateTime.now());
        lostItemService.createLostItem(item2);

        Page<LostItem> medications = lostItemService.getPatientLostItems(1L, null, ItemCategory.MEDICATION, 0, 10);
        assertTrue(medications.getContent().stream().allMatch(i -> i.getCategory() == ItemCategory.MEDICATION));
    }

    // ==================== ALERT GENERATION TESTS ====================

    @Test
    void testCreateMedicationItemGeneratesAlert() {
        testItem.setCategory(ItemCategory.MEDICATION);
        testItem.setStatus(ItemStatus.LOST);

        lostItemService.createLostItem(testItem);

        List<LostItemAlert> alerts = alertRepository.findByPatientId(1L);
        assertTrue(alerts.size() > 0);
        assertTrue(alerts.stream().anyMatch(a -> a.getLevel() == AlertLevel.CRITICAL));
    }

    @Test
    void testCreateFoundItemGeneratesNoAlert() {
        testItem.setStatus(ItemStatus.FOUND);

        lostItemService.createLostItem(testItem);

        List<LostItemAlert> alerts = alertRepository.findByPatientId(1L);
        assertTrue(alerts.isEmpty());
    }

    // ==================== RISK CALCULATION TESTS ====================

    @Test
    void testCalculatePatientItemRiskLow() {
        LostItem item = new LostItem();
        item.setTitle("Found Item");
        item.setCategory(ItemCategory.ACCESSORY);
        item.setPatientId(2L);
        item.setStatus(ItemStatus.FOUND);
        item.setPriority(ItemPriority.LOW);
        item.setCreatedAt(LocalDateTime.now());
        lostItemService.createLostItem(item);

        Map<String, Object> risk = lostItemService.calculatePatientItemRisk(2L);
        assertEquals("LOW", risk.get("riskLevel"));
        assertFalse((Boolean) risk.get("hasMedicationLost"));
    }

    @Test
    void testCalculatePatientItemRiskWithMultipleItems() {
        // Create multiple lost items
        for (int i = 0; i < 3; i++) {
            LostItem item = new LostItem();
            item.setTitle("Item " + i);
            item.setCategory(i == 0 ? ItemCategory.MEDICATION : ItemCategory.CLOTHING);
            item.setPatientId(1L);
            item.setStatus(ItemStatus.LOST);
            item.setPriority(i == 0 ? ItemPriority.CRITICAL : ItemPriority.MEDIUM);
            item.setCreatedAt(LocalDateTime.now().minusDays(i));
            lostItemService.createLostItem(item);
        }

        Map<String, Object> risk = lostItemService.calculatePatientItemRisk(1L);
        assertNotNull(risk.get("riskLevel"));
        assertTrue((Boolean) risk.get("hasMedicationLost"));
        assertNotNull(risk.get("riskScore"));
    }

    // ==================== FREQUENT LOSING DETECTION TESTS ====================

    @Test
    void testDetectFrequentLosingIncreasingTrend() {
        // Create items in different time periods to show increasing trend
        // Oldest period: 1 item
        LostItem item1 = new LostItem();
        item1.setTitle("Old Item");
        item1.setCategory(ItemCategory.CLOTHING);
        item1.setPatientId(1L);
        item1.setStatus(ItemStatus.LOST);
        item1.setPriority(ItemPriority.LOW);
        item1.setCreatedAt(LocalDateTime.now().minusDays(90));
        lostItemService.createLostItem(item1);

        // Middle period: 2 items
        for (int i = 0; i < 2; i++) {
            LostItem item = new LostItem();
            item.setTitle("Middle Item " + i);
            item.setCategory(ItemCategory.CLOTHING);
            item.setPatientId(1L);
            item.setStatus(ItemStatus.LOST);
            item.setPriority(ItemPriority.LOW);
            item.setCreatedAt(LocalDateTime.now().minusDays(45 - i));
            lostItemService.createLostItem(item);
        }

        // Recent period: 3 items
        for (int i = 0; i < 3; i++) {
            LostItem item = new LostItem();
            item.setTitle("Recent Item " + i);
            item.setCategory(ItemCategory.CLOTHING);
            item.setPatientId(1L);
            item.setStatus(ItemStatus.LOST);
            item.setPriority(ItemPriority.LOW);
            item.setCreatedAt(LocalDateTime.now().minusDays(14 - i));
            lostItemService.createLostItem(item);
        }

        Map<String, Object> result = lostItemService.detectFrequentLosing(1L);
        assertNotNull(result.get("trend"));
        assertNotNull(result.get("isFrequentLoser"));
    }

    @Test
    void testDetectFrequentLosingWithData() {
        // Create items across different periods
        for (int i = 0; i < 3; i++) {
            LostItem item = new LostItem();
            item.setTitle("Item " + i);
            item.setCategory(ItemCategory.CLOTHING);
            item.setPatientId(1L);
            item.setStatus(ItemStatus.LOST);
            item.setPriority(ItemPriority.LOW);
            item.setCreatedAt(LocalDateTime.now().minusDays(60 - (i * 20)));
            lostItemService.createLostItem(item);
        }

        Map<String, Object> result = lostItemService.detectFrequentLosing(1L);
        // Verify the result contains expected keys
        assertNotNull(result.get("trend"));
        assertNotNull(result.get("isFrequentLoser"));
        assertTrue(result.get("trend") instanceof String);
    }

    // ==================== CRITICAL ITEMS TESTS ====================

    @Test
    void testGetCriticalLostItems() {
        LostItem critical = new LostItem();
        critical.setTitle("Critical Item");
        critical.setCategory(ItemCategory.MEDICATION);
        critical.setPatientId(1L);
        critical.setStatus(ItemStatus.SEARCHING);
        critical.setPriority(ItemPriority.CRITICAL);
        critical.setCreatedAt(LocalDateTime.now());
        lostItemService.createLostItem(critical);

        List<LostItem> criticalItems = lostItemService.getCriticalLostItems(1L);
        assertTrue(criticalItems.size() > 0);
    }

    @Test
    void testGetAllCriticalItems() {
        // Create critical priority item
        LostItem critical = new LostItem();
        critical.setTitle("Critical Item");
        critical.setCategory(ItemCategory.DOCUMENT);
        critical.setPatientId(2L);
        critical.setStatus(ItemStatus.LOST);
        critical.setPriority(ItemPriority.CRITICAL);
        critical.setCreatedAt(LocalDateTime.now());
        lostItemService.createLostItem(critical);

        // Create searching status item
        LostItem searching = new LostItem();
        searching.setTitle("Searching Item");
        searching.setCategory(ItemCategory.CLOTHING);
        searching.setPatientId(3L);
        searching.setStatus(ItemStatus.SEARCHING);
        searching.setPriority(ItemPriority.LOW);
        searching.setCreatedAt(LocalDateTime.now());
        lostItemService.createLostItem(searching);

        List<LostItem> allCritical = lostItemService.getAllCriticalItems();
        assertTrue(allCritical.size() >= 2);
    }

    // ==================== PATIENT/CAREGIVER ITEMS TESTS ====================

    @Test
    void testGetItemsByPatientIdFlat() {
        // Create multiple items
        for (int i = 0; i < 3; i++) {
            LostItem item = new LostItem();
            item.setTitle("Item " + i);
            item.setCategory(ItemCategory.CLOTHING);
            item.setPatientId(1L);
            item.setStatus(ItemStatus.LOST);
            item.setPriority(ItemPriority.MEDIUM);
            item.setCreatedAt(LocalDateTime.now().minusDays(i));
            lostItemService.createLostItem(item);
        }

        List<LostItem> items = lostItemService.getItemsByPatientIdFlat(1L);
        assertEquals(3, items.size());
        // Verify sorted by created date descending
        assertTrue(items.get(0).getCreatedAt().isAfter(items.get(1).getCreatedAt()));
    }

    @Test
    void testGetItemsByCaregiverId() {
        lostItemService.createLostItem(testItem);

        List<LostItem> items = lostItemService.getItemsByCaregiverId(10L);
        assertTrue(items.size() > 0);
        assertTrue(items.stream().allMatch(i -> i.getCaregiverId().equals(10L)));
    }

    @Test
    void testGetCriticalItemsByCaregiverId() {
        LostItem critical = new LostItem();
        critical.setTitle("Critical Item");
        critical.setCategory(ItemCategory.MEDICATION);
        critical.setPatientId(1L);
        critical.setCaregiverId(10L);
        critical.setStatus(ItemStatus.LOST);
        critical.setPriority(ItemPriority.CRITICAL);
        critical.setCreatedAt(LocalDateTime.now());
        lostItemService.createLostItem(critical);

        List<LostItem> criticalItems = lostItemService.getCriticalItemsByCaregiverId(10L);
        assertTrue(criticalItems.size() > 0);
        assertTrue(criticalItems.stream().allMatch(i -> i.getCaregiverId().equals(10L)));
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    void testCreateMultipleItemsForPatient() {
        for (int i = 0; i < 10; i++) {
            LostItem item = new LostItem();
            item.setTitle("Item " + i);
            item.setCategory(ItemCategory.values()[i % ItemCategory.values().length]);
            item.setPatientId(1L);
            item.setStatus(ItemStatus.values()[i % ItemStatus.values().length]);
            item.setPriority(ItemPriority.values()[i % ItemPriority.values().length]);
            item.setCreatedAt(LocalDateTime.now().minusDays(i));
            lostItemService.createLostItem(item);
        }

        List<LostItem> items = lostItemService.getAllLostItems();
        assertTrue(items.size() >= 10);
    }

    @Test
    void testItemStatusProgression() {
        LostItem created = lostItemService.createLostItem(testItem);
        assertEquals(ItemStatus.LOST, created.getStatus());

        LostItem updated = new LostItem();
        updated.setTitle(created.getTitle());
        updated.setCategory(created.getCategory());
        updated.setPatientId(created.getPatientId());
        updated.setCaregiverId(created.getCaregiverId());
        updated.setLastSeenLocation(created.getLastSeenLocation());
        updated.setLastSeenDate(created.getLastSeenDate());
        updated.setStatus(ItemStatus.SEARCHING);
        updated.setPriority(created.getPriority());

        LostItem step1 = lostItemService.updateLostItem(created.getId(), updated);
        assertEquals(ItemStatus.SEARCHING, step1.getStatus());

        LostItem step2 = lostItemService.markAsFound(step1.getId());
        assertEquals(ItemStatus.FOUND, step2.getStatus());
    }
}
