package tn.esprit.followup_alert_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.followup_alert_service.Entity.Alert;
import tn.esprit.followup_alert_service.Entity.AlertLevel;
import tn.esprit.followup_alert_service.Entity.AlertStatus;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponseDTO {

    private Long id;
    private Long patientId;
    private String title;
    private String description;
    private AlertLevel level;
    private AlertStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime viewedAt;

    public static AlertResponseDTO fromEntity(Alert alert) {
        AlertResponseDTO dto = new AlertResponseDTO();
        dto.setId(alert.getId());
        dto.setPatientId(alert.getPatientId());
        dto.setTitle(alert.getTitle());
        dto.setDescription(alert.getDescription());
        dto.setLevel(alert.getLevel());
        dto.setStatus(alert.getStatus());
        dto.setCreatedAt(alert.getCreatedAt());
        dto.setViewedAt(alert.getViewedAt());
        return dto;
    }
}
