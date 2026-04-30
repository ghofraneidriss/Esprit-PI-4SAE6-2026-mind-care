package tn.esprit.lost_item_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.lost_item_service.entity.*;
import tn.esprit.lost_item_service.repository.LostItemAlertRepository;
import tn.esprit.lost_item_service.repository.LostItemRepository;
import tn.esprit.lost_item_service.repository.SearchReportRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LostItemService — Unit Tests")
class LostItemServiceTest {

    @Mock LostItemRepository lostItemRepository;
    @Mock SearchReportRepository searchReportRepository;
    @Mock LostItemAlertRepository itemAlertRepository;

    @InjectMocks LostItemService service;

    private LostItem medicationItem;
    private LostItem clothingItem;

    @BeforeEach
    void setUp() {
        medicationItem = LostItem.builder()
                .id(1L)
                .title("Blood pressure medication")
                .category(ItemCategory.MEDICATION)
                .patientId(5L)
                .status(ItemStatus.LOST)
                .priority(ItemPriority.CRITICAL)
                .createdAt(LocalDateTime.now().minusDays(2))
                .updatedAt(LocalDateTime.now())
                .build();

        clothingItem = LostItem.builder()
                .id(2L)
                .title("Blue jacket")
                .category(ItemCategory.CLOTHING)
                .patientId(5L)
                .status(ItemStatus.LOST)
                .priority(ItemPriority.MEDIUM)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ── createLostItem ────────────────────────────────────────────────────────

    @Test
    @DisplayName("createLostItem — saves item and generates CRITICAL alert for MEDICATION")
    void createLostItem_generatesCriticalAlert_whenMedicationLost() {
        when(lostItemRepository.save(any())).thenReturn(medicationItem);
        when(lostItemRepository.countByPatientIdAndStatus(5L, ItemStatus.LOST)).thenReturn(1L);
        when(itemAlertRepository.existsByLostItemIdAndTitleAndStatusNot(any(), any(), any())).thenReturn(false);

        LostItem result = service.createLostItem(medicationItem);

        assertThat(result.getTitle()).isEqualTo("Blood pressure medication");
        verify(lostItemRepository).save(medicationItem);
        // CRITICAL alert should be triggered for MEDICATION + LOST
        verify(itemAlertRepository, atLeastOnce()).save(argThat(alert ->
                alert.getLevel() == AlertLevel.CRITICAL
        ));
    }

    @Test
    @DisplayName("createLostItem — generates no alert when item is already FOUND")
    void createLostItem_generatesNoAlert_whenItemAlreadyFound() {
        LostItem foundItem = LostItem.builder()
                .id(3L).title("Found wallet").category(ItemCategory.ACCESSORY)
                .patientId(5L).status(ItemStatus.FOUND).priority(ItemPriority.LOW)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
        when(lostItemRepository.save(any())).thenReturn(foundItem);

        service.createLostItem(foundItem);

        verify(itemAlertRepository, never()).save(any());
    }

    // ── getLostItemById ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getLostItemById — returns item when found")
    void getLostItemById_returnsItem_whenExists() {
        when(lostItemRepository.findById(1L)).thenReturn(Optional.of(medicationItem));

        LostItem result = service.getLostItemById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getCategory()).isEqualTo(ItemCategory.MEDICATION);
    }

