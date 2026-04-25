package tn.esprit.followup_alert_service.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tn.esprit.followup_alert_service.Entity.Alert;
import tn.esprit.followup_alert_service.Entity.AlertLevel;
import tn.esprit.followup_alert_service.Entity.AlertStatus;
import tn.esprit.followup_alert_service.Repository.AlertRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class AlertServiceTest {

    @Autowired
    private AlertService alertService;

    @Autowired
    private AlertRepository alertRepository;

    private Alert testAlert;

    @BeforeEach
    void setUp() {
        alertRepository.deleteAll();
        testAlert = new Alert();
        testAlert.setPatientId(1L);
        testAlert.setTitle("Test Alert Title");
        testAlert.setLevel(AlertLevel.HIGH);
        testAlert.setStatus(AlertStatus.NEW);
        testAlert.setDescription("Test Alert Description");
        testAlert.setCreatedAt(LocalDateTime.now());
    }

    // ==================== BASIC CRUD TESTS ====================

    @Test
    void testCreateAlert() {
        Alert created = alertService.createAlert(testAlert);
        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals("Test Alert Description", created.getDescription());
    }

    @Test
    void testGetAllAlerts() {
        alertService.createAlert(testAlert);
        List<Alert> alerts = alertService.getAllAlerts();
        assertFalse(alerts.isEmpty());
        assertEquals(1, alerts.size());
    }

    @Test
    void testGetAlertById() {
        Alert created = alertService.createAlert(testAlert);
        Alert retrieved = alertService.getAlertById(created.getId());
        assertNotNull(retrieved);
        assertEquals(created.getId(), retrieved.getId());
    }

    @Test
    void testGetAlertsByPatientId() {
        alertService.createAlert(testAlert);
        List<Alert> alerts = alertService.getAlertsByPatientId(1L);
        assertFalse(alerts.isEmpty());
        assertEquals(1, alerts.size());
    }

    @Test
    void testGetAlertsByLevel() {
        alertService.createAlert(testAlert);
        List<Alert> alerts = alertService.getAlertsByLevel(AlertLevel.HIGH);
        assertFalse(alerts.isEmpty());
    }

    @Test
    void testGetAlertsByStatus() {
        alertService.createAlert(testAlert);
        List<Alert> alerts = alertService.getAlertsByStatus(AlertStatus.NEW);
        assertFalse(alerts.isEmpty());
    }

    @Test
    void testUpdateAlert() {
        Alert created = alertService.createAlert(testAlert);

        Alert updated = new Alert();
        updated.setPatientId(2L);
        updated.setTitle("Updated Title");
        updated.setDescription("Updated Description");
        updated.setLevel(AlertLevel.CRITICAL);

        Alert result = alertService.updateAlert(created.getId(), updated);
        assertEquals("Updated Title", result.getTitle());
        assertEquals("Updated Description", result.getDescription());
        assertEquals(AlertLevel.CRITICAL, result.getLevel());
        assertEquals(2L, result.getPatientId());
    }

    @Test
    void testDeleteAlert() {
        Alert created = alertService.createAlert(testAlert);
        Long id = created.getId();

        alertService.deleteAlert(id);

        assertThrows(RuntimeException.class, () -> alertService.getAlertById(id));
    }

    @Test
    void testDeleteNonExistentAlert() {
        assertThrows(RuntimeException.class, () -> alertService.deleteAlert(999L));
    }

    @Test
    void testGetAlertByIdNotFound() {
        assertThrows(RuntimeException.class, () -> alertService.getAlertById(999L));
    }

    // ==================== ADVANCED FEATURE TESTS ====================

    @Test
    void testGetCriticalNewAlerts() {
        // Create a critical new alert
        Alert critical = new Alert();
        critical.setPatientId(1L);
        critical.setLevel(AlertLevel.CRITICAL);
        critical.setStatus(AlertStatus.NEW);
        critical.setTitle("Critical Alert");
        alertService.createAlert(critical);

        // Create a high alert (should not be included)
        Alert high = new Alert();
        high.setPatientId(1L);
        high.setLevel(AlertLevel.HIGH);
        high.setStatus(AlertStatus.NEW);
        high.setTitle("High Alert");
        alertService.createAlert(high);

        List<Alert> criticalNewAlerts = alertService.getCriticalNewAlerts();
        assertEquals(1, criticalNewAlerts.size());
        assertEquals(AlertLevel.CRITICAL, criticalNewAlerts.get(0).getLevel());
    }

    @Test
    void testMarkAsViewed() {
        Alert created = alertService.createAlert(testAlert);

        Alert viewed = alertService.markAsViewed(created.getId());

        assertEquals(AlertStatus.VIEWED, viewed.getStatus());
        assertNotNull(viewed.getViewedAt());
    }

    @Test
    void testResolveAlert() {
        Alert created = alertService.createAlert(testAlert);

        Alert resolved = alertService.resolveAlert(created.getId());

        assertEquals(AlertStatus.RESOLVED, resolved.getStatus());
    }

    @Test
    void testEscalateAlertFromLowToMedium() {
        testAlert.setLevel(AlertLevel.LOW);
        Alert created = alertService.createAlert(testAlert);

        Alert escalated = alertService.escalateAlert(created.getId());

        assertEquals(AlertLevel.MEDIUM, escalated.getLevel());
        assertEquals(AlertStatus.NEW, escalated.getStatus());
    }

    @Test
    void testEscalateAlertFromMediumToHigh() {
        testAlert.setLevel(AlertLevel.MEDIUM);
        Alert created = alertService.createAlert(testAlert);

        Alert escalated = alertService.escalateAlert(created.getId());

        assertEquals(AlertLevel.HIGH, escalated.getLevel());
    }

    @Test
    void testEscalateAlertFromHighToCritical() {
        testAlert.setLevel(AlertLevel.HIGH);
        Alert created = alertService.createAlert(testAlert);

        Alert escalated = alertService.escalateAlert(created.getId());

        assertEquals(AlertLevel.CRITICAL, escalated.getLevel());
    }

    @Test
    void testEscalateAlertAlreadyCritical() {
        testAlert.setLevel(AlertLevel.CRITICAL);
        Alert created = alertService.createAlert(testAlert);

        Alert escalated = alertService.escalateAlert(created.getId());

        assertEquals(AlertLevel.CRITICAL, escalated.getLevel());
    }

    @Test
    void testResolveAllByPatient() {
        // Create multiple alerts for patient 1
        for (int i = 0; i < 5; i++) {
            Alert alert = new Alert();
            alert.setPatientId(1L);
            alert.setLevel(AlertLevel.HIGH);
            alert.setStatus(AlertStatus.NEW);
            alert.setTitle("Alert " + i);
            alertService.createAlert(alert);
        }

        // Resolve one alert manually
        List<Alert> alerts = alertService.getAlertsByPatientId(1L);
        alertService.resolveAlert(alerts.get(0).getId());

        // Resolve all remaining
        int count = alertService.resolveAllByPatient(1L);

        assertEquals(4, count);

        // Verify all are resolved
        List<Alert> allResolved = alertService.getAlertsByPatientId(1L);
        assertTrue(allResolved.stream().allMatch(a -> a.getStatus() == AlertStatus.RESOLVED));
    }

    @Test
    void testResolveAllByPatientWithNoAlerts() {
        int count = alertService.resolveAllByPatient(999L);
        assertEquals(0, count);
    }

    @Test
    void testGetStatistics() {
        // Create alerts with various statuses and levels
        Alert alert1 = new Alert();
        alert1.setPatientId(1L);
        alert1.setLevel(AlertLevel.CRITICAL);
        alert1.setStatus(AlertStatus.NEW);
        alert1.setTitle("Alert 1");
        alertService.createAlert(alert1);

        Alert alert2 = new Alert();
        alert2.setPatientId(1L);
        alert2.setLevel(AlertLevel.HIGH);
        alert2.setStatus(AlertStatus.NEW);
        alert2.setTitle("Alert 2");
        alertService.createAlert(alert2);

        // Mark alert2 as viewed to change its status
        alertService.markAsViewed(alert2.getId());

        Alert alert3 = new Alert();
        alert3.setPatientId(2L);
        alert3.setLevel(AlertLevel.LOW);
        alert3.setStatus(AlertStatus.NEW);
        alert3.setTitle("Alert 3");
        alertService.createAlert(alert3);

        // Resolve alert3
        alertService.resolveAlert(alert3.getId());

        Map<String, Object> stats = alertService.getStatistics();

        assertEquals(3, stats.get("totalAlerts"));
        assertEquals(1L, stats.get("newAlerts"));
        assertEquals(1L, stats.get("viewedAlerts"));
        assertEquals(1L, stats.get("resolvedAlerts"));
        assertEquals(1L, stats.get("criticalUnresolvedCount"));

        @SuppressWarnings("unchecked")
        Map<String, Long> levelDistribution = (Map<String, Long>) stats.get("levelDistribution");
        assertNotNull(levelDistribution);
        assertTrue(levelDistribution.containsKey("CRITICAL"));

        @SuppressWarnings("unchecked")
        Map<String, Long> alertsPerPatient = (Map<String, Long>) stats.get("alertsPerPatient");
        assertNotNull(alertsPerPatient);
        assertEquals(2L, alertsPerPatient.get("1"));
    }

    @Test
    void testGetStatisticsEmpty() {
        Map<String, Object> stats = alertService.getStatistics();

        assertEquals(0, stats.get("totalAlerts"));
        assertEquals(0L, stats.get("newAlerts"));
        assertEquals(0L, stats.get("viewedAlerts"));
        assertEquals(0L, stats.get("resolvedAlerts"));
        assertEquals(0L, stats.get("criticalUnresolvedCount"));
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    void testCreateMultipleAlertsForSamePatient() {
        for (int i = 0; i < 10; i++) {
            Alert alert = new Alert();
            alert.setPatientId(1L);
            alert.setLevel(AlertLevel.values()[i % AlertLevel.values().length]);
            alert.setStatus(AlertStatus.NEW);
            alert.setTitle("Alert " + i);
            alertService.createAlert(alert);
        }

        List<Alert> alerts = alertService.getAlertsByPatientId(1L);
        assertEquals(10, alerts.size());
    }

    @Test
    void testAlertLevelProgression() {
        testAlert.setLevel(AlertLevel.LOW);
        Alert created = alertService.createAlert(testAlert);

        Alert step1 = alertService.escalateAlert(created.getId());
        assertEquals(AlertLevel.MEDIUM, step1.getLevel());

        Alert step2 = alertService.escalateAlert(step1.getId());
        assertEquals(AlertLevel.HIGH, step2.getLevel());

        Alert step3 = alertService.escalateAlert(step2.getId());
        assertEquals(AlertLevel.CRITICAL, step3.getLevel());

        Alert step4 = alertService.escalateAlert(step3.getId());
        assertEquals(AlertLevel.CRITICAL, step4.getLevel());
    }

    @Test
    void testStatusTransitions() {
        Alert created = alertService.createAlert(testAlert);
        assertEquals(AlertStatus.NEW, created.getStatus());

        Alert viewed = alertService.markAsViewed(created.getId());
        assertEquals(AlertStatus.VIEWED, viewed.getStatus());

        Alert resolved = alertService.resolveAlert(viewed.getId());
        assertEquals(AlertStatus.RESOLVED, resolved.getStatus());
    }
}
