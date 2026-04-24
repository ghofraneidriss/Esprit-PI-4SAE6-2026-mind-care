package tn.esprit.microservice.volunteer.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.microservice.volunteer.Entity.VolunteerPresence;
import tn.esprit.microservice.volunteer.Entity.VolunteerStatus;
import tn.esprit.microservice.volunteer.Repository.VolunteerPresenceRepository;
import tn.esprit.microservice.volunteer.dto.VolunteerPresenceDTO;
import tn.esprit.microservice.volunteer.dto.VolunteerSessionRequest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class VolunteerPresenceService {

    private static final List<VolunteerStatus> ACTIVE_STATUSES = List.of(VolunteerStatus.ONLINE, VolunteerStatus.AWAY);
    private static final long SESSION_TTL_SECONDS = 90L;

    private final VolunteerPresenceRepository presenceRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public void markOnline(Long userId, VolunteerSessionRequest request) {
        VolunteerPresence presence = presenceRepository.findByUserId(userId)
                .orElseGet(() -> VolunteerPresence.builder().userId(userId).build());

        LocalDateTime now = LocalDateTime.now();
        presence.setStatus(VolunteerStatus.ONLINE);
        presence.setDisplayName(resolveDisplayName(presence, request));
        presence.setSessionId(resolveSessionId(request));
        presence.setConnectedAt(now);
        presence.setDisconnectedAt(null);
        presence.setLastHeartbeat(now);
        presenceRepository.save(presence);
        publishPresence(presence);
    }

    public void heartbeat(Long userId) {
        VolunteerPresence presence = presenceRepository.findByUserId(userId)
                .orElseGet(() -> VolunteerPresence.builder()
                        .userId(userId)
                        .status(VolunteerStatus.ONLINE)
                        .sessionId(UUID.randomUUID().toString())
                        .connectedAt(LocalDateTime.now())
                        .build());

        LocalDateTime now = LocalDateTime.now();
        if (presence.getConnectedAt() == null) {
            presence.setConnectedAt(now);
        }
        if (presence.getStatus() == null || presence.getStatus() == VolunteerStatus.OFFLINE) {
            presence.setStatus(VolunteerStatus.ONLINE);
            presence.setDisconnectedAt(null);
        }
        presence.setLastHeartbeat(now);
        presenceRepository.save(presence);
        publishPresence(presence);
    }

    public void markOffline(Long userId) {
        presenceRepository.findByUserId(userId).ifPresent(presence -> {
            LocalDateTime now = LocalDateTime.now();
            applyOfflineState(presence, now);
            presenceRepository.save(presence);
            publishPresence(presence);
        });
    }

    public List<VolunteerPresenceDTO> getAllPresences() {
        refreshStalePresences();
        return presenceRepository.findAll().stream().map(this::toDto).toList();
    }

    public List<VolunteerPresenceDTO> getActivePresences() {
        refreshStalePresences();
        return presenceRepository.findByStatusIn(ACTIVE_STATUSES).stream().map(this::toDto).toList();
    }

    public List<VolunteerPresenceDTO> getOnlineVolunteers() {
        refreshStalePresences();
        return presenceRepository.findByStatus(VolunteerStatus.ONLINE).stream().map(this::toDto).toList();
    }

    public long getActiveCount() {
        refreshStalePresences();
        return presenceRepository.countByStatusIn(ACTIVE_STATUSES);
    }

    public Map<String, Long> getStatusSummary() {
        refreshStalePresences();
        Map<String, Long> summary = new HashMap<>();
        for (VolunteerStatus status : VolunteerStatus.values()) {
            summary.put(status.name(), 0L);
        }
        presenceRepository.findAll().forEach(presence -> {
            VolunteerStatus status = presence.getStatus() == null ? VolunteerStatus.OFFLINE : presence.getStatus();
            summary.put(status.name(), summary.get(status.name()) + 1L);
        });
        return summary;
    }

    public VolunteerPresenceDTO getPresenceByUserId(Long userId) {
        refreshStalePresences();
        return presenceRepository.findByUserId(userId)
                .map(this::toDto)
                .orElseGet(() -> VolunteerPresenceDTO.builder()
                        .userId(userId)
                        .status(VolunteerStatus.OFFLINE)
                        .totalOnlineSeconds(0L)
                        .build());
    }

    private void refreshStalePresences() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(SESSION_TTL_SECONDS);
        List<VolunteerPresence> stalePresences = presenceRepository.findStalePresences(ACTIVE_STATUSES, threshold);
        for (VolunteerPresence presence : stalePresences) {
            applyOfflineState(presence, LocalDateTime.now());
            presenceRepository.save(presence);
        }
    }

    private void applyOfflineState(VolunteerPresence presence, LocalDateTime now) {
        if (presence.getConnectedAt() != null) {
            long additionalSeconds = Math.max(0L, Duration.between(presence.getConnectedAt(), now).getSeconds());
            presence.setTotalOnlineSeconds((presence.getTotalOnlineSeconds() == null ? 0L : presence.getTotalOnlineSeconds()) + additionalSeconds);
        }
        presence.setStatus(VolunteerStatus.OFFLINE);
        presence.setDisconnectedAt(now);
        presence.setSessionId(null);
        presence.setConnectedAt(null);
        presence.setLastHeartbeat(now);
    }

    private VolunteerPresenceDTO toDto(VolunteerPresence presence) {
        return VolunteerPresenceDTO.builder()
                .userId(presence.getUserId())
                .displayName(presence.getDisplayName())
                .status(presence.getStatus())
                .lastHeartbeat(presence.getLastHeartbeat())
                .connectedAt(presence.getConnectedAt())
                .disconnectedAt(presence.getDisconnectedAt())
                .totalOnlineSeconds(presence.getTotalOnlineSeconds())
                .phoneNumber(presence.getPhoneNumber())
                .build();
    }

    private void publishPresence(VolunteerPresence presence) {
        messagingTemplate.convertAndSend("/topic/volunteer-presence", toDto(presence));
    }

    private String resolveDisplayName(VolunteerPresence presence, VolunteerSessionRequest request) {
        if (request != null && request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
            return request.getDisplayName().trim();
        }
        return presence.getDisplayName();
    }

    private String resolveSessionId(VolunteerSessionRequest request) {
        if (request != null && request.getSessionId() != null && !request.getSessionId().isBlank()) {
            return request.getSessionId().trim();
        }
        return UUID.randomUUID().toString();
    }
}
