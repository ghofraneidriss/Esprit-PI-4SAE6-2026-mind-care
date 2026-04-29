package tn.esprit.lost_item_service.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.lost_item_service.entity.ItemCategory;
import tn.esprit.lost_item_service.entity.ItemPriority;
import tn.esprit.lost_item_service.entity.ItemStatus;
import tn.esprit.lost_item_service.entity.LostItem;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LostItemRepository extends JpaRepository<LostItem, Long> {

    // ── Paginated patient listing ─────────────────────────────────────────────

    Page<LostItem> findByPatientIdOrderByCreatedAtDesc(Long patientId, Pageable pageable);

    Page<LostItem> findByPatientIdAndStatusOrderByCreatedAtDesc(Long patientId, ItemStatus status, Pageable pageable);

    Page<LostItem> findByPatientIdAndCategoryOrderByCreatedAtDesc(Long patientId, ItemCategory category, Pageable pageable);

    Page<LostItem> findByPatientIdAndStatusAndCategoryOrderByCreatedAtDesc(Long patientId, ItemStatus status, ItemCategory category, Pageable pageable);

    // ── Critical items ────────────────────────────────────────────────────────

    @Query("SELECT l FROM LostItem l WHERE l.patientId = :patientId AND (l.priority = :priority OR l.status = :status)")
    List<LostItem> findCriticalByPatientId(
            @Param("patientId") Long patientId,
            @Param("priority") ItemPriority priority,
            @Param("status") ItemStatus status
    );

    // ── Patient flat listing (for risk/trend analysis) ────────────────────────

    List<LostItem> findByPatientId(Long patientId);

    List<LostItem> findByCaregiverIdOrderByCreatedAtDesc(Long caregiverId);

    List<LostItem> findByCaregiverIdAndStatusOrderByCreatedAtDesc(Long caregiverId, ItemStatus status);

    List<LostItem> findByPatientIdAndStatus(Long patientId, ItemStatus status);

    // ── Status-based listing ──────────────────────────────────────────────────

    List<LostItem> findByStatus(ItemStatus status);

    long countByPatientIdAndStatus(Long patientId, ItemStatus status);

    long countByPatientIdAndCategory(Long patientId, ItemCategory category);

    // ── Scheduler queries ─────────────────────────────────────────────────────

    @Query("SELECT l FROM LostItem l WHERE l.status = :status AND l.priority = :priority AND l.createdAt < :cutoff")
    List<LostItem> findStaleItemsForEscalation(
            @Param("status") ItemStatus status,
            @Param("priority") ItemPriority priority,
            @Param("cutoff") LocalDateTime cutoff
    );

    // ── Trend analysis ────────────────────────────────────────────────────────

    @Query("SELECT l FROM LostItem l WHERE l.patientId = :patientId AND l.createdAt >= :from AND l.createdAt < :to")
    List<LostItem> findByPatientIdAndCreatedAtBetween(
            @Param("patientId") Long patientId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    // ── Category queries ──────────────────────────────────────────────────────

    List<LostItem> findByCategory(ItemCategory category);

    List<LostItem> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    // ── Global statistics ─────────────────────────────────────────────────────

    @Query("SELECT l.status, COUNT(l) FROM LostItem l GROUP BY l.status")
    List<Object[]> countGroupedByStatus();

    @Query("SELECT l.category, COUNT(l) FROM LostItem l GROUP BY l.category")
    List<Object[]> countGroupedByCategory();

    @Query("SELECT l.priority, COUNT(l) FROM LostItem l GROUP BY l.priority")
    List<Object[]> countGroupedByPriority();

    @Query("SELECT l.patientId, COUNT(l) FROM LostItem l GROUP BY l.patientId")
    List<Object[]> countGroupedByPatient();
}
