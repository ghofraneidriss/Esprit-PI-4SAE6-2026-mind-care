package tn.esprit.lost_item_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import tn.esprit.lost_item_service.Entity.AlertLevel;
import tn.esprit.lost_item_service.Entity.AlertStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateLostItemAlertRequest {

    @Size(min = 3, max = 255, message = "Title must be between 3 and 255 characters")
    private String title;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    private AlertLevel level;

    private AlertStatus status;
}
