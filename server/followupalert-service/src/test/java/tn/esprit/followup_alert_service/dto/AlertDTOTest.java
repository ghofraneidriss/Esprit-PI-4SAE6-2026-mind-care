package tn.esprit.followup_alert_service.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import tn.esprit.followup_alert_service.Entity.Alert;
import tn.esprit.followup_alert_service.Entity.AlertLevel;
import tn.esprit.followup_alert_service.Entity.AlertStatus;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Alert DTO Tests")
class AlertDTOTest {

    @Test
    @DisplayName("AlertRequestDTO - Create and all getters")
    void testAlertRequestDTO() {
        AlertRequestDTO dto = new AlertRequestDTO();
        dto.setPatientId(100L);
        dto.setTitle("Test Alert");
        dto.setDescription("Test Description");
        dto.setLevel(AlertLevel.HIGH);

        assertThat(dto.getPatientId()).isEqualTo(100L);
        assertThat(dto.getTitle()).isEqualTo("Test Alert");
        assertThat(dto.getDescription()).isEqualTo("Test Description");
        assertThat(dto.getLevel()).isEqualTo(AlertLevel.HIGH);
    }

    @Test
    @DisplayName("AlertRequestDTO - With null values")
    void testAlertRequestDTOWithNull() {
        AlertRequestDTO dto = new AlertRequestDTO();
        dto.setPatientId(null);
        dto.setTitle(null);
        dto.setDescription(null);
        dto.setLevel(null);

        assertThat(dto.getPatientId()).isNull();
        assertThat(dto.getTitle()).isNull();
        assertThat(dto.getDescription()).isNull();
        assertThat(dto.getLevel()).isNull();
    }

    @Test
    @DisplayName("AlertRequestDTO - Various alert levels")
    void testAlertRequestDTOAllLevels() {
        for (AlertLevel level : AlertLevel.values()) {
            AlertRequestDTO dto = new AlertRequestDTO();
            dto.setLevel(level);
            assertThat(dto.getLevel()).isEqualTo(level);
        }
    }

