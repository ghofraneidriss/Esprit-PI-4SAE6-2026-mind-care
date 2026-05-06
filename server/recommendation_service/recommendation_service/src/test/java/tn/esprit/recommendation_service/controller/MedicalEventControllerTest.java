package tn.esprit.recommendation_service.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import tn.esprit.recommendation_service.dto.medicalevent.JoinMedicalEventRequest;
import tn.esprit.recommendation_service.dto.medicalevent.MedicalEventCreateRequest;
import tn.esprit.recommendation_service.dto.medicalevent.MedicalEventParticipationResponse;
import tn.esprit.recommendation_service.dto.medicalevent.MedicalEventResponse;
import tn.esprit.recommendation_service.dto.medicalevent.MedicalEventUpdateRequest;
import tn.esprit.recommendation_service.dto.medicalevent.ParticipantRankingResponse;
import tn.esprit.recommendation_service.dto.medicalevent.ScoreResponse;
import tn.esprit.recommendation_service.dto.medicalevent.StreakResponse;
import tn.esprit.recommendation_service.enums.DifficultyLevel;
import tn.esprit.recommendation_service.enums.MedicalEventStatus;
import tn.esprit.recommendation_service.enums.MedicalEventType;
import tn.esprit.recommendation_service.enums.ParticipantType;
import tn.esprit.recommendation_service.service.MedicalEventService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicalEventControllerTest {

    @Mock
    private MedicalEventService medicalEventService;

    @InjectMocks
    private MedicalEventController controller;

    @Test
    void shouldDelegateAllEndpoints() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        LocalDateTime end = start.plusDays(2);

        MedicalEventCreateRequest createRequest = MedicalEventCreateRequest.builder()
                .title("Atelier memo")
                .description("test")
                .type(MedicalEventType.SUDOKU)
                .difficulty(DifficultyLevel.EASY)
                .patientId(7L)
                .familyId(9L)
                .startDate(start)
                .endDate(end)
                .build();
        MedicalEventUpdateRequest updateRequest = MedicalEventUpdateRequest.builder()
                .title("Atelier memo 2")
                .description("maj")
                .type(MedicalEventType.PUZZLE)
                .difficulty(DifficultyLevel.MEDIUM)
                .status(MedicalEventStatus.ACTIVE)
                .patientId(7L)
                .familyId(9L)
                .startDate(start)
                .endDate(end)
                .build();
        JoinMedicalEventRequest joinRequest = JoinMedicalEventRequest.builder()
                .participantId(11L)
                .participantType(ParticipantType.PATIENT)
                .participationDate(LocalDate.now())
                .score(50)
                .build();

        MedicalEventResponse response = MedicalEventResponse.builder()
                .id(1L)
                .title("Atelier memo")
                .type(MedicalEventType.SUDOKU)
                .status(MedicalEventStatus.ACTIVE)
                .patientId(7L)
                .build();
        MedicalEventParticipationResponse participationResponse = MedicalEventParticipationResponse.builder()
                .id(5L)
                .medicalEventId(1L)
                .participantId(11L)
                .participantType(ParticipantType.PATIENT)
                .score(50)
                .build();
        StreakResponse streak = StreakResponse.builder().medicalEventId(1L).participantId(11L).streakDays(3).build();
        ScoreResponse score = ScoreResponse.builder().medicalEventId(1L).participantId(11L).totalScore(90).build();
        ParticipantRankingResponse ranking = ParticipantRankingResponse.builder()
                .rank(1)
                .participantId(11L)
                .participantType(ParticipantType.PATIENT)
                .totalScore(90L)
                .participations(2L)
                .build();

        when(medicalEventService.createMedicalEvent(createRequest)).thenReturn(response);
        when(medicalEventService.getAllMedicalEvents()).thenReturn(List.of(response));
        when(medicalEventService.searchMedicalEvents("memo")).thenReturn(List.of(response));
        when(medicalEventService.getMedicalEventById(1L)).thenReturn(response);
        when(medicalEventService.getActiveMedicalEventsByPatient(7L)).thenReturn(List.of(response));
        when(medicalEventService.getCompletedMedicalEvents(7L)).thenReturn(List.of(response));
        when(medicalEventService.getMedicalEventsByType(MedicalEventType.SUDOKU)).thenReturn(List.of(response));
        when(medicalEventService.updateMedicalEvent(1L, updateRequest)).thenReturn(response);
        when(medicalEventService.joinMedicalEvent(1L, joinRequest)).thenReturn(participationResponse);
        when(medicalEventService.calculateStreak(1L, 11L, ParticipantType.PATIENT)).thenReturn(streak);
        when(medicalEventService.calculateTotalScore(1L, 11L, ParticipantType.PATIENT)).thenReturn(score);
        when(medicalEventService.getParticipantRanking(1L)).thenReturn(List.of(ranking));
        when(medicalEventService.hasUserJoined(1L, 11L, ParticipantType.PATIENT)).thenReturn(true);
        when(medicalEventService.completeExpiredMedicalEvents()).thenReturn(2);

        assertThat(controller.create(createRequest).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.getAll().getBody()).containsExactly(response);
        assertThat(controller.search("memo").getBody()).containsExactly(response);
        assertThat(controller.getById(1L).getBody()).isEqualTo(response);
        assertThat(controller.getActiveEventsByPatient(7L).getBody()).containsExactly(response);
        assertThat(controller.getCompletedEventsByPatient(7L).getBody()).containsExactly(response);
        assertThat(controller.getByType(MedicalEventType.SUDOKU).getBody()).containsExactly(response);
        assertThat(controller.update(1L, updateRequest).getBody()).isEqualTo(response);
        assertThat(controller.join(1L, joinRequest).getBody()).isEqualTo(participationResponse);
        assertThat(controller.getStreak(1L, 11L, ParticipantType.PATIENT).getBody()).isEqualTo(streak);
        assertThat(controller.getScore(1L, 11L, ParticipantType.PATIENT).getBody()).isEqualTo(score);
        assertThat(controller.getRanking(1L).getBody()).containsExactly(ranking);
        assertThat(controller.hasJoined(1L, 11L, ParticipantType.PATIENT).getBody()).isTrue();
        assertThat(controller.completeExpired().getBody()).isEqualTo(2);
        assertThat(controller.delete(1L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        verify(medicalEventService).deleteMedicalEvent(1L);
    }
}
