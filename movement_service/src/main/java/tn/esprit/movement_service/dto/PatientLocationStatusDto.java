package tn.esprit.movement_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Lightweight aggregate for dashboards / mobile: zone compliance + GPS freshness.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientLocationStatusDto {
    private Long patientId;
    /** Null if no zones or no GPS — cannot evaluate. */
    private Boolean insideAnySafeZone;
    private int definedSafeZoneCount;
    private boolean hasLatestGps;
    private LocalDateTime latestGpsRecordedAt;
    /** Minutes since last ping; null if no ping. */
    private Long minutesSinceLastGps;
}
