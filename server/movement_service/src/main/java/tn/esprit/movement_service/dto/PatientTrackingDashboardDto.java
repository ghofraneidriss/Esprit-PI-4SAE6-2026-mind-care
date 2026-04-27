package tn.esprit.movement_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.movement_service.entity.LocationPing;
import tn.esprit.movement_service.entity.MovementAlert;

import java.util.List;

/**
 * Advanced business API: localization + movement in one response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientTrackingDashboardDto {
    private Long patientId;

    /**
     * NO_ZONES_DEFINED | NO_GPS_DATA | INSIDE_SAFE_ZONE | OUTSIDE_ALL_ZONES
     */
    private String locationCompliance;

    private List<SafeZoneDto> safeZones;
    private LocationPing latestLocation;
    private NearestSafeZoneInfoDto nearestSafeZone;

    private long unacknowledgedAlertCount;
    private MovementAlert latestUnacknowledgedAlert;

    private List<MovementAlert> recentAlertsPreview;
}
