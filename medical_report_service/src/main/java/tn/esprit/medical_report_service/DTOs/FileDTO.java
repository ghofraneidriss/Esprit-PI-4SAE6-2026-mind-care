package tn.esprit.medical_report_service.DTOs;

import lombok.Data;
import tn.esprit.medical_report_service.Enteties.FileType;

import java.time.LocalDateTime;

@Data
public class FileDTO {
    private Long id;
    private String fileName;
    private FileType fileType;
    private String filePath;
    private Long fileSize;
    private Long uploadedBy;
    private LocalDateTime uploadedAt;
    private String description;
    private Long medicalReportId;
}
