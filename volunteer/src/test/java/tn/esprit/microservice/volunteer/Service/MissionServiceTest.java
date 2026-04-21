package tn.esprit.microservice.volunteer.Service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.microservice.volunteer.Entity.Mission;
import tn.esprit.microservice.volunteer.Entity.MissionStatus;
import tn.esprit.microservice.volunteer.Entity.Priority;
import tn.esprit.microservice.volunteer.Repository.MissionRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MissionServiceTest {

    @Mock
    private MissionRepository missionRepository;

    @InjectMocks
    private MissionService missionService;

    private Mission mission;

    @BeforeEach
    void setUp() {
        mission = new Mission();
        mission.setId(1L);
        mission.setTitle("Community support");
        mission.setDescription("Patient follow-up");
        mission.setLocation("Tunis");
        mission.setCategory("Home Visit");
        mission.setAssignee("Unassigned");
        mission.setDuration("2 hours");
        mission.setPriority(Priority.MEDIUM);
        mission.setStatus(MissionStatus.OPEN);
        mission.setStartDate(new Date());
        mission.setEndDate(new Date());
        mission.setRequiredVolunteers(1);
    }

    @Test
    void getAllMissionsReturnsRepositoryData() {
        when(missionRepository.findAll()).thenReturn(List.of(mission));

        List<Mission> missions = missionService.getAllMissions();

        assertEquals(1, missions.size());
        assertEquals("Community support", missions.get(0).getTitle());
    }

    @Test
    void getMissionByIdThrowsWhenMissing() {
        when(missionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> missionService.getMissionById(99L));
    }

    @Test
    void createMissionDefaultsToOpenStatus() {
        Mission toCreate = new Mission();
        toCreate.setTitle("Mission A");
        toCreate.setStatus(null);
        when(missionRepository.save(any(Mission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Mission created = missionService.createMission(toCreate);

        assertEquals(MissionStatus.OPEN, created.getStatus());
        verify(missionRepository).save(toCreate);
    }

    @Test
    void updateMissionCopiesEditableFields() {
        Mission updated = new Mission();
        updated.setTitle("Updated title");
        updated.setDescription("Updated description");
        updated.setLocation("Sousse");
        updated.setCategory("Transport");
        updated.setAssignee("Volunteer One");
        updated.setDuration("3 hours");
        updated.setPriority(Priority.HIGH);
        updated.setStartDate(new Date(123456L));
        updated.setEndDate(new Date(456789L));
        updated.setRequiredVolunteers(3);
        updated.setStatus(MissionStatus.IN_PROGRESS);

        when(missionRepository.findById(1L)).thenReturn(Optional.of(mission));
        when(missionRepository.save(any(Mission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Mission saved = missionService.updateMission(1L, updated);

        assertEquals("Updated title", saved.getTitle());
        assertEquals("Updated description", saved.getDescription());
        assertEquals("Sousse", saved.getLocation());
        assertEquals(MissionStatus.IN_PROGRESS, saved.getStatus());
    }

    @Test
    void assignVolunteerMarksMissionInProgress() {
        when(missionRepository.findById(1L)).thenReturn(Optional.of(mission));
        when(missionRepository.save(any(Mission.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Mission saved = missionService.assignVolunteer(1L, "Amina");

        assertEquals("Amina", saved.getAssignee());
        assertEquals(MissionStatus.IN_PROGRESS, saved.getStatus());
        verify(missionRepository).save(mission);
    }

    @Test
    void deleteMissionDelegatesToRepository() {
        missionService.deleteMission(7L);

        verify(missionRepository).deleteById(7L);
        verify(missionRepository, never()).save(any(Mission.class));
    }
}
