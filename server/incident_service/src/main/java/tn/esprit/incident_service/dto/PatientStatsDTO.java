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
public class PatientStatsDTO {
    private Long patientId;
    private String patientName;
    private long totalIncidents;
    private long activeIncidents;
    private long resolvedIncidents;
    private int severityScore;          // Score global de gravité (0-100)
    private String riskLevel;           // LOW, MODERATE, HIGH, CRITICAL
    private double avgDaysBetween;      // Durée moyenne entre incidents (jours)
    private Map<String, Long> bySeverity;  // LOW, MEDIUM, HIGH, CRITICAL

    /**
     * Détail du score : moyenne des computed normalisés (0–100), moyenne des niveaux LOW→CRITICAL (0–100),
     * ancien slot fréquence (souvent null).
     */
    private Integer volumeScorePart;
    private Integer severityWeightedPart;
    private Integer frequencyScorePart;
}
