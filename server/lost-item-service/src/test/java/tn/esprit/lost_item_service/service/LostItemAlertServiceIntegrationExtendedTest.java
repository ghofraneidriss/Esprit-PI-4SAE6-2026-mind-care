package tn.esprit.lost_item_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tn.esprit.lost_item_service.entity.*;
import tn.esprit.lost_item_service.repository.LostItemAlertRepository;
import tn.esprit.lost_item_service.repository.LostItemRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class LostItemAlertServiceIntegrationExtendedTest {

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

        testItem = LostItem.builder()
                .title("Test Item")
                .category(ItemCategory.MEDICATION)
                .patientId(1L)
                .status(ItemStatus.LOST)
                .build();
        lostItemRepository.save(testItem);
    }

    @Test
    void testCreateAlertWithFullDetails() {
        LostItemAlert alert = LostItemAlert.builder()
                .lostItemId(testItem.getId())
                .patientId(1L)
                .caregiverId(2L)
                .title("Critical Alert")
                .description("Medication missing")
                .level(AlertLevel.CRITICAL)
                .status(AlertStatus.NEW)
                .build();

        LostItemAlert created = lostItemAlertService.createAlert(alert);

        assertNotNull(created.getId());
        assertEquals("Critical Alert", created.getTitle());
        assertEquals(AlertLevel.CRITICAL, created.getLevel());
        assertEquals(AlertStatus.NEW, created.getStatus());
    }

    @Test
    void testGetAlertById_notFound() {
        assertThrows(RuntimeException.class, () -> lostItemAlertService.getAlertById(999L));
    }

    @Test
    void testGetAllAlerts_empty() {
        List<LostItemAlert> alerts = lostItemAlertService.getAllAlerts();
        assertTrue(alerts.isEmpty());
    }

    @Test
    void testGetAllAlerts_multiple() {
        LostItemAlert alert1 = LostItemAlert.builder()
                .lostItemId(testItem.getId())
                .patientId(1L)
                .title("Alert1")
                .level(AlertLevel.HIGH)
                .status(AlertStatus.NEW)
                .build();

        LostItemAlert alert2 = LostItemAlert.builder()
                .lostItemId(testItem.getId())
                .patientId(1L)
                .title("Alert2")
                .level(AlertLevel.LOW)
                .status(AlertStatus.VIEWED)
                .build();

        alertRepository.save(alert1);
        alertRepository.save(alert2);

        List<LostItemAlert> alerts = lostItemAlertService.getAllAlerts();
        assertEquals(2, alerts.size());
    }

    @Test
    void testGetAlertsByPatientId_multiple() {
        LostItemAlert alert1 = LostItemAlert.builder()
                .lostItemId(testItem.getId())
                .patientId(1L)
                .title("Alert1")
                .level(AlertLevel.HIGH)
                .status(AlertStatus.NEW)
                .build();

        LostItemAlert alert2 = LostItemAlert.builder()
                .lostItemId(testItem.getId())
                .patientId(1L)
                .title("Alert2")
                .level(AlertLevel.MEDIUM)
                .status(AlertStatus.VIEWED)
                .build();

        alertRepository.save(alert1);
        alertRepository.save(alert2);

        List<LostItemAlert> alerts = lostItemAlertService.getAlertsByPatientId(1L);
        assertEquals(2, alerts.size());
    }

    @Test
    void testGetAlertsByPatientId_empty() {
        List<LostItemAlert> alerts = lostItemAlertService.getAlertsByPatientId(999L);
        assertTrue(alerts.isEmpty());
    }

    @Test
    void testGetAlertsByStatus_new() {
        LostItemAlert alert1 = LostItemAlert.builder()
                .lostItemId(testItem.getId())
                .patientId(1L)
                .title("New Alert")
                .level(AlertLevel.HIGH)
                .status(AlertStatus.NEW)
                .build();

        LostItemAlert alert2 = LostItemAlert.builder()
                .lostItemId(testItem.getId())
                .patientId(1L)
                .title("Viewed Alert")
                .level(AlertLevel.LOW)
                .status(AlertStatus.VIEWED)
                .build();

        alertRepository.save(alert1);
        alertRepository.save(alert2);

        List<LostItemAlert> newAlerts = lostItemAlertService.getAlertsByStatus(AlertStatus.NEW);
        assertEquals(1, newAlerts.size());
        assertEquals("New Alert", newAlerts.get(0).getTitle());
    }

    @Test
    void testGetAlertsByStatus_viewed() {
        LostItemAlert alert1 = LostItemAlert.builder()
                .lostItemId(testItem.getId())
                .patientId(1L)
                .title("Viewed Alert")
                .level(AlertLevel.HIGH)
                .status(AlertStatus.VIEWED)
                .build();

        LostItemAlert alert2 = LostItemAlert.builder()
                .lostItemId(testItem.getId())
                .patientId(1L)
                .title("Resolved Alert")
                .level(AlertLevel.LOW)
                .status(AlertStatus.RESOLVED)
                .build();

        alertRepository.save(alert1);
        alertRepository.save(alert2);

        List<LostItemAlert> viewedAlerts = lostItemAlertService.getAlertsByStatus(AlertStatus.VIEWED);
        assertEquals(1, viewedAlerts.size());
    }

    @Test
    void testGetAlertsByLevel_critical() {
        LostItemAlert alert1 = LostItemAlert.builder()
                .lostItemId(testItem.getId())
                .patientId(1L)
                .title("Critical")
                .level(AlertLevel.CRITICAL)
                .status(AlertStatus.NEW)
                .build();

        LostItemAlert alert2 = LostItemAlert.builder()
                .lostItemId(testItem.getId())
                .patientId(1L)
                .title("Low")
                .level(AlertLevel.LOW)
                .status(AlertStatus.NEW)
                .build();

        alertRepository.save(alert1);
        alertRepository.save(alert2);

        List<LostItemAlert> criticalAlerts = lostItemAlertService.getAlertsByLevel(AlertLevel.CRITICAL);
        assertEquals(1, criticalAlerts.size());
    }

    @Test
    void testUpdateAlert() {
        LostItemAlert alert = LostItemAlert.builder()
                .lostItemId(testItem.getId())
                .patientId(1L)
                .title("Original")
                .level(AlertLevel.LOW)
                .status(AlertStatus.NEW)
                .build();
        LostItemAlert saved = alertRepository.save(alert);

        LostItemAlert update = LostItemAlert.builder()
                .lostItemId(testItem.getId())
                .patientId(1L)
                .title("Updated")
                .level(AlertLevel.HIGH)
                .status(AlertStatus.VIEWED)
                .build();

        LostItemAlert updated = lostItemAlertService.updateAlert(saved.getId(), update);
        assertEquals("Updated", updated.getTitle());
        assertEquals(AlertLevel.HIGH, updated.getLevel());
    }

    @Test
    void testResolveAlert() {
        LostItemAlert alert = LostItemAlert.builder()
                .lostItemId(testItem.getId())
                .patientId(1L)
                .title("Alert")
                .level(AlertLevel.CRITICAL)
                .status(AlertStatus.NEW)
                .build();
        LostItemAlert saved = alertRepository.save(alert);

        LostItemAlert resolved = lostItemAlertService.resolveAlert(saved.getId());
        assertEquals(AlertStatus.RESOLVED, resolved.getStatus());
    }

    @Test
    void testDeleteAlert() {
        LostItemAlert alert = LostItemAlert.builder()
                .lostItemId(testItem.getId())
                .patientId(1L)
                .title("To Delete")
                .level(AlertLevel.HIGH)
                .status(AlertStatus.NEW)
                .build();
        LostItemAlert saved = alertRepository.save(alert);
        Long alertId = saved.getId();

        lostItemAlertService.deleteAlert(alertId);

        assertThrows(RuntimeException.class, () -> lostItemAlertService.getAlertById(alertId));
    }

    @Test
    void testGetCriticalAlerts() {
        LostItemAlert alert1 = LostItemAlert.builder()
                .lostItemId(testItem.getId())
                .patientId(1L)
                .title("Critical1")
                .level(AlertLevel.CRITICAL)
                .status(AlertStatus.NEW)
                .build();

        LostItemAlert alert2 = LostItemAlert.builder()
                .lostItemId(testItem.getId())
                .patientId(2L)
                .title("Medium")
                .level(AlertLevel.MEDIUM)
                .status(AlertStatus.NEW)
                .build();

        alertRepository.save(alert1);
        alertRepository.save(alert2);

        List<LostItemAlert> criticalAlerts = lostItemAlertService.getAlertsByLevel(AlertLevel.CRITICAL);
        assertEquals(1, criticalAlerts.size());
    }

    @Test
    void testMultipleAlertsPerPatient() {
        for (int i = 0; i < 5; i++) {
            LostItemAlert alert = LostItemAlert.builder()
                    .lostItemId(testItem.getId())
                    .patientId(1L)
                    .title("Alert " + i)
                    .level(AlertLevel.values()[i % AlertLevel.values().length])
                    .status(AlertStatus.NEW)
                    .build();
            alertRepository.save(alert);
        }

        List<LostItemAlert> alerts = lostItemAlertService.getAlertsByPatientId(1L);
        assertEquals(5, alerts.size());
    }
}
