package tn.esprit.traitement_et_consultation.service;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender emailSender;

    @Test
    void sendEmailBuildsAndDispatchesMessage() {
        EmailService emailService = new EmailService(emailSender);
        ReflectionTestUtils.setField(emailService, "senderEmail", "noreply@mindcare.com");

        emailService.sendEmail("user@example.com", "Hello", "Body");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(emailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("noreply@mindcare.com", message.getFrom());
        org.junit.jupiter.api.Assertions.assertArrayEquals(new String[]{"user@example.com"}, message.getTo());
        org.junit.jupiter.api.Assertions.assertEquals("Hello", message.getSubject());
        org.junit.jupiter.api.Assertions.assertEquals("Body", message.getText());
    }

    @Test
    void sendHtmlEmailUsesMimeMessage() {
        EmailService emailService = new EmailService(emailSender);
        ReflectionTestUtils.setField(emailService, "senderEmail", "noreply@mindcare.com");
        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(emailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendHtmlEmail("user@example.com", "HTML", "<b>Body</b>");

        verify(emailSender).send(mimeMessage);
    }

    @Test
    void sendHtmlEmailWrapsFailures() {
        EmailService emailService = new EmailService(emailSender);
        ReflectionTestUtils.setField(emailService, "senderEmail", "noreply@mindcare.com");
        when(emailSender.createMimeMessage()).thenThrow(new RuntimeException("boom"));

        assertThrows(RuntimeException.class,
                () -> emailService.sendHtmlEmail("user@example.com", "HTML", "<b>Body</b>"));
    }

    @Test
    void sendEmailRethrowsFailures() {
        EmailService emailService = new EmailService(emailSender);
        ReflectionTestUtils.setField(emailService, "senderEmail", "noreply@mindcare.com");
        org.mockito.Mockito.doThrow(new RuntimeException("boom")).when(emailSender).send(any(SimpleMailMessage.class));

        assertThrows(RuntimeException.class, () -> emailService.sendEmail("user@example.com", "Hello", "Body"));
    }
}
