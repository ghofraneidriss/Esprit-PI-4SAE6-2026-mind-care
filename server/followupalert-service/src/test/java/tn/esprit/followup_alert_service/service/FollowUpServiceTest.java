package tn.esprit.followup_alert_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tn.esprit.followup_alert_service.entity.*;
import tn.esprit.followup_alert_service.repository.AlertRepository;
import tn.esprit.followup_alert_service.repository.FollowUpRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class FollowUpServiceTest {

    @Autowired
    private FollowUpService followUpService;

    @Autowired
    private FollowUpRepository followUpRepository;

    @Autowired
    private AlertRepository alertRepository;

    private FollowUp testFollowUp;

    @BeforeEach
    void setUp() {
        followUpRepository.deleteAll();
        alertRepository.deleteAll();

        testFollowUp = new FollowUp();
        testFollowUp.setPatientId(1L);
        testFollowUp.setCaregiverId(10L);
        testFollowUp.setFollowUpDate(LocalDate.now());
        testFollowUp.setCognitiveScore(20);
        testFollowUp.setMood(MoodState.CALM);
        testFollowUp.setEating(IndependenceLevel.INDEPENDENT);
        testFollowUp.setDressing(IndependenceLevel.INDEPENDENT);
        testFollowUp.setMobility(IndependenceLevel.NEEDS_ASSISTANCE);
        testFollowUp.setHoursSlept(6);
        testFollowUp.setSleepQuality(SleepQuality.FAIR);
        testFollowUp.setAgitationObserved(false);
        testFollowUp.setConfusionObserved(false);
    }

    // ==================== BASIC CRUD TESTS ====================

    @Test
    void testCreateFollowUp() {
        FollowUp created = followUpService.createFollowUp(testFollowUp);
        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals(1L, created.getPatientId());
        assertEquals(20, created.getCognitiveScore());
    }

    @Test
    void testCreateFollowUpDuplicate() {
        followUpService.createFollowUp(testFollowUp);

        // Try to create duplicate
        FollowUp duplicate = new FollowUp();
        duplicate.setPatientId(1L);
        duplicate.setCaregiverId(10L);
        duplicate.setFollowUpDate(LocalDate.now());
        duplicate.setCognitiveScore(20);
        duplicate.setMood(MoodState.CALM);
        duplicate.setEating(IndependenceLevel.INDEPENDENT);
        duplicate.setDressing(IndependenceLevel.INDEPENDENT);
        duplicate.setMobility(IndependenceLevel.NEEDS_ASSISTANCE);

        assertThrows(RuntimeException.class, () -> followUpService.createFollowUp(duplicate));
    }

    @Test
    void testGetAllFollowUps() {
        followUpService.createFollowUp(testFollowUp);
        List<FollowUp> followUps = followUpService.getAllFollowUps();
        assertFalse(followUps.isEmpty());
        assertEquals(1, followUps.size());
    }

    @Test
    void testGetFollowUpById() {
        FollowUp created = followUpService.createFollowUp(testFollowUp);
        FollowUp retrieved = followUpService.getFollowUpById(created.getId());
        assertNotNull(retrieved);
        assertEquals(created.getId(), retrieved.getId());
    }

    @Test
    void testGetFollowUpByIdNotFound() {
        assertThrows(RuntimeException.class, () -> followUpService.getFollowUpById(999L));
    }

    @Test
    void testGetFollowUpsByPatientId() {
        followUpService.createFollowUp(testFollowUp);
        List<FollowUp> followUps = followUpService.getFollowUpsByPatientId(1L);
        assertFalse(followUps.isEmpty());
        assertEquals(1, followUps.size());
    }

    @Test
    void testGetFollowUpsByCaregiverId() {
        followUpService.createFollowUp(testFollowUp);
        List<FollowUp> followUps = followUpService.getFollowUpsByCaregiverId(10L);
        assertFalse(followUps.isEmpty());
        assertEquals(1, followUps.size());
    }

    @Test
    void testUpdateFollowUp() {
        FollowUp created = followUpService.createFollowUp(testFollowUp);

        FollowUp updated = new FollowUp();
        updated.setPatientId(1L);
        updated.setCaregiverId(10L);
        updated.setFollowUpDate(LocalDate.now());
        updated.setCognitiveScore(15);
        updated.setMood(MoodState.DEPRESSED);
        updated.setEating(IndependenceLevel.DEPENDENT);
        updated.setDressing(IndependenceLevel.INDEPENDENT);
        updated.setMobility(IndependenceLevel.NEEDS_ASSISTANCE);
        updated.setHoursSlept(5);
        updated.setSleepQuality(SleepQuality.POOR);
        updated.setAgitationObserved(true);
        updated.setConfusionObserved(false);

        FollowUp result = followUpService.updateFollowUp(created.getId(), updated);
        assertEquals(15, result.getCognitiveScore());
        assertEquals(MoodState.DEPRESSED, result.getMood());
        assertEquals(IndependenceLevel.DEPENDENT, result.getEating());
        assertTrue(result.getAgitationObserved());
    }

    @Test
    void testDeleteFollowUp() {
        FollowUp created = followUpService.createFollowUp(testFollowUp);
        Long id = created.getId();

        followUpService.deleteFollowUp(id);

        assertThrows(RuntimeException.class, () -> followUpService.getFollowUpById(id));
    }

    @Test
    void testDeleteNonExistentFollowUp() {
        assertThrows(RuntimeException.class, () -> followUpService.deleteFollowUp(999L));
    }

    // ==================== AUTO-ALERT GENERATION TESTS ====================

    @Test
    void testAutoGenerateAlertForLowCognitiveScore() {
        testFollowUp.setCognitiveScore(15); // Low cognitive score
        followUpService.createFollowUp(testFollowUp);

        List<Alert> alerts = alertRepository.findByPatientId(1L);
        assertTrue(alerts.stream().anyMatch(a -> a.getTitle().contains("Cognitive Score")));
    }

    @Test
    void testAutoGenerateAlertForCriticalCognitiveScore() {
        testFollowUp.setCognitiveScore(8); // Very low score
        followUpService.createFollowUp(testFollowUp);

        List<Alert> alerts = alertRepository.findByPatientId(1L);
        assertTrue(alerts.stream().anyMatch(a -> a.getLevel() == AlertLevel.CRITICAL));
    }

    @Test
    void testAutoGenerateAlertForAgitationAndConfusion() {
        testFollowUp.setAgitationObserved(true);
        testFollowUp.setConfusionObserved(true);
        followUpService.createFollowUp(testFollowUp);

        List<Alert> alerts = alertRepository.findByPatientId(1L);
        assertTrue(alerts.stream().anyMatch(a -> a.getTitle().contains("Agitation & Confusion")));
        assertTrue(alerts.stream().anyMatch(a -> a.getLevel() == AlertLevel.CRITICAL));
    }

    @Test
    void testAutoGenerateAlertForAgitationOnly() {
        testFollowUp.setAgitationObserved(true);
        testFollowUp.setConfusionObserved(false);
        followUpService.createFollowUp(testFollowUp);

        List<Alert> alerts = alertRepository.findByPatientId(1L);
        assertTrue(alerts.stream().anyMatch(a -> a.getTitle().contains("Agitation Observed")));
        assertTrue(alerts.stream().anyMatch(a -> a.getLevel() == AlertLevel.MEDIUM));
    }

    @Test
    void testAutoGenerateAlertForPoorSleep() {
        testFollowUp.setSleepQuality(SleepQuality.POOR);
        testFollowUp.setHoursSlept(3);
        followUpService.createFollowUp(testFollowUp);

        List<Alert> alerts = alertRepository.findByPatientId(1L);
        assertTrue(alerts.stream().anyMatch(a -> a.getTitle().contains("Poor Sleep Quality")));
    }

    @Test
    void testAutoGenerateAlertForFullDependency() {
        testFollowUp.setEating(IndependenceLevel.DEPENDENT);
        testFollowUp.setDressing(IndependenceLevel.DEPENDENT);
        testFollowUp.setMobility(IndependenceLevel.DEPENDENT);
        followUpService.createFollowUp(testFollowUp);

        List<Alert> alerts = alertRepository.findByPatientId(1L);
        assertTrue(alerts.stream().anyMatch(a -> a.getTitle().contains("Full Dependency")));
        assertTrue(alerts.stream().anyMatch(a -> a.getLevel() == AlertLevel.HIGH));
    }

    @Test
    void testAutoGenerateAlertForDepressedMood() {
        testFollowUp.setMood(MoodState.DEPRESSED);
        followUpService.createFollowUp(testFollowUp);

        List<Alert> alerts = alertRepository.findByPatientId(1L);
        assertTrue(alerts.stream().anyMatch(a -> a.getTitle().contains("Depressed Mood")));
    }

    // ==================== COGNITIVE DECLINE DETECTION TESTS ====================

    @Test
    void testDetectCognitiveDecline() {
        // Create three follow-ups with declining cognitive scores
        for (int i = 0; i < 3; i++) {
            FollowUp fu = new FollowUp();
            fu.setPatientId(1L);
            fu.setCaregiverId(10L);
            fu.setFollowUpDate(LocalDate.now().minusDays(2 - i));
            fu.setCognitiveScore(20 - (i * 5)); // 20, 15, 10 (declining)
            fu.setMood(MoodState.CALM);
            fu.setEating(IndependenceLevel.INDEPENDENT);
            fu.setDressing(IndependenceLevel.INDEPENDENT);
            fu.setMobility(IndependenceLevel.NEEDS_ASSISTANCE);
            followUpService.createFollowUp(fu);
        }

        List<Alert> alerts = alertRepository.findByPatientId(1L);
        assertTrue(alerts.stream().anyMatch(a -> a.getTitle().contains("Cognitive Decline Trend")));
    }

    @Test
    void testDetectNoCognitiveDeclineWithImprovement() {
        // Create three follow-ups with improving cognitive scores
        for (int i = 0; i < 3; i++) {
            FollowUp fu = new FollowUp();
            fu.setPatientId(1L);
            fu.setCaregiverId(10L);
            fu.setFollowUpDate(LocalDate.now().minusDays(2 - i));
            fu.setCognitiveScore(10 + (i * 5)); // 10, 15, 20 (improving)
            fu.setMood(MoodState.CALM);
            fu.setEating(IndependenceLevel.INDEPENDENT);
            fu.setDressing(IndependenceLevel.INDEPENDENT);
            fu.setMobility(IndependenceLevel.NEEDS_ASSISTANCE);
            followUpService.createFollowUp(fu);
        }

        boolean isDeclined = followUpService.detectCognitiveDecline(1L);
        assertFalse(isDeclined);
    }

    @Test
    void testDetectCognitiveDeclineInsufficientData() {
        followUpService.createFollowUp(testFollowUp);

        boolean isDeclined = followUpService.detectCognitiveDecline(1L);
        assertFalse(isDeclined);
    }

    // ==================== PATIENT RISK SCORING TESTS ====================

    @Test
    void testCalculatePatientRiskLow() {
        testFollowUp.setCognitiveScore(24);
        testFollowUp.setMood(MoodState.CALM);
        testFollowUp.setEating(IndependenceLevel.INDEPENDENT);
        testFollowUp.setDressing(IndependenceLevel.INDEPENDENT);
        testFollowUp.setMobility(IndependenceLevel.INDEPENDENT);
        testFollowUp.setSleepQuality(SleepQuality.GOOD);

        followUpService.createFollowUp(testFollowUp);

        Map<String, Object> risk = followUpService.calculatePatientRisk(1L);
        assertEquals(1L, risk.get("patientId"));
        assertTrue((int) risk.get("riskScore") < 20);
        assertEquals("LOW", risk.get("riskLevel"));
    }

    @Test
    void testCalculatePatientRiskModerate() {
        testFollowUp.setCognitiveScore(18);
        testFollowUp.setMood(MoodState.ANXIOUS);
        testFollowUp.setSleepQuality(SleepQuality.FAIR);

        followUpService.createFollowUp(testFollowUp);

        Map<String, Object> risk = followUpService.calculatePatientRisk(1L);
        int riskScore = (int) risk.get("riskScore");
        assertTrue(riskScore >= 20 && riskScore < 45);
        assertEquals("MODERATE", risk.get("riskLevel"));
    }

    @Test
    void testCalculatePatientRiskHigh() {
        testFollowUp.setCognitiveScore(15); // Moderate cognitive impairment (20 pts)
        testFollowUp.setMood(MoodState.AGITATED); // Agitated (15 pts)
        testFollowUp.setAgitationObserved(true); // (10 pts)
        testFollowUp.setConfusionObserved(false);
        testFollowUp.setSleepQuality(SleepQuality.FAIR); // No extra points
        testFollowUp.setEating(IndependenceLevel.NEEDS_ASSISTANCE); // No extra points

        followUpService.createFollowUp(testFollowUp);

        Map<String, Object> risk = followUpService.calculatePatientRisk(1L);
        int riskScore = (int) risk.get("riskScore");
        assertTrue(riskScore >= 45, "Risk score " + riskScore + " should be >= 45");
        assertEquals("HIGH", risk.get("riskLevel"));
    }

    @Test
    void testCalculatePatientRiskCritical() {
        testFollowUp.setCognitiveScore(5);
        testFollowUp.setMood(MoodState.CONFUSED);
        testFollowUp.setAgitationObserved(true);
        testFollowUp.setConfusionObserved(true);
        testFollowUp.setSleepQuality(SleepQuality.POOR);
        testFollowUp.setEating(IndependenceLevel.DEPENDENT);
        testFollowUp.setDressing(IndependenceLevel.DEPENDENT);
        testFollowUp.setMobility(IndependenceLevel.DEPENDENT);

        followUpService.createFollowUp(testFollowUp);

        Map<String, Object> risk = followUpService.calculatePatientRisk(1L);
        int riskScore = (int) risk.get("riskScore");
        assertTrue(riskScore >= 70);
        assertEquals("CRITICAL", risk.get("riskLevel"));
    }

    @Test
    void testCalculatePatientRiskNoFollowUps() {
        Map<String, Object> risk = followUpService.calculatePatientRisk(999L);
        assertEquals(999L, risk.get("patientId"));
        assertEquals("LOW", risk.get("riskLevel"));
    }

    // ==================== STATISTICS TESTS ====================

    @Test
    void testGetStatistics() {
        // Create multiple follow-ups
        for (int i = 0; i < 3; i++) {
            FollowUp fu = new FollowUp();
            fu.setPatientId(1L + (i % 2)); // 2 patients
            fu.setCaregiverId(10L);
            fu.setFollowUpDate(LocalDate.now().minusDays(i));
            fu.setCognitiveScore(20 - i);
            fu.setMood(MoodState.CALM);
            fu.setEating(IndependenceLevel.INDEPENDENT);
            fu.setDressing(IndependenceLevel.INDEPENDENT);
            fu.setMobility(IndependenceLevel.NEEDS_ASSISTANCE);
            fu.setHoursSlept(7 - i);
            fu.setSleepQuality(SleepQuality.GOOD);
            fu.setAgitationObserved(false);
            fu.setConfusionObserved(false);
            followUpService.createFollowUp(fu);
        }

        Map<String, Object> stats = followUpService.getStatistics();

        assertEquals(3, stats.get("totalFollowUps"));
        assertNotNull(stats.get("averageCognitiveScore"));
        assertNotNull(stats.get("averageHoursSlept"));
        assertNotNull(stats.get("moodDistribution"));
        assertNotNull(stats.get("sleepQualityDistribution"));
    }

    @Test
    void testGetStatisticsByPatient() {
        // Create follow-ups for patient 1
        for (int i = 0; i < 3; i++) {
            FollowUp fu = new FollowUp();
            fu.setPatientId(1L);
            fu.setCaregiverId(10L);
            fu.setFollowUpDate(LocalDate.now().minusDays(i));
            fu.setCognitiveScore(20 - i);
            fu.setMood(MoodState.CALM);
            fu.setEating(IndependenceLevel.INDEPENDENT);
            fu.setDressing(IndependenceLevel.INDEPENDENT);
            fu.setMobility(IndependenceLevel.NEEDS_ASSISTANCE);
            followUpService.createFollowUp(fu);
        }

        Map<String, Object> stats = followUpService.getStatisticsByPatient(1L);

        assertEquals(1L, stats.get("patientId"));
        assertEquals(3, stats.get("totalFollowUps"));
        assertNotNull(stats.get("averageCognitiveScore"));
        assertNotNull(stats.get("moodDistribution"));
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    void testCreateMultipleFollowUpsForSamePatient() {
        for (int i = 0; i < 5; i++) {
            FollowUp fu = new FollowUp();
            fu.setPatientId(1L);
            fu.setCaregiverId(10L);
            fu.setFollowUpDate(LocalDate.now().minusDays(i));
            fu.setCognitiveScore(20 - (i % 5));
            fu.setMood(MoodState.CALM);
            fu.setEating(IndependenceLevel.INDEPENDENT);
            fu.setDressing(IndependenceLevel.INDEPENDENT);
            fu.setMobility(IndependenceLevel.NEEDS_ASSISTANCE);
            followUpService.createFollowUp(fu);
        }

        List<FollowUp> followUps = followUpService.getFollowUpsByPatientId(1L);
        assertEquals(5, followUps.size());
    }

    @Test
    void testFollowUpWithNullCognitiveScore() {
        testFollowUp.setCognitiveScore(null);
        FollowUp created = followUpService.createFollowUp(testFollowUp);

        assertNotNull(created.getId());
        assertNull(created.getCognitiveScore());
    }
}
