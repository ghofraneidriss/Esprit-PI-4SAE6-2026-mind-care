package tn.esprit.recommendation_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.recommendation_service.entity.ClinicalEscalationAlert;
import tn.esprit.recommendation_service.enums.AlertStatus;
import tn.esprit.recommendation_service.enums.RecommendationType;

import java.util.List;
import java.util.Optional;

public interface ClinicalEscalationAlertRepository extends JpaRepository<ClinicalEscalationAlert, Long> {

    List<ClinicalEscalationAlert> findByDoctorIdOrderByCreatedAtDesc(Long doctorId);

    List<ClinicalEscalationAlert> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    Optional<ClinicalEscalationAlert> findTopByDoctorIdAndPatientIdAndRecommendationTypeAndStatusOrderByCreatedAtDesc(
            Long doctorId,
            Long patientId,
            RecommendationType recommendationType,
            AlertStatus status
    );
}
