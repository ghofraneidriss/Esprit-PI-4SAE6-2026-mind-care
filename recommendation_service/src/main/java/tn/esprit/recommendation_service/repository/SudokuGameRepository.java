package tn.esprit.recommendation_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.recommendation_service.entity.SudokuGame;

import java.util.List;
import java.util.Optional;

public interface SudokuGameRepository extends JpaRepository<SudokuGame, Long> {
    List<SudokuGame> findByPatientIdOrderByCreatedAtDesc(Long patientId);
    Optional<SudokuGame> findByMedicalEvent_Id(Long medicalEventId);
}
