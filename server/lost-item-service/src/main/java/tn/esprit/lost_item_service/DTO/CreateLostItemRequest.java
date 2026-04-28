package tn.esprit.lost_item_service.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.lost_item_service.Entity.ItemCategory;
import tn.esprit.lost_item_service.Entity.ItemPriority;

import java.time.LocalDate;

/**
 * Request DTO for creating a lost item.
 * Safe input validation object for REST API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLostItemRequest {

    @NotBlank
    @Size(min = 2, max = 100)
    private String title;

    @Size(max = 500)
    private String description;

    @NotNull
    private ItemCategory category;

    @NotNull
    private Long patientId;

    private Long caregiverId;
    private String lastSeenLocation;
    private LocalDate lastSeenDate;
    private ItemPriority priority;
    private String imageUrl;

}
