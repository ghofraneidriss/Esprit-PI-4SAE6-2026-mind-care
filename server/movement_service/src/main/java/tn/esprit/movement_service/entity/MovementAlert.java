package tn.esprit.movement_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "movement_alert",
        indexes = {
                @Index(name = "idx_alert_patient_time", columnList = "patientId,createdAt"),
                @Index(name = "idx_alert_patient_type_ack", columnList = "patientId,alertType,acknowledged")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovementAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long patientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    private AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertSeverity severity;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false)
    private boolean acknowledged;

    @Column(nullable = false)
    private boolean emailSent;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime acknowledgedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
