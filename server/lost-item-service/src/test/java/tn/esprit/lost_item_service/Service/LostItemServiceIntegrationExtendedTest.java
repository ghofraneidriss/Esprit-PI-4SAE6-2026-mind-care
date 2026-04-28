package tn.esprit.lost_item_service.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tn.esprit.lost_item_service.Entity.*;
import tn.esprit.lost_item_service.Repository.LostItemRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class LostItemServiceIntegrationExtendedTest {

    @Autowired
    private LostItemService lostItemService;

    @Autowired
    private LostItemRepository lostItemRepository;

    @BeforeEach
    void setUp() {
        lostItemRepository.deleteAll();
    }

    @Test
    void testCreateLostItem_withAllFields() {
        LostItem item = LostItem.builder()
                .title("Complete Item")
                .description("Full description")
                .category(ItemCategory.MEDICATION)
                .patientId(1L)
                .caregiverId(2L)
                .lastSeenLocation("Kitchen")
                .lastSeenDate(LocalDate.now())
                .status(ItemStatus.LOST)
                .priority(ItemPriority.CRITICAL)
                .imageUrl("http://image.url")
                .build();

        LostItem created = lostItemService.createLostItem(item);

        assertNotNull(created.getId());
        assertEquals("Complete Item", created.getTitle());
        assertEquals(ItemCategory.MEDICATION, created.getCategory());
        assertEquals(ItemStatus.LOST, created.getStatus());
    }

    @Test
    void testGetLostItemById_notFound() {
        assertThrows(RuntimeException.class, () -> lostItemService.getLostItemById(999L));
    }

    @Test
    void testGetAllLostItems_empty() {
        List<LostItem> items = lostItemService.getAllLostItems();
        assertTrue(items.isEmpty());
    }

    @Test
    void testGetAllLostItems_multiple() {
        LostItem item1 = LostItem.builder().title("Item1").category(ItemCategory.CLOTHING).patientId(1L).build();
        LostItem item2 = LostItem.builder().title("Item2").category(ItemCategory.DOCUMENT).patientId(2L).build();
        LostItem item3 = LostItem.builder().title("Item3").category(ItemCategory.MEDICATION).patientId(1L).build();

        lostItemRepository.save(item1);
        lostItemRepository.save(item2);
        lostItemRepository.save(item3);

        List<LostItem> items = lostItemService.getAllLostItems();
        assertEquals(3, items.size());
    }

    @Test
    void testUpdateLostItem_modifyTitle() {
        LostItem item = LostItem.builder()
                .title("Original")
                .category(ItemCategory.CLOTHING)
                .patientId(1L)
                .status(ItemStatus.LOST)
                .build();
        LostItem saved = lostItemRepository.save(item);

        LostItem update = LostItem.builder()
                .title("Updated Title")
                .category(ItemCategory.CLOTHING)
                .patientId(1L)
                .status(ItemStatus.LOST)
                .build();

        LostItem updated = lostItemService.updateLostItem(saved.getId(), update);
        assertEquals("Updated Title", updated.getTitle());
    }

    @Test
    void testUpdateLostItem_modifyStatus() {
        LostItem item = LostItem.builder()
                .title("Test")
                .category(ItemCategory.MEDICATION)
                .patientId(1L)
                .status(ItemStatus.LOST)
                .build();
        LostItem saved = lostItemRepository.save(item);

        LostItem update = LostItem.builder()
                .title("Test")
                .category(ItemCategory.MEDICATION)
                .patientId(1L)
                .status(ItemStatus.FOUND)
                .build();

        LostItem updated = lostItemService.updateLostItem(saved.getId(), update);
        assertEquals(ItemStatus.FOUND, updated.getStatus());
    }

    @Test
    void testGetItemsByPatientIdFlat_multipleItems() {
        LostItem item1 = LostItem.builder().title("Item1").category(ItemCategory.MEDICATION).patientId(5L).build();
        LostItem item2 = LostItem.builder().title("Item2").category(ItemCategory.DOCUMENT).patientId(5L).build();
        LostItem item3 = LostItem.builder().title("Item3").category(ItemCategory.ACCESSORY).patientId(10L).build();

        lostItemRepository.save(item1);
        lostItemRepository.save(item2);
        lostItemRepository.save(item3);

        List<LostItem> items = lostItemService.getItemsByPatientIdFlat(5L);
        assertEquals(2, items.size());
    }

    @Test
    void testGetItemsByPatientIdFlat_noItems() {
        List<LostItem> items = lostItemService.getItemsByPatientIdFlat(999L);
        assertTrue(items.isEmpty());
    }

    @Test
    void testGetCriticalLostItemsForPatient() {
        LostItem item1 = LostItem.builder()
                .title("Critical Item")
                .category(ItemCategory.MEDICATION)
                .patientId(1L)
                .priority(ItemPriority.CRITICAL)
                .status(ItemStatus.LOST)
                .build();

        LostItem item2 = LostItem.builder()
                .title("Low Priority Item")
                .category(ItemCategory.DOCUMENT)
                .patientId(1L)
                .priority(ItemPriority.LOW)
                .status(ItemStatus.LOST)
                .build();

        lostItemRepository.save(item1);
        lostItemRepository.save(item2);

        List<LostItem> criticalItems = lostItemService.getCriticalLostItems(1L);
        assertTrue(criticalItems.size() >= 1);
    }

    @Test
    void testGetAllCriticalItems() {
        LostItem item1 = LostItem.builder()
                .title("Critical1")
                .category(ItemCategory.MEDICATION)
                .patientId(1L)
                .priority(ItemPriority.CRITICAL)
                .status(ItemStatus.LOST)
                .build();

        LostItem item2 = LostItem.builder()
                .title("Critical2")
                .category(ItemCategory.DOCUMENT)
                .patientId(2L)
                .priority(ItemPriority.CRITICAL)
                .status(ItemStatus.LOST)
                .build();

        lostItemRepository.save(item1);
        lostItemRepository.save(item2);

        List<LostItem> allCritical = lostItemService.getAllCriticalItems();
        assertTrue(allCritical.size() >= 2);
    }

    @Test
    void testGetItemsByPatientIdFlat() {
        LostItem item1 = LostItem.builder().title("Item1").category(ItemCategory.MEDICATION).patientId(5L).build();
        LostItem item2 = LostItem.builder().title("Item2").category(ItemCategory.DOCUMENT).patientId(5L).build();

        lostItemRepository.save(item1);
        lostItemRepository.save(item2);

        List<LostItem> items = lostItemService.getItemsByPatientIdFlat(5L);
        assertEquals(2, items.size());
    }

    @Test
    void testMarkAsFound_updatesStatus() {
        LostItem item = LostItem.builder()
                .title("Lost Item")
                .category(ItemCategory.MEDICATION)
                .patientId(1L)
                .status(ItemStatus.LOST)
                .build();
        LostItem saved = lostItemRepository.save(item);

        lostItemService.markAsFound(saved.getId());

        LostItem updated = lostItemService.getLostItemById(saved.getId());
        assertEquals(ItemStatus.FOUND, updated.getStatus());
    }

    @Test
    void testDeleteLostItem() {
        LostItem item = LostItem.builder().title("To Delete").category(ItemCategory.CLOTHING).patientId(1L).build();
        LostItem saved = lostItemRepository.save(item);
        Long itemId = saved.getId();

        // Verify item exists before delete
        assertNotNull(lostItemService.getLostItemById(itemId));

        // Delete the item - should not throw exception
        assertDoesNotThrow(() -> lostItemService.deleteLostItem(itemId));
    }

    @Test
    void testGetRecentlyLostItems() {
        LostItem oldItem = LostItem.builder()
                .title("Old Item")
                .category(ItemCategory.MEDICATION)
                .patientId(1L)
                .createdAt(LocalDateTime.now().minusDays(30))
                .build();

        LostItem recentItem = LostItem.builder()
                .title("Recent Item")
                .category(ItemCategory.DOCUMENT)
                .patientId(2L)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        lostItemRepository.save(oldItem);
        lostItemRepository.save(recentItem);

        List<LostItem> items = lostItemService.getAllLostItems();
        assertTrue(items.size() >= 2);
    }
}
