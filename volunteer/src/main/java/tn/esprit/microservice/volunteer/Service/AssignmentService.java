package tn.esprit.microservice.volunteer.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.microservice.volunteer.Service.TwilioService;
import tn.esprit.microservice.volunteer.client.UserClient;
import tn.esprit.microservice.volunteer.Entity.*;
import tn.esprit.microservice.volunteer.Repository.AssignmentRepository;
import tn.esprit.microservice.volunteer.Repository.MissionRepository;
import tn.esprit.microservice.volunteer.Repository.VolunteerPresenceRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AssignmentService {

    private static final List<AssignmentStatus> ASSIGNMENT_HISTORY_STATUSES = List.of(
            AssignmentStatus.ASSIGNED,
            AssignmentStatus.IN_PROGRESS,
            AssignmentStatus.COMPLETED);

    private static final List<AssignmentStatus> ACTIVE_ASSIGNMENT_STATUSES = List.of(
            AssignmentStatus.ASSIGNED,
            AssignmentStatus.IN_PROGRESS);

    private static final int MAX_ACTIVE_ASSIGNMENTS = 3;
    private static final String ASSIGNMENT_SMS_TEMPLATE = "New Mission Assigned: %s";

    private final AssignmentRepository assignmentRepository;
    private final MissionRepository missionRepository;
    private final VolunteerPresenceRepository presenceRepository;
    private final UserClient userClient;
    private final TwilioService twilioService;

    // BASIC CRUD

    public List<Assignment> getAllAssignments() {
        return assignmentRepository.findAll();
    }

    public Assignment getAssignmentById(Long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Assignment not found with id: " + id));
    }

    public List<Assignment> getAssignmentsByMission(Long missionId) {
        return assignmentRepository.findByMissionId(missionId);
    }

    public List<Assignment> getAssignmentsByVolunteer(Long volunteerId) {
        return assignmentRepository.findByVolunteerId(volunteerId);
    }

    // MANUAL ASSIGNMENT

    public Assignment createAssignment(Assignment request) {
        if (request == null || request.getMission() == null || request.getMission().getId() == null) {
            throw new IllegalArgumentException("Mission details are required");
        }

        return manualAssign(
                request.getMission().getId(),
                request.getVolunteerId(),
                request.getVolunteerUserId(),
                request.getNotes(),
                request.getFeedback(),
                request.getRating());
    }

    public Assignment manualAssign(Long missionId,
            Long volunteerId,
            Long volunteerUserId,
            String notes,
            String feedback,
            Double rating) {

        if (volunteerId == null) {
            throw new IllegalArgumentException("Volunteer id is required");
        }

        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new IllegalStateException("Mission not found"));

        ensureMissionOpen(mission);
        ensureMissionNotAssigned(missionId);
        ensureVolunteerCapacity(volunteerId);

        Assignment assignment = new Assignment();
        assignment.setMission(mission);
        assignment.setVolunteerId(volunteerId);
        assignment.setVolunteerUserId(Optional.ofNullable(volunteerUserId).orElse(volunteerId));
        assignment.setStatus(AssignmentStatus.ASSIGNED);
        assignment.setAssignedAt(LocalDateTime.now());
        assignment.setNotes(notes);
        assignment.setFeedback(feedback);
        assignment.setRating(rating);

        String name = presenceRepository.findByUserId(volunteerId)
                .map(VolunteerPresence::getDisplayName)
                .orElse(null);

        markMissionAssigned(mission, name);

        log.info("Manual assignment → mission {} assigned to volunteer {}", missionId, volunteerId);

        Assignment saved = assignmentRepository.save(assignment);
        notifyVolunteer(volunteerId, mission);
        return saved;
    }

    // SMART ASSIGNMENT

    public Assignment smartAssign(Long missionId) {
        return smartAssignWithFallback(missionId);
    }

    public Assignment smartAssignWithFallback(Long missionId) {

        Mission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new IllegalStateException("Mission not found"));

        ensureMissionOpen(mission);
        ensureMissionNotAssigned(missionId);

        List<VolunteerPresence> candidates = fetchCandidates(true);

        if (candidates.isEmpty()) {
            candidates = fetchCandidates(false);
        }

        if (candidates.isEmpty()) {
            throw new IllegalStateException("No volunteers available");
        }

        VolunteerPresence best = candidates.stream()
                .max(Comparator.comparingDouble(v -> calculateScore(v, mission)))
                .orElseThrow();

        log.info("Smart assignment → mission {} assigned to volunteer {}", missionId, best.getUserId());

        return assignMissionToVolunteer(mission, best);
    }

    // PRIORITY ASSIGNMENT

    public Assignment assignWithPriority(Mission mission) {

        Priority priority = Optional.ofNullable(mission.getPriority())
                .orElse(Priority.LOW);

        return switch (priority) {
            case HIGH -> fastAssign(mission);
            case MEDIUM -> smartAssign(mission.getId());
            case LOW -> smartAssign(mission.getId());
        };
    }

    // FAST ASSIGNMENT

    public Assignment fastAssign(Mission mission) {

        ensureMissionOpen(mission);
        ensureMissionNotAssigned(mission.getId());

        List<VolunteerPresence> candidates = fetchCandidates(true);

        if (candidates.isEmpty()) {
            candidates = fetchCandidates(false);
        }

        if (candidates.isEmpty()) {
            throw new IllegalStateException("No volunteers available");
        }

        VolunteerPresence selected = candidates.stream()
                .min(Comparator.comparingInt(v -> countActiveMissions(v.getUserId())))
                .orElseThrow();

        log.info("Fast assignment → mission {} assigned to volunteer {}", mission.getId(), selected.getUserId());

        return assignMissionToVolunteer(mission, selected);
    }

    // SCORE CALCULATION

    public double calculateScore(VolunteerPresence v, Mission m) {

        double rating = Optional.ofNullable(
                assignmentRepository.averageRatingForVolunteer(v.getUserId())).orElse(3.5);

        double ratingScore = rating / 5.0;
        double workloadScore = 1d / (1 + countActiveMissions(v.getUserId()));

        double presenceBonus = v.getStatus() == VolunteerStatus.ONLINE ? 0.15 : 0d;

        double priorityBonus = switch (Optional.ofNullable(m.getPriority()).orElse(Priority.LOW)) {
            case HIGH -> 0.1;
            case MEDIUM -> 0.05;
            default -> 0d;
        };

        return (0.6 * ratingScore) + (0.3 * workloadScore) + presenceBonus + priorityBonus;
    }

    // UPDATE & STATUS

    public Assignment updateAssignment(Long id, Assignment updated) {

        Assignment existing = getAssignmentById(id);

        if (updated.getVolunteerId() != null &&
                !updated.getVolunteerId().equals(existing.getVolunteerId())) {

            ensureVolunteerCapacity(updated.getVolunteerId());

            existing.setVolunteerId(updated.getVolunteerId());
            existing.setVolunteerUserId(
                    Optional.ofNullable(updated.getVolunteerUserId())
                            .orElse(updated.getVolunteerId()));
        }

        if (updated.getStatus() != null) {
            applyStatusTransition(existing, updated.getStatus());
        }

        Optional.ofNullable(updated.getNotes()).ifPresent(existing::setNotes);
        Optional.ofNullable(updated.getFeedback()).ifPresent(existing::setFeedback);
        Optional.ofNullable(updated.getRating()).ifPresent(existing::setRating);

        return assignmentRepository.save(existing);
    }

    public Assignment acceptAssignment(Long id) {
        Assignment existing = getAssignmentById(id);
        return updateAssignment(id, statusOnly(existing, AssignmentStatus.IN_PROGRESS));
    }

    public Assignment refuseAssignment(Long id) {
        Assignment existing = getAssignmentById(id);
        return updateAssignment(id, statusOnly(existing, AssignmentStatus.CANCELLED));
    }

    public Assignment completeAssignment(Long id) {
        Assignment existing = getAssignmentById(id);
        return updateAssignment(id, statusOnly(existing, AssignmentStatus.COMPLETED));
    }

    public void deleteAssignment(Long id) {
        assignmentRepository.deleteById(id);
    }

    // HELPERS

    private List<VolunteerPresence> fetchCandidates(boolean onlyOnline) {
        List<VolunteerPresence> base = onlyOnline
                ? presenceRepository.findByStatus(VolunteerStatus.ONLINE)
                : presenceRepository.findAll();

        return base.stream()
                .filter(v -> v.getUserId() != null)
                .filter(v -> countActiveMissions(v.getUserId()) < MAX_ACTIVE_ASSIGNMENTS)
                .collect(Collectors.toList());
    }

    private Assignment assignMissionToVolunteer(Mission mission, VolunteerPresence v) {

        ensureVolunteerCapacity(v.getUserId());

        Assignment a = new Assignment();
        a.setMission(mission);
        a.setVolunteerId(v.getUserId());
        a.setVolunteerUserId(v.getUserId());
        a.setStatus(AssignmentStatus.ASSIGNED);
        a.setAssignedAt(LocalDateTime.now());

        log.info("Assigning mission {} to volunteer {}", mission.getId(), v.getUserId());
        markMissionAssigned(mission, v.getDisplayName());
        Assignment saved = assignmentRepository.save(a);
        notifyVolunteer(v.getUserId(), mission);
        log.info("Notification triggered for volunteer {}", v.getUserId());
        return saved;
    }

    public void notifyVolunteer(Long volunteerId, Mission mission) {
        if (volunteerId == null) {
            return;
        }

        presenceRepository.findByUserId(volunteerId)
                .ifPresent(presence -> {
                    String phone = presence.getPhoneNumber();
                    if (phone != null && !phone.isEmpty()) {
                        String message = "Hello, you have been assigned a new mission. Please check your application.";
                        if (mission != null && mission.getTitle() != null) {
                            message = "Hello, you have been assigned to mission: " + mission.getTitle()
                                    + ". Please check your application.";
                        }

                        Priority priority = (mission != null && mission.getPriority() != null)
                                ? mission.getPriority()
                                : Priority.LOW;

                        if (priority == Priority.HIGH) {
                            log.info("Priority is HIGH, triggering voice call for volunteer {}", volunteerId);
                            twilioService.makeCall(phone, message);
                        } else {
                            log.info("Priority is {}, sending SMS for volunteer {}", priority, volunteerId);
                            twilioService.sendSms(phone, message);
                        }
                    } else {
                        log.warn("Volunteer {} has no phone number saved. Notification skipped.", volunteerId);
                    }
                });
    }

    private int countActiveMissions(Long volunteerId) {
        return assignmentRepository.countByVolunteerIdAndStatusIn(
                volunteerId, ACTIVE_ASSIGNMENT_STATUSES);
    }

    private void ensureMissionOpen(Mission m) {
        if (m.getStatus() != MissionStatus.OPEN) {
            throw new IllegalStateException("Mission not open");
        }
    }

    private void ensureMissionNotAssigned(Long missionId) {
        if (assignmentRepository.existsByMissionIdAndStatusIn(
                missionId, ASSIGNMENT_HISTORY_STATUSES)) {
            throw new IllegalStateException("Mission already assigned");
        }
    }

    private void ensureVolunteerCapacity(Long volunteerId) {
        if (countActiveMissions(volunteerId) >= MAX_ACTIVE_ASSIGNMENTS) {
            throw new IllegalStateException("Volunteer overloaded");
        }
    }

    private void markMissionAssigned(Mission m, String name) {
        // Use IN_PROGRESS to stay compatible with DB enum values.
        // Frontend already maps IN_PROGRESS -> "Assigned".
        m.setStatus(MissionStatus.IN_PROGRESS);
        if (name != null)
            m.setAssignee(name);
        missionRepository.save(m);
    }

    private void applyStatusTransition(Assignment a, AssignmentStatus s) {

        if (a.getStatus() == s)
            return;

        a.setStatus(s);

        LocalDateTime now = LocalDateTime.now();

        if (s == AssignmentStatus.IN_PROGRESS) {
            a.setStartedAt(now);
            updateMissionStatus(a, MissionStatus.IN_PROGRESS);
        }

        if (s == AssignmentStatus.COMPLETED) {
            a.setCompletedAt(now);
            updateMissionStatus(a, MissionStatus.COMPLETED);
        }

        if (s == AssignmentStatus.CANCELLED) {
            updateMissionStatus(a, MissionStatus.OPEN);
            if (a.getMission() != null) {
                a.getMission().setAssignee(null);
                missionRepository.save(a.getMission());
            }
        }
    }

    private Assignment statusOnly(Assignment source, AssignmentStatus status) {
        Assignment updated = new Assignment();
        updated.setStatus(status);
        updated.setVolunteerId(source.getVolunteerId());
        updated.setVolunteerUserId(source.getVolunteerUserId());
        updated.setNotes(source.getNotes());
        updated.setFeedback(source.getFeedback());
        updated.setRating(source.getRating());
        return updated;
    }

    private void updateMissionStatus(Assignment a, MissionStatus s) {
        if (a.getMission() == null)
            return;
        a.getMission().setStatus(s);
        missionRepository.save(a.getMission());
    }
}
