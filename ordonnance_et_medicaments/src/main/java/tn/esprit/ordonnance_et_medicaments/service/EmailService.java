package tn.esprit.ordonnance_et_medicaments.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Service d'envoi d'email — copie exacte du pattern utilisé dans traitement_et_consultation.
 * Fournit sendHtmlEmail() utilisé par PrescriptionMailService lors de la signature d'une ordonnance.
 */
@Service
public class EmailService {

    private final JavaMailSender emailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    public EmailService(JavaMailSender emailSender) {
        this.emailSender = emailSender;
    }

    /**
     * Envoie un email HTML.
     * Même implémentation que traitement_et_consultation/EmailService.
     */
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            System.out.println("Attempting to send HTML email from: " + senderEmail + " to: " + to);
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(senderEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            emailSender.send(message);
            System.out.println("HTML email sent successfully!");
        } catch (Exception e) {
            System.err.println("Fatal error sending HTML email: " + e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
