package tn.esprit.incident_service.service;

import jakarta.activation.DataSource;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import tn.esprit.incident_service.dto.PatientStatsDTO;
import tn.esprit.incident_service.entity.Incident;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    /** Sends a notification email to the caregiver when a doctor creates an incident for their patient. */
    @Async
    public void sendIncidentNotification(String caregiverEmail,
                                          String caregiverName,
                                          String patientName,
                                          Incident incident) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("mouhanedmliki66@gmail.com");
            helper.setTo(caregiverEmail);
            helper.setSubject("🚨 MindCare — New incident reported for " + patientName);

            String html = buildEmailHtml(caregiverName, patientName, incident);
            helper.setText(html, true);

            mailSender.send(message);
            log.info("✅ Notification email sent to {} for incident #{}", caregiverEmail, incident.getId());

        } catch (MessagingException e) {
            log.error("❌ Failed to send email to {} : {}", caregiverEmail, e.getMessage());
        }
    }

    private String buildEmailHtml(String caregiverName, String patientName, Incident incident) {
        String typeName = incident.getType() != null ? incident.getType().getName() : "Not specified";
        String severity = incident.getSeverityLevel() != null ? incident.getSeverityLevel().name() : "N/A";
        String description = incident.getDescription() != null ? incident.getDescription() : "No description";

        String severityColor;
        switch (severity) {
            case "CRITICAL": severityColor = "#dc2626"; break;
            case "HIGH":     severityColor = "#ea580c"; break;
            case "MEDIUM":   severityColor = "#d97706"; break;
            default:         severityColor = "#16a34a"; break;
        }

        return """
            <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 600px; margin: 0 auto; background: #f8fafc; padding: 24px;">
                <div style="background: linear-gradient(135deg, #1e40af, #7c3aed); padding: 24px; border-radius: 12px 12px 0 0; text-align: center;">
                    <h1 style="color: white; margin: 0; font-size: 24px;">🧠 MindCare</h1>
                    <p style="color: #c7d2fe; margin: 8px 0 0 0;">Incident tracking</p>
                </div>
                <div style="background: white; padding: 24px; border-radius: 0 0 12px 12px; border: 1px solid #e2e8f0;">
                    <p style="font-size: 16px; color: #334155;">Hello <strong>%s</strong>,</p>
                    <p style="color: #475569;">A doctor has reported a new incident for your patient:</p>

                    <div style="background: #f1f5f9; border-radius: 8px; padding: 16px; margin: 16px 0;">
                        <table style="width: 100%%; border-collapse: collapse;">
                            <tr>
                                <td style="padding: 8px 0; color: #64748b; font-weight: 600;">Patient</td>
                                <td style="padding: 8px 0; color: #1e293b;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding: 8px 0; color: #64748b; font-weight: 600;">Type</td>
                                <td style="padding: 8px 0; color: #1e293b;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding: 8px 0; color: #64748b; font-weight: 600;">Severity</td>
                                <td style="padding: 8px 0;">
                                    <span style="background: %s; color: white; padding: 4px 12px; border-radius: 20px; font-size: 13px; font-weight: 600;">%s</span>
                                </td>
                            </tr>
                            <tr>
                                <td style="padding: 8px 0; color: #64748b; font-weight: 600;">Description</td>
                                <td style="padding: 8px 0; color: #1e293b;">%s</td>
                            </tr>
                        </table>
                    </div>

                    <p style="color: #475569; font-size: 14px;">
                        Sign in to MindCare to review the details and take any action needed.
                    </p>

                    <div style="text-align: center; margin-top: 24px;">
                        <a href="http://localhost:4200/incidents"
                           style="background: linear-gradient(135deg, #1e40af, #7c3aed); color: white; text-decoration: none;
                                  padding: 12px 32px; border-radius: 8px; font-weight: 600; font-size: 14px;">
                            View incidents
                        </a>
                    </div>
                </div>
                <p style="text-align: center; color: #94a3b8; font-size: 12px; margin-top: 16px;">
                    This email was sent automatically by MindCare — please do not reply.
                </p>
            </div>
            """.formatted(caregiverName, patientName, typeName, severityColor, severity, description);
    }

    /** Sends patient statistics by email with a PDF attachment. */
    public void sendPatientStatsEmail(String recipientEmail, PatientStatsDTO stats, byte[] pdfBytes) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("mouhanedmliki66@gmail.com");
            helper.setTo(recipientEmail);
            helper.setSubject("📊 MindCare — Statistics for " + stats.getPatientName());

            String html = buildStatsEmailHtml(stats);
            helper.setText(html, true);

            // Attach PDF
            if (pdfBytes != null && pdfBytes.length > 0) {
                DataSource pdfDataSource = new ByteArrayDataSource(pdfBytes, "application/pdf");
                String fileName = "statistics-" + stats.getPatientName().replaceAll("\\s+", "-").toLowerCase() + ".pdf";
                helper.addAttachment(fileName, pdfDataSource);
            }

            mailSender.send(message);
            log.info("✅ Statistics email (+ PDF) sent to {} for patient {}", recipientEmail, stats.getPatientName());

        } catch (MessagingException e) {
            log.error("❌ Failed to send statistics email to {} : {}", recipientEmail, e.getMessage());
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }

    // Backward-compatible overload for configurations expecting caregiverName argument.
    public void sendPatientStatsEmail(String recipientEmail, String caregiverName, PatientStatsDTO stats, byte[] pdfBytes) {
        sendPatientStatsEmail(recipientEmail, stats, pdfBytes);
    }

    private String buildStatsEmailHtml(PatientStatsDTO stats) {
        String riskColor;
        String riskLabel;
        switch (stats.getRiskLevel()) {
            case "CRITICAL": riskColor = "#ef4444"; riskLabel = "Critical"; break;
            case "HIGH":     riskColor = "#f97316"; riskLabel = "High"; break;
            case "MODERATE": riskColor = "#eab308"; riskLabel = "Moderate"; break;
            default:         riskColor = "#22c55e"; riskLabel = "Low"; break;
        }

        long low = stats.getBySeverity() != null && stats.getBySeverity().containsKey("LOW") ? stats.getBySeverity().get("LOW") : 0;
        long medium = stats.getBySeverity() != null && stats.getBySeverity().containsKey("MEDIUM") ? stats.getBySeverity().get("MEDIUM") : 0;
        long high = stats.getBySeverity() != null && stats.getBySeverity().containsKey("HIGH") ? stats.getBySeverity().get("HIGH") : 0;
        long critical = stats.getBySeverity() != null && stats.getBySeverity().containsKey("CRITICAL") ? stats.getBySeverity().get("CRITICAL") : 0;

        return """
            <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 600px; margin: 0 auto; background: #f8fafc; padding: 24px;">
                <div style="background: linear-gradient(135deg, #1e3a5f, #2563eb, #7c3aed); padding: 28px; border-radius: 12px 12px 0 0; text-align: center;">
                    <h1 style="color: white; margin: 0; font-size: 22px;">📊 Patient statistics report</h1>
                    <p style="color: rgba(255,255,255,0.8); margin: 8px 0 0; font-size: 14px;">MindCare — Incident follow-up</p>
                </div>
                <div style="background: white; padding: 28px; border-radius: 0 0 12px 12px; border: 1px solid #e2e8f0;">
                    <!-- Patient header -->
                    <div style="display: flex; align-items: center; gap: 16px; margin-bottom: 24px; padding-bottom: 20px; border-bottom: 1px solid #f1f5f9;">
                        <div style="width: 52px; height: 52px; background: linear-gradient(135deg, #2563eb, #7c3aed); border-radius: 50%%; display: flex; align-items: center; justify-content: center;">
                            <span style="color: white; font-size: 20px; font-weight: 700;">%s</span>
                        </div>
                        <div>
                            <h2 style="margin: 0; font-size: 20px; color: #1e293b; font-weight: 700;">%s</h2>
                            <p style="margin: 4px 0 0; color: #64748b; font-size: 13px;">Patient ID: %d</p>
                        </div>
                    </div>

                    <!-- Score + Risk -->
                    <div style="text-align: center; margin-bottom: 24px;">
                        <div style="display: inline-block; width: 100px; height: 100px; border-radius: 50%%; border: 8px solid %s; position: relative; line-height: 84px;">
                            <span style="font-size: 32px; font-weight: 800; color: %s;">%d</span>
                        </div>
                        <p style="margin: 12px 0 4px; font-size: 13px; color: #64748b;">Severity score</p>
                        <span style="display: inline-block; padding: 4px 16px; border-radius: 20px; font-size: 13px; font-weight: 700; background: %s20; color: %s;">%s</span>
                    </div>

                    <!-- Key metrics -->
                    <table style="width: 100%%; border-collapse: collapse; margin-bottom: 24px;">
                        <tr>
                            <td style="padding: 14px; text-align: center; background: #f8fafc; border-radius: 8px;">
                                <p style="font-size: 28px; font-weight: 800; color: #1e293b; margin: 0;">%d</p>
                                <p style="font-size: 12px; color: #64748b; margin: 4px 0 0;">Total incidents</p>
                            </td>
                            <td style="width: 8px;"></td>
                            <td style="padding: 14px; text-align: center; background: #fff7ed; border-radius: 8px;">
                                <p style="font-size: 28px; font-weight: 800; color: #ea580c; margin: 0;">%d</p>
                                <p style="font-size: 12px; color: #64748b; margin: 4px 0 0;">Active</p>
                            </td>
                            <td style="width: 8px;"></td>
                            <td style="padding: 14px; text-align: center; background: #f0fdf4; border-radius: 8px;">
                                <p style="font-size: 28px; font-weight: 800; color: #16a34a; margin: 0;">%d</p>
                                <p style="font-size: 12px; color: #64748b; margin: 4px 0 0;">Resolved</p>
                            </td>
                        </tr>
                    </table>

                    <!-- Severity breakdown -->
                    <p style="font-size: 14px; font-weight: 600; color: #475569; margin-bottom: 12px;">Breakdown by severity</p>
                    <table style="width: 100%%; border-collapse: collapse;">
                        <tr>
                            <td style="padding: 8px 12px; color: #64748b; font-size: 13px;">🔴 Critical</td>
                            <td style="padding: 8px 12px; text-align: right; font-weight: 700; color: #ef4444;">%d</td>
                        </tr>
                        <tr style="background: #f8fafc;">
                            <td style="padding: 8px 12px; color: #64748b; font-size: 13px;">🟠 High</td>
                            <td style="padding: 8px 12px; text-align: right; font-weight: 700; color: #f97316;">%d</td>
                        </tr>
                        <tr>
                            <td style="padding: 8px 12px; color: #64748b; font-size: 13px;">🟡 Medium</td>
                            <td style="padding: 8px 12px; text-align: right; font-weight: 700; color: #eab308;">%d</td>
                        </tr>
                        <tr style="background: #f8fafc;">
                            <td style="padding: 8px 12px; color: #64748b; font-size: 13px;">🟢 Low</td>
                            <td style="padding: 8px 12px; text-align: right; font-weight: 700; color: #22c55e;">%d</td>
                        </tr>
                    </table>

                    <p style="margin-top: 24px; color: #64748b; font-size: 13px;">
                        Average time between incidents: <strong style="color: #1e293b;">%.1f days</strong>
                    </p>

                    <div style="text-align: center; margin-top: 28px;">
                        <a href="http://localhost:4200/incidents/patient-stats"
                           style="background: linear-gradient(135deg, #1e40af, #7c3aed); color: white; text-decoration: none;
                                  padding: 12px 32px; border-radius: 8px; font-weight: 600; font-size: 14px;">
                            View detailed statistics
                        </a>
                    </div>
                </div>
                <p style="text-align: center; color: #94a3b8; font-size: 12px; margin-top: 16px;">
                    This email was sent from the MindCare platform.
                </p>
            </div>
            """.formatted(
                getInitials(stats.getPatientName()),
                stats.getPatientName(),
                stats.getPatientId(),
                riskColor, riskColor, stats.getSeverityScore(),
                riskColor, riskColor, riskLabel,
                stats.getTotalIncidents(), stats.getActiveIncidents(), stats.getResolvedIncidents(),
                critical, high, medium, low,
                stats.getAvgDaysBetween()
            );
    }

    private String getInitials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) sb.append(Character.toUpperCase(part.charAt(0)));
        }
        return sb.length() > 2 ? sb.substring(0, 2) : sb.toString();
    }

    // Manual test method for email
    public String sendTestEmail() {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom("mouhanedmliki66@gmail.com");
            helper.setTo("mouhanedmliki66@gmail.com");
            helper.setSubject("[TEST] MindCare Email Test");
            helper.setText("<h2>MindCare SMTP test</h2><p>If you received this email, SMTP is configured correctly.</p>", true);
            mailSender.send(message);
            log.info("✅ Test email sent successfully to mouhanedmliki66@gmail.com");
            return "SUCCESS - Email sent to mouhanedmliki66@gmail.com";
        } catch (Exception e) {
            log.error("❌ Failed to send test email: {}", e.getMessage(), e);
            return "ERROR - " + e.getMessage();
        }
    }
}
