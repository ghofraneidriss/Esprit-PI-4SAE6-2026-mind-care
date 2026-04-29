package tn.esprit.lost_item_service.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tn.esprit.lost_item_service.entity.*;
import tn.esprit.lost_item_service.repository.LostItemRepository;
import tn.esprit.lost_item_service.repository.SearchReportRepository;
import tn.esprit.lost_item_service.dto.CreateSearchReportRequest;
import tn.esprit.lost_item_service.dto.UpdateSearchReportRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@ActiveProfiles("test")
class SearchReportControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private SearchReportRepository searchReportRepository;

    @Autowired
    private LostItemRepository lostItemRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private LostItem testItem;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        searchReportRepository.deleteAll();
        lostItemRepository.deleteAll();

        testItem = LostItem.builder()
                .title("Test Item")
                .category(ItemCategory.MEDICATION)
                .patientId(1L)
                .status(ItemStatus.LOST)
                .priority(ItemPriority.CRITICAL)
                .lastSeenLocation("Kitchen")
                .lastSeenDate(LocalDate.now())
                .build();
        lostItemRepository.save(testItem);
    }

    @Test
    void testCreateSearchReportWithValidReport() throws Exception {
        CreateSearchReportRequest request = CreateSearchReportRequest.builder()
                .lostItemId(testItem.getId())
                .reportedBy(2L)
                .searchDate(LocalDate.now())
                .locationSearched("Living Room")
                .searchResult(SearchResult.NOT_FOUND)
                .notes("Not found in living room")
                .build();

        mockMvc.perform(post("/api/search-reports")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.report.id").exists())
                .andExpect(jsonPath("$.report.locationSearched").value("Living Room"))
                .andExpect(jsonPath("$.searchSuggestions").isArray());
    }

    @Test
    void testGetSearchReportsByLostItemId() throws Exception {
        SearchReport report = SearchReport.builder()
                .lostItemId(testItem.getId())
                .reportedBy(2L)
                .searchDate(LocalDate.now())
                .locationSearched("Kitchen")
                .searchResult(SearchResult.FOUND)
                .status(ReportStatus.OPEN)
                .build();
        searchReportRepository.save(report);

        mockMvc.perform(get("/api/search-reports/lost-item/" + testItem.getId())
                .header("X-User-Id", "1")
                .header("X-User-Role", "PATIENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)))
                .andExpect(jsonPath("$[0].locationSearched").value("Kitchen"));
    }

    @Test
    void testGetSearchReportByIdWithValidId() throws Exception {
        SearchReport report = SearchReport.builder()
                .lostItemId(testItem.getId())
                .reportedBy(2L)
                .searchDate(LocalDate.now())
                .locationSearched("Bedroom")
                .searchResult(SearchResult.PARTIALLY_FOUND)
                .status(ReportStatus.OPEN)
                .build();
        SearchReport saved = searchReportRepository.save(report);

        mockMvc.perform(get("/api/search-reports/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.locationSearched").value("Bedroom"));
    }


    @Test
    void testDeleteSearchReportWithValidId() throws Exception {
        SearchReport report = SearchReport.builder()
                .lostItemId(testItem.getId())
                .reportedBy(2L)
                .searchDate(LocalDate.now())
                .locationSearched("To Delete")
                .searchResult(SearchResult.NOT_FOUND)
                .status(ReportStatus.OPEN)
                .build();
        SearchReport saved = searchReportRepository.save(report);

        mockMvc.perform(delete("/api/search-reports/" + saved.getId())
                .header("X-User-Id", "2")
                .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetOpenReportsCount() throws Exception {
        SearchReport report = SearchReport.builder()
                .lostItemId(testItem.getId())
                .reportedBy(2L)
                .searchDate(LocalDate.now())
                .locationSearched("Location")
                .searchResult(SearchResult.NOT_FOUND)
                .status(ReportStatus.OPEN)
                .build();
        searchReportRepository.save(report);

        mockMvc.perform(get("/api/search-reports/lost-item/" + testItem.getId() + "/open-count")
                .header("X-User-Id", "1")
                .header("X-User-Role", "PATIENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openCount").isNumber());
    }

    @Test
    void testAdvancedSearch() throws Exception {
        SearchReport report = SearchReport.builder()
                .lostItemId(testItem.getId())
                .reportedBy(2L)
                .searchDate(LocalDate.now())
                .locationSearched("Kitchen")
                .searchResult(SearchResult.FOUND)
                .status(ReportStatus.CLOSED)
                .build();
        searchReportRepository.save(report);

        mockMvc.perform(get("/api/search-reports/search")
                .param("lostItemId", testItem.getId().toString())
                .param("searchResult", "FOUND"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)));
    }

    @Test
    void testGetSearchTimeline() throws Exception {
        SearchReport report1 = SearchReport.builder()
                .lostItemId(testItem.getId())
                .reportedBy(2L)
                .searchDate(LocalDate.now())
                .locationSearched("Kitchen")
                .searchResult(SearchResult.NOT_FOUND)
                .status(ReportStatus.OPEN)
                .build();
        SearchReport report2 = SearchReport.builder()
                .lostItemId(testItem.getId())
                .reportedBy(3L)
                .searchDate(LocalDate.now().minusDays(1))
                .locationSearched("Bedroom")
                .searchResult(SearchResult.PARTIALLY_FOUND)
                .status(ReportStatus.OPEN)
                .build();
        searchReportRepository.save(report1);
        searchReportRepository.save(report2);

        mockMvc.perform(get("/api/search-reports/lost-item/" + testItem.getId() + "/timeline")
                .header("X-User-Id", "1")
                .header("X-User-Role", "PATIENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSearches").isNumber())
                .andExpect(jsonPath("$.timeline").isArray());
    }

    @Test
    void testGetGlobalSearchLogStats() throws Exception {
        SearchReport report = SearchReport.builder()
                .lostItemId(testItem.getId())
                .reportedBy(2L)
                .searchDate(LocalDate.now())
                .locationSearched("Location")
                .searchResult(SearchResult.FOUND)
                .status(ReportStatus.CLOSED)
                .build();
        searchReportRepository.save(report);

        mockMvc.perform(get("/api/search-reports/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalReports").isNumber())
                .andExpect(jsonPath("$.globalSuccessRate").isNumber());
    }

    @Test
    void testGetReportsByReporter() throws Exception {
        SearchReport report = SearchReport.builder()
                .lostItemId(testItem.getId())
                .reportedBy(2L)
                .searchDate(LocalDate.now())
                .locationSearched("Location")
                .searchResult(SearchResult.NOT_FOUND)
                .status(ReportStatus.OPEN)
                .build();
        searchReportRepository.save(report);

        mockMvc.perform(get("/api/search-reports/reporter/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)));
    }

    @Test
    void testGetReportsByPatient() throws Exception {
        SearchReport report = SearchReport.builder()
                .lostItemId(testItem.getId())
                .reportedBy(2L)
                .searchDate(LocalDate.now())
                .locationSearched("Location")
                .searchResult(SearchResult.NOT_FOUND)
                .status(ReportStatus.OPEN)
                .build();
        searchReportRepository.save(report);

        mockMvc.perform(get("/api/search-reports/patient/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)));
    }

    @Test
    void testUpdateSearchReportWithValidRequest() throws Exception {
        SearchReport report = SearchReport.builder()
                .lostItemId(testItem.getId())
                .reportedBy(2L)
                .searchDate(LocalDate.now())
                .locationSearched("Kitchen")
                .searchResult(SearchResult.NOT_FOUND)
                .status(ReportStatus.OPEN)
                .build();
        SearchReport saved = searchReportRepository.save(report);

        UpdateSearchReportRequest updatePayload = UpdateSearchReportRequest.builder()
                .lostItemId(testItem.getId())
                .reportedBy(2L)
                .searchDate(LocalDate.now())
                .locationSearched("Living Room")
                .searchResult(SearchResult.FOUND)
                .status(ReportStatus.CLOSED)
                .notes("Found in living room")
                .build();

        mockMvc.perform(put("/api/search-reports/" + saved.getId())
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(updatePayload))
                .header("X-User-Id", "2")
                .header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locationSearched").value("Living Room"));
    }

    @Test
    void testAdvancedSearchWithMultipleParams() throws Exception {
        SearchReport report1 = SearchReport.builder()
                .lostItemId(testItem.getId())
                .reportedBy(2L)
                .searchDate(LocalDate.now())
                .locationSearched("Kitchen")
                .searchResult(SearchResult.FOUND)
                .status(ReportStatus.CLOSED)
                .build();
        SearchReport report2 = SearchReport.builder()
                .lostItemId(testItem.getId())
                .reportedBy(3L)
                .searchDate(LocalDate.now().minusDays(1))
                .locationSearched("Bedroom")
                .searchResult(SearchResult.NOT_FOUND)
                .status(ReportStatus.OPEN)
                .build();
        searchReportRepository.save(report1);
        searchReportRepository.save(report2);

        mockMvc.perform(get("/api/search-reports/search")
                .param("lostItemId", testItem.getId().toString())
                .param("reportedBy", "2")
                .param("searchResult", "FOUND")
                .param("status", "CLOSED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)));
    }

    @Test
    void testAdvancedSearchWithDateRange() throws Exception {
        SearchReport report = SearchReport.builder()
                .lostItemId(testItem.getId())
                .reportedBy(2L)
                .searchDate(LocalDate.now())
                .locationSearched("Kitchen")
                .searchResult(SearchResult.FOUND)
                .status(ReportStatus.CLOSED)
                .build();
        searchReportRepository.save(report);

        mockMvc.perform(get("/api/search-reports/search")
                .param("lostItemId", testItem.getId().toString())
                .param("from", LocalDate.now().minusDays(7).toString())
                .param("to", LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)));
    }

    @Test
    void testAdvancedSearchWithLocationKeyword() throws Exception {
        SearchReport report = SearchReport.builder()
                .lostItemId(testItem.getId())
                .reportedBy(2L)
                .searchDate(LocalDate.now())
                .locationSearched("Living Room Floor")
                .searchResult(SearchResult.NOT_FOUND)
                .status(ReportStatus.OPEN)
                .build();
        searchReportRepository.save(report);

        mockMvc.perform(get("/api/search-reports/search")
                .param("locationKeyword", "Floor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(java.util.List.class)));
    }
}
