package tn.esprit.activities_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import tn.esprit.activities_service.entity.GameResult;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.text.SimpleDateFormat;

/**
 * Service d'envoi d'alertes email automatiques via SMTP.
 * Envoie un email quand le risque d'Alzheimer est HIGH ou CRITICAL.
 */
@Service
public class EmailAlertService {

    private static final Logger log = LoggerFactory.getLogger(EmailAlertService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private AlzheimerRiskService riskService;

    @Value("${spring.mail.username:noreply@mindcare.com}")
    private String fromEmail;

    @Value("${alzheimer.alert.email.to:admin@mindcare.com}")
    private String defaultAlertEmail;

    /**
     * Envoie une alerte email si le risque est HIGH ou CRITICAL.
     * Retourne true si l'email a été envoyé.
     */
    public boolean sendAlertIfNeeded(GameResult result) {
        if (!riskService.shouldSendAlert(result)) {
            log.info("Pas d'alerte nécessaire pour le patient {} (risque: {})",
                    result.getPatientId(), result.getRiskLevel());
            return false;
        }

        try {
            // Envoyer à l'email du patient s'il est renseigné, sinon à l'admin
            String recipientEmail = (result.getPatientEmail() != null && !result.getPatientEmail().isBlank())
                    ? result.getPatientEmail()
                    : defaultAlertEmail;

            sendAlertEmail(result, recipientEmail);

            // Envoyer aussi à l'admin si différent du patient
            if (!recipientEmail.equals(defaultAlertEmail)) {
                sendAlertEmail(result, defaultAlertEmail);
            }

            result.setAlertSent(true);
            log.info("Alerte email envoyée pour le patient {} (risque: {})",
                    result.getPatientId(), result.getRiskLevel());
            return true;

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de l'alerte email pour le patient {}: {}",
                    result.getPatientId(), e.getMessage());
            result.setAlertSent(false);
            return false;
        }
    }

    /**
     * Force l'envoi d'un email d'alerte pour un résultat, quel que soit le niveau de risque.
     * Utilisé pour l'envoi manuel depuis le dashboard.
     */
    public boolean sendAlertForResult(GameResult result) {
        try {
            String recipientEmail = (result.getPatientEmail() != null && !result.getPatientEmail().isBlank())
                    ? result.getPatientEmail()
                    : defaultAlertEmail;

            sendAlertEmail(result, recipientEmail);

            if (!recipientEmail.equals(defaultAlertEmail)) {
                sendAlertEmail(result, defaultAlertEmail);
            }

            result.setAlertSent(true);
            log.info("Email d'alerte envoyé manuellement pour le patient {} (résultat {})",
                    result.getPatientId(), result.getId());
            return true;
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi manuel de l'alerte pour le résultat {}: {}",
                    result.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * Envoie l'email d'alerte formaté en HTML.
     */
    private void sendAlertEmail(GameResult result, String toEmail) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(toEmail);
        helper.setSubject(getSubject(result));
        helper.setText(buildEmailBody(result), true); // true = HTML

        mailSender.send(message);
        log.info("Email envoyé à {}", toEmail);
    }

    private String getSubject(GameResult result) {
        String risk = result.getRiskLevel();
        if ("CRITICAL".equals(risk)) {
            return "⚠️ ALERTE CRITIQUE - Détection de risque Alzheimer - Patient " + result.getPatientName();
        }
        return "🔴 ALERTE - Risque élevé détecté - Patient " + result.getPatientName();
    }

    private String buildEmailBody(GameResult result) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        String date = result.getCompletedAt() != null ? sdf.format(result.getCompletedAt()) : "N/A";
        String riskMessage = riskService.getRiskMessage(result);
        String riskColor = "CRITICAL".equals(result.getRiskLevel()) ? "#dc3545" : "#fd7e14";

        return """
                <html>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                    <div style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 20px; color: white; border-radius: 10px 10px 0 0;">
                        <h1 style="margin: 0;">🧠 Mind Care - Alerte Alzheimer</h1>
                    </div>
                    
                    <div style="padding: 20px; border: 1px solid #ddd; border-top: none;">
                        <div style="background-color: %s; color: white; padding: 15px; border-radius: 8px; margin-bottom: 20px;">
                            <strong>Niveau de risque : %s</strong>
                        </div>
                        
                        <p>%s</p>
                        
                        <h3>📊 Détails du test</h3>
                        <table style="width: 100%%; border-collapse: collapse;">
                            <tr style="border-bottom: 1px solid #eee;">
                                <td style="padding: 8px;"><strong>Patient</strong></td>
                                <td style="padding: 8px;">%s (ID: %d)</td>
                            </tr>
                            <tr style="border-bottom: 1px solid #eee;">
                                <td style="padding: 8px;"><strong>Activité</strong></td>
                                <td style="padding: 8px;">%s - %s</td>
                            </tr>
                            <tr style="border-bottom: 1px solid #eee;">
                                <td style="padding: 8px;"><strong>Score brut</strong></td>
                                <td style="padding: 8px;">%d / %d</td>
                            </tr>
                            <tr style="border-bottom: 1px solid #eee;">
                                <td style="padding: 8px;"><strong>Score pondéré</strong></td>
                                <td style="padding: 8px;">%.1f / 100</td>
                            </tr>
                            <tr style="border-bottom: 1px solid #eee;">
                                <td style="padding: 8px;"><strong>Réponses correctes</strong></td>
                                <td style="padding: 8px;">%d / %d</td>
                            </tr>
                            <tr style="border-bottom: 1px solid #eee;">
                                <td style="padding: 8px;"><strong>Temps moyen/question</strong></td>
                                <td style="padding: 8px;">%.1f secondes</td>
                            </tr>
                            <tr style="border-bottom: 1px solid #eee;">
                                <td style="padding: 8px;"><strong>Difficulté</strong></td>
                                <td style="padding: 8px;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding: 8px;"><strong>Date</strong></td>
                                <td style="padding: 8px;">%s</td>
                            </tr>
                        </table>
                        
                        <div style="margin-top: 20px; padding: 15px; background-color: #f8f9fa; border-radius: 8px;">
                            <strong>💡 Recommandation :</strong>
                            <p style="margin-bottom: 0;">Veuillez consulter un professionnel de santé pour une évaluation cognitive complète.</p>
                        </div>
                    </div>
                    
                    <div style="background-color: #f8f9fa; padding: 15px; text-align: center; border-radius: 0 0 10px 10px; border: 1px solid #ddd; border-top: none;">
                        <small style="color: #6c757d;">Mind Care - Plateforme de suivi cognitif Alzheimer</small>
                    </div>
                </body>
                </html>
                """.formatted(
                riskColor,
                result.getRiskLevel(),
                riskMessage,
                result.getPatientName() != null ? result.getPatientName() : "Inconnu",
                result.getPatientId(),
                result.getActivityType(),
                result.getActivityTitle() != null ? result.getActivityTitle() : "N/A",
                result.getScore() != null ? result.getScore() : 0,
                result.getMaxScore() != null ? result.getMaxScore() : 0,
                result.getWeightedScore() != null ? result.getWeightedScore() : 0.0,
                result.getCorrectAnswers() != null ? result.getCorrectAnswers() : 0,
                result.getTotalQuestions() != null ? result.getTotalQuestions() : 0,
                result.getAvgResponseTime() != null ? result.getAvgResponseTime() : 0.0,
                result.getDifficulty() != null ? result.getDifficulty() : "N/A",
                date
        );
    }
}