    @Test
    @DisplayName("getLostItemById — throws RuntimeException when item does not exist")
    void getLostItemById_throwsException_whenNotFound() {
        when(lostItemRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLostItemById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    // ── markAsFound ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("markAsFound — sets status to FOUND and closes open reports")
    void markAsFound_setsStatusFoundAndClosesReports() {
        SearchReport openReport = SearchReport.builder()
                .id(10L).lostItemId(1L).reportedBy(7L)
                .status(ReportStatus.OPEN)
                .searchDate(java.time.LocalDate.now())
                .build();

        when(lostItemRepository.findById(1L)).thenReturn(Optional.of(medicationItem));
        when(searchReportRepository.findByLostItemIdAndStatus(1L, ReportStatus.OPEN))
                .thenReturn(new java.util.ArrayList<>(List.of(openReport)));
        when(lostItemRepository.save(any())).thenReturn(medicationItem);
        // resolveItemAlerts calls addAll on the returned list — must be mutable
        when(itemAlertRepository.findByLostItemIdAndStatus(any(), any()))
                .thenReturn(new java.util.ArrayList<>());

        service.markAsFound(1L);

        assertThat(medicationItem.getStatus()).isEqualTo(ItemStatus.FOUND);
        assertThat(openReport.getStatus()).isEqualTo(ReportStatus.CLOSED);
        verify(searchReportRepository).saveAll(anyList());
    }

    // ── deleteLostItem ────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteLostItem — soft-deletes item and closes open search reports")
    void deleteLostItem_setsStatusClosedAndClosesReports() {
        SearchReport openReport = SearchReport.builder()
                .id(11L).lostItemId(2L).reportedBy(7L)
                .status(ReportStatus.OPEN)
                .searchDate(java.time.LocalDate.now())
                .build();

        when(lostItemRepository.findById(2L)).thenReturn(Optional.of(clothingItem));
        when(searchReportRepository.findByLostItemIdAndStatus(2L, ReportStatus.OPEN))
                .thenReturn(new java.util.ArrayList<>(List.of(openReport)));
        when(lostItemRepository.save(any())).thenReturn(clothingItem);
        when(itemAlertRepository.findByLostItemIdAndStatus(any(), any()))
                .thenReturn(new java.util.ArrayList<>());

        service.deleteLostItem(2L);

        assertThat(clothingItem.getStatus()).isEqualTo(ItemStatus.CLOSED);
        assertThat(openReport.getStatus()).isEqualTo(ReportStatus.CLOSED);
    }

    // ── calculatePatientItemRisk ──────────────────────────────────────────────

    @Test
    @DisplayName("calculatePatientItemRisk — returns CRITICAL with many active critical items and alerts")
    void calculatePatientItemRisk_returnsCritical_whenMedicationLostWithAlerts() {
        // 3 active items (24 pts) + 2 CRITICAL priority (30 pts) + MEDICATION (20 pts)
        // + 2 CRITICAL alerts (20 pts) = 94 pts → CRITICAL
        LostItem item2 = LostItem.builder().id(4L).title("ID card").category(ItemCategory.DOCUMENT)
                .patientId(5L).status(ItemStatus.LOST).priority(ItemPriority.CRITICAL)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        LostItem item3 = LostItem.builder().id(5L).title("Jacket").category(ItemCategory.CLOTHING)
                .patientId(5L).status(ItemStatus.SEARCHING).priority(ItemPriority.HIGH)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        LostItemAlert alert1 = LostItemAlert.builder().id(1L).patientId(5L).lostItemId(1L)
                .level(AlertLevel.CRITICAL).status(AlertStatus.NEW).title("Alert 1").build();
        LostItemAlert alert2 = LostItemAlert.builder().id(2L).patientId(5L).lostItemId(4L)
                .level(AlertLevel.CRITICAL).status(AlertStatus.NEW).title("Alert 2").build();

        when(lostItemRepository.findByPatientId(5L))
                .thenReturn(List.of(medicationItem, item2, item3));
        when(itemAlertRepository.findByPatientIdAndStatus(5L, AlertStatus.NEW))
                .thenReturn(new java.util.ArrayList<>(List.of(alert1, alert2)));
        when(itemAlertRepository.findByPatientIdAndStatus(5L, AlertStatus.VIEWED))
                .thenReturn(new java.util.ArrayList<>());
        when(lostItemRepository.findByPatientIdAndCreatedAtBetween(eq(5L), any(), any()))
                .thenReturn(List.of());

        Map<String, Object> result = service.calculatePatientItemRisk(5L);

        assertThat(result).containsEntry("riskLevel", "CRITICAL");
        assertThat((Boolean) result.get("hasMedicationLost")).isTrue();
        assertThat((Integer) result.get("riskScore")).isGreaterThanOrEqualTo(76);
    }

    @Test
    @DisplayName("calculatePatientItemRisk — returns LOW when patient has no lost items")
    void calculatePatientItemRisk_returnsLow_whenNoLostItems() {
        LostItem foundItem = LostItem.builder()
                .id(3L).title("Found keys").category(ItemCategory.ACCESSORY)
                .patientId(6L).status(ItemStatus.FOUND).priority(ItemPriority.LOW)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        when(lostItemRepository.findByPatientId(6L)).thenReturn(List.of(foundItem));
        when(itemAlertRepository.findByPatientIdAndStatus(6L, AlertStatus.NEW))
                .thenReturn(new java.util.ArrayList<>());
        when(itemAlertRepository.findByPatientIdAndStatus(6L, AlertStatus.VIEWED))
                .thenReturn(new java.util.ArrayList<>());
        when(lostItemRepository.findByPatientIdAndCreatedAtBetween(eq(6L), any(), any()))
                .thenReturn(List.of());

        Map<String, Object> result = service.calculatePatientItemRisk(6L);

        assertThat(result).containsEntry("riskLevel", "LOW");
        assertThat((Integer) result.get("riskScore")).isLessThanOrEqualTo(25);
    }

    @Test
    @DisplayName("calculatePatientItemRisk — recovery rate is 0 when no items found yet")
    void calculatePatientItemRisk_recoveryRateIsZero_whenNothingFound() {
        when(lostItemRepository.findByPatientId(5L)).thenReturn(List.of(medicationItem, clothingItem));
        when(itemAlertRepository.findByPatientIdAndStatus(5L, AlertStatus.NEW))
                .thenReturn(new java.util.ArrayList<>());
        when(itemAlertRepository.findByPatientIdAndStatus(5L, AlertStatus.VIEWED))
                .thenReturn(new java.util.ArrayList<>());
        when(lostItemRepository.findByPatientIdAndCreatedAtBetween(eq(5L), any(), any()))
                .thenReturn(List.of());

        Map<String, Object> result = service.calculatePatientItemRisk(5L);

        assertThat((Double) result.get("recoveryRate")).isEqualTo(0.0);
    }

    // ── detectFrequentLosing ──────────────────────────────────────────────────

    @Test
    @DisplayName("detectFrequentLosing — detects INCREASING trend when recent > previous > oldest")
    void detectFrequentLosing_detectsIncreasingTrend() {
        LostItem i1 = LostItem.builder().id(10L).title("A").category(ItemCategory.CLOTHING)
                .patientId(5L).status(ItemStatus.LOST).priority(ItemPriority.LOW)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        when(lostItemRepository.findByPatientIdAndCreatedAtBetween(eq(5L), any(), any()))
                .thenReturn(List.of(i1, i1, i1))   // recent: 3
                .thenReturn(List.of(i1, i1))          // previous: 2
                .thenReturn(List.of(i1));              // oldest: 1

        when(itemAlertRepository.findByPatientIdAndStatus(5L, AlertStatus.NEW)).thenReturn(List.of());

        Map<String, Object> result = service.detectFrequentLosing(5L);

        assertThat(result).containsEntry("trend", "INCREASING");
        assertThat((Boolean) result.get("isFrequentLoser")).isTrue();
    }

    @Test
    @DisplayName("detectFrequentLosing — returns STABLE when counts are equal")
    void detectFrequentLosing_returnsStable_whenCountsEqual() {
        LostItem item = LostItem.builder().id(10L).title("A").category(ItemCategory.CLOTHING)
                .patientId(5L).status(ItemStatus.LOST).priority(ItemPriority.LOW)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        when(lostItemRepository.findByPatientIdAndCreatedAtBetween(eq(5L), any(), any()))
                .thenReturn(List.of(item))   // recent: 1
                .thenReturn(List.of(item))   // previous: 1
                .thenReturn(List.of(item));  // oldest: 1

        Map<String, Object> result = service.detectFrequentLosing(5L);

        assertThat(result).containsEntry("trend", "STABLE");
        assertThat((Boolean) result.get("isFrequentLoser")).isFalse();
    }

    @Test
    @DisplayName("detectFrequentLosing — returns DECREASING when recent < previous")
    void detectFrequentLosing_returnsDecreasing_whenRecentLessThanPrevious() {
        LostItem item = LostItem.builder().id(10L).title("A").category(ItemCategory.CLOTHING)
                .patientId(5L).status(ItemStatus.LOST).priority(ItemPriority.LOW)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        when(lostItemRepository.findByPatientIdAndCreatedAtBetween(eq(5L), any(), any()))
                .thenReturn(List.of())                // recent: 0
                .thenReturn(List.of(item, item))      // previous: 2
                .thenReturn(List.of(item, item, item)); // oldest: 3

        Map<String, Object> result = service.detectFrequentLosing(5L);

        assertThat(result).containsEntry("trend", "DECREASING");
        assertThat((Boolean) result.get("isFrequentLoser")).isFalse();
    }
}
