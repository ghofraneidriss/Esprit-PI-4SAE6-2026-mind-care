package tn.esprit.lost_item_service.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.lost_item_service.Entity.AlertLevel;
import tn.esprit.lost_item_service.Entity.AlertStatus;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for LostItemAlert.
 * Contains only safe fields to expose in REST API responses.
 * Excludes internal sensitive data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LostItemAlertDTO {

    private Long id;
    private Long lostItemId;
    private Long patientId;
    private Long caregiverId;
    private String title;
    private String description;
    private AlertLevel level;
    private AlertStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime viewedAt;

}
