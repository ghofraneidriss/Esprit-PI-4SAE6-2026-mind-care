package tn.esprit.movement_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.movement_service.dto.NearestSafeZoneInfoDto;
import tn.esprit.movement_service.dto.PatientLocationStatusDto;
import tn.esprit.movement_service.dto.PatientTrackingDashboardDto;
import tn.esprit.movement_service.dto.SafeZoneDto;
import tn.esprit.movement_service.entity.LocationPing;
import tn.esprit.movement_service.entity.MovementAlert;
import tn.esprit.movement_service.util.GeoDistance;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Couche métier avancée : agrège localization-service (zones) et données mouvement (GPS, alertes).
 */
@Service
@RequiredArgsConstructor
public class PatientTrackingFacadeService {

    public static final String COMPLIANCE_NO_ZONES = "NO_ZONES_DEFINED";
    public static final String COMPLIANCE_NO_GPS = "NO_GPS_DATA";
    public static final String COMPLIANCE_INSIDE = "INSIDE_SAFE_ZONE";
    public static final String COMPLIANCE_OUTSIDE = "OUTSIDE_ALL_ZONES";

    private static final int DASHBOARD_ALERT_PREVIEW = 12;

    private final LocalizationClient localizationClient;
    private final MovementMonitoringService movementMonitoringService;

    public PatientTrackingDashboardDto getDashboard(Long patientId) {
        List<SafeZoneDto> zones = localizationClient.getSafeZonesByPatientId(patientId);
        Optional<LocationPing> latestOpt = movementMonitoringService.getLatestLocation(patientId);
        List<MovementAlert> alerts = movementMonitoringService.getPatientAlerts(patientId);

        long unack = alerts.stream().filter(a -> !a.isAcknowledged()).count();
        MovementAlert latestOpen = alerts.stream()
                .filter(a -> !a.isAcknowledged())
                .max(Comparator.comparing(MovementAlert::getCreatedAt))
                .orElse(null);

        List<MovementAlert> preview = alerts.stream()
                .limit(DASHBOARD_ALERT_PREVIEW)
                .toList();

        PatientTrackingDashboardDto.PatientTrackingDashboardDtoBuilder b = PatientTrackingDashboardDto.builder()
                .patientId(patientId)
                .safeZones(zones)
                .latestLocation(latestOpt.orElse(null))
                .unacknowledgedAlertCount(unack)
                .latestUnacknowledgedAlert(latestOpen)
                .recentAlertsPreview(preview);

        if (zones.isEmpty()) {
            return b.locationCompliance(COMPLIANCE_NO_ZONES)
                    .nearestSafeZone(null)
                    .build();
        }

        if (latestOpt.isEmpty()) {
            return b.locationCompliance(COMPLIANCE_NO_GPS)
                    .nearestSafeZone(null)
                    .build();
        }

        LocationPing ping = latestOpt.get();
        boolean inside = zones.stream().anyMatch(z ->
                GeoDistance.meters(ping.getLatitude(), ping.getLongitude(), z.getCenterLatitude(), z.getCenterLongitude())
                        <= z.getRadius());

        NearestSafeZoneInfoDto nearest = computeNearestZone(ping, zones);

        String compliance = inside ? COMPLIANCE_INSIDE : COMPLIANCE_OUTSIDE;
        return b.locationCompliance(compliance)
                .nearestSafeZone(nearest)
                .build();
    }

    public PatientLocationStatusDto getLocationStatus(Long patientId) {
        List<SafeZoneDto> zones = localizationClient.getSafeZonesByPatientId(patientId);
        Optional<LocationPing> latestOpt = movementMonitoringService.getLatestLocation(patientId);
        LocalDateTime now = LocalDateTime.now();

        PatientLocationStatusDto.PatientLocationStatusDtoBuilder b = PatientLocationStatusDto.builder()
                .patientId(patientId)
                .definedSafeZoneCount(zones.size())
                .hasLatestGps(latestOpt.isPresent())
                .latestGpsRecordedAt(latestOpt.map(LocationPing::getRecordedAt).orElse(null));

        if (latestOpt.isEmpty()) {
            return b.insideAnySafeZone(null)
                    .minutesSinceLastGps(null)
                    .build();
        }

        LocationPing ping = latestOpt.get();
        long minutes = Duration.between(ping.getRecordedAt(), now).toMinutes();

        if (zones.isEmpty()) {
            return b.insideAnySafeZone(null)
                    .minutesSinceLastGps(minutes)
                    .build();
        }

        boolean inside = zones.stream().anyMatch(z ->
                GeoDistance.meters(ping.getLatitude(), ping.getLongitude(), z.getCenterLatitude(), z.getCenterLongitude())
                        <= z.getRadius());

        return b.insideAnySafeZone(inside)
                .minutesSinceLastGps(minutes)
                .build();
    }

    private NearestSafeZoneInfoDto computeNearestZone(LocationPing ping, List<SafeZoneDto> zones) {
        return zones.stream()
                .map(z -> {
                    double dCenter = GeoDistance.meters(
                            ping.getLatitude(), ping.getLongitude(),
                            z.getCenterLatitude(), z.getCenterLongitude());
                    double edge = Math.max(0, dCenter - z.getRadius());
                    boolean inside = dCenter <= z.getRadius();
                    return NearestSafeZoneInfoDto.builder()
                            .safeZoneId(z.getId())
                            .name(z.getName())
                            .distanceMetersToCenter(dCenter)
                            .radiusMeters(z.getRadius())
                            .insideThisZone(inside)
                            .distanceMetersToEdge(edge)
                            .build();
                })
                .min(Comparator.comparingDouble(NearestSafeZoneInfoDto::getDistanceMetersToEdge)
                        .thenComparingDouble(NearestSafeZoneInfoDto::getDistanceMetersToCenter))
                .orElse(null);
    }
}
