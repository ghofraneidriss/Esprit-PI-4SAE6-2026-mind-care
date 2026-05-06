package tn.esprit.traitement_et_consultation.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tn.esprit.traitement_et_consultation.dto.AlertRequest;
import tn.esprit.traitement_et_consultation.dto.SlotSuggestionResponse;
import tn.esprit.traitement_et_consultation.entity.Appointment;
import tn.esprit.traitement_et_consultation.exception.SlotUnavailableException;
import tn.esprit.traitement_et_consultation.service.AppointmentService;
import tn.esprit.traitement_et_consultation.service.EmailService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentControllerTest {

    @Mock
    private AppointmentService appointmentService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AppointmentController appointmentController;

    @Test
    void handleSlotUnavailableReturnsConflict() {
        SlotSuggestionResponse response = new SlotSuggestionResponse("busy",
                LocalDateTime.of(2026, 5, 7, 9, 0),
                LocalDateTime.of(2026, 5, 7, 10, 0));

        ResponseEntity<SlotSuggestionResponse> entity =
                appointmentController.handleSlotUnavailable(new SlotUnavailableException(response));

        assertEquals(HttpStatus.CONFLICT, entity.getStatusCode());
        assertEquals(response, entity.getBody());
    }

    @Test
    void crudEndpointsDelegateToService() {
        Appointment appointment = Appointment.builder().id(1L).build();
        when(appointmentService.createAppointment(appointment)).thenReturn(appointment);
        when(appointmentService.confirmAppointment(1L)).thenReturn(appointment);
        when(appointmentService.cancelAppointment(1L)).thenReturn(appointment);
        when(appointmentService.getAppointmentsByDoctor(2L)).thenReturn(List.of(appointment));
        when(appointmentService.getAppointmentsByPatient(3L)).thenReturn(List.of(appointment));
        when(appointmentService.getAllAppointments()).thenReturn(List.of(appointment));
        when(appointmentService.getFilteredAppointments(2L, 3L, "pending", true, "2026-05-07", 1, 9, true))
                .thenReturn(List.of(appointment));
        when(appointmentService.getFilterDates(2L)).thenReturn(List.of("2026-05-07"));
        when(appointmentService.getFilterPatients(2L)).thenReturn(List.of(3L));
        when(appointmentService.getAppointmentById(1L)).thenReturn(Optional.of(appointment));
        when(appointmentService.getAppointmentById(8L)).thenReturn(Optional.empty());
        when(appointmentService.updateAppointment(1L, appointment)).thenReturn(appointment);

        assertEquals(HttpStatus.OK, appointmentController.createAppointment(appointment).getStatusCode());
        assertEquals(HttpStatus.OK, appointmentController.confirmAppointment(1L).getStatusCode());
        assertEquals(HttpStatus.OK, appointmentController.cancelAppointment(1L).getStatusCode());
        assertEquals(1, appointmentController.getDoctorAppointments(2L).getBody().size());
        assertEquals(1, appointmentController.getPatientAppointments(3L).getBody().size());
        assertEquals(1, appointmentController.getAll().getBody().size());
        assertEquals(1, appointmentController.getFilteredAppointments(2L, 3L, "pending", true,
                "2026-05-07", 1, 9, true).getBody().size());
        assertEquals(List.of("2026-05-07"), appointmentController.getFilterDates(2L).getBody());
        assertEquals(List.of(3L), appointmentController.getFilterPatients(2L).getBody());
        assertEquals(HttpStatus.OK, appointmentController.getById(1L).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, appointmentController.getById(8L).getStatusCode());
        assertEquals(HttpStatus.OK, appointmentController.update(1L, appointment).getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, appointmentController.delete(1L).getStatusCode());
        verify(appointmentService).deleteAppointment(1L);
    }

    @Test
    void sendAlertEmailReturnsSuccessOrFailure() {
        AlertRequest request = new AlertRequest();
        request.setEmail("patient@example.com");
        request.setSubject("Alert");
        request.setMessage("Body");

        assertEquals(HttpStatus.OK, appointmentController.sendAlertEmail(request).getStatusCode());
        verify(emailService).sendEmail("patient@example.com", "Alert", "Body");

        doThrow(new RuntimeException("smtp down")).when(emailService)
                .sendEmail("patient@example.com", "Alert", "Body");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, appointmentController.sendAlertEmail(request).getStatusCode());
    }

    @Test
    void suggestSlotReturnsOkOrNotFound() {
        LocalDateTime suggested = LocalDateTime.of(2026, 5, 9, 11, 0);
        when(appointmentService.suggestBestSlot(2L, 5L, LocalDate.of(2026, 5, 9), LocalDate.of(2026, 5, 10)))
                .thenReturn(suggested);
        when(appointmentService.suggestBestSlot(3L, 5L, LocalDate.of(2026, 5, 9), LocalDate.of(2026, 5, 10)))
                .thenReturn(null);

        assertEquals(HttpStatus.OK,
                appointmentController.suggestSlot(2L, 5L, LocalDate.of(2026, 5, 9), LocalDate.of(2026, 5, 10))
                        .getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND,
                appointmentController.suggestSlot(3L, 5L, LocalDate.of(2026, 5, 9), LocalDate.of(2026, 5, 10))
                        .getStatusCode());
    }
}
