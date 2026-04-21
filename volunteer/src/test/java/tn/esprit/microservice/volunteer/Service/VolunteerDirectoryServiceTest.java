package tn.esprit.microservice.volunteer.Service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.microservice.volunteer.Entity.VolunteerPresence;
import tn.esprit.microservice.volunteer.Entity.VolunteerStatus;
import tn.esprit.microservice.volunteer.Repository.AssignmentRepository;
import tn.esprit.microservice.volunteer.Repository.VolunteerPresenceRepository;
import tn.esprit.microservice.volunteer.client.UserClient;
import tn.esprit.microservice.volunteer.dto.VolunteerDirectoryEntryDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VolunteerDirectoryServiceTest {

    @Mock
    private UserClient userClient;

    @Mock
    private VolunteerPresenceRepository presenceRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @InjectMocks
    private VolunteerDirectoryService directoryService;

    @Test
    void getDirectoryEntriesEnrichesVolunteerProfiles() {
        UserClient.UserSummary volunteer = new UserClient.UserSummary(
                12L,
                "Sara",
                "Khaled",
                "SARA@MAIL.COM",
                "VOLUNTEER",
                "ONLINE",
                "+216111111");

        VolunteerPresence presence = VolunteerPresence.builder()
                .userId(12L)
                .status(VolunteerStatus.ONLINE)
                .lastHeartbeat(LocalDateTime.of(2026, 4, 16, 8, 30))
                .build();

        when(userClient.fetchVolunteers()).thenReturn(List.of(volunteer));
        when(presenceRepository.findAll()).thenReturn(List.of(presence));
        when(assignmentRepository.averageRatingForVolunteer(12L)).thenReturn(4.26);
        when(assignmentRepository.countByVolunteerId(12L)).thenReturn(7);

        List<VolunteerDirectoryEntryDTO> entries = directoryService.getDirectoryEntries();

        assertEquals(1, entries.size());
        VolunteerDirectoryEntryDTO entry = entries.get(0);
        assertEquals(12L, entry.userId());
        assertEquals("SK", entry.initials());
        assertEquals("Sara Khaled", entry.name());
        assertEquals("sara@mail.com", entry.email());
        assertEquals("Available now", entry.availability());
        assertEquals(4.3, entry.rating());
        assertEquals(7, entry.missions());
        assertEquals(VolunteerStatus.ONLINE.name(), entry.onlineStatus());
        assertEquals("2026-04-16 08:30", entry.lastSeen());
    }

    @Test
    void getVolunteerReturnsSpecificEntry() {
        UserClient.UserSummary volunteer = new UserClient.UserSummary(
                18L,
                "Maya",
                "Ben",
                "maya@example.com",
                "VOLUNTEER",
                "AWAY",
                null);
        VolunteerPresence presence = VolunteerPresence.builder()
                .userId(18L)
                .status(VolunteerStatus.AWAY)
                .disconnectedAt(LocalDateTime.of(2026, 4, 16, 9, 0))
                .build();

        when(userClient.fetchVolunteerById(18L)).thenReturn(Optional.of(volunteer));
        when(presenceRepository.findByUserId(18L)).thenReturn(Optional.of(presence));
        when(assignmentRepository.averageRatingForVolunteer(18L)).thenReturn(null);
        when(assignmentRepository.countByVolunteerId(18L)).thenReturn(2);

        VolunteerDirectoryEntryDTO entry = directoryService.getVolunteer(18L);

        assertEquals("MB", entry.initials());
        assertEquals("Away", entry.availability());
        assertEquals(0.0, entry.rating());
        assertEquals("2026-04-16 09:00", entry.lastSeen());
    }

    @Test
    void updatePhoneNumberCreatesPresenceWhenMissing() {
        when(presenceRepository.findByUserId(55L)).thenReturn(Optional.empty());
        when(presenceRepository.save(any(VolunteerPresence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        directoryService.updatePhoneNumber(55L, "+216999999");

        ArgumentCaptor<VolunteerPresence> captor = ArgumentCaptor.forClass(VolunteerPresence.class);
        verify(presenceRepository).save(captor.capture());
        VolunteerPresence saved = captor.getValue();
        assertEquals(55L, saved.getUserId());
        assertEquals("+216999999", saved.getPhoneNumber());
        assertEquals(VolunteerStatus.OFFLINE, saved.getStatus());
    }
}
