package tn.esprit.medical_report_service.DTOs;

import lombok.Data;
import tn.esprit.medical_report_service.Enteties.ReportStatus;
import tn.esprit.medical_report_service.Enteties.RiskLevel;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MedicalReportDTO {
    private Long reportid;
    private Long patientid;
    private Long doctorid;
    private String title;
    private String description;
    private String diagnosis;
    private RiskLevel riskLevel;
    private LocalDateTime approvedAt;
    private ReportStatus status;
    private String doctorName;
    private String doctorEmail;
    private String reportUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<FileDTO> files;
    private List<MRIScanDTO> mriScans;
}
