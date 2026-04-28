package tn.esprit.lost_item_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

    @NotBlank(message = "Title cannot be blank")
    @Size(min = 3, max = 255, message = "Title must be between 3 and 255 characters")
    private String title;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;

    private ItemCategory category;

    private Long caregiverId;

    private String lastSeenLocation;

    private LocalDate lastSeenDate;

    private ItemStatus status;

    private ItemPriority priority;

    private String imageUrl;
}
