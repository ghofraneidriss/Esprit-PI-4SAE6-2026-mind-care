package tn.esprit.lost_item_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.lost_item_service.entity.AlertLevel;
import tn.esprit.lost_item_service.entity.AlertStatus;
import tn.esprit.lost_item_service.entity.LostItemAlert;

import java.util.List;

@Repository
public interface LostItemAlertRepository extends JpaRepository<LostItemAlert, Long> {

    List<LostItemAlert> findByLostItemId(Long lostItemId);

    List<LostItemAlert> findByPatientId(Long patientId);

    List<LostItemAlert> findByLevel(AlertLevel level);

    List<LostItemAlert> findByStatus(AlertStatus status);

    List<LostItemAlert> findByLostItemIdAndStatus(Long lostItemId, AlertStatus status);

    List<LostItemAlert> findByPatientIdAndStatus(Long patientId, AlertStatus status);

    List<LostItemAlert> findByLevelAndStatus(AlertLevel level, AlertStatus status);

    List<LostItemAlert> findByCaregiverId(Long caregiverId);

    List<LostItemAlert> findByCaregiverIdAndStatus(Long caregiverId, AlertStatus status);

    boolean existsByLostItemIdAndTitleAndStatusNot(Long lostItemId, String title, AlertStatus status);

    long countByPatientIdAndStatus(Long patientId, AlertStatus status);

    long countByLevelAndStatus(AlertLevel level, AlertStatus status);
}
