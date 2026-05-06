package tn.esprit.traitement_et_consultation.service;

import com.google.api.services.calendar.model.Event;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.traitement_et_consultation.dto.SlotSuggestionResponse;
import tn.esprit.traitement_et_consultation.entity.Appointment;
import tn.esprit.traitement_et_consultation.entity.AppointmentCategory;
import tn.esprit.traitement_et_consultation.entity.AppointmentStatus;
import tn.esprit.traitement_et_consultation.entity.AppointmentType;
import tn.esprit.traitement_et_consultation.entity.PatientProfile;
import tn.esprit.traitement_et_consultation.exception.SlotUnavailableException;
import tn.esprit.traitement_et_consultation.repository.AppointmentRepository;
import tn.esprit.traitement_et_consultation.repository.PatientProfileRepository;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private PatientProfileRepository patientProfileRepository;

    @Mock
    private GoogleCalendarService googleCalendarService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AppointmentService appointmentService;

    @Test
    void createAppointmentSetsPendingAndSendsCreatedEmail() {
        Appointment appointment = Appointment.builder()
                .patientId(10L)
                .doctorId(90L)
                .appointmentDate(LocalDateTime.of(2026, 5, 7, 10, 0))
                .type(AppointmentType.IN_PERSON)
                .build();
        PatientProfile profile = PatientProfile.builder().userId(10L).email("patient@example.com").build();

        when(appointmentRepository.findOverlappingAppointments(eq(90L), any(), any(), any())).thenReturn(List.of());
        when(appointmentRepository.save(appointment)).thenReturn(appointment);
        when(patientProfileRepository.findByUserId(10L)).thenReturn(Optional.of(profile));

        Appointment saved = appointmentService.createAppointment(appointment);

        assertEquals(AppointmentStatus.PENDING, saved.getStatus());
        assertEquals(LocalDateTime.of(2026, 5, 7, 10, 35), saved.getAppointmentEndDate());
        verify(emailService).sendHtmlEmail(eq("patient@example.com"), eq("Appointment Received - Mind Care"), any());
    }

    @Test
    void createAppointmentRejectsOutsideWorkingHours() {
        Appointment appointment = Appointment.builder()
                .doctorId(1L)
                .appointmentDate(LocalDateTime.of(2026, 5, 7, 8, 30))
                .build();

        assertThrows(IllegalStateException.class, () -> appointmentService.createAppointment(appointment));
    }

    @Test
    void createAppointmentRejectsLunchOverlap() {
        Appointment appointment = Appointment.builder()
                .doctorId(1L)
                .appointmentDate(LocalDateTime.of(2026, 5, 7, 12, 50))
                .appointmentEndDate(LocalDateTime.of(2026, 5, 7, 13, 25))
                .build();

        assertThrows(IllegalStateException.class, () -> appointmentService.createAppointment(appointment));
    }

    @Test
    void createAppointmentRejectsDuplicateNewConsultation() {
        Appointment appointment = Appointment.builder()
                .patientId(4L)
                .doctorId(2L)
                .appointmentDate(LocalDateTime.of(2026, 5, 7, 10, 0))
                .category(AppointmentCategory.NEW_CONSULTATION)
                .build();

        when(appointmentRepository.findOverlappingAppointments(eq(2L), any(), any(), any())).thenReturn(List.of());
        when(appointmentRepository.findByPatientId(4L)).thenReturn(List.of(Appointment.builder().id(99L).build()));

        assertThrows(IllegalStateException.class, () -> appointmentService.createAppointment(appointment));
    }

    @Test
    void createAppointmentProvidesSlotSuggestionsWhenOverlapExists() {
        Appointment conflicting = Appointment.builder()
                .doctorId(7L)
                .appointmentDate(LocalDateTime.of(2026, 5, 7, 10, 0))
                .appointmentEndDate(LocalDateTime.of(2026, 5, 7, 10, 35))
                .status(AppointmentStatus.CONFIRMED)
                .build();
        Appointment appointment = Appointment.builder()
                .doctorId(7L)
                .appointmentDate(LocalDateTime.of(2026, 5, 7, 10, 10))
                .build();

        when(appointmentRepository.findOverlappingAppointments(eq(7L), any(), any(), any()))
                .thenReturn(List.of(conflicting))
                .thenReturn(List.of())
                .thenReturn(List.of());

        SlotUnavailableException ex = assertThrows(SlotUnavailableException.class,
                () -> appointmentService.createAppointment(appointment));

        SlotSuggestionResponse response = ex.getSuggestionResponse();
        assertNotNull(response);
        assertEquals(LocalDateTime.of(2026, 5, 7, 9, 25), response.getOptionA());
        assertEquals(LocalDateTime.of(2026, 5, 7, 10, 35), response.getOptionB());
    }

    @Test
    void confirmAppointmentAddsGoogleDataAndSendsEmail() {
        Appointment appointment = Appointment.builder()
                .id(5L)
                .patientId(11L)
                .doctorId(2L)
                .appointmentDate(LocalDateTime.of(2026, 5, 7, 11, 0))
                .type(AppointmentType.ONLINE)
                .status(AppointmentStatus.PENDING)
                .build();
        PatientProfile profile = PatientProfile.builder().userId(11L).email("confirmed@example.com").build();
        Event event = new Event().setId("google-1").setHangoutLink("https://meet.google.com/test-room");

        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appointment));
        when(googleCalendarService.createGoogleEvent(appointment)).thenReturn(event);
        when(appointmentRepository.save(appointment)).thenReturn(appointment);
        when(patientProfileRepository.findByUserId(11L)).thenReturn(Optional.of(profile));

        Appointment saved = appointmentService.confirmAppointment(5L);

        assertEquals(AppointmentStatus.CONFIRMED, saved.getStatus());
        assertEquals("google-1", saved.getGoogleEventId());
        assertEquals("https://meet.google.com/test-room", saved.getMeetLink());

        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendHtmlEmail(eq("confirmed@example.com"), eq("Appointment Confirmed - Mind Care"),
                htmlCaptor.capture());
        assertTrue(htmlCaptor.getValue().contains("https://meet.google.com/test-room"));
    }

    @Test
    void cancelAppointmentMarksCancelledAndSendsEmail() {
        Appointment appointment = Appointment.builder()
                .id(8L)
                .patientId(22L)
                .status(AppointmentStatus.PENDING)
                .appointmentDate(LocalDateTime.of(2026, 5, 7, 12, 0))
                .build();
        PatientProfile profile = PatientProfile.builder().userId(22L).email("cancel@example.com").build();

        when(appointmentRepository.findById(8L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(appointment)).thenReturn(appointment);
        when(patientProfileRepository.findByUserId(22L)).thenReturn(Optional.of(profile));

        Appointment saved = appointmentService.cancelAppointment(8L);

        assertEquals(AppointmentStatus.CANCELLED, saved.getStatus());
        verify(emailService).sendHtmlEmail(eq("cancel@example.com"), eq("Appointment Cancelled - Mind Care"), any());
    }

    @Test
    void suggestBestSlotAdvancesAfterConflict() {
        Appointment conflicting = Appointment.builder()
                .doctorId(1L)
                .appointmentDate(LocalDateTime.of(2026, 5, 7, 9, 0))
                .appointmentEndDate(LocalDateTime.of(2026, 5, 7, 9, 35))
                .status(AppointmentStatus.CONFIRMED)
                .build();

        when(appointmentRepository.findOverlappingAppointments(eq(1L), any(), any(), any()))
                .thenReturn(List.of(conflicting))
                .thenReturn(List.of())
                .thenReturn(List.of());

        LocalDateTime slot = appointmentService.suggestBestSlot(1L, 2L,
                LocalDate.of(2026, 5, 7), LocalDate.of(2026, 5, 7));

        assertEquals(LocalDateTime.of(2026, 5, 7, 9, 45), slot);
    }

    @Test
    void calculateIsHighPrioritySupportsUrgentAndLowScoreCases() {
        Appointment urgent = Appointment.builder().isUrgent(true).patientId(1L).build();
        Appointment cognitive = Appointment.builder().isUrgent(false).patientId(2L).build();
        Appointment normal = Appointment.builder().isUrgent(false).patientId(3L).build();

        when(patientProfileRepository.findById(2L))
                .thenReturn(Optional.of(PatientProfile.builder().externalCognitiveScore(18.0).build()));
        when(patientProfileRepository.findById(3L))
                .thenReturn(Optional.of(PatientProfile.builder().externalCognitiveScore(24.0).build()));

        assertTrue(appointmentService.calculateIsHighPriority(urgent));
        assertTrue(appointmentService.calculateIsHighPriority(cognitive));
        assertFalse(appointmentService.calculateIsHighPriority(normal));
    }

    @Test
    void getFilteredAppointmentsParsesInputsFiltersScoresAndSorts() {
        Appointment a1 = Appointment.builder()
                .id(1L)
                .patientId(101L)
                .appointmentDate(LocalDateTime.of(2026, 5, 8, 11, 0))
                .build();
        Appointment a2 = Appointment.builder()
                .id(2L)
                .patientId(202L)
                .appointmentDate(LocalDateTime.of(2026, 5, 7, 11, 0))
                .build();
        PatientProfile p1 = PatientProfile.builder()
                .userId(101L)
                .hypertension(true)
                .type2Diabetes(true)
                .hypercholesterolemia(true)
                .sleepDisorders(true)
                .familyHistoryAlzheimer(true)
                .dateOfBirth(LocalDate.now().minusYears(80))
                .externalCognitiveScore(4.0)
                .build();
        PatientProfile p2 = PatientProfile.builder()
                .userId(202L)
                .dateOfBirth(LocalDate.now().minusYears(30))
                .externalCognitiveScore(28.0)
                .build();

        when(appointmentRepository.findFilteredAppointments(eq(7L), eq(8L), eq(AppointmentStatus.CONFIRMED),
                eq(Boolean.TRUE), eq(Date.valueOf("2026-05-08"))))
                .thenReturn(List.of(a2, a1));
        when(patientProfileRepository.findByUserIdIn(List.of(202L, 101L))).thenReturn(List.of(p1, p2));

        List<Appointment> results = appointmentService.getFilteredAppointments(7L, 8L, "confirmed", true,
                "2026-05-08", 5, 20, true);

        assertEquals(1, results.size());
        assertEquals(1L, results.get(0).getId());
        assertTrue(results.get(0).getPriorityScore() >= 5);
    }

    @Test
    void getFilterHelpersStripNullValues() {
        when(appointmentRepository.findDistinctDates(2L))
                .thenReturn(java.util.Arrays.asList(Date.valueOf("2026-05-06"), null, Date.valueOf("2026-05-07")));
        when(appointmentRepository.findDistinctPatientIds(2L))
                .thenReturn(java.util.Arrays.asList(11L, null, 22L));

        assertEquals(List.of("2026-05-06", "2026-05-07"), appointmentService.getFilterDates(2L));
        assertEquals(List.of(11L, 22L), appointmentService.getFilterPatients(2L));
    }

    @Test
    void updateAppointmentUpdatesFieldsAndSendsEmail() {
        Appointment existing = Appointment.builder()
                .id(13L)
                .patientId(41L)
                .category(AppointmentCategory.DAILY_FOLLOW_UP)
                .status(AppointmentStatus.PENDING)
                .build();
        Appointment details = Appointment.builder()
                .patientId(41L)
                .appointmentDate(LocalDateTime.of(2026, 5, 7, 15, 0))
                .isUrgent(true)
                .type(AppointmentType.ONLINE)
                .category(AppointmentCategory.DAILY_FOLLOW_UP)
                .status(AppointmentStatus.RESCHEDULED)
                .build();
        PatientProfile profile = PatientProfile.builder().userId(41L).email("updated@example.com").build();

        when(appointmentRepository.findById(13L)).thenReturn(Optional.of(existing));
        when(appointmentRepository.save(existing)).thenReturn(existing);
        when(patientProfileRepository.findByUserId(41L)).thenReturn(Optional.of(profile));

        Appointment updated = appointmentService.updateAppointment(13L, details);

        assertEquals(LocalDateTime.of(2026, 5, 7, 15, 0), updated.getAppointmentDate());
        assertTrue(updated.getIsUrgent());
        assertEquals(AppointmentType.ONLINE, updated.getType());
        assertEquals(AppointmentStatus.RESCHEDULED, updated.getStatus());
        verify(emailService).sendHtmlEmail(eq("updated@example.com"), eq("Appointment Updated - Mind Care"), any());
    }

    @Test
    void updateAppointmentRejectsDuplicateNewConsultationCategory() {
        Appointment existing = Appointment.builder().id(13L).patientId(41L).build();
        Appointment other = Appointment.builder().id(99L).patientId(41L).build();
        Appointment details = Appointment.builder()
                .patientId(41L)
                .appointmentDate(LocalDateTime.of(2026, 5, 7, 15, 0))
                .category(AppointmentCategory.NEW_CONSULTATION)
                .build();

        when(appointmentRepository.findById(13L)).thenReturn(Optional.of(existing));
        when(appointmentRepository.findByPatientId(41L)).thenReturn(List.of(existing, other));

        assertThrows(IllegalStateException.class, () -> appointmentService.updateAppointment(13L, details));
    }

    @Test
    void updateAppointmentThrowsWhenMissing() {
        Appointment details = Appointment.builder()
                .patientId(41L)
                .appointmentDate(LocalDateTime.of(2026, 5, 7, 15, 0))
                .build();
        when(appointmentRepository.findById(13L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> appointmentService.updateAppointment(13L, details));
    }

    @Test
    void listAndLookupMethodsDelegateToRepository() {
        Appointment byDoctor = Appointment.builder().patientId(1L).doctorId(2L).build();
        Appointment byPatient = Appointment.builder().patientId(1L).doctorId(3L).build();
        PatientProfile profile = PatientProfile.builder().userId(1L).externalCognitiveScore(26.0).build();

        when(appointmentRepository.findByDoctorId(2L)).thenReturn(List.of(byDoctor));
        when(appointmentRepository.findByPatientId(1L)).thenReturn(List.of(byPatient));
        when(appointmentRepository.findAll()).thenReturn(List.of(byDoctor, byPatient));
        when(patientProfileRepository.findByUserIdIn(List.of(1L))).thenReturn(List.of(profile));
        when(appointmentRepository.findById(50L)).thenReturn(Optional.of(byDoctor));

        assertEquals(1, appointmentService.getAppointmentsByDoctor(2L).size());
        assertEquals(1, appointmentService.getAppointmentsByPatient(1L).size());
        assertEquals(2, appointmentService.getAllAppointments().size());
        assertEquals(Optional.of(byDoctor), appointmentService.getAppointmentById(50L));
        assertEquals(LocalDateTime.of(2026, 5, 10, 9, 0),
                appointmentService.findBestAvailableSlot(2L, 1L, LocalDate.of(2026, 5, 10),
                        LocalDate.of(2026, 5, 10)));
    }

    @Test
    void findBestAvailableSlotReturnsNullWhenNoSlotExists() {
        Appointment busy = Appointment.builder()
                .doctorId(2L)
                .appointmentDate(LocalDateTime.of(2026, 5, 10, 9, 0))
                .appointmentEndDate(LocalDateTime.of(2026, 5, 10, 9, 35))
                .status(AppointmentStatus.CONFIRMED)
                .build();
        when(appointmentRepository.findOverlappingAppointments(eq(2L), any(), any(), any()))
                .thenReturn(List.of(busy));
        when(appointmentRepository.findMinStartTimeAfter(eq(2L), any())).thenReturn(null);
        when(appointmentRepository.findMaxEndTimeBefore(eq(2L), any())).thenReturn(null);

        assertNull(appointmentService.findBestAvailableSlot(2L, 1L, LocalDate.of(2026, 5, 10),
                LocalDate.of(2026, 5, 10)));
    }

    @Test
    void createAndUpdateIgnoreEmailFailures() {
        Appointment create = Appointment.builder()
                .patientId(55L)
                .doctorId(3L)
                .appointmentDate(LocalDateTime.of(2026, 5, 7, 9, 0))
                .type(AppointmentType.IN_PERSON)
                .build();
        Appointment existing = Appointment.builder().id(88L).patientId(55L).build();
        Appointment update = Appointment.builder()
                .patientId(55L)
                .appointmentDate(LocalDateTime.of(2026, 5, 7, 10, 0))
                .status(AppointmentStatus.CONFIRMED)
                .category(AppointmentCategory.DAILY_FOLLOW_UP)
                .type(AppointmentType.IN_PERSON)
                .build();
        PatientProfile profile = PatientProfile.builder().userId(55L).email("boom@example.com").build();

        when(appointmentRepository.findOverlappingAppointments(eq(3L), any(), any(), any())).thenReturn(List.of());
        when(appointmentRepository.save(create)).thenReturn(create);
        when(appointmentRepository.findById(88L)).thenReturn(Optional.of(existing));
        when(appointmentRepository.save(existing)).thenReturn(existing);
        when(patientProfileRepository.findByUserId(55L)).thenReturn(Optional.of(profile));
        doThrow(new RuntimeException("mail failed")).when(emailService).sendHtmlEmail(eq("boom@example.com"), any(), any());

        Appointment created = appointmentService.createAppointment(create);
        Appointment updated = appointmentService.updateAppointment(88L, update);

        assertEquals(AppointmentStatus.PENDING, created.getStatus());
        assertEquals(AppointmentStatus.CONFIRMED, updated.getStatus());
        verify(emailService, times(2)).sendHtmlEmail(eq("boom@example.com"), any(), any());
    }

    @Test
    void deleteAppointmentDelegatesToRepository() {
        appointmentService.deleteAppointment(77L);
        verify(appointmentRepository).deleteById(77L);
    }
}