    @Test
    @DisplayName("AlertResponseDTO - Create and all getters")
    void testAlertResponseDTO() {
        AlertResponseDTO dto = new AlertResponseDTO();
        dto.setId(1L);
        dto.setPatientId(100L);
        dto.setTitle("Test Alert");
        dto.setLevel(AlertLevel.HIGH);
        dto.setStatus(AlertStatus.NEW);
        LocalDateTime now = LocalDateTime.now();
        dto.setCreatedAt(now);
        dto.setViewedAt(now);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getPatientId()).isEqualTo(100L);
        assertThat(dto.getTitle()).isEqualTo("Test Alert");
        assertThat(dto.getLevel()).isEqualTo(AlertLevel.HIGH);
        assertThat(dto.getStatus()).isEqualTo(AlertStatus.NEW);
        assertThat(dto.getCreatedAt()).isEqualTo(now);
        assertThat(dto.getViewedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("AlertResponseDTO - With null timestamps")
    void testAlertResponseDTOWithNullTimestamps() {
        AlertResponseDTO dto = new AlertResponseDTO();
        dto.setId(1L);
        dto.setPatientId(100L);
        dto.setTitle("Test Alert");
        dto.setCreatedAt(null);
        dto.setViewedAt(null);

        assertThat(dto.getCreatedAt()).isNull();
        assertThat(dto.getViewedAt()).isNull();
    }

    @Test
    @DisplayName("AlertResponseDTO - Various statuses")
    void testAlertResponseDTOAllStatuses() {
        for (AlertStatus status : AlertStatus.values()) {
            AlertResponseDTO dto = new AlertResponseDTO();
            dto.setStatus(status);
            assertThat(dto.getStatus()).isEqualTo(status);
        }
    }

    @Test
    @DisplayName("AlertResponseDTO.fromEntity - Convert Alert to DTO")
    void testAlertResponseDTOFromEntity() {
        Alert alert = new Alert();
        alert.setId(1L);
        alert.setPatientId(100L);
        alert.setTitle("Test Alert");
        alert.setDescription("Description");
        alert.setLevel(AlertLevel.HIGH);
        alert.setStatus(AlertStatus.NEW);
        LocalDateTime now = LocalDateTime.now();
        alert.setCreatedAt(now);
        alert.setViewedAt(now);

        AlertResponseDTO dto = AlertResponseDTO.fromEntity(alert);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getPatientId()).isEqualTo(100L);
        assertThat(dto.getTitle()).isEqualTo("Test Alert");
        assertThat(dto.getDescription()).isEqualTo("Description");
        assertThat(dto.getLevel()).isEqualTo(AlertLevel.HIGH);
        assertThat(dto.getStatus()).isEqualTo(AlertStatus.NEW);
        assertThat(dto.getCreatedAt()).isEqualTo(now);
        assertThat(dto.getViewedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("AlertResponseDTO.fromEntity - With null fields")
    void testAlertResponseDTOFromEntityWithNulls() {
        Alert alert = new Alert();
        alert.setId(2L);
        alert.setPatientId(null);
        alert.setTitle(null);
        alert.setDescription(null);
        alert.setLevel(null);
        alert.setStatus(null);
        alert.setCreatedAt(null);
        alert.setViewedAt(null);

        AlertResponseDTO dto = AlertResponseDTO.fromEntity(alert);

        assertThat(dto.getId()).isEqualTo(2L);
        assertThat(dto.getPatientId()).isNull();
        assertThat(dto.getTitle()).isNull();
        assertThat(dto.getDescription()).isNull();
        assertThat(dto.getLevel()).isNull();
        assertThat(dto.getStatus()).isNull();
        assertThat(dto.getCreatedAt()).isNull();
        assertThat(dto.getViewedAt()).isNull();
    }

    @Test
    @DisplayName("AlertRequestDTO and AlertResponseDTO independence")
    void testDTOFieldIndependence() {
        AlertRequestDTO requestDTO = new AlertRequestDTO();
        requestDTO.setPatientId(50L);
        requestDTO.setTitle("Request Title");

        AlertResponseDTO responseDTO = new AlertResponseDTO();
        responseDTO.setPatientId(100L);
        responseDTO.setTitle("Response Title");

        assertThat(requestDTO.getPatientId()).isEqualTo(50L);
        assertThat(responseDTO.getPatientId()).isEqualTo(100L);
        assertThat(requestDTO.getTitle()).isEqualTo("Request Title");
        assertThat(responseDTO.getTitle()).isEqualTo("Response Title");
    }

    @Test
    @DisplayName("AlertRequestDTO - Multiple sequential updates")
    void testAlertRequestDTOSequentialUpdates() {
        AlertRequestDTO dto = new AlertRequestDTO();

        dto.setPatientId(1L);
        assertThat(dto.getPatientId()).isEqualTo(1L);

        dto.setPatientId(2L);
        assertThat(dto.getPatientId()).isEqualTo(2L);

        dto.setTitle("First Title");
        assertThat(dto.getTitle()).isEqualTo("First Title");

        dto.setTitle("Second Title");
        assertThat(dto.getTitle()).isEqualTo("Second Title");

        dto.setLevel(AlertLevel.LOW);
        assertThat(dto.getLevel()).isEqualTo(AlertLevel.LOW);

        dto.setLevel(AlertLevel.CRITICAL);
        assertThat(dto.getLevel()).isEqualTo(AlertLevel.CRITICAL);
    }

    @Test
    @DisplayName("AlertResponseDTO - Complete lifecycle")
    void testAlertResponseDTOCompleteLifecycle() {
        Alert alert1 = new Alert();
        alert1.setId(10L);
        alert1.setPatientId(5L);
        alert1.setTitle("Original");
        alert1.setDescription("Desc1");
        alert1.setLevel(AlertLevel.LOW);
        alert1.setStatus(AlertStatus.NEW);
        LocalDateTime time1 = LocalDateTime.now().minusDays(1);
        alert1.setCreatedAt(time1);
        alert1.setViewedAt(null);

        AlertResponseDTO dto1 = AlertResponseDTO.fromEntity(alert1);
        assertThat(dto1.getId()).isEqualTo(10L);
        assertThat(dto1.getStatus()).isEqualTo(AlertStatus.NEW);
        assertThat(dto1.getViewedAt()).isNull();

        Alert alert2 = new Alert();
        alert2.setId(20L);
        alert2.setPatientId(15L);
        alert2.setTitle("Updated");
        alert2.setDescription("Desc2");
        alert2.setLevel(AlertLevel.CRITICAL);
        alert2.setStatus(AlertStatus.RESOLVED);
        LocalDateTime time2 = LocalDateTime.now();
        alert2.setCreatedAt(time1);
        alert2.setViewedAt(time2);

        AlertResponseDTO dto2 = AlertResponseDTO.fromEntity(alert2);
        assertThat(dto2.getId()).isEqualTo(20L);
        assertThat(dto2.getStatus()).isEqualTo(AlertStatus.RESOLVED);
        assertThat(dto2.getViewedAt()).isNotNull();
        assertThat(dto2.getDescription()).isEqualTo("Desc2");
    }

    @Test
    @DisplayName("AlertRequestDTO - Large values")
    void testAlertRequestDTOWithLargeValues() {
        AlertRequestDTO dto = new AlertRequestDTO();
        dto.setPatientId(Long.MAX_VALUE);
        dto.setTitle("A".repeat(1000));
        dto.setDescription("B".repeat(5000));
        dto.setLevel(AlertLevel.CRITICAL);

        assertThat(dto.getPatientId()).isEqualTo(Long.MAX_VALUE);
        assertThat(dto.getTitle()).hasSize(1000);
        assertThat(dto.getDescription()).hasSize(5000);
        assertThat(dto.getLevel()).isEqualTo(AlertLevel.CRITICAL);
    }
}
