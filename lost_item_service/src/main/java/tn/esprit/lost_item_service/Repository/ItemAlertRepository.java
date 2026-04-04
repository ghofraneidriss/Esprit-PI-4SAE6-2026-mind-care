package tn.esprit.lost_item_service.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.lost_item_service.Entity.AlertLevel;
import tn.esprit.lost_item_service.Entity.AlertStatus;
import tn.esprit.lost_item_service.Entity.ItemAlert;

import java.util.List;

@Repository
public interface ItemAlertRepository extends JpaRepository<ItemAlert, Long> {

    List<ItemAlert> findByLostItemId(Long lostItemId);

    List<ItemAlert> findByPatientId(Long patientId);

    List<ItemAlert> findByLevel(AlertLevel level);

    List<ItemAlert> findByStatus(AlertStatus status);

    List<ItemAlert> findByLostItemIdAndStatus(Long lostItemId, AlertStatus status);

    List<ItemAlert> findByPatientIdAndStatus(Long patientId, AlertStatus status);

    List<ItemAlert> findByLevelAndStatus(AlertLevel level, AlertStatus status);

    List<ItemAlert> findByCaregiverId(Long caregiverId);

    List<ItemAlert> findByCaregiverIdAndStatus(Long caregiverId, AlertStatus status);

    boolean existsByLostItemIdAndTitleAndStatusNot(Long lostItemId, String title, AlertStatus status);

    long countByPatientIdAndStatus(Long patientId, AlertStatus status);

    long countByLevelAndStatus(AlertLevel level, AlertStatus status);
}
