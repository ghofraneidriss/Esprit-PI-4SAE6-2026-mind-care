package tn.esprit.movement_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.movement_service.entity.AlertType;
import tn.esprit.movement_service.entity.MovementAlert;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovementAlertRepository extends JpaRepository<MovementAlert, Long> {

    List<MovementAlert> findTop200ByOrderByCreatedAtDesc();

    List<MovementAlert> findByAcknowledgedFalseOrderByCreatedAtDesc();

    List<MovementAlert> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    boolean existsByPatientIdAndAlertTypeAndAcknowledgedFalseAndCreatedAtAfter(
            Long patientId,
            AlertType alertType,
            LocalDateTime createdAt
    );

    @Query(
            "SELECT m.patientId, COUNT(m) FROM MovementAlert m "
                    + "WHERE m.alertType = :alertType GROUP BY m.patientId"
    )
    List<Object[]> countByAlertTypeGroupByPatientId(@Param("alertType") AlertType alertType);

    /** Every persisted alert type (full traceability for clinical dashboards). */
    @Query("SELECT m.patientId, COUNT(m) FROM MovementAlert m GROUP BY m.patientId")
    List<Object[]> countAllAlertsGroupByPatientId();

    @Modifying
    @Query("DELETE FROM MovementAlert m WHERE m.patientId = :patientId")
    void deleteByPatientId(@Param("patientId") Long patientId);

    @Modifying
    @Query("DELETE FROM MovementAlert")
    void deleteAllRows();
}
