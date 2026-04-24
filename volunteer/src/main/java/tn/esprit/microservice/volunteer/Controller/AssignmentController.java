package tn.esprit.microservice.volunteer.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.microservice.volunteer.Entity.Assignment;
import tn.esprit.microservice.volunteer.Service.AssignmentService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/volunteer/assignments")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    @GetMapping
    public List<Assignment> getAllAssignments() {
        return assignmentService.getAllAssignments();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Assignment> getAssignmentById(@PathVariable Long id) {
        return ResponseEntity.ok(assignmentService.getAssignmentById(id));
    }

    @GetMapping("/mission/{missionId}")
    public List<Assignment> getAssignmentsByMission(@PathVariable Long missionId) {
        return assignmentService.getAssignmentsByMission(missionId);
    }

    @GetMapping("/volunteer/{volunteerId}")
    public List<Assignment> getAssignmentsByVolunteer(@PathVariable Long volunteerId) {
        return assignmentService.getAssignmentsByVolunteer(volunteerId);
    }

    @PostMapping
    public ResponseEntity<?> createAssignment(@RequestBody Assignment assignment) {
        try {
            return ResponseEntity.ok(assignmentService.createAssignment(assignment));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Assignment> updateAssignment(@PathVariable Long id, @RequestBody Assignment assignment) {
        return ResponseEntity.ok(assignmentService.updateAssignment(id, assignment));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<Assignment> acceptAssignment(@PathVariable Long id) {
        return ResponseEntity.ok(assignmentService.acceptAssignment(id));
    }

    @PostMapping("/{id}/refuse")
    public ResponseEntity<Assignment> refuseAssignment(@PathVariable Long id) {
        return ResponseEntity.ok(assignmentService.refuseAssignment(id));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<Assignment> completeAssignment(@PathVariable Long id) {
        return ResponseEntity.ok(assignmentService.completeAssignment(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAssignment(@PathVariable Long id) {
        assignmentService.deleteAssignment(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/test-notification/{volunteerId}")
    public ResponseEntity<String> testNotification(@PathVariable Long volunteerId) {
        assignmentService.notifyVolunteer(volunteerId, null);
        return ResponseEntity.ok("Notification sent successfully");
    }
}
