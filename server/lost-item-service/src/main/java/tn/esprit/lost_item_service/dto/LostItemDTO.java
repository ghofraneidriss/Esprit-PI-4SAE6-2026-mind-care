package tn.esprit.lost_item_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.lost_item_service.Entity.ItemCategory;
import tn.esprit.lost_item_service.Entity.ItemPriority;
import tn.esprit.lost_item_service.Entity.ItemStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for LostItem.
 * Contains only safe fields to expose in REST API responses.
 * Excludes internal timestamps and sensitive data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LostItemDTO {

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
    private LocalDateTime createdAt;

}
