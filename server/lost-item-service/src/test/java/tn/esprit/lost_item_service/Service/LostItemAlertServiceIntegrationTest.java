package tn.esprit.lost_item_service.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tn.esprit.lost_item_service.Entity.*;
import tn.esprit.lost_item_service.Repository.LostItemAlertRepository;
import tn.esprit.lost_item_service.Repository.LostItemRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class LostItemAlertServiceIntegrationTest {

    @Autowired
    private LostItemAlertService lostItemAlertService;

    @Autowired
    private LostItemAlertRepository alertRepository;

    @Autowired
    private LostItemRepository lostItemRepository;

    private LostItem testItem;

    @BeforeEach
    void setUp() {
        alertRepository.deleteAll();
        lostItemRepository.deleteAll();

        testItem = new LostItem();
        testItem.setTitle("Test Item");
        testItem.setCategory(ItemCategory.MEDICATION);
        testItem.setPatientId(1L);
        testItem.setStatus(ItemStatus.LOST);
        testItem.setPriority(ItemPriority.CRITICAL);
        testItem.setCreatedAt(LocalDateTime.now());
        lostItemRepository.save(testItem);
    }

    @Test
    void testCreateAlert() {
        LostItemAlert alert = new LostItemAlert();
        alert.setPatientId(1L);
        alert.setLostItemId(testItem.getId());
        alert.setTitle("Test Alert");
        alert.setDescription("Test description");
        alert.setLevel(AlertLevel.CRITICAL);
        alert.setStatus(AlertStatus.NEW);

        LostItemAlert created = lostItemAlertService.createAlert(alert);
        assertNotNull(created.getId());
        assertEquals("Test Alert", created.getTitle());
    }

    @Test
    void testGetAlertsByPatientId() {
        LostItemAlert alert = new LostItemAlert();
        alert.setPatientId(1L);
        alert.setLostItemId(testItem.getId());
        alert.setTitle("Alert");
        alert.setLevel(AlertLevel.HIGH);
        alert.setStatus(AlertStatus.NEW);
        lostItemAlertService.createAlert(alert);

        List<LostItemAlert> alerts = lostItemAlertService.getAlertsByPatientId(1L);
        assertTrue(alerts.size() > 0);
    }

    @Test
    void testGetAlertsByStatus() {
        LostItemAlert alert = new LostItemAlert();
        alert.setPatientId(1L);
        alert.setLostItemId(testItem.getId());
        alert.setTitle("Alert");
        alert.setLevel(AlertLevel.MEDIUM);
        alert.setStatus(AlertStatus.NEW);
        lostItemAlertService.createAlert(alert);

        List<LostItemAlert> newAlerts = lostItemAlertService.getAlertsByStatus(AlertStatus.NEW);
        assertTrue(newAlerts.size() > 0);
    }

    @Test
    void testUpdateAlertStatus() {
        LostItemAlert alert = new LostItemAlert();
        alert.setPatientId(1L);
        alert.setLostItemId(testItem.getId());
        alert.setTitle("Alert");
        alert.setLevel(AlertLevel.LOW);
        alert.setStatus(AlertStatus.NEW);
        LostItemAlert created = lostItemAlertService.createAlert(alert);

        LostItemAlert resolved = lostItemAlertService.resolveAlert(created.getId());
        assertEquals(AlertStatus.RESOLVED, resolved.getStatus());
    }

    @Test
    void testDeleteAlert() {
        LostItemAlert alert = new LostItemAlert();
        alert.setPatientId(1L);
        alert.setLostItemId(testItem.getId());
        alert.setTitle("Alert");
        alert.setLevel(AlertLevel.CRITICAL);
        alert.setStatus(AlertStatus.NEW);
        LostItemAlert created = lostItemAlertService.createAlert(alert);

        lostItemAlertService.deleteAlert(created.getId());
        assertThrows(RuntimeException.class, () -> lostItemAlertService.getAlertById(created.getId()));
    }
}
