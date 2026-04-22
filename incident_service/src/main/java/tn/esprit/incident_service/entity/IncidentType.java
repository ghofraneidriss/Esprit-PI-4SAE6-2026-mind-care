package tn.esprit.incident_service.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import tn.esprit.incident_service.enums.SeverityLevel;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE incident_type SET deleted = true WHERE id = ?")
// Relation OneToMany vers Incident
public class IncidentType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    private String description;

    // --- SCORING ---
    @Enumerated(EnumType.STRING)
    @Column(name = "default_severity")
    private SeverityLevel defaultSeverity = SeverityLevel.MEDIUM;

    @Column(name = "points")
    private Integer points = 10;

    @OneToMany(mappedBy = "type", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore // Important pour éviter boucle infinie en JSON
    private List<Incident> incidents;

    // Pour l'incident type aussi, on met le soft delete pour cohérence
    @Column(name = "deleted")
    private boolean deleted = false;
}
