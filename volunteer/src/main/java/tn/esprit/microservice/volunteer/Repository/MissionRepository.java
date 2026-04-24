package tn.esprit.microservice.volunteer.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.microservice.volunteer.Entity.Mission;
import tn.esprit.microservice.volunteer.Entity.MissionStatus;

import java.util.Date;
import java.util.List;

@Repository
public interface MissionRepository extends JpaRepository<Mission, Long> {

    List<Mission> findByStatus(MissionStatus status);

    List<Mission> findByEndDateBeforeAndStatus(Date deadline, MissionStatus status);
}
