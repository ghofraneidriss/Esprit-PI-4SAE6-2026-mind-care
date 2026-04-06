package tn.esprit.traitement_et_consultation.service;

import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender emailSender;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.username}")
    private String senderEmail;

    public EmailService(JavaMailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void sendEmail(String to, String subject, String text) {
        try {
            System.out.println("Attempting to send simple email from: " + senderEmail + " to: " + to);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            emailSender.send(message);
            System.out.println("Simple email sent successfully!");
        } catch (Exception e) {
            System.err.println("Fatal error sending simple email: " + e.getMessage());
            throw e;
        }
    }

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
