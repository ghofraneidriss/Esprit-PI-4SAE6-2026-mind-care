package tn.esprit.followup_alert_service.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "follow_up")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FollowUp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "L'ID du patient est obligatoire")
    private Long patientId;

    @NotNull(message = "L'ID du caregiver est obligatoire")
    private Long caregiverId;

    @NotNull(message = "La date de suivi est obligatoire")
    private LocalDate followUpDate;

    // ========== Cognitif ==========
    @Min(value = 0, message = "Le score cognitif ne peut pas être négatif")
    @Max(value = 30, message = "Le score cognitif ne peut pas dépasser 30")
    private Integer cognitiveScore;

    // ========== Comportement ==========
    @NotNull(message = "L'humeur est obligatoire")
    @Enumerated(EnumType.STRING)
    private MoodState mood;

    private Boolean agitationObserved;
    private Boolean confusionObserved;

    // ========== Activités Quotidiennes (ADL) ==========
    @NotNull(message = "Le niveau d'indépendance pour manger est obligatoire")
    @Enumerated(EnumType.STRING)
    private IndependenceLevel eating;

    @NotNull(message = "Le niveau d'indépendance pour s'habiller est obligatoire")
    @Enumerated(EnumType.STRING)
    private IndependenceLevel dressing;

    @NotNull(message = "Le niveau de mobilité est obligatoire")
    @Enumerated(EnumType.STRING)
    private IndependenceLevel mobility;

    // ========== Sommeil ==========
    @Min(value = 0, message = "Les heures de sommeil ne peuvent pas être négatives")
    @Max(value = 24, message = "Les heures de sommeil ne peuvent pas dépasser 24")
    private Integer hoursSlept;

    @Enumerated(EnumType.STRING)
    private SleepQuality sleepQuality;

    // ========== Notes ==========
    @Column(columnDefinition = "TEXT")
    @Size(max = 2000, message = "Les notes ne doivent pas dépasser 2000 caractères")
    private String notes;

    @Column(columnDefinition = "TEXT")
    @Size(max = 500, message = "Les signes vitaux ne doivent pas dépasser 500 caractères")
    private String vitalSigns;

    // ========== Timestamps ==========
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}