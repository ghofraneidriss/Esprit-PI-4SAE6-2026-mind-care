package tn.esprit.microservice.volunteer.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.microservice.volunteer.Entity.Mission;
import tn.esprit.microservice.volunteer.Entity.MissionStatus;
import tn.esprit.microservice.volunteer.Service.MissionService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/volunteer/missions")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class MissionController {

    private final MissionService missionService;

    /** GET /api/volunteer/missions */
    @GetMapping
    public List<Mission> getAllMissions() {
        return missionService.getAllMissions();
    }

    /** GET /api/volunteer/missions/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<Mission> getMissionById(@PathVariable Long id) {
        return ResponseEntity.ok(missionService.getMissionById(id));
    }

    /** GET /api/volunteer/missions/status/{status} */
    @GetMapping("/status/{status}")
    public List<Mission> getMissionsByStatus(@PathVariable MissionStatus status) {
        return missionService.getMissionsByStatus(status);
    }

    /** POST /api/volunteer/missions */
    @PostMapping
    public ResponseEntity<Mission> createMission(@RequestBody Mission mission) {
        return ResponseEntity.ok(missionService.createMission(mission));
    }

    /** PUT /api/volunteer/missions/{id} */
    @PutMapping("/{id}")
    public ResponseEntity<Mission> updateMission(@PathVariable Long id, @RequestBody Mission mission) {
        return ResponseEntity.ok(missionService.updateMission(id, mission));
    }

    /** DELETE /api/volunteer/missions/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMission(@PathVariable Long id) {
        missionService.deleteMission(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/volunteer/missions/{id}/assign
     * Body: { "volunteerName": "John Doe" }
     */
    @PostMapping("/{id}/assign")
    public ResponseEntity<Mission> assignVolunteer(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String volunteerName = body.get("volunteerName");
        return ResponseEntity.ok(missionService.assignVolunteer(id, volunteerName));
    }
}
