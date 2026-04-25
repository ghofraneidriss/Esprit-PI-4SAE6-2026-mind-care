package tn.esprit.lost_item_service.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.lost_item_service.Entity.*;
import tn.esprit.lost_item_service.Repository.LostItemRepository;
import tn.esprit.lost_item_service.Repository.SearchReportRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SearchReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LostItemRepository lostItemRepository;

    @Autowired
    private SearchReportRepository searchReportRepository;

    private LostItem testItem;
    private SearchReport testReport;

    @BeforeEach
    void setUp() {
        searchReportRepository.deleteAll();
        lostItemRepository.deleteAll();

        testItem = new LostItem();
        testItem.setTitle("Test Item");
        testItem.setCategory(ItemCategory.MEDICATION);
        testItem.setPatientId(1L);
        testItem.setStatus(ItemStatus.LOST);
        testItem.setPriority(ItemPriority.CRITICAL);
        testItem.setCreatedAt(LocalDateTime.now());
        lostItemRepository.save(testItem);

        testReport = new SearchReport();
        testReport.setLostItemId(testItem.getId());
        testReport.setReportedBy(10L);
        testReport.setSearchDate(LocalDate.now());
        testReport.setLocationSearched("Living Room");
        testReport.setSearchResult(SearchResult.NOT_FOUND);
        testReport.setStatus(ReportStatus.OPEN);
        searchReportRepository.save(testReport);
    }

    // ==================== CREATE REPORT TESTS ====================

    @Test
    void testCreateSearchReport() throws Exception {
        SearchReport newReport = new SearchReport();
        newReport.setLostItemId(testItem.getId());
        newReport.setReportedBy(10L);
        newReport.setSearchDate(LocalDate.now().minusDays(1));
        newReport.setLocationSearched("Kitchen");
        newReport.setSearchResult(SearchResult.NOT_FOUND);
        newReport.setStatus(ReportStatus.OPEN);

        mockMvc.perform(post("/api/search-reports")
                .header("X-User-Id", "1")
                .header("X-User-Role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newReport)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.locationSearched", equalTo("Kitchen")));
    }

    @Test
    void testCreateDuplicateSearchReportThrowsException() throws Exception {
        SearchReport duplicate = new SearchReport();
        duplicate.setLostItemId(testItem.getId());
        duplicate.setReportedBy(10L);
        duplicate.setSearchDate(LocalDate.now()); // Same date as testReport
        duplicate.setLocationSearched("Bedroom");
        duplicate.setSearchResult(SearchResult.NOT_FOUND);

        mockMvc.perform(post("/api/search-reports")
                .header("X-User-Id", "1")
                .header("X-User-Role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isBadRequest());
    }

    // ==================== GET REPORT TESTS ====================

    @Test
    void testGetSearchReportById() throws Exception {
        mockMvc.perform(get("/api/search-reports/" + testReport.getId())
                .header("X-User-Id", "1")
                .header("X-User-Role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locationSearched", equalTo("Living Room")));
    }

    @Test
    void testGetSearchReportByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/search-reports/999")
                .header("X-User-Id", "1")
                .header("X-User-Role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // ==================== UPDATE REPORT TESTS ====================

    @Test
    void testUpdateSearchReport() throws Exception {
        SearchReport updated = new SearchReport();
        updated.setLostItemId(testItem.getId());
        updated.setReportedBy(10L);
        updated.setSearchDate(LocalDate.now());
        updated.setLocationSearched("Bedroom");
        updated.setSearchResult(SearchResult.FOUND);
        updated.setStatus(ReportStatus.CLOSED);

        mockMvc.perform(put("/api/search-reports/" + testReport.getId())
                .header("X-User-Id", "1")
                .header("X-User-Role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locationSearched", equalTo("Bedroom")))
                .andExpect(jsonPath("$.searchResult", equalTo("FOUND")));
    }

    // ==================== DELETE REPORT TESTS ====================

    @Test
    void testDeleteSearchReport() throws Exception {
        mockMvc.perform(delete("/api/search-reports/" + testReport.getId())
                .header("X-User-Id", "1")
                .header("X-User-Role", "ADMIN")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    // ==================== SEARCH TIMELINE TESTS ====================

    @Test
    void testGetSearchTimeline() throws Exception {
        // Create multiple reports
        for (int i = 0; i < 3; i++) {
            SearchReport report = new SearchReport();
            report.setLostItemId(testItem.getId());
            report.setReportedBy(10L);
            report.setSearchDate(LocalDate.now().minusDays(i));
            report.setLocationSearched("Location " + i);
            report.setSearchResult(SearchResult.NOT_FOUND);
            report.setStatus(ReportStatus.OPEN);
            searchReportRepository.save(report);
        }

        mockMvc.perform(get("/api/search-reports/item/" + testItem.getId() + "/timeline")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSearches", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.successRate", notNullValue()))
                .andExpect(jsonPath("$.locationFrequency", notNullValue()));
    }

    // ==================== ADVANCED SEARCH TESTS ====================

    @Test
    void testAdvancedSearchByResult() throws Exception {
        // Create reports with different results
        SearchReport found = new SearchReport();
        found.setLostItemId(testItem.getId());
        found.setReportedBy(10L);
        found.setSearchDate(LocalDate.now().minusDays(1));
        found.setLocationSearched("Kitchen");
        found.setSearchResult(SearchResult.FOUND);
        found.setStatus(ReportStatus.CLOSED);
        searchReportRepository.save(found);

        mockMvc.perform(get("/api/search-reports/search")
                .param("lostItemId", String.valueOf(testItem.getId()))
                .param("searchResult", "FOUND")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void testAdvancedSearchByLocation() throws Exception {
        mockMvc.perform(get("/api/search-reports/search")
                .param("lostItemId", String.valueOf(testItem.getId()))
                .param("locationKeyword", "living")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    // ==================== PATIENT REPORTS TESTS ====================

    @Test
    void testGetReportsByPatient() throws Exception {
        mockMvc.perform(get("/api/search-reports/patient/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void testGetReportsByPatientNoItems() throws Exception {
        mockMvc.perform(get("/api/search-reports/patient/999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ==================== SEARCH SUGGESTIONS TESTS ====================

    @Test
    void testGetSearchSuggestions() throws Exception {
        mockMvc.perform(get("/api/search-reports/item/" + testItem.getId() + "/suggestions")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.Collection.class)));
    }

    // ==================== RECOVERY STRATEGY TESTS ====================

    @Test
    void testGetRecoveryStrategy() throws Exception {
        mockMvc.perform(get("/api/search-reports/item/" + testItem.getId() + "/recovery-strategy")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategy", notNullValue()));
    }
}
