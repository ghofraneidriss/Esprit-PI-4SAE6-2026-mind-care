package tn.esprit.recommendation_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.recommendation_service.entity.MedicalEvent;
import tn.esprit.recommendation_service.enums.MedicalEventStatus;
import tn.esprit.recommendation_service.enums.MedicalEventType;

import java.time.LocalDateTime;
import java.util.List;

public interface MedicalEventRepository extends JpaRepository<MedicalEvent, Long> {

    List<MedicalEvent> findByPatientIdAndStatusOrderByCreatedAtDesc(Long patientId, MedicalEventStatus status);

    List<MedicalEvent> findByTypeOrderByCreatedAtDesc(MedicalEventType type);

    List<MedicalEvent> findByPatientIdAndTypeOrderByCreatedAtDesc(Long patientId, MedicalEventType type);

    List<MedicalEvent> findByPatientIdAndStatusOrderByEndDateDesc(Long patientId, MedicalEventStatus status);

    List<MedicalEvent> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String titleQuery, String descriptionQuery);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE MedicalEvent e
            SET e.status = tn.esprit.recommendation_service.enums.MedicalEventStatus.COMPLETED
            WHERE e.status = tn.esprit.recommendation_service.enums.MedicalEventStatus.ACTIVE
              AND e.endDate < :now
            """)
    int completeExpiredEvents(@Param("now") LocalDateTime now);
}
