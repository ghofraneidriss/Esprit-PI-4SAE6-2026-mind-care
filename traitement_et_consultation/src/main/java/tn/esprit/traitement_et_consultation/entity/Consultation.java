package tn.esprit.traitement_et_consultation.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entité représentant une consultation médicale.
 * Contrainte : un rendez-vous (appointmentId) ne peut être lié qu'à une seule
 * consultation.
 * La contrainte unique est assurée à la fois au niveau JPA (@Column unique) et
 * au niveau service.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Consultation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Identifiant du rendez-vous associé.
     * Contrainte UNIQUE : un rendez-vous ne peut avoir qu'une seule consultation.
     */
    @Column(unique = true)
    private Long appointmentId;

    @Column(columnDefinition = "TEXT")
    private String clinicalNotes;

    private Double currentWeight;
    private String bloodPressure;

    private Integer mmseScore;

    @Enumerated(EnumType.STRING)
    private AlzheimerStage alzheimerStage;
}
