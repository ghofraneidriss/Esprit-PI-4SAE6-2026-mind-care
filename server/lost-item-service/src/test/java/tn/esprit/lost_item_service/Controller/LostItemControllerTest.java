package tn.esprit.lost_item_service.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tn.esprit.lost_item_service.Entity.*;
import tn.esprit.lost_item_service.Repository.LostItemRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LostItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LostItemRepository lostItemRepository;

    private LostItem testItem;

    @BeforeEach
    void setUp() {
        lostItemRepository.deleteAll();

        testItem = new LostItem();
        testItem.setTitle("Test Medication");
        testItem.setDescription("Lost medication");
        testItem.setCategory(ItemCategory.MEDICATION);
        testItem.setPatientId(1L);
        testItem.setCaregiverId(10L);
        testItem.setStatus(ItemStatus.LOST);
        testItem.setPriority(ItemPriority.CRITICAL);
        testItem.setLastSeenLocation("Living room");
        testItem.setCreatedAt(LocalDateTime.now());
        testItem.setUpdatedAt(LocalDateTime.now());
        lostItemRepository.save(testItem);
    }

    // ==================== GET ALL ITEMS TESTS ====================

    @Test
    void testGetAllLostItemsAsAdmin() throws Exception {
        mockMvc.perform(get("/api/lost-items")
                .header("X-User-Role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void testGetAllLostItemsAsPatient() throws Exception {
        mockMvc.perform(get("/api/lost-items")
                .header("X-User-Id", "1")
                .header("X-User-Role", "PATIENT")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void testGetAllLostItemsAsCaregiver() throws Exception {
        mockMvc.perform(get("/api/lost-items")
                .header("X-User-Id", "10")
                .header("X-User-Role", "CAREGIVER")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    // ==================== CREATE ITEM TESTS ====================

    @Test
    void testCreateLostItem() throws Exception {
        LostItem newItem = new LostItem();
        newItem.setTitle("New Lost Item");
        newItem.setCategory(ItemCategory.CLOTHING);
        newItem.setPatientId(2L);
        newItem.setCaregiverId(11L);
        newItem.setStatus(ItemStatus.LOST);
        newItem.setPriority(ItemPriority.MEDIUM);
        newItem.setLastSeenLocation("Kitchen");

        mockMvc.perform(post("/api/lost-items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newItem)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", equalTo("New Lost Item")))
                .andExpect(jsonPath("$.category", equalTo("CLOTHING")));
    }

    // ==================== GET BY ID TESTS ====================

    @Test
    void testGetLostItemById() throws Exception {
        mockMvc.perform(get("/api/lost-items/" + testItem.getId())
                .header("X-User-Id", "1")
                .header("X-User-Role", "PATIENT")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", equalTo("Test Medication")));
    }

    @Test
    void testGetLostItemByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/lost-items/999")
                .header("X-User-Id", "1")
                .header("X-User-Role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // ==================== PATIENT ITEMS WITH PAGINATION ====================

    @Test
    void testGetPatientLostItemsWithPagination() throws Exception {
        // Create multiple items
        for (int i = 0; i < 5; i++) {
            LostItem item = new LostItem();
            item.setTitle("Item " + i);
            item.setCategory(ItemCategory.CLOTHING);
            item.setPatientId(1L);
            item.setStatus(ItemStatus.LOST);
            item.setPriority(ItemPriority.MEDIUM);
            item.setCreatedAt(LocalDateTime.now().minusDays(i));
            lostItemRepository.save(item);
        }

        mockMvc.perform(get("/api/lost-items/patient/1")
                .param("page", "0")
                .param("size", "2")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(5)))
                .andExpect(jsonPath("$.totalPages", greaterThan(0)))
                .andExpect(jsonPath("$.currentPage", equalTo(0)))
                .andExpect(jsonPath("$.pageSize", equalTo(2)));
    }

    @Test
    void testGetPatientLostItemsFilteredByStatus() throws Exception {
        mockMvc.perform(get("/api/lost-items/patient/1")
                .param("status", "LOST")
                .param("page", "0")
                .param("size", "20")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void testGetPatientLostItemsFilteredByCategory() throws Exception {
        mockMvc.perform(get("/api/lost-items/patient/1")
                .param("category", "MEDICATION")
                .param("page", "0")
                .param("size", "20")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))));
    }

    // ==================== UPDATE ITEM TESTS ====================

    @Test
    void testUpdateLostItem() throws Exception {
        LostItem updated = new LostItem();
        updated.setTitle("Updated Title");
        updated.setDescription("Updated description");
        updated.setCategory(ItemCategory.DOCUMENT);
        updated.setPatientId(1L);
        updated.setCaregiverId(10L);
        updated.setLastSeenLocation("Kitchen");
        updated.setStatus(ItemStatus.SEARCHING);
        updated.setPriority(ItemPriority.HIGH);

        mockMvc.perform(put("/api/lost-items/" + testItem.getId())
                .header("X-User-Id", "1")
                .header("X-User-Role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", equalTo("Updated Title")))
                .andExpect(jsonPath("$.status", equalTo("SEARCHING")));
    }

    // ==================== MARK AS FOUND TESTS ====================

    @Test
    void testMarkLostItemAsFound() throws Exception {
        mockMvc.perform(patch("/api/lost-items/" + testItem.getId() + "/found")
                .header("X-User-Id", "1")
                .header("X-User-Role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", equalTo("FOUND")));
    }

    // ==================== DELETE ITEM TESTS ====================

    @Test
    void testDeleteLostItem() throws Exception {
        mockMvc.perform(delete("/api/lost-items/" + testItem.getId())
                .header("X-User-Id", "1")
                .header("X-User-Role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    // ==================== RISK CALCULATION TESTS ====================

    @Test
    void testGetPatientItemRisk() throws Exception {
        mockMvc.perform(get("/api/lost-items/patient/1/risk")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskLevel", notNullValue()))
                .andExpect(jsonPath("$.riskScore", notNullValue()));
    }

    // ==================== FREQUENT LOSING DETECTION ====================

    @Test
    void testDetectFrequentLosingPattern() throws Exception {
        // Create multiple items to detect pattern
        for (int i = 0; i < 5; i++) {
            LostItem item = new LostItem();
            item.setTitle("Item " + i);
            item.setCategory(ItemCategory.CLOTHING);
            item.setPatientId(1L);
            item.setStatus(ItemStatus.LOST);
            item.setPriority(ItemPriority.LOW);
            item.setCreatedAt(LocalDateTime.now().minusDays(60 - (i * 15)));
            lostItemRepository.save(item);
        }

        mockMvc.perform(get("/api/lost-items/patient/1/frequent-losing")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trend", notNullValue()))
                .andExpect(jsonPath("$.isFrequentLoser", notNullValue()));
    }
}
