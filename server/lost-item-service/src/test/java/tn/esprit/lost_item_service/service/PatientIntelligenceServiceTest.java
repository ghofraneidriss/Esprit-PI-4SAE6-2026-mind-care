package tn.esprit.lost_item_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import tn.esprit.lost_item_service.entity.*;
import tn.esprit.lost_item_service.repository.LostItemRepository;
import tn.esprit.lost_item_service.repository.SearchReportRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PatientIntelligenceService — Unit Tests")
class PatientIntelligenceServiceTest {

    @Mock LostItemRepository lostItemRepository;
    @Mock SearchReportRepository searchReportRepository;

    // Deep stubs allow mocking the ChatClient fluent chain:
    // chatClient.prompt().system(...).user(...).call().content()
    @Mock(answer = RETURNS_DEEP_STUBS)
    ChatClient.Builder chatClientBuilder;

    @Mock(answer = RETURNS_DEEP_STUBS)
    ChatClient chatClient;

    PatientIntelligenceService service;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        service = new PatientIntelligenceService(chatClientBuilder, lostItemRepository);
    }

    private LostItem makeLostItem(Long id, ItemCategory cat, ItemStatus status, ItemPriority priority, int daysAgo) {
        return LostItem.builder()
                .id(id)
                .title("Test item " + id)
                .category(cat)
                .patientId(5L)
                .status(status)
                .priority(priority)
                .createdAt(LocalDateTime.now().minusDays(daysAgo))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ── Statistics returned in the response map ────────────────────────────────

    @Test
    @DisplayName("analyzePatient — returns correct totalItemsLost and totalFound counts")
    void analyzePatient_returnsCorrectCounts() {
        List<LostItem> items = List.of(
                makeLostItem(1L, ItemCategory.MEDICATION, ItemStatus.LOST,   ItemPriority.CRITICAL, 5),
                makeLostItem(2L, ItemCategory.CLOTHING,   ItemStatus.FOUND,  ItemPriority.MEDIUM,   10),
                makeLostItem(3L, ItemCategory.CLOTHING,   ItemStatus.LOST,   ItemPriority.LOW,       3)
        );
        when(lostItemRepository.findByPatientIdOrderByCreatedAtDesc(5L)).thenReturn(items);
        when(lostItemRepository.findByPatientIdAndCreatedAtBetween(eq(5L), any(), any()))
                .thenReturn(List.of());
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn("CLINICAL ASSESSMENT: test analysis");

        Map<String, Object> result = service.analyzePatient(5L);

        assertThat(result)
                .containsEntry("totalItemsLost", 3L)
                .containsEntry("totalFound", 1L);
    }

    @Test
    @DisplayName("analyzePatient — recovery rate is 0 when no items found")
    void analyzePatient_recoveryRateIsZero_whenNothingFound() {
        List<LostItem> items = List.of(
                makeLostItem(1L, ItemCategory.CLOTHING, ItemStatus.LOST, ItemPriority.LOW, 2)
        );
        when(lostItemRepository.findByPatientIdOrderByCreatedAtDesc(5L)).thenReturn(items);
        when(lostItemRepository.findByPatientIdAndCreatedAtBetween(eq(5L), any(), any()))
                .thenReturn(List.of());
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn("CLINICAL ASSESSMENT: nothing found");

        Map<String, Object> result = service.analyzePatient(5L);

        assertThat((Double) result.get("recoveryRate")).isEqualTo(0.0);
    }

    @Test
    @DisplayName("analyzePatient — recovery rate is 100 when all items found")
    void analyzePatient_recoveryRateIs100_whenAllFound() {
        List<LostItem> items = List.of(
                makeLostItem(1L, ItemCategory.ACCESSORY, ItemStatus.FOUND, ItemPriority.LOW, 5),
                makeLostItem(2L, ItemCategory.ACCESSORY, ItemStatus.FOUND, ItemPriority.LOW, 8)
        );
        when(lostItemRepository.findByPatientIdOrderByCreatedAtDesc(5L)).thenReturn(items);
        when(lostItemRepository.findByPatientIdAndCreatedAtBetween(eq(5L), any(), any()))
                .thenReturn(List.of());
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn("CLINICAL ASSESSMENT: all found");

        Map<String, Object> result = service.analyzePatient(5L);

        assertThat((Double) result.get("recoveryRate")).isEqualTo(100.0);
    }

    // ── overallRiskLevel (computeRiskLevel tested indirectly) ─────────────────

    @Test
    @DisplayName("analyzePatient — overallRiskLevel is CRITICAL with 2+ unresolved critical + increasing trend")
    void analyzePatient_returnsCriticalRisk_whenHighRiskFactors() {
        LostItem critical1 = makeLostItem(1L, ItemCategory.MEDICATION, ItemStatus.LOST,      ItemPriority.CRITICAL, 5);
        LostItem critical2 = makeLostItem(2L, ItemCategory.DOCUMENT,   ItemStatus.SEARCHING, ItemPriority.CRITICAL, 3);

        when(lostItemRepository.findByPatientIdOrderByCreatedAtDesc(5L))
                .thenReturn(List.of(critical1, critical2));

        // The monthly trend loop fires 6 times FIRST, then recentCount, then previousCount
        when(lostItemRepository.findByPatientIdAndCreatedAtBetween(eq(5L), any(), any()))
                .thenReturn(List.of())                                       // monthly i=5
                .thenReturn(List.of())                                       // monthly i=4
                .thenReturn(List.of())                                       // monthly i=3
                .thenReturn(List.of())                                       // monthly i=2
                .thenReturn(List.of())                                       // monthly i=1
                .thenReturn(List.of())                                       // monthly i=0
                .thenReturn(List.of(critical1, critical1, critical1, critical1, critical1)) // recentCount = 5
                .thenReturn(List.of(critical1));                             // previousCount = 1

        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn("CLINICAL ASSESSMENT: high risk patient");

        Map<String, Object> result = service.analyzePatient(5L);

        // INCREASING(+2) + unresolvedCritical=2(+3) + recent>=5(+2) + recoveryRate<20(+2) = 9 → CRITICAL
        assertThat(result).containsEntry("overallRiskLevel", "CRITICAL");
    }

    @Test
    @DisplayName("analyzePatient — overallRiskLevel is LOW when patient has no items")
    void analyzePatient_returnsLowRisk_whenNoItems() {
        when(lostItemRepository.findByPatientIdOrderByCreatedAtDesc(5L)).thenReturn(List.of());
        when(lostItemRepository.findByPatientIdAndCreatedAtBetween(eq(5L), any(), any()))
                .thenReturn(List.of());
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn("CLINICAL ASSESSMENT: nothing to report");

        Map<String, Object> result = service.analyzePatient(5L);

        assertThat(result)
                .containsEntry("overallRiskLevel", "LOW")
                .containsEntry("trendDirection", "STABLE");
    }

    // ── AI error handling ─────────────────────────────────────────────────────

    @Test
    @DisplayName("analyzePatient — gracefully handles LLM failure, still returns stats")
    void analyzePatient_handlesLLMFailure_returnsStatsWithAiError() {
        when(lostItemRepository.findByPatientIdOrderByCreatedAtDesc(5L)).thenReturn(List.of());
        when(lostItemRepository.findByPatientIdAndCreatedAtBetween(eq(5L), any(), any()))
                .thenReturn(List.of());
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenThrow(new RuntimeException("Connection refused"));

        Map<String, Object> result = service.analyzePatient(5L);

        assertThat(result)
                .containsKey("aiError")
                .containsKeys("totalItemsLost", "overallRiskLevel", "monthlyTrend");
        assertThat(result.get("aiAnalysis")).isNull();
    }

    // ── Trend direction ───────────────────────────────────────────────────────

    @Test
    @DisplayName("analyzePatient — trendDirection is INCREASING when recent > previous + 1")
    void analyzePatient_trendIsIncreasing_whenRecentExceedsPrevious() {
        LostItem item = makeLostItem(1L, ItemCategory.CLOTHING, ItemStatus.LOST, ItemPriority.LOW, 1);
        when(lostItemRepository.findByPatientIdOrderByCreatedAtDesc(5L)).thenReturn(List.of(item));

        // Monthly trend loop (6 calls) comes BEFORE recentCount and previousCount
        when(lostItemRepository.findByPatientIdAndCreatedAtBetween(eq(5L), any(), any()))
                .thenReturn(List.of())                                  // monthly i=5
                .thenReturn(List.of())                                  // monthly i=4
                .thenReturn(List.of())                                  // monthly i=3
                .thenReturn(List.of())                                  // monthly i=2
                .thenReturn(List.of())                                  // monthly i=1
                .thenReturn(List.of())                                  // monthly i=0
                .thenReturn(List.of(item, item, item, item, item))      // recentCount = 5
                .thenReturn(List.of(item));                             // previousCount = 1

        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn("CLINICAL ASSESSMENT: increasing");

        Map<String, Object> result = service.analyzePatient(5L);

        assertThat(result)
                .containsEntry("trendDirection", "INCREASING")
                .containsEntry("recentMonthCount", 5L)
                .containsEntry("previousMonthCount", 1L);
    }
}
