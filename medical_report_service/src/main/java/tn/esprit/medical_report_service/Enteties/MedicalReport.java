package tn.esprit.medical_report_service.Enteties;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonManagedReference;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class MedicalReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportid;

    @NotNull(message = "patientid is required")
    @Column(name = "patient_id", nullable = false)
    private Long patientid;
    @NotNull(message = "doctorid is required")
    @Column(name = "doctor_id", nullable = false)
    private Long doctorid;
    @Column(name = "patient_name")
    private String patientName;
    @Column(name = "doctor_name")
    private String doctorName;

    @Column(name = "doctor_email")
    private String doctorEmail;

    @NotNull(message = "status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @NotBlank(message = "title is required")
    @Column(nullable = false)
    private String title;

    @NotBlank(message = "description is required")
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "approval_by_docter")
    private Long approvalByDocter;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(columnDefinition = "TEXT")
    private String diagnosis;

    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "report_url", length = 500)
    private String reportUrl;

    @OneToMany(mappedBy = "medicalReport", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<File> files;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = ReportStatus.DRAFT;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
