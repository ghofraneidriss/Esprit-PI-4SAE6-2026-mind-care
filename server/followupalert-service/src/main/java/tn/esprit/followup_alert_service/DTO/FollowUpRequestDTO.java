package tn.esprit.followup_alert_service.DTO;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.followup_alert_service.Entity.IndependenceLevel;
import tn.esprit.followup_alert_service.Entity.MoodState;
import tn.esprit.followup_alert_service.Entity.SleepQuality;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FollowUpRequestDTO {

    @NotNull(message = "L'ID du patient est obligatoire")
    private Long patientId;

    @NotNull(message = "L'ID du caregiver est obligatoire")
    private Long caregiverId;

    @NotNull(message = "La date de suivi est obligatoire")
    private LocalDate followUpDate;

    @Min(value = 0, message = "Le score cognitif ne peut pas être négatif")
    @Max(value = 30, message = "Le score cognitif ne peut pas dépasser 30")
    private Integer cognitiveScore;

    @NotNull(message = "L'humeur est obligatoire")
    private MoodState mood;

    private Boolean agitationObserved;
    private Boolean confusionObserved;

    @NotNull(message = "Le niveau d'indépendance pour manger est obligatoire")
    private IndependenceLevel eating;

    @NotNull(message = "Le niveau d'indépendance pour s'habiller est obligatoire")
    private IndependenceLevel dressing;

    @NotNull(message = "Le niveau de mobilité est obligatoire")
    private IndependenceLevel mobility;

    @Min(value = 0, message = "Les heures de sommeil ne peuvent pas être négatives")
    @Max(value = 24, message = "Les heures de sommeil ne peuvent pas dépasser 24")
    private Integer hoursSlept;

    private SleepQuality sleepQuality;

    @Size(max = 2000, message = "Les notes ne doivent pas dépasser 2000 caractères")
    private String notes;

    @Size(max = 500, message = "Les signes vitaux ne doivent pas dépasser 500 caractères")
    private String vitalSigns;
}
