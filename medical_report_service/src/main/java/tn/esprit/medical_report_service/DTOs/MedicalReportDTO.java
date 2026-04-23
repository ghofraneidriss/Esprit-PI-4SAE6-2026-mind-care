package tn.esprit.medical_report_service.DTOs;

import lombok.Data;
import tn.esprit.medical_report_service.Enteties.ReportStatus;
import tn.esprit.medical_report_service.Enteties.RiskLevel;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MedicalReportDTO {
    private Long id;
    private Long patientId;
    private Long doctorId;
    private String title;
    private String description;
    private String diagnosis;
    private RiskLevel riskLevel;
    private LocalDateTime reportDate;
    private ReportStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<FileDTO> files;
    private List<MRIScanDTO> mriScans;
}
