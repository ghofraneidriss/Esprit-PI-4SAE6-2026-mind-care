package tn.esprit.movement_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import tn.esprit.movement_service.entity.LocationPing;
import tn.esprit.movement_service.entity.MovementAlert;

import java.util.ArrayList;
import java.util.List;

@Service
public class MovementAlertNotifier {

    private static final Logger log = LoggerFactory.getLogger(MovementAlertNotifier.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired
    private UsersClient usersClient;

    @Value("${movement.alert.email.to:admin@mindcare.com}")
    private String adminEmail;

    @Value("${movement.alert.email.from:noreply@mindcare.com}")
    private String fromEmail;

    public boolean notifyByEmail(MovementAlert alert, LocationPing latestPing) {
        if (mailSender == null) {
            log.warn("JavaMailSender not configured. Email alert skipped for patient {}.", alert.getPatientId());
            return false;
        }

        List<String> recipients = resolveAlertRecipients(alert.getPatientId());
        if (recipients.isEmpty()) {
            log.warn("No alert email recipients found for patient {}.", alert.getPatientId());
            return false;
        }

        String subject = "[Mind Care] Movement alert - Patient " + alert.getPatientId();
        String body = buildBody(alert, latestPing);
        boolean sentAtLeastOnce = false;

        for (String recipient : recipients) {
            try {
                sendSimpleEmail(recipient, subject, body);
                sentAtLeastOnce = true;
            } catch (Exception ex) {
                log.error("Failed to send movement alert email to {}: {}", recipient, ex.getMessage());
            }
        }

        return sentAtLeastOnce;
    }

    private List<String> resolveAlertRecipients(Long patientId) {
        List<String> recipients = new ArrayList<>();

        recipients.addAll(usersClient.getDoctorEmails());
        recipients.addAll(usersClient.getCareNetworkEmailsForPatient(patientId));

        // Keep admin fallback for operational safety.
        if (adminEmail != null && !adminEmail.isBlank()) {
            recipients.add(adminEmail.trim());
        }

        return recipients.stream()
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .distinct()
                .toList();
    }

    /**
     * In-app CVP notification for linked caregiver + volunteer when the patient shares live GPS (BROWSER_GPS).
     * Snippet field carries Google Maps directions URL (destination = patient pin).
     */
    public void notifyCaregiversLiveLocationInApp(LocationPing ping) {
        List<Long> targets = usersClient.getCareNetworkUserIdsForPatient(ping.getPatientId());
        if (targets.isEmpty()) {
            log.info("In-app live location: no caregiver/volunteer linked to patient {}.", ping.getPatientId());
            return;
        }
        String label = usersClient.getPatientDisplayName(ping.getPatientId());
        String mapsUrl = String.format(
                "https://www.google.com/maps/dir/?api=1&destination=%s,%s",
                ping.getLatitude(), ping.getLongitude());
        String message = label + " shared their live position.";
        for (Long uid : targets) {
            usersClient.postInAppNotification(
                    uid,
                    message,
                    "INFO",
                    "PATIENT_LOCATION_SHARED",
                    label,
                    ping.getPatientId(),
                    mapsUrl);
        }
    }

    /**
     * Optional: notify caregiver + volunteer by email when the patient shares a live GPS position (not simulation).
     */
    public boolean notifyLiveLocationShared(LocationPing ping) {
        if (mailSender == null) {
            log.warn("JavaMailSender not configured. Live location email skipped for patient {}.", ping.getPatientId());
            return false;
        }

        List<String> recipients = usersClient.getCareNetworkEmailsForPatient(ping.getPatientId());
        if (recipients.isEmpty()) {
            log.warn("No caregiver/volunteer emails for live location ping, patient {}.", ping.getPatientId());
            return false;
        }

        String subject = "[Mind Care] Patient location " + ping.getPatientId();
        StringBuilder body = new StringBuilder();
        body.append("The patient shared their GPS position.\n\n");
        body.append("Patient ID: ").append(ping.getPatientId()).append("\n");
        body.append("Latitude: ").append(ping.getLatitude()).append("\n");
        body.append("Longitude: ").append(ping.getLongitude()).append("\n");
        body.append("Time: ").append(ping.getRecordedAt()).append("\n");
        if (ping.getSpeedKmh() != null) {
            body.append("Speed (km/h): ").append(String.format("%.2f", ping.getSpeedKmh())).append("\n");
        }
        body.append("\nMap: https://www.google.com/maps?q=").append(ping.getLatitude()).append(",").append(ping.getLongitude());

        boolean sentAtLeastOnce = false;
        for (String recipient : recipients) {
            try {
                sendSimpleEmail(recipient, subject, body.toString());
                sentAtLeastOnce = true;
            } catch (Exception ex) {
                log.error("Failed to send live location email to {}: {}", recipient, ex.getMessage());
            }
        }
        return sentAtLeastOnce;
    }

    private void sendSimpleEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }

    private String buildBody(MovementAlert alert, LocationPing latestPing) {
        StringBuilder sb = new StringBuilder();
        sb.append("Movement alert\n\n");
        sb.append("Patient ID: ").append(alert.getPatientId()).append("\n");
        sb.append("Type: ").append(alert.getAlertType()).append("\n");
        sb.append("Severity: ").append(alert.getSeverity()).append("\n");
        sb.append("Message: ").append(alert.getMessage()).append("\n");
        sb.append("Date: ").append(alert.getCreatedAt()).append("\n\n");

        if (latestPing != null) {
            sb.append("Latest position\n");
            sb.append("Latitude: ").append(latestPing.getLatitude()).append("\n");
            sb.append("Longitude: ").append(latestPing.getLongitude()).append("\n");
            sb.append("Position time: ").append(latestPing.getRecordedAt()).append("\n");
            if (latestPing.getSpeedKmh() != null) {
                sb.append("Speed (km/h): ").append(String.format("%.2f", latestPing.getSpeedKmh())).append("\n");
            }
        }

        sb.append("\nPlease review the patient in the monitoring dashboard.");
        return sb.toString();
    }
}
