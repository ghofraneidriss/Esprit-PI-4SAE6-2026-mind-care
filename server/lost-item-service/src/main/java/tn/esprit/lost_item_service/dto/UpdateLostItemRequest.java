package tn.esprit.lost_item_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import tn.esprit.lost_item_service.entity.ItemCategory;
import tn.esprit.lost_item_service.entity.ItemPriority;
import tn.esprit.lost_item_service.entity.ItemStatus;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateLostItemRequest {

    private Long id;

    private String title;

    private String description;

    private ItemCategory category;

    private Long patientId;

    private Long caregiverId;

    private String lastSeenLocation;

    private LocalDate lastSeenDate;

    private ItemStatus status;

    private ItemPriority priority;

    private String imageUrl;

    private LocalDate createdAt;
}
