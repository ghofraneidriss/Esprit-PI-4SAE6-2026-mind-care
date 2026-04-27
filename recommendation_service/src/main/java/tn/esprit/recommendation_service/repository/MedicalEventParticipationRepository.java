package tn.esprit.recommendation_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.recommendation_service.entity.MedicalEventParticipation;
import tn.esprit.recommendation_service.enums.ParticipantType;
import tn.esprit.recommendation_service.repository.projection.ParticipantRankingProjection;

import java.time.LocalDate;
import java.util.List;

public interface MedicalEventParticipationRepository extends JpaRepository<MedicalEventParticipation, Long> {

    boolean existsByMedicalEventIdAndParticipantIdAndParticipantType(Long medicalEventId, Long participantId, ParticipantType participantType);

    boolean existsByMedicalEventIdAndParticipantIdAndParticipantTypeAndParticipationDate(
            Long medicalEventId,
            Long participantId,
            ParticipantType participantType,
            LocalDate participationDate
    );

    List<MedicalEventParticipation> findByMedicalEventIdAndParticipantIdAndParticipantTypeOrderByParticipationDateDesc(
            Long medicalEventId,
            Long participantId,
            ParticipantType participantType
    );

    @Query("""
            SELECT COALESCE(SUM(p.score), 0)
            FROM MedicalEventParticipation p
            WHERE p.medicalEvent.id = :medicalEventId
              AND p.participantId = :participantId
              AND p.participantType = :participantType
            """)
    Integer sumScoreByEventAndParticipant(
            @Param("medicalEventId") Long medicalEventId,
            @Param("participantId") Long participantId,
            @Param("participantType") ParticipantType participantType
    );

    @Query("""
            SELECT DISTINCT p.participationDate
            FROM MedicalEventParticipation p
            WHERE p.medicalEvent.id = :medicalEventId
              AND p.participantId = :participantId
              AND p.participantType = :participantType
            ORDER BY p.participationDate DESC
            """)
    List<LocalDate> findDistinctParticipationDates(
            @Param("medicalEventId") Long medicalEventId,
            @Param("participantId") Long participantId,
            @Param("participantType") ParticipantType participantType
    );

    @Query("""
            SELECT
              p.participantId AS participantId,
              p.participantType AS participantType,
              COALESCE(SUM(p.score), 0) AS totalScore,
              COUNT(p.id) AS participations
            FROM MedicalEventParticipation p
            WHERE p.medicalEvent.id = :medicalEventId
            GROUP BY p.participantId, p.participantType
            ORDER BY COALESCE(SUM(p.score), 0) DESC, COUNT(p.id) DESC
            """)
    List<ParticipantRankingProjection> rankingByMedicalEvent(@Param("medicalEventId") Long medicalEventId);
}
