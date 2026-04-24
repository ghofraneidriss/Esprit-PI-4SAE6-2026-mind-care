package tn.esprit.medical_report_service.DTOs;

import lombok.Data;
import tn.esprit.medical_report_service.Enteties.QualityStatus;

import java.time.LocalDateTime;

@Data
public class MRIScanDTO {
    private Long id;
    private Long patientId;
    private LocalDateTime scanDate;
    private Long fileId;
    private QualityStatus qualityStatus;
    private String notes;
    private LocalDateTime createdAt;
    private Long medicalReportId;
    private AIResultDTO aiResult;
}
