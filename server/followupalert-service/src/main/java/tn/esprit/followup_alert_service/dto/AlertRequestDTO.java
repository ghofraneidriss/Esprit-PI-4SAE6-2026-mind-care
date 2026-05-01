package tn.esprit.followup_alert_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.followup_alert_service.Entity.AlertLevel;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertRequestDTO {

    @NotNull(message = "L'ID du patient est obligatoire")
    private Long patientId;

    @NotBlank(message = "Le titre est obligatoire")
    @Size(min = 3, max = 100, message = "Le titre doit contenir entre 3 et 100 caractères")
    private String title;

    @Size(max = 1000, message = "La description ne doit pas dépasser 1000 caractères")
    private String description;

    @NotNull(message = "Le niveau d'alerte est obligatoire")
    private AlertLevel level;
}
