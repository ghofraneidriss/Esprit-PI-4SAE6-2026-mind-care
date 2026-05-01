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
    @DisplayName("AlertRequestDTO - Create and getters")
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
    @DisplayName("AlertResponseDTO - Create and getters")
    void testAlertResponseDTO() {
        AlertResponseDTO dto = new AlertResponseDTO();
        dto.setId(1L);
        dto.setPatientId(100L);
        dto.setTitle("Test Alert");
        dto.setLevel(AlertLevel.HIGH);
        dto.setStatus(AlertStatus.NEW);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getPatientId()).isEqualTo(100L);
        assertThat(dto.getTitle()).isEqualTo("Test Alert");
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
        alert.setCreatedAt(LocalDateTime.now());
        alert.setViewedAt(LocalDateTime.now());

        AlertResponseDTO dto = AlertResponseDTO.fromEntity(alert);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getPatientId()).isEqualTo(100L);
        assertThat(dto.getTitle()).isEqualTo("Test Alert");
        assertThat(dto.getLevel()).isEqualTo(AlertLevel.HIGH);
        assertThat(dto.getStatus()).isEqualTo(AlertStatus.NEW);
    }
}
