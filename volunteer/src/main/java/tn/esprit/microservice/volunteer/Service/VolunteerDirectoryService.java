package tn.esprit.microservice.volunteer.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.microservice.volunteer.Entity.VolunteerPresence;
import tn.esprit.microservice.volunteer.Entity.VolunteerStatus;
import tn.esprit.microservice.volunteer.Repository.AssignmentRepository;
import tn.esprit.microservice.volunteer.Repository.VolunteerPresenceRepository;
import tn.esprit.microservice.volunteer.client.UserClient;
import tn.esprit.microservice.volunteer.dto.VolunteerDirectoryEntryDTO;
import tn.esprit.microservice.volunteer.exception.VolunteerNotFoundException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VolunteerDirectoryService {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final List<List<String>> SKILL_BUCKETS = List.of(
            List.of("Patient Care", "Companionship"),
            List.of("Transportation", "Medication Pickup"),
            List.of("Phone Support", "Nutrition Coaching"),
            List.of("Administrative Support", "Workshop Facilitation"));

    private static final List<String> CITY_BUCKETS = List.of(
            "Tunis", "Sousse", "Monastir", "Nabeul", "Sfax", "Bizerte");

    private static final List<String> AVATAR_COLORS = List.of(
            "#3b82f6", "#ef4444", "#f59e0b", "#14b8a6", "#8b5cf6", "#ec4899");

    private final UserClient userClient;
    private final VolunteerPresenceRepository presenceRepository;
    private final AssignmentRepository assignmentRepository;

    public List<VolunteerDirectoryEntryDTO> getDirectoryEntries() {
        Map<Long, VolunteerPresence> presenceMap = presenceRepository.findAll()
                .stream()
                .filter(presence -> presence.getUserId() != null)
                .collect(Collectors.toMap(VolunteerPresence::getUserId, Function.identity()));

        return userClient.fetchVolunteers()
                .stream()
                .map(user -> toEntry(user, presenceMap.get(user.userId())))
                .collect(Collectors.toList());
    }

    public VolunteerDirectoryEntryDTO getVolunteer(Long volunteerId) {
        VolunteerPresence presence = presenceRepository.findByUserId(volunteerId).orElse(null);
        UserClient.UserSummary summary = userClient.fetchVolunteerById(volunteerId)
                .orElseThrow(() -> new VolunteerNotFoundException(volunteerId));
        return toEntry(summary, presence);
    }

    public void updatePhoneNumber(Long userId, String phoneNumber) {
        VolunteerPresence presence = presenceRepository.findByUserId(userId)
                .orElseGet(() -> VolunteerPresence.builder()
                        .userId(userId)
                        .status(VolunteerStatus.OFFLINE)
                        .build());
        presence.setPhoneNumber(phoneNumber);
        presenceRepository.save(presence);
    }

    private VolunteerDirectoryEntryDTO toEntry(UserClient.UserSummary summary, VolunteerPresence presence) {
        double rating = Optional.ofNullable(assignmentRepository.averageRatingForVolunteer(summary.userId()))
                .orElse(0d);
        int missions = assignmentRepository.countByVolunteerId(summary.userId());

        return new VolunteerDirectoryEntryDTO(
                summary.userId(),
                buildInitials(summary.displayName()),
                summary.displayName(),
                summary.normalizedEmail(),
                summary.isVerified(),
                pickCity(summary.userId()),
                computeAvailability(presence),
                pickSkills(summary.userId()),
                roundRating(rating),
                missions,
                pickColor(summary.userId()),
                presence == null ? VolunteerStatus.OFFLINE.name() : presence.getStatus().name(),
                formatTimestamp(presence));
    }

    private List<String> pickSkills(Long userId) {
        int index = Math.floorMod(userId.hashCode(), SKILL_BUCKETS.size());
        return new ArrayList<>(SKILL_BUCKETS.get(index));
    }

    private String pickCity(Long userId) {
        int index = Math.floorMod(userId.hashCode(), CITY_BUCKETS.size());
        return CITY_BUCKETS.get(index);
    }

    private String pickColor(Long userId) {
        int index = Math.floorMod(userId.hashCode(), AVATAR_COLORS.size());
        return AVATAR_COLORS.get(index);
    }

    private String buildInitials(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            return "";
        }
        String[] parts = displayName.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            initials.append(Character.toUpperCase(part.charAt(0)));
            if (initials.length() == 2) {
                break;
            }
        }
        return initials.toString();
    }

    private String computeAvailability(VolunteerPresence presence) {
        if (presence == null) {
            return "Offline";
        }
        return switch (presence.getStatus()) {
            case ONLINE -> "Available now";
            case AWAY -> "Away";
            default -> "Offline";
        };
    }

    private String formatTimestamp(VolunteerPresence presence) {
        if (presence == null) {
            return null;
        }
        LocalDateTime timestamp = presence.getDisconnectedAt() != null
                ? presence.getDisconnectedAt()
                : presence.getLastHeartbeat();
        return timestamp == null ? null : timestamp.format(TIMESTAMP_FORMATTER);
    }

    private double roundRating(double rating) {
        return Math.round(rating * 10d) / 10d;
    }
}
