package tn.esprit.recommendation_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.recommendation_service.entity.Recommendation;
import tn.esprit.recommendation_service.enums.RecommendationStatus;
import tn.esprit.recommendation_service.repository.projection.RecommendationStatusCountProjection;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    List<Recommendation> findByPatientId(Long patientId);

    List<Recommendation> findByPatientIdOrderByPriorityDescCreatedAtDesc(Long patientId);

    List<Recommendation> findByPatientIdAndStatusOrderByPriorityDescCreatedAtDesc(Long patientId, RecommendationStatus status);

    @Query("""
            SELECT r
            FROM Recommendation r
            WHERE r.patientId = :patientId
              AND r.status = tn.esprit.recommendation_service.enums.RecommendationStatus.ACTIVE
              AND r.dismissed = false
              AND (r.expirationDate IS NULL OR r.expirationDate >= :today)
            ORDER BY r.priority DESC, r.createdAt DESC
            """)
    List<Recommendation> findActiveByPatientId(@Param("patientId") Long patientId, @Param("today") LocalDate today);

    @Query("""
            SELECT
                SUM(CASE WHEN r.status = tn.esprit.recommendation_service.enums.RecommendationStatus.ACCEPTED THEN 1 ELSE 0 END) AS acceptedCount,
                SUM(CASE WHEN r.status = tn.esprit.recommendation_service.enums.RecommendationStatus.REJECTED OR r.dismissed = true THEN 1 ELSE 0 END) AS rejectedCount
            FROM Recommendation r
            WHERE r.patientId = :patientId
            """)
    RecommendationStatusCountProjection countAcceptedAndRejectedByPatientId(@Param("patientId") Long patientId);

    List<Recommendation> findByContentContainingIgnoreCase(String query);

    @Query("""
            SELECT COALESCE(SUM(COALESCE(r.rejectionCount, 0)), 0)
            FROM Recommendation r
            WHERE r.patientId = :patientId
              AND r.type = :type
            """)
    long sumRejectionCountByPatientIdAndType(@Param("patientId") Long patientId, @Param("type") tn.esprit.recommendation_service.enums.RecommendationType type);

    Optional<Recommendation> findTopByPatientIdAndTypeOrderByCreatedAtDesc(Long patientId, tn.esprit.recommendation_service.enums.RecommendationType type);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Recommendation r
            SET r.status = tn.esprit.recommendation_service.enums.RecommendationStatus.EXPIRED
            WHERE r.status <> tn.esprit.recommendation_service.enums.RecommendationStatus.EXPIRED
              AND r.expirationDate IS NOT NULL
              AND r.expirationDate < :today
            """)
    int archiveExpiredRecommendations(@Param("today") LocalDate today);
}
