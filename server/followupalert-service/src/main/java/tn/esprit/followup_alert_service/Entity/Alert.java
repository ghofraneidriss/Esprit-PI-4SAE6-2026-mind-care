package tn.esprit.followup_alert_service.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alert")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "L'ID du patient est obligatoire")
    private Long patientId;

    @NotBlank(message = "Le titre est obligatoire")
    @Size(min = 3, max = 100, message = "Le titre doit contenir entre 3 et 100 caractères")
    private String title;

    @Column(columnDefinition = "TEXT")
    @Size(max = 1000, message = "La description ne doit pas dépasser 1000 caractères")
    private String description;

    @NotNull(message = "Le niveau d'alerte est obligatoire")
    @Enumerated(EnumType.STRING)
    private AlertLevel level;

    @Enumerated(EnumType.STRING)
    private AlertStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime viewedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        status = AlertStatus.NEW;
    }
}