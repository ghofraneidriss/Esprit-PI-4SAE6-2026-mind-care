package tn.esprit.users_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private Long patientId;

    private Long caregiverId;

    // VISIT, ACTIVITY, MEDICAL, OTHER
    @Column(nullable = false)
    private String type;

    // Color for calendar display (Bootstrap color: primary, success, warning, danger, info)
    @Builder.Default
    private String color = "primary";

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
