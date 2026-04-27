package tn.esprit.recommendation_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.recommendation_service.entity.SudokuSession;

import java.util.List;

public interface SudokuSessionRepository extends JpaRepository<SudokuSession, Long> {
    List<SudokuSession> findBySudokuGame_IdAndPatientIdOrderByStartedAtDesc(Long gameId, Long patientId);
    List<SudokuSession> findBySudokuGame_IdAndCompletedTrueOrderByScoreDescDurationSecondsAsc(Long gameId);
}
