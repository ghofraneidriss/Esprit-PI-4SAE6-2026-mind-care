package tn.esprit.ordonnance_et_medicaments.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité représentant une ordonnance médicale (Prescription).
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prescription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Références vers d'autres microservices
    private Long consultationId;
    private Long patientId;
    private Long doctorId;

    @Column(columnDefinition = "TEXT")
    private String doctorSignature; // Signature du médecin (base64 ou URL)

    private LocalDateTime createdAt; // dateCreation

    // "PENDING", "COMPLETED", "CANCELLED"
    private String status; // statut

    @Builder.Default
    @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, orphanRemoval = true)
    @com.fasterxml.jackson.annotation.JsonManagedReference
    private List<PrescriptionLine> prescriptionLines = new ArrayList<>(); // lignesOrdonnance

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
