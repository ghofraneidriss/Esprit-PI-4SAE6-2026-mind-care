package tn.esprit.recommendation_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.recommendation_service.entity.PuzzleSession;

import java.util.List;

public interface PuzzleSessionRepository extends JpaRepository<PuzzleSession, Long> {

    List<PuzzleSession> findByPuzzle_IdAndPatientIdOrderByStartedAtDesc(Long puzzleId, Long patientId);

    List<PuzzleSession> findByPuzzle_IdAndCompletedTrueOrderByScoreDescDurationSecondsAscFinishedAtAsc(Long puzzleId);
}
