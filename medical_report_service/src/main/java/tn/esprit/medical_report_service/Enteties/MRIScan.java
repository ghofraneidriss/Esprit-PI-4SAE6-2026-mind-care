package tn.esprit.medical_report_service.Enteties;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class MRIScan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medical_report_id")
    @JsonIgnore
    private MedicalReport medicalReport;

    @NotNull(message = "Scan date is required")
    private LocalDateTime scanDate;

    @NotNull(message = "File ID is required")
    private Long fileId;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Quality status is required")
    private QualityStatus qualityStatus;

    private String notes;

    private LocalDateTime createdAt;

    @OneToOne(mappedBy = "mriScan", cascade = CascadeType.ALL)
    private AIResult aiResult;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
