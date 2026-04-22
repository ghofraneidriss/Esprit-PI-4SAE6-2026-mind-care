package tn.esprit.recommendation_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import tn.esprit.recommendation_service.enums.ParticipantType;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "medical_event_participations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_event_participant_day",
                        columnNames = {"medical_event_id", "participant_id", "participant_type", "participation_date"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalEventParticipation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medical_event_id", nullable = false)
    private MedicalEvent medicalEvent;

    @Column(name = "participant_id", nullable = false)
    private Long participantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "participant_type", nullable = false, length = 30)
    private ParticipantType participantType;

    @Column(name = "participation_date", nullable = false)
    private LocalDate participationDate;

    @Column(nullable = false)
    @Builder.Default
    private Integer score = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
