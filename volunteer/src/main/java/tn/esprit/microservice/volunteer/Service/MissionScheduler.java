package tn.esprit.microservice.volunteer.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tn.esprit.microservice.volunteer.Entity.Mission;
import tn.esprit.microservice.volunteer.Entity.MissionStatus;
import tn.esprit.microservice.volunteer.Repository.MissionRepository;

import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MissionScheduler {

    private final MissionRepository missionRepository;

    @Scheduled(fixedRate = 60000)
    public void cancelExpiredMissions() {
        List<Mission> expired = missionRepository.findByEndDateBeforeAndStatus(new Date(), MissionStatus.OPEN);
        if (!expired.isEmpty()) {
            expired.forEach(m -> m.setStatus(MissionStatus.CANCELLED));
            missionRepository.saveAll(expired);
        }
    }
}
