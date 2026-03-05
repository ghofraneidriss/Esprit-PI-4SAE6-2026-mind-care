package tn.esprit.medical_report_service.Services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    public void sendReportLink(String to, Long reportId, String url, String doctorName, String doctorEmail,
            String patientName) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(to);
            msg.setSubject("MindCare - Your medical report #" + reportId);
            msg.setText(
                    "Hello " + (patientName != null ? patientName : "Patient") + ",\n\n" +
                            "Your medical report is ready.\n\n" +
                            "Exported by Doctor: " + doctorName + "\n" +
                            "Doctor Email: " + doctorEmail + "\n\n" +
                            "Access your reports in the MindCare Patient Portal here:\n" +
                            url + "\n\n" +
                            "Thank you,\nMindCare");

            mailSender.send(msg);
            log.info("Email with report link sent to {}", to);
        } catch (Exception e) {
            log.error("Email with report link failed to {}: {}", to, e.getMessage(), e);
            throw e;
        }
    }

    public void sendReportAttachment(String to, Long reportId, byte[] pdfBytes, String doctorName, String doctorEmail) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);

            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject("MindCare - Your medical report #" + reportId);
            helper.setText("Hello,\n\nYour medical report is attached as a PDF file.\n\nExported by Doctor: "
                    + doctorName + "\nDoctor Email: " + doctorEmail + "\n\nMindCare");
            helper.addAttachment(
                    "medical_report_" + reportId + ".pdf",
                    new ByteArrayResource(pdfBytes),
                    "application/pdf");

            mailSender.send(mimeMessage);
            log.info("Email with PDF attachment sent to {}", to);
        } catch (Exception e) {
            log.error("Email with PDF attachment failed to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send report attachment", e);
        }
    }
}
