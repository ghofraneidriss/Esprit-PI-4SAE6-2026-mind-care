package tn.esprit.medical_report_service.DTOs;

import lombok.Data;
import tn.esprit.medical_report_service.Enteties.RiskLevel;

import java.time.LocalDateTime;

@Data
public class AIResultDTO {
    private Long id;
    private Long mriScanId;
    private RiskLevel riskLevel;
    private Double confidenceScore;
    private String analysisDetails;
    private LocalDateTime createdAt;
}
