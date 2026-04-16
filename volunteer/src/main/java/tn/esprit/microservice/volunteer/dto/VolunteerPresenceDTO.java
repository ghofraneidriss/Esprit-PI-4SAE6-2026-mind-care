package tn.esprit.microservice.volunteer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.microservice.volunteer.Entity.VolunteerStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VolunteerPresenceDTO {
    private Long userId;
    private String displayName;
    private VolunteerStatus status;
    private LocalDateTime lastHeartbeat;
    private LocalDateTime connectedAt;
    private LocalDateTime disconnectedAt;
    private Long totalOnlineSeconds;
    private String phoneNumber;
}
