package tn.esprit.activities_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.activities_service.entity.GameResult;

import java.util.List;

@Repository
public interface GameResultRepository extends JpaRepository<GameResult, Long> {
    
    List<GameResult> findByPatientId(Long patientId);
    
    List<GameResult> findByActivityTypeAndActivityId(String activityType, Long activityId);
    
    List<GameResult> findByPatientIdAndActivityType(Long patientId, String activityType);
    
    @Query("SELECT g FROM GameResult g WHERE g.patientId = :patientId ORDER BY g.completedAt DESC")
    List<GameResult> findByPatientIdOrderByCompletedAtDesc(Long patientId);
    
    @Query("SELECT COUNT(g) FROM GameResult g WHERE g.patientId = :patientId AND g.activityType = :activityType")
    Long countByPatientIdAndActivityType(Long patientId, String activityType);
    
    @Query("SELECT AVG(g.score) FROM GameResult g WHERE g.patientId = :patientId AND g.activityType = :activityType")
    Double getAverageScoreByPatientAndActivityType(Long patientId, String activityType);

    @Query("SELECT CASE WHEN COUNT(g) > 0 THEN true ELSE false END FROM GameResult g WHERE g.patientId = :patientId AND UPPER(TRIM(g.activityType)) = :activityType AND g.activityId = :activityId")
    boolean existsByPatientAndActivityCaseInsensitive(
            @Param("patientId") Long patientId,
            @Param("activityType") String activityType,
            @Param("activityId") Long activityId);
}
