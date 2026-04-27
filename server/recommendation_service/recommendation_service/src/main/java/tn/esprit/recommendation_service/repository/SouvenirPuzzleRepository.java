package tn.esprit.recommendation_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.recommendation_service.entity.SouvenirPuzzle;

import java.util.List;
import java.util.Optional;

public interface SouvenirPuzzleRepository extends JpaRepository<SouvenirPuzzle, Long> {

    List<SouvenirPuzzle> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    Optional<SouvenirPuzzle> findByMedicalEvent_Id(Long medicalEventId);

    boolean existsBySouvenirEntryIdAndPatientId(Long souvenirEntryId, Long patientId);
}
