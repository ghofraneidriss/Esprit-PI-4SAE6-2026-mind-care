package tn.esprit.microservice.volunteer.Service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.microservice.volunteer.Entity.Assignment;
import tn.esprit.microservice.volunteer.Entity.AssignmentStatus;
import tn.esprit.microservice.volunteer.Entity.Mission;
import tn.esprit.microservice.volunteer.Entity.MissionStatus;
import tn.esprit.microservice.volunteer.Entity.Priority;
import tn.esprit.microservice.volunteer.Entity.VolunteerPresence;
import tn.esprit.microservice.volunteer.Entity.VolunteerStatus;
import tn.esprit.microservice.volunteer.Repository.AssignmentRepository;
import tn.esprit.microservice.volunteer.Repository.MissionRepository;
import tn.esprit.microservice.volunteer.Repository.VolunteerPresenceRepository;
import tn.esprit.microservice.volunteer.client.UserClient;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private MissionRepository missionRepository;

    @Mock
    private VolunteerPresenceRepository presenceRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private TwilioService twilioService;

    @InjectMocks
    private AssignmentService assignmentService;

    @Test
    void createAssignmentRejectsMissingMission() {
        Assignment request = new Assignment();

        assertThrows(IllegalArgumentException.class, () -> assignmentService.createAssignment(request));
    }

    @Test
    void manualAssignSendsSmsAndMarksMissionInProgress() {
        Mission mission = new Mission();
        mission.setId(1L);
        mission.setTitle("Mission A");
        mission.setStatus(MissionStatus.OPEN);
        mission.setPriority(Priority.LOW);

        VolunteerPresence presence = VolunteerPresence.builder()
                .userId(3L)
                .status(VolunteerStatus.ONLINE)
                .displayName("Volunteer One")
                .phoneNumber("+216123456")
                .build();

        when(missionRepository.findById(1L)).thenReturn(Optional.of(mission));
        when(assignmentRepository.countByVolunteerIdAndStatusIn(eq(3L), anyList())).thenReturn(0);
        when(assignmentRepository.existsByMissionIdAndStatusIn(eq(1L), anyList())).thenReturn(false);
        when(presenceRepository.findByUserId(3L)).thenReturn(Optional.of(presence));
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Assignment saved = assignmentService.manualAssign(1L, 3L, null, "Please call", null, null);

        assertEquals(3L, saved.getVolunteerId());
        assertEquals(AssignmentStatus.ASSIGNED, saved.getStatus());
        assertEquals(3L, saved.getVolunteerUserId());
        assertEquals(MissionStatus.IN_PROGRESS, mission.getStatus());
        assertEquals("Volunteer One", mission.getAssignee());
        verify(twilioService).sendSms(eq("+216123456"), anyString());
        verify(twilioService, never()).makeCall(any(), anyString());
    }

    @Test
    void manualAssignTriggersVoiceCallForHighPriorityMissions() {
        Mission mission = new Mission();
        mission.setId(8L);
        mission.setTitle("High priority mission");
        mission.setStatus(MissionStatus.OPEN);
        mission.setPriority(Priority.HIGH);

        VolunteerPresence presence = VolunteerPresence.builder()
                .userId(9L)
                .status(VolunteerStatus.ONLINE)
                .displayName("Volunteer Two")
                .phoneNumber("+216777777")
                .build();

        when(missionRepository.findById(8L)).thenReturn(Optional.of(mission));
        when(assignmentRepository.countByVolunteerIdAndStatusIn(eq(9L), anyList())).thenReturn(0);
        when(assignmentRepository.existsByMissionIdAndStatusIn(eq(8L), anyList())).thenReturn(false);
        when(presenceRepository.findByUserId(9L)).thenReturn(Optional.of(presence));
        when(assignmentRepository.save(any(Assignment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assignmentService.manualAssign(8L, 9L, 9L, null, null, null);

        verify(twilioService).makeCall(eq("+216777777"), anyString());
        verify(twilioService, never()).sendSms(any(), anyString());
    }

    @Test
    void getAssignmentsByVolunteerDelegatesToRepository() {
        Assignment assignment = new Assignment();
        assignment.setId(4L);
        when(assignmentRepository.findByVolunteerId(11L)).thenReturn(List.of(assignment));

        List<Assignment> assignments = assignmentService.getAssignmentsByVolunteer(11L);

        assertEquals(1, assignments.size());
        assertEquals(4L, assignments.get(0).getId());
    }
}
