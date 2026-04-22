package tn.esprit.lost_item_service.Service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.lost_item_service.Entity.*;
import tn.esprit.lost_item_service.Repository.LostItemRepository;
import tn.esprit.lost_item_service.Repository.SearchReportRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchSuggestionService — Unit Tests")
class SearchSuggestionServiceTest {

    @Mock LostItemRepository lostItemRepository;
    @Mock SearchReportRepository searchReportRepository;

    @InjectMocks SearchSuggestionService service;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private LostItem makeItem(Long id, ItemCategory category) {
        return LostItem.builder()
                .id(id).title("Item " + id)
                .category(category).patientId(5L)
                .status(ItemStatus.LOST).priority(ItemPriority.MEDIUM)
                .createdAt(LocalDateTime.now().minusDays(3))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private SearchReport makeReport(Long itemId, String location, SearchResult result) {
        return SearchReport.builder()
                .id(itemId * 10)
                .lostItemId(itemId)
                .reportedBy(7L)
                .locationSearched(location)
                .searchResult(result)
                .searchDate(LocalDate.now())
                .status(ReportStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // ── No history ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getSuggestions — returns empty list when patient has no items")
    void getSuggestions_returnsEmpty_whenNoItems() {
        when(lostItemRepository.findByPatientIdOrderByCreatedAtDesc(5L)).thenReturn(List.of());

        List<Map<String, Object>> result = service.getSuggestions(5L, "CLOTHING");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getSuggestions — returns empty list when items exist but no search reports")
    void getSuggestions_returnsEmpty_whenNoSearchReports() {
        when(lostItemRepository.findByPatientIdOrderByCreatedAtDesc(5L))
                .thenReturn(List.of(makeItem(1L, ItemCategory.CLOTHING)));
        when(searchReportRepository.findByLostItemIdIn(anyList())).thenReturn(List.of());

        List<Map<String, Object>> result = service.getSuggestions(5L, "CLOTHING");

        assertThat(result).isEmpty();
    }

    // ── Ranking by success rate ───────────────────────────────────────────────

    @Test
    @DisplayName("getSuggestions — returns at most 3 suggestions")
    void getSuggestions_returnsAtMostThree() {
        LostItem item = makeItem(1L, ItemCategory.CLOTHING);
        when(lostItemRepository.findByPatientIdOrderByCreatedAtDesc(5L)).thenReturn(List.of(item));
        when(searchReportRepository.findByLostItemIdIn(anyList())).thenReturn(List.of(
                makeReport(1L, "Living room",  SearchResult.FOUND),
                makeReport(1L, "Bedroom",      SearchResult.NOT_FOUND),
                makeReport(1L, "Bathroom",     SearchResult.PARTIALLY_FOUND),
                makeReport(1L, "Kitchen",      SearchResult.NOT_FOUND),
                makeReport(1L, "Garden",       SearchResult.FOUND)
        ));

        List<Map<String, Object>> result = service.getSuggestions(5L, "CLOTHING");

        assertThat(result).hasSizeLessThanOrEqualTo(3);
    }

    @Test
    @DisplayName("getSuggestions — ranks FOUND location above NOT_FOUND location")
    void getSuggestions_ranksFoundLocationFirst() {
        LostItem item = makeItem(1L, ItemCategory.ACCESSORY);
        when(lostItemRepository.findByPatientIdOrderByCreatedAtDesc(5L)).thenReturn(List.of(item));
        when(searchReportRepository.findByLostItemIdIn(anyList())).thenReturn(List.of(
                makeReport(1L, "Living room", SearchResult.FOUND),
                makeReport(1L, "Bedroom",     SearchResult.NOT_FOUND),
                makeReport(1L, "Bedroom",     SearchResult.NOT_FOUND)
        ));

        List<Map<String, Object>> result = service.getSuggestions(5L, "ACCESSORY");

        assertThat(result).isNotEmpty();
        // Living room has 100% success → should be rank 1
        Map<String, Object> top = result.get(0);
        assertThat(top.get("location")).isEqualTo("Living room");
        assertThat((Double) top.get("confidenceScore")).isEqualTo(100.0);
    }

    @Test
    @DisplayName("getSuggestions — confidence score is 50 for PARTIALLY_FOUND location")
    void getSuggestions_confidenceScore50_forPartiallyFound() {
        LostItem item = makeItem(1L, ItemCategory.DOCUMENT);
        when(lostItemRepository.findByPatientIdOrderByCreatedAtDesc(5L)).thenReturn(List.of(item));
        when(searchReportRepository.findByLostItemIdIn(anyList())).thenReturn(List.of(
                makeReport(1L, "Office desk", SearchResult.PARTIALLY_FOUND)
        ));

        List<Map<String, Object>> result = service.getSuggestions(5L, "DOCUMENT");

        assertThat(result).hasSize(1);
        assertThat((Double) result.get(0).get("confidenceScore")).isEqualTo(50.0);
    }

    @Test
    @DisplayName("getSuggestions — confidence score is 0 for all NOT_FOUND searches")
    void getSuggestions_confidenceScoreIsZero_forAllNotFound() {
        LostItem item = makeItem(1L, ItemCategory.CLOTHING);
        when(lostItemRepository.findByPatientIdOrderByCreatedAtDesc(5L)).thenReturn(List.of(item));
        when(searchReportRepository.findByLostItemIdIn(anyList())).thenReturn(List.of(
                makeReport(1L, "Garage", SearchResult.NOT_FOUND),
                makeReport(1L, "Garage", SearchResult.NOT_FOUND)
        ));

        List<Map<String, Object>> result = service.getSuggestions(5L, "CLOTHING");

        assertThat(result).hasSize(1);
        assertThat((Double) result.get(0).get("confidenceScore")).isEqualTo(0.0);
    }

    // ── Category filter ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getSuggestions — filters items by category when category is provided")
    void getSuggestions_filtersItemsByCategory() {
        LostItem clothing = makeItem(1L, ItemCategory.CLOTHING);
        LostItem medication = makeItem(2L, ItemCategory.MEDICATION);

        // Patient has both clothing and medication items
        when(lostItemRepository.findByPatientIdOrderByCreatedAtDesc(5L))
                .thenReturn(List.of(clothing, medication));

        // Only clothing item (id=1) has reports at "Living room"
        when(searchReportRepository.findByLostItemIdIn(anyList())).thenReturn(List.of(
                makeReport(1L, "Living room", SearchResult.FOUND)
        ));

        // Request suggestions for CLOTHING only — medication item should be excluded
        List<Map<String, Object>> result = service.getSuggestions(5L, "CLOTHING");

        assertThat(result).isNotEmpty();
        assertThat(result.get(0).get("foundCount")).isEqualTo(1);
    }

    @Test
    @DisplayName("getSuggestions — uses all items when no category filter provided")
    void getSuggestions_usesAllItems_whenNoCategoryFilter() {
        LostItem clothing  = makeItem(1L, ItemCategory.CLOTHING);
        LostItem accessory = makeItem(2L, ItemCategory.ACCESSORY);

        when(lostItemRepository.findByPatientIdOrderByCreatedAtDesc(5L))
                .thenReturn(List.of(clothing, accessory));
        when(searchReportRepository.findByLostItemIdIn(anyList())).thenReturn(List.of(
                makeReport(1L, "Bedroom",   SearchResult.FOUND),
                makeReport(2L, "Bedroom",   SearchResult.FOUND)
        ));

        // No category → both items are used
        List<Map<String, Object>> result = service.getSuggestions(5L, null);

        assertThat(result).hasSize(1); // both map to "Bedroom"
        assertThat(result.get(0).get("foundCount")).isEqualTo(2);
    }

    // ── rank assignment ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getSuggestions — assigns sequential rank starting at 1")
    void getSuggestions_assignsSequentialRanks() {
        LostItem item = makeItem(1L, ItemCategory.ELECTRONIC);
        when(lostItemRepository.findByPatientIdOrderByCreatedAtDesc(5L)).thenReturn(List.of(item));
        when(searchReportRepository.findByLostItemIdIn(anyList())).thenReturn(List.of(
                makeReport(1L, "Living room", SearchResult.FOUND),
                makeReport(1L, "Kitchen",     SearchResult.PARTIALLY_FOUND),
                makeReport(1L, "Bathroom",    SearchResult.NOT_FOUND)
        ));

        List<Map<String, Object>> result = service.getSuggestions(5L, "ELECTRONIC");

        for (int i = 0; i < result.size(); i++) {
            assertThat(result.get(i).get("rank")).isEqualTo(i + 1);
        }
    }

    // ── tip generation ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getSuggestions — tip mentions 'time-critical' for MEDICATION high confidence")
    void getSuggestions_tipMentionsTimeCritical_forMedicationHighConfidence() {
        LostItem item = makeItem(1L, ItemCategory.MEDICATION);
        when(lostItemRepository.findByPatientIdOrderByCreatedAtDesc(5L)).thenReturn(List.of(item));
        when(searchReportRepository.findByLostItemIdIn(anyList())).thenReturn(List.of(
                makeReport(1L, "Bathroom", SearchResult.FOUND),
                makeReport(1L, "Bathroom", SearchResult.FOUND),
                makeReport(1L, "Bathroom", SearchResult.FOUND)
        ));

        List<Map<String, Object>> result = service.getSuggestions(5L, "MEDICATION");

        assertThat(result).isNotEmpty();
        String tip = (String) result.get(0).get("tip");
        assertThat(tip.toLowerCase()).containsAnyOf("time-critical", "immediate", "medication");
    }
}
