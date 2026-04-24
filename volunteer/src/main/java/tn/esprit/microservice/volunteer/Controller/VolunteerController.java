package tn.esprit.microservice.volunteer.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.microservice.volunteer.Entity.Assignment;
import tn.esprit.microservice.volunteer.Service.AssignmentService;
import tn.esprit.microservice.volunteer.Service.VolunteerDirectoryService;
import tn.esprit.microservice.volunteer.dto.VolunteerDirectoryEntryDTO;
import tn.esprit.microservice.volunteer.dto.VolunteerSessionRequest;
import tn.esprit.microservice.volunteer.Service.VolunteerPresenceService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/volunteers")
@RequiredArgsConstructor
public class VolunteerController {

    private final VolunteerDirectoryService directoryService;
    private final VolunteerPresenceService presenceService;
    private final AssignmentService assignmentService;

    // Directory

    @GetMapping("/directory")
    public List<VolunteerDirectoryEntryDTO> getDirectory() {
        return directoryService.getDirectoryEntries();
    }

    @GetMapping("/{id}")
    public VolunteerDirectoryEntryDTO getVolunteer(@PathVariable("id") Long id) {
        return directoryService.getVolunteer(id);
    }

    // Phone Number Registration

    @PostMapping("/phone")
    public ResponseEntity<Void> savePhoneNumber(@RequestBody Map<String, Object> payload) {
        Long userId = ((Number) payload.get("userId")).longValue();
        String phoneNumber = (String) payload.get("phone");
        directoryService.updatePhoneNumber(userId, phoneNumber);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/phone")
    public ResponseEntity<Void> updatePhoneNumber(
            @PathVariable("id") Long id,
            @RequestBody Map<String, String> payload) {
        directoryService.updatePhoneNumber(id, payload.get("phone"));
        return ResponseEntity.ok().build();
    }

    // Session Tracking

    @PostMapping("/{id}/login")
    public ResponseEntity<Void> markOnline(
            @PathVariable("id") Long id,
            @RequestBody(required = false) VolunteerSessionRequest request) {
        presenceService.markOnline(id, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/logout")
    public ResponseEntity<Void> markOffline(@PathVariable("id") Long id) {
        presenceService.markOffline(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/heartbeat/{id}")
    public ResponseEntity<Void> heartbeat(@PathVariable("id") Long id) {
        presenceService.heartbeat(id);
        return ResponseEntity.ok().build();
    }

    // Public assignment route used by frontend "Assign and notify" flow.
    // It reuses AssignmentService manual assignment logic and triggers Twilio notify.
    @PostMapping("/assign-and-notify")
    public ResponseEntity<?> assignAndNotify(@RequestBody Map<String, Object> payload) {
        try {
            Long missionId = ((Number) payload.get("missionId")).longValue();
            Long volunteerId = ((Number) payload.get("volunteerId")).longValue();
            String notes = payload.get("notes") == null ? "" : String.valueOf(payload.get("notes"));

            Assignment assignment = assignmentService.manualAssign(
                    missionId,
                    volunteerId,
                    volunteerId,
                    notes,
                    null,
                    null);

            return ResponseEntity.ok(assignment);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }
}
