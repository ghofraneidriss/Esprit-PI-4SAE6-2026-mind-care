package tn.esprit.lost_item_service.dto;

import lombok.*;
import tn.esprit.lost_item_service.entity.AlertLevel;
import tn.esprit.lost_item_service.entity.AlertStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateLostItemAlertRequest {

    private String title;

    private String description;

    private AlertLevel level;

    private AlertStatus status;
}
