package tn.esprit.followup_alert_service.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tn.esprit.followup_alert_service.Entity.FollowUp;
import tn.esprit.followup_alert_service.Entity.IndependenceLevel;
import tn.esprit.followup_alert_service.Entity.MoodState;
import tn.esprit.followup_alert_service.Repository.FollowUpRepository;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class FollowUpServiceTest {

    @Autowired
    private FollowUpService followUpService;

    @Autowired
    private FollowUpRepository followUpRepository;

    private FollowUp testFollowUp;

    @BeforeEach
    void setUp() {
        followUpRepository.deleteAll();
        testFollowUp = new FollowUp();
        testFollowUp.setPatientId(1L);
        testFollowUp.setCaregiverId(2L);
        testFollowUp.setFollowUpDate(LocalDate.now());
        testFollowUp.setCognitiveScore(25);
        testFollowUp.setMood(MoodState.CALM);
        testFollowUp.setEating(IndependenceLevel.INDEPENDENT);
        testFollowUp.setDressing(IndependenceLevel.INDEPENDENT);
        testFollowUp.setMobility(IndependenceLevel.NEEDS_ASSISTANCE);
        testFollowUp.setHoursSlept(8);
        testFollowUp.setNotes("Test Follow-up Note");
    }

    @Test
    void testCreateFollowUp() {
        FollowUp created = followUpService.createFollowUp(testFollowUp);
        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals(1L, created.getPatientId());
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
    void testGetFollowUpsByPatientId() {
        followUpService.createFollowUp(testFollowUp);
        List<FollowUp> followUps = followUpService.getFollowUpsByPatientId(1L);
        assertFalse(followUps.isEmpty());
    }

    @Test
    void testUpdateFollowUp() {
        FollowUp created = followUpService.createFollowUp(testFollowUp);
        created.setCognitiveScore(20);
        FollowUp updated = followUpService.updateFollowUp(created.getId(), created);
        assertNotNull(updated);
        assertEquals(20, updated.getCognitiveScore());
    }
}
