package tn.esprit.movement_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.movement_service.dto.LocationReportRequest;
import tn.esprit.movement_service.dto.SafeZoneDto;
import tn.esprit.movement_service.entity.AlertSeverity;
import tn.esprit.movement_service.entity.AlertType;
import tn.esprit.movement_service.entity.LocationPing;
import tn.esprit.movement_service.entity.MovementAlert;
import tn.esprit.movement_service.repository.LocationPingRepository;
import tn.esprit.movement_service.repository.MovementAlertRepository;
import tn.esprit.movement_service.util.GeoDistance;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MovementMonitoringService {

    private final LocalizationClient localizationClient;
    private final LocationPingRepository locationPingRepository;
    private final MovementAlertRepository movementAlertRepository;
    private final MovementAlertNotifier movementAlertNotifier;

    @Value("${movement.alerts.abrupt-distance-meters:300}")
    private double abruptDistanceThresholdMeters;

    @Value("${movement.alerts.immobile-seconds:20}")
    private long immobileSeconds;

    @Value("${movement.alerts.immobile-radius-meters:25}")
    private double immobileRadiusMeters;

    @Value("${movement.alerts.no-gps-minutes:30}")
    private long noGpsMinutes;

    @Value("${movement.alerts.cooldown-minutes:20}")
    private long duplicateCooldownMinutes;

    @Value("${movement.notify.email.on-live-gps:false}")
    private boolean emailOnLiveGps;

    /**
     * Ingests one GPS ping. Real tracking uses sources such as {@code BROWSER_GPS}; demo uses {@code SIMULATION_*}.
     * All sources: ping is saved, then rules run (safe zone, home, abrupt move, immobility). Simulations additionally
     * insert an explicit test alert via {@link #createSimulationAlert}.
     */
    @Transactional
    public LocationPing reportLocation(LocationReportRequest request) {
        validateRequest(request);

        LocalDateTime recordedAt = request.getRecordedAt() != null ? request.getRecordedAt() : LocalDateTime.now();
        Optional<LocationPing> previous = locationPingRepository.findTopByPatientIdOrderByRecordedAtDesc(request.getPatientId());
        String source = request.getSource();

        LocationPing ping = LocationPing.builder()
                .patientId(request.getPatientId())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .accuracyMeters(request.getAccuracyMeters())
            .source(source)
                .recordedAt(recordedAt)
                .build();

        previous.ifPresent(prev -> {
            double speed = calculateSpeedKmh(prev, ping);
            ping.setSpeedKmh(speed >= 0 ? speed : null);
        });

        LocationPing saved = locationPingRepository.save(ping);

        boolean liveBrowserGps = source != null && "BROWSER_GPS".equalsIgnoreCase(source.trim());
        if (liveBrowserGps) {
            movementAlertNotifier.notifyCaregiversLiveLocationInApp(saved);
        }
        if (emailOnLiveGps && liveBrowserGps) {
            movementAlertNotifier.notifyLiveLocationShared(saved);
        }

        if (isSimulationSource(source)) {
            createSimulationAlert(saved, previous.orElse(null));
        }

        boolean homeExitAlert = evaluateLeftRegisteredHome(previous.orElse(null), saved);
        evaluateOutOfSafeZone(saved, homeExitAlert);
        evaluateAbruptMovement(previous.orElse(null), saved);
        evaluateImmobility(saved.getPatientId());

        return saved;
    }

    public Optional<LocationPing> getLatestLocation(Long patientId) {
        return locationPingRepository.findTopByPatientIdOrderByRecordedAtDesc(patientId);
    }

    public List<LocationPing> getPatientHistory(Long patientId, int minutes) {
        int boundedMinutes = Math.max(5, Math.min(minutes, 10080));
        LocalDateTime start = LocalDateTime.now().minusMinutes(boundedMinutes);
        List<LocationPing> data = locationPingRepository.findByPatientIdAndRecordedAtAfterOrderByRecordedAtAsc(patientId, start);
        return data.stream().sorted(Comparator.comparing(LocationPing::getRecordedAt)).toList();
    }

    public List<MovementAlert> getRecentAlerts(boolean onlyUnacknowledged) {
        if (onlyUnacknowledged) {
            return movementAlertRepository.findByAcknowledgedFalseOrderByCreatedAtDesc();
        }
        return movementAlertRepository.findTop200ByOrderByCreatedAtDesc();
    }

    public List<MovementAlert> getPatientAlerts(Long patientId) {
        return movementAlertRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
    }

    /**
     * Full counts per patient for OUT_OF_SAFE_ZONE (not limited to the global “recent 200” feed).
     * Used by admin dashboards for critical-patient rules and PDF exports.
     */
    public Map<Long, Long> getOutOfSafeZoneExitCountsByPatient() {
        List<Object[]> rows = movementAlertRepository.countByAlertTypeGroupByPatientId(AlertType.OUT_OF_SAFE_ZONE);
        return rowsToPatientCountMap(rows);
    }

    /**
     * Total persisted movement alerts per patient (all types: safe zone, home exit, immobility, no GPS, abrupt movement).
     * Used for “critical” lists and matches what {@link #getPatientAlerts(Long)} returns for history/PDF.
     */
    public Map<Long, Long> getTotalMovementAlertCountsByPatient() {
        List<Object[]> rows = movementAlertRepository.countAllAlertsGroupByPatientId();
        return rowsToPatientCountMap(rows);
    }

    private Map<Long, Long> rowsToPatientCountMap(List<Object[]> rows) {
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            if (row == null || row.length < 2 || row[0] == null) {
                continue;
            }
            Long patientId = ((Number) row[0]).longValue();
            long cnt = ((Number) row[1]).longValue();
            map.put(patientId, cnt);
        }
        return map;
    }

    @Transactional
    public MovementAlert acknowledgeAlert(Long alertId) {
        MovementAlert alert = movementAlertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found: " + alertId));
        alert.setAcknowledged(true);
        alert.setAcknowledgedAt(LocalDateTime.now());
        return movementAlertRepository.save(alert);
    }

    @Transactional
    public void deleteAlert(Long alertId) {
        movementAlertRepository.deleteById(alertId);
    }

    @Transactional
    public void deleteAllAlertsForPatient(Long patientId) {
        movementAlertRepository.deleteByPatientId(patientId);
    }

    @Transactional
    public void deleteAllAlerts() {
        movementAlertRepository.deleteAllRows();
    }

    @Scheduled(fixedDelayString = "${movement.alerts.no-gps-check-ms:600000}")
    public void checkGpsSilence() {
        LocalDateTime now = LocalDateTime.now();
        List<Long> patientsWithZones = localizationClient.getPatientIdsWithSafeZones();
        if (patientsWithZones.isEmpty()) {
            patientsWithZones = locationPingRepository.findDistinctPatientIds();
        }

        for (Long patientId : patientsWithZones) {
            Optional<LocationPing> latest = locationPingRepository.findTopByPatientIdOrderByRecordedAtDesc(patientId);
            if (latest.isEmpty()) {
                createAlertIfNeeded(
                        patientId,
                        AlertType.GPS_NO_DATA,
                        AlertSeverity.CRITICAL,
                        "No GPS data received for this patient."
                );
                continue;
            }

            long minutesSinceLastGps = Duration.between(latest.get().getRecordedAt(), now).toMinutes();
            if (minutesSinceLastGps >= noGpsMinutes) {
                LocationPing stale = latest.get();
                createAlertIfNeeded(
                        patientId,
                        AlertType.GPS_NO_DATA,
                        AlertSeverity.CRITICAL,
                        "No GPS data for " + minutesSinceLastGps + " minutes.",
                        false,
                        stale.getLatitude(),
                        stale.getLongitude()
                );
            }
        }
    }

    @Scheduled(fixedDelayString = "${movement.alerts.immobility-check-ms:10000}")
    public void checkImmobilePatients() {
        List<Long> patientIds = locationPingRepository.findDistinctPatientIds();
        for (Long patientId : patientIds) {
            evaluateImmobility(patientId);
        }
    }

    private void validateRequest(LocationReportRequest request) {
        if (request.getPatientId() == null) {
            throw new IllegalArgumentException("patientId is required");
        }
        if (request.getLatitude() == null || request.getLongitude() == null) {
            throw new IllegalArgumentException("latitude and longitude are required");
        }
        if (request.getLatitude() < -90 || request.getLatitude() > 90 || request.getLongitude() < -180 || request.getLongitude() > 180) {
            throw new IllegalArgumentException("Invalid latitude/longitude values");
        }
    }

    /**
     * Registered home exit: alert on every transition from inside to outside the home circle (no cooldown).
     * If such an alert is raised and the patient is outside all zones, we skip the generic
     * {@link AlertType#OUT_OF_SAFE_ZONE} for the same ping to avoid duplicate notifications.
     */
    private boolean evaluateLeftRegisteredHome(LocationPing previous, LocationPing current) {
        if (previous == null || current == null) {
            return false;
        }
        List<SafeZoneDto> zones = localizationClient.getSafeZonesByPatientId(current.getPatientId());
        List<SafeZoneDto> homes = zones.stream().filter(SafeZoneDto::isHomeReference).toList();
        if (homes.isEmpty()) {
            return false;
        }
        boolean anyExit = false;
        for (SafeZoneDto h : homes) {
            boolean prevIn = GeoDistance.meters(
                    previous.getLatitude(), previous.getLongitude(),
                    h.getCenterLatitude(), h.getCenterLongitude()) <= h.getRadius();
            boolean currIn = GeoDistance.meters(
                    current.getLatitude(), current.getLongitude(),
                    h.getCenterLatitude(), h.getCenterLongitude()) <= h.getRadius();
            if (prevIn && !currIn) {
                createAlertIfNeeded(
                        current.getPatientId(),
                        AlertType.LEFT_REGISTERED_HOME,
                        AlertSeverity.CRITICAL,
                        "The patient left their registered home area.",
                        true,
                        current.getLatitude(),
                        current.getLongitude()
                );
                anyExit = true;
            }
        }
        return anyExit;
    }

    private void evaluateOutOfSafeZone(LocationPing ping, boolean skipGenericZoneAlertAfterHomeExit) {
        List<SafeZoneDto> zones = localizationClient.getSafeZonesByPatientId(ping.getPatientId());
        if (zones.isEmpty()) {
            return;
        }

        boolean insideAllowedZone = zones.stream().anyMatch(zone -> {
            double d = GeoDistance.meters(ping.getLatitude(), ping.getLongitude(), zone.getCenterLatitude(), zone.getCenterLongitude());
            return d <= zone.getRadius();
        });

        if (!insideAllowedZone) {
            if (skipGenericZoneAlertAfterHomeExit) {
                return;
            }
            createAlertIfNeeded(
                    ping.getPatientId(),
                    AlertType.OUT_OF_SAFE_ZONE,
                    AlertSeverity.CRITICAL,
                    "The patient left all safe zones.",
                    false,
                    ping.getLatitude(),
                    ping.getLongitude()
            );
        }
    }

    private void evaluateAbruptMovement(LocationPing previous, LocationPing current) {
        if (previous == null) {
            return;
        }

        double displacementMeters = GeoDistance.meters(
                previous.getLatitude(),
                previous.getLongitude(),
                current.getLatitude(),
                current.getLongitude()
        );

        if (displacementMeters >= abruptDistanceThresholdMeters) {
            createAlertIfNeeded(
                    current.getPatientId(),
                    AlertType.RAPID_OR_UNUSUAL_MOVEMENT,
                    AlertSeverity.WARNING,
                    "Abrupt movement detected (" + String.format("%.0f", displacementMeters) + " m between two positions).",
                    false,
                    current.getLatitude(),
                    current.getLongitude()
            );
        }
    }

    private void evaluateImmobility(Long patientId) {
        LocalDateTime start = LocalDateTime.now().minusSeconds(immobileSeconds);
        List<LocationPing> pings = locationPingRepository.findByPatientIdAndRecordedAtAfterOrderByRecordedAtAsc(patientId, start);

        if (pings.size() < 2) {
            return;
        }

        LocationPing first = pings.get(0);
        LocationPing last = pings.get(pings.size() - 1);
        long immobilizedSeconds = Duration.between(first.getRecordedAt(), last.getRecordedAt()).getSeconds();
        if (immobilizedSeconds < immobileSeconds) {
            return;
        }

        double maxDistance = pings.stream()
                .mapToDouble(p -> GeoDistance.meters(first.getLatitude(), first.getLongitude(), p.getLatitude(), p.getLongitude()))
                .max()
                .orElse(Double.MAX_VALUE);

        if (maxDistance <= immobileRadiusMeters) {
            createAlertIfNeeded(
                    patientId,
                    AlertType.IMMOBILE_TOO_LONG,
                    AlertSeverity.WARNING,
                    "Patient immobile for more than " + immobileSeconds + " seconds.",
                    false,
                    last.getLatitude(),
                    last.getLongitude()
            );
        }
    }

    private void createAlertIfNeeded(Long patientId, AlertType type, AlertSeverity severity, String message) {
        createAlertIfNeeded(patientId, type, severity, message, false, null, null);
    }

    private void createAlertIfNeeded(Long patientId, AlertType type, AlertSeverity severity, String message, boolean forceCreate) {
        createAlertIfNeeded(patientId, type, severity, message, forceCreate, null, null);
    }

    /**
     * Persists an alert; when {@code latitude}/{@code longitude} are set, appends the patient’s position and a Google Maps link
     * so caregivers see it in the dashboard, WhatsApp prefill, and email body.
     */
    private void createAlertIfNeeded(
            Long patientId,
            AlertType type,
            AlertSeverity severity,
            String message,
            boolean forceCreate,
            Double latitude,
            Double longitude
    ) {
        String text = appendPatientLocationToMessage(message, latitude, longitude);

        if (!forceCreate) {
            LocalDateTime cooldownStart = LocalDateTime.now().minusMinutes(duplicateCooldownMinutes);
            boolean existsRecentOpenAlert = movementAlertRepository
                    .existsByPatientIdAndAlertTypeAndAcknowledgedFalseAndCreatedAtAfter(patientId, type, cooldownStart);

            if (existsRecentOpenAlert) {
                return;
            }
        }

        MovementAlert alert = MovementAlert.builder()
                .patientId(patientId)
                .alertType(type)
                .severity(severity)
                .message(text)
                .acknowledged(false)
                .emailSent(false)
                .createdAt(LocalDateTime.now())
                .build();

        MovementAlert saved = movementAlertRepository.save(alert);
        boolean emailSent = movementAlertNotifier.notifyByEmail(saved, getLatestLocation(patientId).orElse(null));
        if (emailSent) {
            saved.setEmailSent(true);
            movementAlertRepository.save(saved);
        }
    }

    private String appendPatientLocationToMessage(String message, Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return message;
        }
        String maps = String.format("https://www.google.com/maps?q=%s,%s", latitude, longitude);
        return message
                + " — Position (patient): "
                + String.format("%.6f, %.6f", latitude, longitude)
                + " — "
                + maps;
    }

    private boolean isSimulationSource(String source) {
        return source != null && source.startsWith("SIMULATION_");
    }

    private void createSimulationAlert(LocationPing current, LocationPing previous) {
        String source = current.getSource() != null ? current.getSource() : "SIMULATION";

        AlertType type;
        AlertSeverity severity;
        String message;

        if (source.contains("OUT_OF_ZONE")) {
            type = AlertType.OUT_OF_SAFE_ZONE;
            severity = AlertSeverity.CRITICAL;
            message = "Test alert: simulated safe-zone exit.";
        } else if (source.contains("IMMOBILE")) {
            type = AlertType.IMMOBILE_TOO_LONG;
            severity = AlertSeverity.WARNING;
            message = "Test alert: simulated prolonged immobility (e.g. 10 min without movement).";
        } else if (source.contains("ABRUPT")) {
            type = AlertType.RAPID_OR_UNUSUAL_MOVEMENT;
            severity = AlertSeverity.WARNING;

            if (previous != null) {
                double displacementMeters = GeoDistance.meters(
                        previous.getLatitude(),
                        previous.getLongitude(),
                        current.getLatitude(),
                        current.getLongitude()
                );
                message = "Test alert: simulated abrupt movement (" + String.format("%.0f", displacementMeters) + " m).";
            } else {
                message = "Test alert: simulated abrupt movement.";
            }
        } else {
            type = AlertType.RAPID_OR_UNUSUAL_MOVEMENT;
            severity = AlertSeverity.WARNING;
            message = "Test alert: simulated movement.";
        }

        // Force a new alert for every simulation click so it appears in active alerts and triggers email each time.
        createAlertIfNeeded(
                current.getPatientId(),
                type,
                severity,
                message,
                true,
                current.getLatitude(),
                current.getLongitude()
        );
    }

    private double calculateSpeedKmh(LocationPing prev, LocationPing current) {
        long seconds = Duration.between(prev.getRecordedAt(), current.getRecordedAt()).getSeconds();
        if (seconds <= 0) {
            return -1;
        }
        double meters = GeoDistance.meters(prev.getLatitude(), prev.getLongitude(), current.getLatitude(), current.getLongitude());
        return (meters / seconds) * 3.6;
    }

}
