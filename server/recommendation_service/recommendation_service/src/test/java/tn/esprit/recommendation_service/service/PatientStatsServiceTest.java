package tn.esprit.recommendation_service.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.recommendation_service.dto.stats.PatientStatsResponse;
import tn.esprit.recommendation_service.entity.PatientStats;
import tn.esprit.recommendation_service.entity.PuzzleSession;
import tn.esprit.recommendation_service.repository.PatientStatsRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientStatsServiceTest {

    @Mock
    private PatientStatsRepository patientStatsRepository;

    @InjectMocks
    private PatientStatsService patientStatsService;

    @Test
    void getByPatient_shouldReturnDefaultStats_whenPatientHasNoStats() {
        when(patientStatsRepository.findById(99L)).thenReturn(Optional.empty());

        PatientStatsResponse response = patientStatsService.getByPatient(99L);

        assertThat(response.getPatientId()).isEqualTo(99L);
        assertThat(response.getTotalSessions()).isZero();
        assertThat(response.getCompletedSessions()).isZero();
        assertThat(response.getAvgScore()).isNull();
        assertThat(response.getAvgDurationSeconds()).isNull();
    }

    @Test
    void getByPatient_shouldCalculateAverages_whenCompletedSessionsExist() {
        PatientStats stats = PatientStats.builder()
                .patientId(10L)
                .totalSessions(3)
                .completedSessions(2)
                .completedScoreSum(160)
                .completedDurationSum(90)
                .bestScore(95)
                .totalMoves(42)
                .totalErrors(5)
                .build();
        when(patientStatsRepository.findById(10L)).thenReturn(Optional.of(stats));

        PatientStatsResponse response = patientStatsService.getByPatient(10L);

        assertThat(response.getAvgScore()).isEqualTo(80.0);
        assertThat(response.getAvgDurationSeconds()).isEqualTo(45.0);
        assertThat(response.getBestScore()).isEqualTo(95);
        assertThat(response.getTotalMoves()).isEqualTo(42);
        assertThat(response.getTotalErrors()).isEqualTo(5);
    }

    @Test
    void recordSessionResult_shouldIgnoreNullAndUnfinishedSessions() {
        patientStatsService.recordSessionResult(null);
        patientStatsService.recordSessionResult(PuzzleSession.builder().patientId(10L).build());
        patientStatsService.recordSessionResult(PuzzleSession.builder().finishedAt(LocalDateTime.now()).build());

        verify(patientStatsRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void recordSessionResult_shouldCreateStatsAndUpdateCompletedSession() {
        LocalDateTime finishedAt = LocalDateTime.of(2026, 4, 30, 13, 30);
        PuzzleSession session = PuzzleSession.builder()
                .patientId(10L)
                .finishedAt(finishedAt)
                .movesCount(20)
                .errorsCount(2)
                .completed(true)
                .score(88)
                .durationSeconds(120)
                .build();
        when(patientStatsRepository.findById(10L)).thenReturn(Optional.empty());

        patientStatsService.recordSessionResult(session);

        ArgumentCaptor<PatientStats> captor = ArgumentCaptor.forClass(PatientStats.class);
        verify(patientStatsRepository).save(captor.capture());
        PatientStats saved = captor.getValue();
        assertThat(saved.getPatientId()).isEqualTo(10L);
        assertThat(saved.getTotalSessions()).isEqualTo(1);
        assertThat(saved.getCompletedSessions()).isEqualTo(1);
        assertThat(saved.getCompletedScoreSum()).isEqualTo(88);
        assertThat(saved.getCompletedDurationSum()).isEqualTo(120);
        assertThat(saved.getBestScore()).isEqualTo(88);
        assertThat(saved.getTotalMoves()).isEqualTo(20);
        assertThat(saved.getTotalErrors()).isEqualTo(2);
        assertThat(saved.getLastSessionAt()).isEqualTo(finishedAt);
    }

    @Test
    void recordSessionResult_shouldKeepBestScore_whenExistingBestIsHigher() {
        PatientStats existing = PatientStats.builder()
                .patientId(10L)
                .totalSessions(1)
                .completedSessions(1)
                .completedScoreSum(95)
                .completedDurationSum(60)
                .bestScore(95)
                .build();
        PuzzleSession session = PuzzleSession.builder()
                .patientId(10L)
                .finishedAt(LocalDateTime.now())
                .completed(true)
                .score(70)
                .durationSeconds(40)
                .build();
        when(patientStatsRepository.findById(10L)).thenReturn(Optional.of(existing));

        patientStatsService.recordSessionResult(session);

        assertThat(existing.getBestScore()).isEqualTo(95);
        assertThat(existing.getCompletedSessions()).isEqualTo(2);
        assertThat(existing.getCompletedScoreSum()).isEqualTo(165);
        verify(patientStatsRepository).save(existing);
    }
}
