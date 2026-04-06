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

    /**
     * Envoie un e-mail de notification au caregiver lorsqu'un docteur
     * crée un incident pour l'un de ses patients.
     */
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
            helper.setSubject("🚨 MindCare — Nouvel incident signalé pour " + patientName);

            String html = buildEmailHtml(caregiverName, patientName, incident);
            helper.setText(html, true);

            mailSender.send(message);
            log.info("✅ E-mail de notification envoyé à {} pour l'incident #{}", caregiverEmail, incident.getId());

        } catch (MessagingException e) {
            log.error("❌ Échec de l'envoi de l'e-mail à {} : {}", caregiverEmail, e.getMessage());
        }
    }

    private String buildEmailHtml(String caregiverName, String patientName, Incident incident) {
        String typeName = incident.getType() != null ? incident.getType().getName() : "Non spécifié";
        String severity = incident.getSeverityLevel() != null ? incident.getSeverityLevel().name() : "N/A";
        String description = incident.getDescription() != null ? incident.getDescription() : "Aucune description";

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
                    <p style="color: #c7d2fe; margin: 8px 0 0 0;">Système de suivi des incidents</p>
                </div>
                <div style="background: white; padding: 24px; border-radius: 0 0 12px 12px; border: 1px solid #e2e8f0;">
                    <p style="font-size: 16px; color: #334155;">Bonjour <strong>%s</strong>,</p>
                    <p style="color: #475569;">Un nouveau incident a été signalé par un médecin pour votre patient :</p>

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
                                <td style="padding: 8px 0; color: #64748b; font-weight: 600;">Sévérité</td>
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
                        Veuillez vous connecter à la plateforme MindCare pour consulter les détails et prendre les mesures nécessaires.
                    </p>

                    <div style="text-align: center; margin-top: 24px;">
                        <a href="http://localhost:4200/incidents"
                           style="background: linear-gradient(135deg, #1e40af, #7c3aed); color: white; text-decoration: none;
                                  padding: 12px 32px; border-radius: 8px; font-weight: 600; font-size: 14px;">
                            Voir les incidents
                        </a>
                    </div>
                </div>
                <p style="text-align: center; color: #94a3b8; font-size: 12px; margin-top: 16px;">
                    Cet e-mail a été envoyé automatiquement par MindCare — ne pas répondre.
                </p>
            </div>
            """.formatted(caregiverName, patientName, typeName, severityColor, severity, description);
    }

    /**
     * Envoie les statistiques d'un patient par e-mail avec un PDF en pièce jointe.
     */
    public void sendPatientStatsEmail(String recipientEmail, PatientStatsDTO stats, byte[] pdfBytes) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("mouhanedmliki66@gmail.com");
            helper.setTo(recipientEmail);
            helper.setSubject("📊 MindCare — Statistiques de " + stats.getPatientName());

            String html = buildStatsEmailHtml(stats);
            helper.setText(html, true);

            // Attach PDF
            if (pdfBytes != null && pdfBytes.length > 0) {
                DataSource pdfDataSource = new ByteArrayDataSource(pdfBytes, "application/pdf");
                String fileName = "statistiques-" + stats.getPatientName().replaceAll("\\s+", "-").toLowerCase() + ".pdf";
                helper.addAttachment(fileName, pdfDataSource);
            }

            mailSender.send(message);
            log.info("✅ E-mail de statistiques (+ PDF) envoyé à {} pour le patient {}", recipientEmail, stats.getPatientName());

        } catch (MessagingException e) {
            log.error("❌ Échec de l'envoi de l'e-mail de statistiques à {} : {}", recipientEmail, e.getMessage());
            throw new RuntimeException("Échec de l'envoi de l'e-mail : " + e.getMessage());
        }
    }

    private String buildStatsEmailHtml(PatientStatsDTO stats) {
        String riskColor;
        String riskLabel;
        switch (stats.getRiskLevel()) {
            case "CRITICAL": riskColor = "#ef4444"; riskLabel = "Critique"; break;
            case "HIGH":     riskColor = "#f97316"; riskLabel = "Élevé"; break;
            case "MODERATE": riskColor = "#eab308"; riskLabel = "Modéré"; break;
            default:         riskColor = "#22c55e"; riskLabel = "Faible"; break;
        }

        long low = stats.getBySeverity() != null && stats.getBySeverity().containsKey("LOW") ? stats.getBySeverity().get("LOW") : 0;
        long medium = stats.getBySeverity() != null && stats.getBySeverity().containsKey("MEDIUM") ? stats.getBySeverity().get("MEDIUM") : 0;
        long high = stats.getBySeverity() != null && stats.getBySeverity().containsKey("HIGH") ? stats.getBySeverity().get("HIGH") : 0;
        long critical = stats.getBySeverity() != null && stats.getBySeverity().containsKey("CRITICAL") ? stats.getBySeverity().get("CRITICAL") : 0;

        return """
            <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 600px; margin: 0 auto; background: #f8fafc; padding: 24px;">
                <div style="background: linear-gradient(135deg, #1e3a5f, #2563eb, #7c3aed); padding: 28px; border-radius: 12px 12px 0 0; text-align: center;">
                    <h1 style="color: white; margin: 0; font-size: 22px;">📊 Rapport Statistique Patient</h1>
                    <p style="color: rgba(255,255,255,0.8); margin: 8px 0 0; font-size: 14px;">MindCare — Suivi des incidents</p>
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
                        <p style="margin: 12px 0 4px; font-size: 13px; color: #64748b;">Score de gravité</p>
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
                                <p style="font-size: 12px; color: #64748b; margin: 4px 0 0;">Actifs</p>
                            </td>
                            <td style="width: 8px;"></td>
                            <td style="padding: 14px; text-align: center; background: #f0fdf4; border-radius: 8px;">
                                <p style="font-size: 28px; font-weight: 800; color: #16a34a; margin: 0;">%d</p>
                                <p style="font-size: 12px; color: #64748b; margin: 4px 0 0;">Résolus</p>
                            </td>
                        </tr>
                    </table>

                    <!-- Severity breakdown -->
                    <p style="font-size: 14px; font-weight: 600; color: #475569; margin-bottom: 12px;">Répartition par sévérité</p>
                    <table style="width: 100%%; border-collapse: collapse;">
                        <tr>
                            <td style="padding: 8px 12px; color: #64748b; font-size: 13px;">🔴 Critique</td>
                            <td style="padding: 8px 12px; text-align: right; font-weight: 700; color: #ef4444;">%d</td>
                        </tr>
                        <tr style="background: #f8fafc;">
                            <td style="padding: 8px 12px; color: #64748b; font-size: 13px;">🟠 Élevé</td>
                            <td style="padding: 8px 12px; text-align: right; font-weight: 700; color: #f97316;">%d</td>
                        </tr>
                        <tr>
                            <td style="padding: 8px 12px; color: #64748b; font-size: 13px;">🟡 Moyen</td>
                            <td style="padding: 8px 12px; text-align: right; font-weight: 700; color: #eab308;">%d</td>
                        </tr>
                        <tr style="background: #f8fafc;">
                            <td style="padding: 8px 12px; color: #64748b; font-size: 13px;">🟢 Faible</td>
                            <td style="padding: 8px 12px; text-align: right; font-weight: 700; color: #22c55e;">%d</td>
                        </tr>
                    </table>

                    <p style="margin-top: 24px; color: #64748b; font-size: 13px;">
                        Durée moyenne entre incidents : <strong style="color: #1e293b;">%.1f jours</strong>
                    </p>

                    <div style="text-align: center; margin-top: 28px;">
                        <a href="http://localhost:4200/incidents/patient-stats"
                           style="background: linear-gradient(135deg, #1e40af, #7c3aed); color: white; text-decoration: none;
                                  padding: 12px 32px; border-radius: 8px; font-weight: 600; font-size: 14px;">
                            Voir les statistiques détaillées
                        </a>
                    </div>
                </div>
                <p style="text-align: center; color: #94a3b8; font-size: 12px; margin-top: 16px;">
                    Cet e-mail a été envoyé depuis la plateforme MindCare.
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
            helper.setText("<h2>Test SMTP MindCare</h2><p>Si vous recevez cet e-mail, la configuration SMTP fonctionne correctement.</p>", true);
            mailSender.send(message);
            log.info("✅ Test e-mail envoyé avec succès à mouhanedmliki66@gmail.com");
            return "SUCCESS - Email envoyé à mouhanedmliki66@gmail.com";
        } catch (Exception e) {
            log.error("❌ Échec de l'envoi du test e-mail : {}", e.getMessage(), e);
            return "ERREUR - " + e.getMessage();
        }
    }
}
