package tn.esprit.microservice.volunteer.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.microservice.volunteer.Entity.Mission;
import tn.esprit.microservice.volunteer.Entity.MissionStatus;
import tn.esprit.microservice.volunteer.Repository.MissionRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MissionService {

    private final MissionRepository missionRepository;

    public List<Mission> getAllMissions() {
        return missionRepository.findAll();
    }

    public Mission getMissionById(Long id) {
        return missionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mission not found with id: " + id));
    }

    public List<Mission> getMissionsByStatus(MissionStatus status) {
        return missionRepository.findByStatus(status);
    }

    public Mission createMission(Mission mission) {
        if (mission.getStatus() == null) {
            mission.setStatus(MissionStatus.OPEN);
        }
        return missionRepository.save(mission);
    }

    public Mission updateMission(Long id, Mission updated) {
        Mission existing = getMissionById(id);
        existing.setTitle(updated.getTitle());
        existing.setDescription(updated.getDescription());
        existing.setLocation(updated.getLocation());
        existing.setCategory(updated.getCategory());
        existing.setAssignee(updated.getAssignee());
        existing.setDuration(updated.getDuration());
        existing.setPriority(updated.getPriority());
        existing.setStartDate(updated.getStartDate());
        existing.setEndDate(updated.getEndDate());
        existing.setRequiredVolunteers(updated.getRequiredVolunteers());
        existing.setStatus(updated.getStatus());
        existing.setType(updated.getType());
        return missionRepository.save(existing);
    }

    public void deleteMission(Long id) {
        missionRepository.deleteById(id);
    }

    /** Assign a volunteer name to a mission and mark it ASSIGNED */
    public Mission assignVolunteer(Long missionId, String volunteerName) {
        Mission mission = getMissionById(missionId);
        mission.setAssignee(volunteerName);
        mission.setStatus(MissionStatus.IN_PROGRESS);
        return missionRepository.save(mission);
    }

}
