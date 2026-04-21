package tn.esprit.microservice.volunteer.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import tn.esprit.microservice.volunteer.Entity.VolunteerPresence;
import tn.esprit.microservice.volunteer.Entity.VolunteerStatus;
import tn.esprit.microservice.volunteer.Repository.VolunteerPresenceRepository;
import tn.esprit.microservice.volunteer.dto.VolunteerPresenceDTO;
import tn.esprit.microservice.volunteer.dto.VolunteerSessionRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VolunteerPresenceServiceTest {

    @Mock
    private VolunteerPresenceRepository presenceRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private VolunteerPresenceService presenceService;

    private VolunteerPresence presence;

    @BeforeEach
    void setUp() {
        presence = VolunteerPresence.builder()
                .userId(5L)
                .status(VolunteerStatus.OFFLINE)
                .displayName("John Doe")
                .sessionId("session-1")
                .lastHeartbeat(LocalDateTime.now().minusMinutes(10))
                .connectedAt(LocalDateTime.now().minusMinutes(20))
                .disconnectedAt(LocalDateTime.now().minusMinutes(5))
                .totalOnlineSeconds(120L)
                .phoneNumber("+216000000")
                .build();
    }

    @Test
    void markOnlineCreatesOrUpdatesPresenceAndPublishesIt() {
        when(presenceRepository.findByUserId(5L)).thenReturn(Optional.of(presence));
        when(presenceRepository.save(any(VolunteerPresence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VolunteerSessionRequest request = new VolunteerSessionRequest();
        request.setSessionId("session-42");
        request.setDisplayName("  Amina  ");
        presenceService.markOnline(5L, request);

        ArgumentCaptor<VolunteerPresence> captor = ArgumentCaptor.forClass(VolunteerPresence.class);
        verify(presenceRepository).save(captor.capture());
        VolunteerPresence saved = captor.getValue();
        assertEquals(VolunteerStatus.ONLINE, saved.getStatus());
        assertEquals("Amina", saved.getDisplayName());
        assertEquals("session-42", saved.getSessionId());
        assertNull(saved.getDisconnectedAt());
        verify(messagingTemplate).convertAndSend(eq("/topic/volunteer-presence"), (Object) any(VolunteerPresenceDTO.class));
    }

    @Test
    void heartbeatReactivatesOfflinePresence() {
        presence.setStatus(VolunteerStatus.OFFLINE);
        when(presenceRepository.findByUserId(5L)).thenReturn(Optional.of(presence));
        when(presenceRepository.save(any(VolunteerPresence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        presenceService.heartbeat(5L);

        ArgumentCaptor<VolunteerPresence> captor = ArgumentCaptor.forClass(VolunteerPresence.class);
        verify(presenceRepository).save(captor.capture());
        VolunteerPresence saved = captor.getValue();
        assertEquals(VolunteerStatus.ONLINE, saved.getStatus());
        assertNull(saved.getDisconnectedAt());
        assertEquals("John Doe", saved.getDisplayName());
        verify(messagingTemplate).convertAndSend(eq("/topic/volunteer-presence"), (Object) any(VolunteerPresenceDTO.class));
    }

    @Test
    void getAllPresencesReturnsDtosAndRefreshesStaleRecords() {
        VolunteerPresence stale = VolunteerPresence.builder()
                .userId(8L)
                .status(VolunteerStatus.ONLINE)
                .displayName("Stale Volunteer")
                .lastHeartbeat(LocalDateTime.now().minusMinutes(2))
                .connectedAt(LocalDateTime.now().minusMinutes(4))
                .totalOnlineSeconds(50L)
                .build();

        when(presenceRepository.findStalePresences(anyList(), any(LocalDateTime.class))).thenReturn(List.of());
        when(presenceRepository.findAll()).thenReturn(List.of(stale));

        List<VolunteerPresenceDTO> presences = presenceService.getAllPresences();

        assertEquals(1, presences.size());
        assertEquals(8L, presences.get(0).getUserId());
        assertEquals(VolunteerStatus.ONLINE, presences.get(0).getStatus());
    }

    @Test
    void getStatusSummaryCountsStatuses() {
        VolunteerPresence online = VolunteerPresence.builder()
                .userId(1L)
                .status(VolunteerStatus.ONLINE)
                .build();
        VolunteerPresence away = VolunteerPresence.builder()
                .userId(2L)
                .status(VolunteerStatus.AWAY)
                .build();

        when(presenceRepository.findStalePresences(anyList(), any(LocalDateTime.class))).thenReturn(List.of());
        when(presenceRepository.findAll()).thenReturn(List.of(online, away));

        Map<String, Long> summary = presenceService.getStatusSummary();

        assertEquals(1L, summary.get("ONLINE"));
        assertEquals(1L, summary.get("AWAY"));
        assertTrue(summary.containsKey("OFFLINE"));
    }

    @Test
    void getPresenceByUserIdReturnsOfflineDefaultWhenMissing() {
        when(presenceRepository.findStalePresences(anyList(), any(LocalDateTime.class))).thenReturn(List.of());
        when(presenceRepository.findByUserId(anyLong())).thenReturn(Optional.empty());

        VolunteerPresenceDTO dto = presenceService.getPresenceByUserId(77L);

        assertEquals(77L, dto.getUserId());
        assertEquals(VolunteerStatus.OFFLINE, dto.getStatus());
        assertEquals(0L, dto.getTotalOnlineSeconds());
    }
}
