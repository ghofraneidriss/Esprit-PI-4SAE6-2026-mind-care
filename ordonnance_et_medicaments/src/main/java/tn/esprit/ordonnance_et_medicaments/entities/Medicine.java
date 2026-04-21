package tn.esprit.ordonnance_et_medicaments.entities;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entité représentant un médicament (Medicine).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Medicine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String commercialName; // Nom commercial
    private String inn;             // DCI (Dénomination Commune Internationale)
    private String therapeuticFamily; // Famille thérapeutique

    @Column(columnDefinition = "TEXT")
    private String contraindications; // Contre-indications
}
