package tn.esprit.lost_item_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "lost_item_alert")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LostItemAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private Long lostItemId;

    @NotNull
    private Long patientId;

    private Long caregiverId;

    @NotBlank
    @Size(min = 3, max = 150)
    private String title;

    @Column(columnDefinition = "TEXT")
    @Size(max = 1000)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    private AlertLevel level;

    @Enumerated(EnumType.STRING)
    private AlertStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime viewedAt;

    @PrePersist
    protected void prePersist() {
        createdAt = LocalDateTime.now();
        if (status == null) status = AlertStatus.NEW;
    }
}
