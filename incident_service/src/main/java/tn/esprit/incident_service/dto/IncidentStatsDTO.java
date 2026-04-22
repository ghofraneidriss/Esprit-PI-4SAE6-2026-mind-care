package tn.esprit.incident_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class IncidentStatsDTO {
    private long totalActive;
    private long totalHistory;
    private Map<String, Long> bySeverity;   // LOW, MEDIUM, HIGH, CRITICAL
    private Map<String, Long> byStatus;     // OPEN, IN_PROGRESS, RESOLVED
    private Map<String, Long> byMonth;      // "Jan", "Feb", ... last 6 months
}
