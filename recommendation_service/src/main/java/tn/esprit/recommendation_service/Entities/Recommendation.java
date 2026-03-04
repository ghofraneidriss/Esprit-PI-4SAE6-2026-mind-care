package tn.esprit.recommendation_service.Entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecommendationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecommendationStatus status = RecommendationStatus.PENDING;

    // --- New Fields for Microservices Architecture ---
    @Column(nullable = false)
    private Long doctorId;

    @Column(nullable = false)
    private Long patientId;

    private String doctorName;
    private String patientName;

    // --- New Relationship for Medical Games Scenario ---
    @ManyToMany
    @JoinTable(name = "recommendation_events", joinColumns = @JoinColumn(name = "recommendation_id"), inverseJoinColumns = @JoinColumn(name = "event_id"))
    private List<MedicalEvent> medicalEvents;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
