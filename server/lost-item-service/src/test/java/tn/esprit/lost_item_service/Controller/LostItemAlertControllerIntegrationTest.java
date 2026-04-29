package tn.esprit.lost_item_service.Controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tn.esprit.lost_item_service.dto.CreateLostItemAlertRequest;
import tn.esprit.lost_item_service.Entity.*;
import tn.esprit.lost_item_service.Repository.LostItemAlertRepository;
import tn.esprit.lost_item_service.Repository.LostItemRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@ActiveProfiles("test")
class LostItemAlertControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private LostItemAlertRepository alertRepository;

    @Autowired
    private LostItemRepository lostItemRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private LostItem testItem;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
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
    void testCreateAlertWithValidRequest() throws Exception {
        CreateLostItemAlertRequest request = CreateLostItemAlertRequest.builder()
                .lostItemId(testItem.getId())
                .patientId(1L)
                .caregiverId(2L)
                .title("Critical Alert")
                .description("Medication is missing")
                .level(AlertLevel.CRITICAL)
                .build();

        mockMvc.perform(post("/api/item-alerts")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request))
                .header("X-User-Id", "1")
                .header("X-User-Role", "CAREGIVER"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Critical Alert"))
                .andExpect(jsonPath("$.level").value("CRITICAL"));
    }

    @Test
    void testGetAlertByIdWithValidId() throws Exception {
        LostItemAlert alert = LostItemAlert.builder()
                .lostItemId(testItem.getId())
                .patientId(1L)
                .title("Test Alert")
                .level(AlertLevel.HIGH)
                .status(AlertStatus.NEW)
                .build();
        LostItemAlert saved = alertRepository.save(alert);

        mockMvc.perform(get("/api/item-alerts/" + saved.getId())
                .header("X-User-Id", "1")
                .header("X-User-Role", "PATIENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.title").value("Test Alert"));
    }

    @Test
    void testGetAlertsByPatientId() throws Exception {
        LostItemAlert alert1 = LostItemAlert.builder()
                .lostItemId(testItem.getId())
                .patientId(1L)
                .title("Alert 1")
                .level(AlertLevel.HIGH)
                .status(AlertStatus.NEW)
                .build();
        LostItemAlert alert2 = LostItemAlert.builder()
                .lostItemId(testItem.getId())
                .patientId(1L)
                .title("Alert 2")
                .level(AlertLevel.MEDIUM)
                .status(AlertStatus.VIEWED)
                .build();
        alertRepository.save(alert1);
        alertRepository.save(alert2);

        mockMvc.perform(get("/api/item-alerts/patient/1")
                .header("X-User-Id", "1")
                .header("X-User-Role", "PATIENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    void testUpdateAlertWithValidRequest() throws Exception {
        LostItemAlert alert = LostItemAlert.builder()
                .lostItemId(testItem.getId())
                .patientId(1L)
                .title("Original Title")
                .level(AlertLevel.LOW)
                .status(AlertStatus.NEW)
                .build();
        LostItemAlert saved = alertRepository.save(alert);

        CreateLostItemAlertRequest updateRequest = CreateLostItemAlertRequest.builder()
                .lostItemId(testItem.getId())
                .patientId(1L)
                .title("Updated Title")
                .level(AlertLevel.MEDIUM)
                .build();

        mockMvc.perform(put("/api/item-alerts/" + saved.getId())
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.level").value("MEDIUM"));
    }

    @Test
    void testDeleteAlertWithValidId() throws Exception {
        LostItemAlert alert = LostItemAlert.builder()
                .lostItemId(testItem.getId())
                .patientId(1L)
                .title("To Delete")
                .level(AlertLevel.HIGH)
                .status(AlertStatus.NEW)
                .build();
        LostItemAlert saved = alertRepository.save(alert);

        mockMvc.perform(delete("/api/item-alerts/" + saved.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void testGetAllAlerts() throws Exception {
        LostItemAlert alert = LostItemAlert.builder()
                .lostItemId(testItem.getId())
                .patientId(1L)
                .title("Test Alert")
                .level(AlertLevel.CRITICAL)
                .status(AlertStatus.NEW)
                .build();
        alertRepository.save(alert);

        mockMvc.perform(get("/api/item-alerts")
                .header("X-User-Id", "1")
                .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)));
    }

    @Test
    void testGetAlertsByStatus() throws Exception {
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
                .title("Resolved Alert")
                .level(AlertLevel.LOW)
                .status(AlertStatus.RESOLVED)
                .build();
        alertRepository.save(alert1);
        alertRepository.save(alert2);

        mockMvc.perform(get("/api/item-alerts/status/NEW")
                .header("X-User-Id", "1")
                .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void testCreateAlertWithMinimalRequest() throws Exception {
        CreateLostItemAlertRequest request = CreateLostItemAlertRequest.builder()
                .lostItemId(testItem.getId())
                .patientId(1L)
                .title("Minimal Alert")
                .level(AlertLevel.MEDIUM)
                .build();

        mockMvc.perform(post("/api/item-alerts")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request))
                .header("X-User-Id", "1")
                .header("X-User-Role", "CAREGIVER"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Minimal Alert"));
    }
}
