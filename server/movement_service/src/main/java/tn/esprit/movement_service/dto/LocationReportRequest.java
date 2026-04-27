package tn.esprit.movement_service.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LocationReportRequest {
    private Long patientId;
    private Double latitude;
    private Double longitude;
    private Double accuracyMeters;
    private String source;
    private LocalDateTime recordedAt;
}
