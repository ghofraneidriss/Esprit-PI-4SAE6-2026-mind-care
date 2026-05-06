package tn.esprit.traitement_et_consultation.service;

import org.junit.jupiter.api.Test;
import tn.esprit.traitement_et_consultation.entity.Appointment;
import tn.esprit.traitement_et_consultation.entity.AppointmentType;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoogleCalendarServiceTest {

    private final GoogleCalendarService googleCalendarService = new GoogleCalendarService();

    @Test
    void createGoogleEventFallsBackWhenClientIsMissing() {
        Appointment appointment = Appointment.builder()
                .id(1L)
                .patientId(99L)
                .appointmentDate(LocalDateTime.of(2026, 5, 7, 14, 0))
                .type(AppointmentType.ONLINE)
                .build();

        com.google.api.services.calendar.model.Event event = googleCalendarService.createGoogleEvent(appointment);

        assertTrue(event.getId().startsWith("external-"));
        assertEquals("https://meet.google.com/mind-care-appt", event.getHangoutLink());
        assertNotNull(event.getConferenceData());
    }

    @Test
    void deleteGoogleEventWithoutClientDoesNotThrow() {
        googleCalendarService.deleteGoogleEvent("google-id");
    }

    private static void assertEquals(String expected, String actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
}
