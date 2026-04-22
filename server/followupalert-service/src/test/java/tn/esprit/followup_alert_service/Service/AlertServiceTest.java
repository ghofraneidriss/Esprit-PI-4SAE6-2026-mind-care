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
}
