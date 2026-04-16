package tn.esprit.microservice.volunteer.Repository;

import tn.esprit.microservice.volunteer.Entity.VolunteerPresence;
import tn.esprit.microservice.volunteer.Entity.VolunteerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VolunteerPresenceRepository extends JpaRepository<VolunteerPresence, Long> {

    Optional<VolunteerPresence> findByUserId(Long userId);

    Optional<VolunteerPresence> findBySessionId(String sessionId);

    List<VolunteerPresence> findByStatus(VolunteerStatus status);

    List<VolunteerPresence> findByStatusIn(List<VolunteerStatus> statuses);

    /** Count volunteers currently online or away. */
    long countByStatusIn(List<VolunteerStatus> statuses);

    /**
     * Find all volunteers whose last heartbeat is before the given threshold
     * AND are still marked as ONLINE or AWAY (i.e. stale sessions).
     */
    @Query("SELECT vp FROM VolunteerPresence vp " +
            "WHERE vp.status IN :activeStatuses " +
            "AND vp.lastHeartbeat < :threshold")
    List<VolunteerPresence> findStalePresences(
            @Param("activeStatuses") List<VolunteerStatus> activeStatuses,
            @Param("threshold") LocalDateTime threshold);

    /**
     * Bulk-update stale sessions to OFFLINE for performance
     * when there are many volunteers.
     */
    @Modifying
    @Query("UPDATE VolunteerPresence vp SET vp.status = :offlineStatus, " +
            "vp.disconnectedAt = :now, vp.sessionId = null " +
            "WHERE vp.status IN :activeStatuses " +
            "AND vp.lastHeartbeat < :threshold")
    int markStaleAsOffline(
            @Param("offlineStatus") VolunteerStatus offlineStatus,
            @Param("activeStatuses") List<VolunteerStatus> activeStatuses,
            @Param("threshold") LocalDateTime threshold,
            @Param("now") LocalDateTime now);
}
