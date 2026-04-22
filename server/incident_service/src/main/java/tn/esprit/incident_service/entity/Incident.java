package tn.esprit.incident_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import tn.esprit.incident_service.enums.IncidentStatus;
import tn.esprit.incident_service.enums.SeverityLevel;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "UPDATE incident SET deleted = true WHERE id = ?")
// Pour Spring Boot 3+ / Hibernate 6+, @Where est déprécié, on utilise @SQLRestriction ou on filtre manuellement.
// Je vais utiliser le filtrage manuel dans le Repository pour garder le contrôle sur l'historique complet.
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "incident_type_id", nullable = false)
    @JsonIgnoreProperties("incidents")
    private IncidentType type;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private SeverityLevel severityLevel;

    @Enumerated(EnumType.STRING)
    private IncidentStatus status;

    @CreatedDate
    private LocalDateTime incidentDate;

    // IDs pour référence aux autres microservices (User/Patient)
    private Long patientId;
    private Long caregiverId;
    /** When the reporter is a volunteer (distinct from caregiver). */
    private Long volunteerId;

    // Soft delete flag
    @Column(name = "deleted")
    private boolean deleted = false;

    private String source;

    /** Utilisateur ayant saisi l’incident (ex. userId du médecin si source = DOCTOR). */
    private Long reporterUserId;

    // Score calculé automatiquement à la création (basé sur type.points + récurrence)
    @Column(name = "computed_score")
    private Integer computedScore;

    @PrePersist
    public void prePersist() {
        if (status == null) status = IncidentStatus.OPEN;
        if (deleted) deleted = false;
    }
}
