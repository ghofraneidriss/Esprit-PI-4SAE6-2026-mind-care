package tn.esprit.microservice.volunteer.Entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VolunteerPresence {

    @Id
    private Long userId;

    @Enumerated(EnumType.STRING)
    private VolunteerStatus status;

    private String displayName;
    private String sessionId;

    private LocalDateTime lastHeartbeat;
    private LocalDateTime connectedAt;
    private LocalDateTime disconnectedAt;

    @Column(nullable = false)
    @Builder.Default
    private Long totalOnlineSeconds = 0L;

    /** Twilio phone number for voice calls. */
    private String phoneNumber;
}
