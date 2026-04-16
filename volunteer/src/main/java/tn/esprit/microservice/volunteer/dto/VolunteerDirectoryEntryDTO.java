package tn.esprit.microservice.volunteer.dto;

import java.util.List;

public record VolunteerDirectoryEntryDTO(
        Long userId,
        String initials,
        String name,
        String email,
        boolean verified,
        String city,
        String availability,
        List<String> skills,
        double rating,
        int missions,
        String avatarColor,
        String onlineStatus,
        String lastSeen
) {
}
