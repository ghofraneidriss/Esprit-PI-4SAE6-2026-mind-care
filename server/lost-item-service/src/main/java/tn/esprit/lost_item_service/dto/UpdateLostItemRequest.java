package tn.esprit.lost_item_service.dto;

import lombok.*;
import tn.esprit.lost_item_service.Entity.ItemCategory;
import tn.esprit.lost_item_service.Entity.ItemPriority;
import tn.esprit.lost_item_service.Entity.ItemStatus;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateLostItemRequest {

    private String title;

    private String description;

    private ItemCategory category;

    private Long caregiverId;

    private String lastSeenLocation;

    private LocalDate lastSeenDate;

    private ItemStatus status;

    private ItemPriority priority;

    private String imageUrl;
}
