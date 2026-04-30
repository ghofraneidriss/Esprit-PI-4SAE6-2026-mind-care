package tn.esprit.lost_item_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.lost_item_service.entity.*;
import tn.esprit.lost_item_service.exception.DuplicateReportException;
import tn.esprit.lost_item_service.repository.LostItemRepository;
import tn.esprit.lost_item_service.repository.SearchReportRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchReportService — Unit Tests")
class SearchReportServiceTest {

    @Mock SearchReportRepository searchReportRepository;
    @Mock LostItemRepository lostItemRepository;

    @InjectMocks SearchReportService service;

    private SearchReport report;

    @BeforeEach
    void setUp() {
        report = SearchReport.builder()
                .id(1L)
                .lostItemId(10L)
                .reportedBy(7L)
                .searchDate(LocalDate.of(2026, 4, 15))
                .locationSearched("Living room")
                .searchResult(SearchResult.NOT_FOUND)
                .status(ReportStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ── createSearchReport ────────────────────────────────────────────────────

    @Test
    @DisplayName("createSearchReport — saves and returns report when no duplicate exists")
    void createSearchReport_savesReport_whenNoDuplicate() {
        when(searchReportRepository.existsByLostItemIdAndSearchDate(10L, report.getSearchDate()))
                .thenReturn(false);
        when(searchReportRepository.save(report)).thenReturn(report);

        SearchReport result = service.createSearchReport(report);

        assertThat(result.getLostItemId()).isEqualTo(10L);
        assertThat(result.getLocationSearched()).isEqualTo("Living room");
        verify(searchReportRepository).save(report);
    }

    @Test
    @DisplayName("createSearchReport — throws DuplicateReportException when same item+date already exists")
    void createSearchReport_throwsDuplicateException_whenDuplicateExists() {
        when(searchReportRepository.existsByLostItemIdAndSearchDate(10L, report.getSearchDate()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.createSearchReport(report))
                .isInstanceOf(DuplicateReportException.class)
                .hasMessageContaining("10")
                .hasMessageContaining("one report per item per day");

        verify(searchReportRepository, never()).save(any());
    }

    // ── getSearchReportById ───────────────────────────────────────────────────

    @Test
    @DisplayName("getSearchReportById — returns report when found")
    void getSearchReportById_returnsReport_whenExists() {
        when(searchReportRepository.findById(1L)).thenReturn(Optional.of(report));

        SearchReport result = service.getSearchReportById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getLocationSearched()).isEqualTo("Living room");
    }

    @Test
    @DisplayName("getSearchReportById — throws RuntimeException when not found")
    void getSearchReportById_throwsException_whenNotFound() {
        when(searchReportRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSearchReportById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    // ── updateSearchReport ────────────────────────────────────────────────────

    @Test
    @DisplayName("updateSearchReport — updates fields and saves")
    void updateSearchReport_updatesFieldsAndSaves() {
        SearchReport updated = SearchReport.builder()
                .lostItemId(10L).reportedBy(7L)
                .searchDate(LocalDate.of(2026, 4, 15))
                .locationSearched("Bedroom")
                .searchResult(SearchResult.PARTIALLY_FOUND)
                .status(ReportStatus.OPEN)
                .build();

        when(searchReportRepository.findById(1L)).thenReturn(Optional.of(report));
        when(searchReportRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SearchReport result = service.updateSearchReport(1L, updated);

        assertThat(result.getLocationSearched()).isEqualTo("Bedroom");
        assertThat(result.getSearchResult()).isEqualTo(SearchResult.PARTIALLY_FOUND);
        verify(searchReportRepository).save(report);
    }

    // ── deleteSearchReport ────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteSearchReport — deletes when report exists")
    void deleteSearchReport_deletesReport_whenExists() {
        when(searchReportRepository.existsById(1L)).thenReturn(true);

        service.deleteSearchReport(1L);

        verify(searchReportRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteSearchReport — throws RuntimeException when report does not exist")
    void deleteSearchReport_throwsException_whenNotFound() {
        when(searchReportRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteSearchReport(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");

        verify(searchReportRepository, never()).deleteById(any());
    }

    // ── advancedSearch ────────────────────────────────────────────────────────

    @Test
    @DisplayName("advancedSearch — filters by searchResult in memory")
    void advancedSearch_filtersBySearchResult() {
        SearchReport notFound = SearchReport.builder().id(1L).lostItemId(10L).reportedBy(7L)
                .searchDate(LocalDate.now()).locationSearched("Kitchen")
                .searchResult(SearchResult.NOT_FOUND).status(ReportStatus.OPEN)
                .createdAt(LocalDateTime.now()).build();
        SearchReport found = SearchReport.builder().id(2L).lostItemId(10L).reportedBy(7L)
                .searchDate(LocalDate.now()).locationSearched("Bedroom")
                .searchResult(SearchResult.FOUND).status(ReportStatus.CLOSED)
                .createdAt(LocalDateTime.now()).build();

        when(searchReportRepository.findByLostItemIdOrderBySearchDateDesc(10L))
                .thenReturn(List.of(notFound, found));

        List<SearchReport> result = service.advancedSearch(
                10L, null, SearchResult.FOUND, null, null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSearchResult()).isEqualTo(SearchResult.FOUND);
    }

    @Test
    @DisplayName("advancedSearch — filters by locationKeyword case-insensitively")
    void advancedSearch_filtersByLocationKeyword() {
        SearchReport r1 = SearchReport.builder().id(1L).lostItemId(10L).reportedBy(7L)
                .searchDate(LocalDate.now()).locationSearched("Living Room")
                .searchResult(SearchResult.NOT_FOUND).status(ReportStatus.OPEN)
                .createdAt(LocalDateTime.now()).build();
        SearchReport r2 = SearchReport.builder().id(2L).lostItemId(10L).reportedBy(7L)
                .searchDate(LocalDate.now()).locationSearched("Bathroom")
                .searchResult(SearchResult.NOT_FOUND).status(ReportStatus.OPEN)
                .createdAt(LocalDateTime.now()).build();

        when(searchReportRepository.findByLostItemIdOrderBySearchDateDesc(10L))
                .thenReturn(List.of(r1, r2));

        List<SearchReport> result = service.advancedSearch(
                10L, null, null, null, "living", null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLocationSearched()).isEqualTo("Living Room");
    }

    @Test
    @DisplayName("advancedSearch — returns all reports when no filters provided")
    void advancedSearch_returnsAll_whenNoFilters() {
        SearchReport r1 = SearchReport.builder().id(1L).lostItemId(10L).reportedBy(7L)
                .searchDate(LocalDate.now()).searchResult(SearchResult.NOT_FOUND)
                .status(ReportStatus.OPEN).createdAt(LocalDateTime.now()).build();
        SearchReport r2 = SearchReport.builder().id(2L).lostItemId(11L).reportedBy(8L)
                .searchDate(LocalDate.now()).searchResult(SearchResult.FOUND)
                .status(ReportStatus.CLOSED).createdAt(LocalDateTime.now()).build();

        when(searchReportRepository.findAll()).thenReturn(new java.util.ArrayList<>(List.of(r1, r2)));

        List<SearchReport> result = service.advancedSearch(
                null, null, null, null, null, null, null);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("advancedSearch — filters by status in memory")
    void advancedSearch_filtersByStatus() {
        SearchReport open   = SearchReport.builder().id(1L).lostItemId(10L).reportedBy(7L)
                .searchDate(LocalDate.now()).searchResult(SearchResult.NOT_FOUND)
                .status(ReportStatus.OPEN).createdAt(LocalDateTime.now()).build();
        SearchReport closed = SearchReport.builder().id(2L).lostItemId(10L).reportedBy(7L)
                .searchDate(LocalDate.now()).searchResult(SearchResult.FOUND)
                .status(ReportStatus.CLOSED).createdAt(LocalDateTime.now()).build();

        when(searchReportRepository.findByLostItemIdOrderBySearchDateDesc(10L))
                .thenReturn(List.of(open, closed));

        List<SearchReport> result = service.advancedSearch(
                10L, null, null, ReportStatus.OPEN, null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(ReportStatus.OPEN);
    }

    // ── getSearchTimeline ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getSearchTimeline — computes correct success rate")
    void getSearchTimeline_computesCorrectSuccessRate() {
        SearchReport r1 = SearchReport.builder().id(1L).lostItemId(10L).reportedBy(7L)
                .searchDate(LocalDate.now()).locationSearched("Bedroom")
                .searchResult(SearchResult.FOUND).status(ReportStatus.CLOSED)
                .createdAt(LocalDateTime.now()).build();
        SearchReport r2 = SearchReport.builder().id(2L).lostItemId(10L).reportedBy(7L)
                .searchDate(LocalDate.now().minusDays(1)).locationSearched("Kitchen")
                .searchResult(SearchResult.NOT_FOUND).status(ReportStatus.OPEN)
                .createdAt(LocalDateTime.now()).build();
        SearchReport r3 = SearchReport.builder().id(3L).lostItemId(10L).reportedBy(7L)
                .searchDate(LocalDate.now().minusDays(2)).locationSearched("Bathroom")
                .searchResult(SearchResult.PARTIALLY_FOUND).status(ReportStatus.OPEN)
                .createdAt(LocalDateTime.now()).build();

        when(searchReportRepository.findByLostItemIdOrderBySearchDateDesc(10L))
                .thenReturn(List.of(r1, r2, r3));

        Map<String, Object> result = service.getSearchTimeline(10L);

        // (1 found + 1 partial) / 3 total = 66.7%
        assertThat(result)
                .containsEntry("totalSearches", 3L)
                .containsEntry("foundCount", 1L)
                .containsEntry("partiallyFoundCount", 1L);
        assertThat((Double) result.get("successRate")).isGreaterThan(60.0);
    }

    @Test
    @DisplayName("getSearchTimeline — success rate is 0 when all reports are NOT_FOUND")
    void getSearchTimeline_successRateIsZero_whenAllNotFound() {
        SearchReport r1 = SearchReport.builder().id(1L).lostItemId(10L).reportedBy(7L)
                .searchDate(LocalDate.now()).locationSearched("Garage")
                .searchResult(SearchResult.NOT_FOUND).status(ReportStatus.OPEN)
                .createdAt(LocalDateTime.now()).build();
        SearchReport r2 = SearchReport.builder().id(2L).lostItemId(10L).reportedBy(7L)
                .searchDate(LocalDate.now().minusDays(1)).locationSearched("Garden")
                .searchResult(SearchResult.NOT_FOUND).status(ReportStatus.OPEN)
                .createdAt(LocalDateTime.now()).build();

        when(searchReportRepository.findByLostItemIdOrderBySearchDateDesc(10L))
                .thenReturn(List.of(r1, r2));

        Map<String, Object> result = service.getSearchTimeline(10L);

        assertThat(result)
                .containsEntry("successRate", 0.0)
                .containsEntry("notFoundCount", 2L);
    }

    @Test
    @DisplayName("getSearchTimeline — returns empty timeline when no reports exist")
    void getSearchTimeline_returnsEmptyTimeline_whenNoReports() {
        when(searchReportRepository.findByLostItemIdOrderBySearchDateDesc(10L))
                .thenReturn(List.of());

        Map<String, Object> result = service.getSearchTimeline(10L);

        assertThat(result.get("totalSearches")).isEqualTo(0L);
        assertThat(result.get("successRate")).isEqualTo(0.0);
        assertThat((List<?>) result.get("timeline")).isEmpty();
    }

    @Test
    @DisplayName("getSearchTimeline — locationFrequency counts correctly")
    void getSearchTimeline_countsLocationFrequencyCorrectly() {
        SearchReport r1 = SearchReport.builder().id(1L).lostItemId(10L).reportedBy(7L)
                .searchDate(LocalDate.now()).locationSearched("Bedroom")
                .searchResult(SearchResult.NOT_FOUND).status(ReportStatus.OPEN)
                .createdAt(LocalDateTime.now()).build();
        SearchReport r2 = SearchReport.builder().id(2L).lostItemId(10L).reportedBy(7L)
                .searchDate(LocalDate.now().minusDays(1)).locationSearched("Bedroom")
                .searchResult(SearchResult.FOUND).status(ReportStatus.CLOSED)
                .createdAt(LocalDateTime.now()).build();
        SearchReport r3 = SearchReport.builder().id(3L).lostItemId(10L).reportedBy(7L)
                .searchDate(LocalDate.now().minusDays(2)).locationSearched("Kitchen")
                .searchResult(SearchResult.NOT_FOUND).status(ReportStatus.OPEN)
                .createdAt(LocalDateTime.now()).build();

        when(searchReportRepository.findByLostItemIdOrderBySearchDateDesc(10L))
                .thenReturn(List.of(r1, r2, r3));

        Map<String, Object> result = service.getSearchTimeline(10L);

        @SuppressWarnings("unchecked")
        Map<String, Long> freq = (Map<String, Long>) result.get("locationFrequency");
        assertThat(freq)
                .containsEntry("Bedroom", 2L)
                .containsEntry("Kitchen", 1L);
    }

    // ── getReportsByPatient ───────────────────────────────────────────────────

    @Test
    @DisplayName("getReportsByPatient — returns empty list when patient has no items")
    void getReportsByPatient_returnsEmpty_whenNoItems() {
        when(lostItemRepository.findByPatientId(5L)).thenReturn(List.of());

        List<SearchReport> result = service.getReportsByPatient(5L);

        assertThat(result).isEmpty();
        verify(searchReportRepository, never()).findAll();
    }

    @Test
    @DisplayName("getReportsByPatient — returns only reports belonging to patient items")
    void getReportsByPatient_returnsOnlyPatientReports() {
        LostItem item = LostItem.builder().id(10L).title("Wallet").category(ItemCategory.ACCESSORY)
                .patientId(5L).status(ItemStatus.LOST).priority(ItemPriority.MEDIUM)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        SearchReport patientReport = SearchReport.builder().id(1L).lostItemId(10L).reportedBy(7L)
                .searchDate(LocalDate.now()).searchResult(SearchResult.NOT_FOUND)
                .status(ReportStatus.OPEN).createdAt(LocalDateTime.now()).build();
        SearchReport otherReport = SearchReport.builder().id(2L).lostItemId(99L).reportedBy(7L)
                .searchDate(LocalDate.now()).searchResult(SearchResult.NOT_FOUND)
                .status(ReportStatus.OPEN).createdAt(LocalDateTime.now()).build();

        when(lostItemRepository.findByPatientId(5L)).thenReturn(List.of(item));
        when(searchReportRepository.findAll()).thenReturn(List.of(patientReport, otherReport));

        List<SearchReport> result = service.getReportsByPatient(5L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLostItemId()).isEqualTo(10L);
    }
}
