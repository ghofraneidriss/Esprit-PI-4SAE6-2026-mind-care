package tn.esprit.followup_alert_service.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tn.esprit.followup_alert_service.Entity.FollowUp;
import tn.esprit.followup_alert_service.Entity.FollowUpStatus;
import tn.esprit.followup_alert_service.Repository.FollowUpRepository;

import java.time.LocalDateTime;
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
        testFollowUp.setStatus(FollowUpStatus.PENDING);
        testFollowUp.setDescription("Test Follow-up");
        testFollowUp.setScheduledDate(LocalDateTime.now().plusDays(1));
    }

    @Test
    void testCreateFollowUp() {
        FollowUp created = followUpService.createFollowUp(testFollowUp);
        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals("Test Follow-up", created.getDescription());
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
    void testGetFollowUpsByStatus() {
        followUpService.createFollowUp(testFollowUp);
        List<FollowUp> followUps = followUpService.getFollowUpsByStatus(FollowUpStatus.PENDING);
        assertFalse(followUps.isEmpty());
    }
}
