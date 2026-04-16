package tn.esprit.microservice.volunteer.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class VolunteerSessionRequest {
    private Long userId;
    private String displayName;
    private String sessionId;
    private String ipAddress;
    private String userAgent;
}
