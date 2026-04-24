package tn.esprit.microservice.volunteer.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import tn.esprit.microservice.volunteer.Service.VolunteerPresenceService;
import tn.esprit.microservice.volunteer.dto.VolunteerSessionRequest;

@Controller
@RequiredArgsConstructor
public class PresenceSocketController {

    private final VolunteerPresenceService presenceService;

    @MessageMapping("/presence.connect")
    public void connect(VolunteerSessionRequest request) {
        if (request != null && request.getUserId() != null) {
            presenceService.markOnline(request.getUserId(), request);
        }
    }

    @MessageMapping("/presence.disconnect")
    public void disconnect(VolunteerSocketPayload payload) {
        if (payload != null && payload.userId() != null) {
            presenceService.markOffline(payload.userId());
        }
    }

    @MessageMapping("/presence.heartbeat")
    public void heartbeat(VolunteerSocketPayload payload) {
        if (payload != null && payload.userId() != null) {
            presenceService.heartbeat(payload.userId());
        }
    }
    public record VolunteerSocketPayload(Long userId) {
    }
}
