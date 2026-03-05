package tn.esprit.users_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Target user (admin or caregiver)
    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String message;

    // INFO, WARNING, CRITICAL
    @Column(nullable = false)
    private String type;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    // Optional: reference to the incident that triggered this
    private Long incidentId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
