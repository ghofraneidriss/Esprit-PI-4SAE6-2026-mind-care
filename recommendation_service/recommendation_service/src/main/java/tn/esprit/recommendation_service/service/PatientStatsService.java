package tn.esprit.recommendation_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.recommendation_service.dto.stats.PatientStatsResponse;
import tn.esprit.recommendation_service.entity.PatientStats;
import tn.esprit.recommendation_service.entity.PuzzleSession;
import tn.esprit.recommendation_service.repository.PatientStatsRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class PatientStatsService {

    private final PatientStatsRepository patientStatsRepository;

    @Transactional(readOnly = true)
    public PatientStatsResponse getByPatient(Long patientId) {
        PatientStats stats = patientStatsRepository.findById(patientId)
                .orElseGet(() -> PatientStats.builder().patientId(patientId).build());
        return toResponse(stats);
    }

    public void recordSessionResult(PuzzleSession session) {
        if (session == null || session.getPatientId() == null) {
            return;
        }
        if (session.getFinishedAt() == null) {
            return;
        }

        PatientStats stats = patientStatsRepository.findById(session.getPatientId())
                .orElseGet(() -> PatientStats.builder().patientId(session.getPatientId()).build());

        stats.setTotalSessions(stats.getTotalSessions() + 1);
        stats.setLastSessionAt(session.getFinishedAt());
        stats.setTotalMoves(stats.getTotalMoves() + safeInt(session.getMovesCount()));
        stats.setTotalErrors(stats.getTotalErrors() + safeInt(session.getErrorsCount()));

        if (Boolean.TRUE.equals(session.getCompleted())) {
            stats.setCompletedSessions(stats.getCompletedSessions() + 1);
            stats.setCompletedScoreSum(stats.getCompletedScoreSum() + safeInt(session.getScore()));
            stats.setCompletedDurationSum(stats.getCompletedDurationSum() + safeInt(session.getDurationSeconds()));

            Integer score = session.getScore();
            if (score != null) {
                Integer best = stats.getBestScore();
                if (best == null || score > best) {
                    stats.setBestScore(score);
                }
            }
        }

        if (stats.getLastSessionAt() == null) {
            stats.setLastSessionAt(LocalDateTime.now());
        }

        patientStatsRepository.save(stats);
    }

    private PatientStatsResponse toResponse(PatientStats stats) {
        int completed = safeInt(stats.getCompletedSessions());
        Double avgScore = completed == 0 ? null : (double) safeInt(stats.getCompletedScoreSum()) / completed;
        Double avgDuration = completed == 0 ? null : (double) safeInt(stats.getCompletedDurationSum()) / completed;

        return PatientStatsResponse.builder()
                .patientId(stats.getPatientId())
                .totalSessions(safeInt(stats.getTotalSessions()))
                .completedSessions(completed)
                .avgScore(avgScore)
                .avgDurationSeconds(avgDuration)
                .bestScore(stats.getBestScore())
                .totalMoves(safeInt(stats.getTotalMoves()))
                .totalErrors(safeInt(stats.getTotalErrors()))
                .lastSessionAt(stats.getLastSessionAt())
                .updatedAt(stats.getUpdatedAt())
                .build();
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}

