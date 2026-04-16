package tn.esprit.microservice.volunteer.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.microservice.volunteer.Service.VolunteerPresenceService;
import tn.esprit.microservice.volunteer.dto.VolunteerPresenceDTO;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/volunteer/presence")
@RequiredArgsConstructor
public class PresenceController {

    private final VolunteerPresenceService presenceService;

    @GetMapping
    public List<VolunteerPresenceDTO> getAllPresences() {
        return presenceService.getAllPresences();
    }

    @GetMapping("/all")
    public List<VolunteerPresenceDTO> getAllPresencesAlias() {
        return presenceService.getAllPresences();
    }

    @GetMapping("/active")
    public List<VolunteerPresenceDTO> getActivePresences() {
        return presenceService.getActivePresences();
    }

    @GetMapping("/online")
    public List<VolunteerPresenceDTO> getOnlineVolunteers() {
        return presenceService.getOnlineVolunteers();
    }

    @GetMapping("/active/count")
    public Map<String, Long> getActiveCount() {
        return Map.of("activeCount", presenceService.getActiveCount());
    }

    @GetMapping("/summary")
    public Map<String, Long> getStatusSummary() {
        return presenceService.getStatusSummary();
    }

    @GetMapping("/{userId}")
    public VolunteerPresenceDTO getPresenceByUserId(@PathVariable Long userId) {
        return presenceService.getPresenceByUserId(userId);
    }
}
