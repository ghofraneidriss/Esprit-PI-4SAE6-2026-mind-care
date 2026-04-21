package tn.esprit.ordonnance_et_medicaments.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "medicine_id")
    private Medicine medicine; // médicament
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id")
    @com.fasterxml.jackson.annotation.JsonBackReference
    private Prescription prescription; // ordonnance

    private String dosage; // posologie
    private LocalDate startDate; // dateDebut
    private LocalDate endDate; // dateFin
}
