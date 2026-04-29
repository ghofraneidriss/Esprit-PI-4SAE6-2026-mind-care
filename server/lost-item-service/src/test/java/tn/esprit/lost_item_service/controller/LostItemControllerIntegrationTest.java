package tn.esprit.lost_item_service.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tn.esprit.lost_item_service.dto.CreateLostItemRequest;
import tn.esprit.lost_item_service.dto.UpdateLostItemRequest;
import tn.esprit.lost_item_service.entity.ItemCategory;
import tn.esprit.lost_item_service.entity.ItemPriority;
import tn.esprit.lost_item_service.entity.ItemStatus;
import tn.esprit.lost_item_service.entity.LostItem;
import tn.esprit.lost_item_service.repository.LostItemRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@ActiveProfiles("test")
class LostItemControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private LostItemRepository lostItemRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        lostItemRepository.deleteAll();
    }

    @Test
    void testCreateLostItemWithValidRequest() throws Exception {
        CreateLostItemRequest request = CreateLostItemRequest.builder()
                .title("Lost Medication")
                .description("Important medication")
                .category(ItemCategory.MEDICATION)
                .patientId(1L)
                .caregiverId(2L)
                .lastSeenLocation("Kitchen")
                .lastSeenDate(LocalDate.now())
                .priority(ItemPriority.CRITICAL)
                .build();

        mockMvc.perform(post("/api/lost-items")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request))
                .header("X-User-Id", "1")
                .header("X-User-Role", "CAREGIVER"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Lost Medication"))
                .andExpect(jsonPath("$.category").value("MEDICATION"));
    }

    @Test
    void testGetLostItemByIdWithValidId() throws Exception {
        LostItem item = LostItem.builder()
                .title("Test Item")
                .category(ItemCategory.CLOTHING)
                .patientId(1L)
                .status(ItemStatus.LOST)
                .build();
        LostItem saved = lostItemRepository.save(item);

        mockMvc.perform(get("/api/lost-items/" + saved.getId())
                .header("X-User-Id", "1")
                .header("X-User-Role", "PATIENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.title").value("Test Item"));
    }

    @Test
    void testGetLostItemsByPatientId() throws Exception {
        LostItem item1 = LostItem.builder()
                .title("Item 1")
                .category(ItemCategory.DOCUMENT)
                .patientId(5L)
                .status(ItemStatus.LOST)
                .build();
        LostItem item2 = LostItem.builder()
                .title("Item 2")
                .category(ItemCategory.MEDICATION)
                .patientId(5L)
                .status(ItemStatus.FOUND)
                .build();
        lostItemRepository.save(item1);
        lostItemRepository.save(item2);

        mockMvc.perform(get("/api/lost-items/patient/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.Map.class)));
    }


    @Test
    void testDeleteLostItemWithValidId() throws Exception {
        LostItem item = LostItem.builder()
                .title("To Delete")
                .category(ItemCategory.CLOTHING)
                .patientId(1L)
                .status(ItemStatus.LOST)
                .build();
        LostItem saved = lostItemRepository.save(item);

        mockMvc.perform(delete("/api/lost-items/" + saved.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void testGetAllLostItems() throws Exception {
        LostItem item1 = LostItem.builder()
                .title("Item 1")
                .category(ItemCategory.MEDICATION)
                .patientId(1L)
                .status(ItemStatus.LOST)
                .build();
        lostItemRepository.save(item1);

        mockMvc.perform(get("/api/lost-items")
                .header("X-User-Id", "1")
                .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)));
    }

    @Test
    void testMarkItemAsFound() throws Exception {
        LostItem item = LostItem.builder()
                .title("Lost Item")
                .category(ItemCategory.DOCUMENT)
                .patientId(1L)
                .status(ItemStatus.LOST)
                .build();
        LostItem saved = lostItemRepository.save(item);

        mockMvc.perform(patch("/api/lost-items/" + saved.getId() + "/mark-found"))
                .andExpect(status().isOk());
    }

    @Test
    void testCreateLostItemWithMinimalRequest() throws Exception {
        CreateLostItemRequest request = CreateLostItemRequest.builder()
                .title("Minimal Item")
                .category(ItemCategory.ACCESSORY)
                .patientId(1L)
                .build();

        mockMvc.perform(post("/api/lost-items")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request))
                .header("X-User-Id", "1")
                .header("X-User-Role", "CAREGIVER"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Minimal Item"));
    }

    @Test
    void testUpdateLostItemWithValidRequest() throws Exception {
        LostItem item = LostItem.builder()
                .title("Original Item")
                .category(ItemCategory.CLOTHING)
                .patientId(1L)
                .status(ItemStatus.LOST)
                .build();
        LostItem saved = lostItemRepository.save(item);

        UpdateLostItemRequest updatePayload = UpdateLostItemRequest.builder()
                .title("Updated Item")
                .description("Updated description")
                .category(ItemCategory.MEDICATION)
                .patientId(1L)
                .status(ItemStatus.LOST)
                .build();

        mockMvc.perform(put("/api/lost-items/" + saved.getId())
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(updatePayload))
                .header("X-User-Id", "1")
                .header("X-User-Role", "PATIENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Item"));
    }

    @Test
    void testGetCriticalLostItems() throws Exception {
        LostItem item1 = LostItem.builder()
                .title("Critical Item 1")
                .category(ItemCategory.MEDICATION)
                .patientId(1L)
                .status(ItemStatus.LOST)
                .priority(ItemPriority.CRITICAL)
                .build();
        LostItem item2 = LostItem.builder()
                .title("Critical Item 2")
                .category(ItemCategory.DOCUMENT)
                .patientId(1L)
                .status(ItemStatus.LOST)
                .priority(ItemPriority.CRITICAL)
                .build();
        lostItemRepository.save(item1);
        lostItemRepository.save(item2);

        mockMvc.perform(get("/api/lost-items/patient/1/critical"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", isA(java.util.List.class)))
                .andExpect(jsonPath("$.urgentCount").value(2));
    }

    @Test
    void testGetAllCriticalItems() throws Exception {
        LostItem item = LostItem.builder()
                .title("Critical Item")
                .category(ItemCategory.MEDICATION)
                .patientId(5L)
                .status(ItemStatus.LOST)
                .priority(ItemPriority.CRITICAL)
                .build();
        lostItemRepository.save(item);

        mockMvc.perform(get("/api/lost-items/critical/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", isA(java.util.List.class)));
    }

    @Test
    void testGetItemsByCaregiver() throws Exception {
        LostItem item = LostItem.builder()
                .title("Caregiver Item")
                .category(ItemCategory.CLOTHING)
                .patientId(1L)
                .caregiverId(2L)
                .status(ItemStatus.LOST)
                .build();
        lostItemRepository.save(item);

        mockMvc.perform(get("/api/lost-items/caregiver/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)));
    }

    @Test
    void testGetCriticalItemsByCaregiver() throws Exception {
        LostItem item = LostItem.builder()
                .title("Critical Caregiver Item")
                .category(ItemCategory.MEDICATION)
                .patientId(1L)
                .caregiverId(3L)
                .status(ItemStatus.LOST)
                .priority(ItemPriority.CRITICAL)
                .build();
        lostItemRepository.save(item);

        mockMvc.perform(get("/api/lost-items/caregiver/3/critical"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", isA(java.util.List.class)));
    }

    @Test
    void testGetGlobalStatistics() throws Exception {
        LostItem item = LostItem.builder()
                .title("Stat Item")
                .category(ItemCategory.CLOTHING)
                .patientId(1L)
                .status(ItemStatus.LOST)
                .priority(ItemPriority.HIGH)
                .build();
        lostItemRepository.save(item);

        mockMvc.perform(get("/api/lost-items/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.Map.class)));
    }

    @Test
    void testGetPatientItemRisk() throws Exception {
        LostItem item = LostItem.builder()
                .title("Risk Item")
                .category(ItemCategory.MEDICATION)
                .patientId(5L)
                .status(ItemStatus.LOST)
                .priority(ItemPriority.CRITICAL)
                .build();
        lostItemRepository.save(item);

        mockMvc.perform(get("/api/lost-items/patient/5/risk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.Map.class)));
    }

    @Test
    void testGetPatientFrequencyTrend() throws Exception {
        LostItem item = LostItem.builder()
                .title("Trend Item")
                .category(ItemCategory.CLOTHING)
                .patientId(5L)
                .status(ItemStatus.LOST)
                .build();
        lostItemRepository.save(item);

        mockMvc.perform(get("/api/lost-items/patient/5/trend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.Map.class)));
    }
}
