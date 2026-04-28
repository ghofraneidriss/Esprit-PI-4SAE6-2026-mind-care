package tn.esprit.lost_item_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.lost_item_service.Entity.AlertLevel;

/**
 * Request DTO for creating a lost item alert.
 * Safe input validation object for REST API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLostItemAlertRequest {

    @NotNull
    private Long lostItemId;

    @NotNull
    private Long patientId;

    private Long caregiverId;

    @NotBlank
    @Size(min = 3, max = 150)
    private String title;

    @Size(max = 1000)
    private String description;

    @NotNull
    private AlertLevel level;

}
