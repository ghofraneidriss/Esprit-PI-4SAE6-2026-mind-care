package tn.esprit.microservice.volunteer.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.microservice.volunteer.Entity.Assignment;
import tn.esprit.microservice.volunteer.Entity.AssignmentStatus;

import java.util.Collection;
import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findByMissionId(Long missionId);

    List<Assignment> findByVolunteerId(Long volunteerId);

    boolean existsByMissionIdAndStatusIn(Long missionId, Collection<AssignmentStatus> statuses);

    int countByVolunteerIdAndStatusIn(Long volunteerId, Collection<AssignmentStatus> statuses);

    int countByVolunteerId(Long volunteerId);

    @Query("SELECT AVG(a.rating) FROM Assignment a WHERE a.volunteerId = :volunteerId AND a.rating IS NOT NULL")
    Double averageRatingForVolunteer(@Param("volunteerId") Long volunteerId);
}
