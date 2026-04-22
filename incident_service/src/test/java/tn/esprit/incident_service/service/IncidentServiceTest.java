package tn.esprit.incident_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import tn.esprit.incident_service.entity.Incident;
import tn.esprit.incident_service.entity.IncidentComment;
import tn.esprit.incident_service.entity.IncidentType;
import tn.esprit.incident_service.enums.IncidentStatus;
import tn.esprit.incident_service.enums.SeverityLevel;
import tn.esprit.incident_service.repository.IncidentCommentRepository;
import tn.esprit.incident_service.repository.IncidentRepository;
import tn.esprit.incident_service.repository.IncidentTypeRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private IncidentTypeRepository incidentTypeRepository;

    @Mock
    private IncidentCommentRepository incidentCommentRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private IncidentService incidentService;

    private Incident incident;
    private IncidentType incidentType;

    @BeforeEach
    void setUp() {
        incidentType = new IncidentType();
        incidentType.setId(1L);
        incidentType.setName("Fall");
        incidentType.setPoints(15);

        incident = new Incident();
        incident.setId(1L);
        incident.setPatientId(100L);
        incident.setCaregiverId(200L);
        incident.setDescription("Patient fell in bathroom");
        incident.setSeverityLevel(SeverityLevel.MEDIUM);
        incident.setStatus(IncidentStatus.OPEN);
        incident.setSource("CAREGIVER");
        incident.setIncidentDate(LocalDateTime.now());
        incident.setType(incidentType);
    }

    // ✅ Test 1 : getAllActiveIncidents
    @Test
    void testGetAllActiveIncidents() {
        when(incidentRepository.findAllActive()).thenReturn(Arrays.asList(incident));

        List<Incident> result = incidentService.getAllActiveIncidents();

        assertEquals(1, result.size());
        verify(incidentRepository, times(1)).findAllActive();
    }

    // ✅ Test 2 : getActiveIncidentsByPatient
    @Test
    void testGetActiveIncidentsByPatient() {
        when(incidentRepository.findByPatientIdActive(100L)).thenReturn(Arrays.asList(incident));

        List<Incident> result = incidentService.getActiveIncidentsByPatient(100L);

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getPatientId());
        verify(incidentRepository, times(1)).findByPatientIdActive(100L);
    }

    // ✅ Test 3 : createIncident source CAREGIVER (pas d'email)
    @Test
    void testCreateIncident_caregiverSource_noEmail() {
        incident.setSource("CAREGIVER");

        when(incidentTypeRepository.findById(1L)).thenReturn(Optional.of(incidentType));
        when(incidentRepository.countRecentByPatient(any(), any())).thenReturn(0L);
        when(incidentRepository.save(any(Incident.class))).thenReturn(incident);

        Incident result = incidentService.createIncident(incident);

        assertNotNull(result);
        verify(incidentRepository, times(1)).save(incident);
        verify(restTemplate, never()).getForObject(anyString(), any());
    }

    // ✅ Test 4 : createIncident auto-scoring
    @Test
    void testCreateIncident_autoScoring() {
        when(incidentTypeRepository.findById(1L)).thenReturn(Optional.of(incidentType));
        when(incidentRepository.countRecentByPatient(eq(100L), any())).thenReturn(1L);
        when(incidentRepository.save(any(Incident.class))).thenReturn(incident);

        incidentService.createIncident(incident);

        // score = 15 (type points) + 1*5 (recurrence) = 20 → HIGH
        assertEquals(20, incident.getComputedScore());
        assertEquals(SeverityLevel.HIGH, incident.getSeverityLevel());
    }

    // ✅ Test 5 : updateIncident trouvé
    @Test
    void testUpdateIncident_found() {
        Incident updated = new Incident();
        updated.setDescription("Updated description");
        updated.setSeverityLevel(SeverityLevel.HIGH);
        updated.setStatus(IncidentStatus.IN_PROGRESS);
        updated.setType(incidentType);

        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any(Incident.class))).thenReturn(incident);

        Incident result = incidentService.updateIncident(1L, updated);

        assertNotNull(result);
        verify(incidentRepository, times(1)).save(any(Incident.class));
    }

    // ✅ Test 6 : updateIncident non trouvé → exception
    @Test
    void testUpdateIncident_notFound_throwsException() {
        when(incidentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> incidentService.updateIncident(99L, incident));
    }

    // ✅ Test 7 : updateIncidentStatus
    @Test
    void testUpdateIncidentStatus() {
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any(Incident.class))).thenReturn(incident);

        Incident result = incidentService.updateIncidentStatus(1L, "RESOLVED");

        assertNotNull(result);
        assertEquals(IncidentStatus.RESOLVED, incident.getStatus());
    }

    // ✅ Test 8 : deleteIncident
    @Test
    void testDeleteIncident() {
        doNothing().when(incidentRepository).deleteById(1L);

        incidentService.deleteIncident(1L);

        verify(incidentRepository, times(1)).deleteById(1L);
    }

    // ✅ Test 9 : getAllIncidentTypes
    @Test
    void testGetAllIncidentTypes() {
        when(incidentTypeRepository.findAll()).thenReturn(Arrays.asList(incidentType));

        List<IncidentType> result = incidentService.getAllIncidentTypes();

        assertEquals(1, result.size());
        assertEquals("Fall", result.get(0).getName());
    }

    // ✅ Test 10 : addComment
    @Test
    void testAddComment() {
        IncidentComment comment = new IncidentComment();
        comment.setAuthorName("Dr. Smith");
        comment.setContent("Patient needs monitoring");

        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        when(incidentCommentRepository.save(any(IncidentComment.class))).thenReturn(comment);

        IncidentComment result = incidentService.addComment(1L, comment);

        assertNotNull(result);
        assertEquals("Dr. Smith", result.getAuthorName());
        verify(incidentCommentRepository, times(1)).save(comment);
    }

    // ✅ Test 11 : addComment incident non trouvé → exception
    @Test
    void testAddComment_incidentNotFound_throwsException() {
        when(incidentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> incidentService.addComment(99L, new IncidentComment()));
    }

    // ✅ Test 12 : deleteComment
    @Test
    void testDeleteComment() {
        doNothing().when(incidentCommentRepository).deleteById(1L);

        incidentService.deleteComment(1L);

        verify(incidentCommentRepository, times(1)).deleteById(1L);
    }

    // ✅ Test 13 : getPatientIncidentsHistory
    @Test
    void testGetPatientIncidentsHistory() {
        when(incidentRepository.findByPatientIdAll(100L)).thenReturn(Arrays.asList(incident));

        List<Incident> result = incidentService.getPatientIncidentsHistory(100L);

        assertEquals(1, result.size());
        verify(incidentRepository, times(1)).findByPatientIdAll(100L);
    }

    // ✅ Test 14 : scoreToSeverity via createIncident (score LOW)
    @Test
    void testCreateIncident_scoreLow() {
        incidentType.setPoints(5);

        when(incidentTypeRepository.findById(1L)).thenReturn(Optional.of(incidentType));
        when(incidentRepository.countRecentByPatient(any(), any())).thenReturn(0L);
        when(incidentRepository.save(any(Incident.class))).thenReturn(incident);

        incidentService.createIncident(incident);

        assertEquals(SeverityLevel.LOW, incident.getSeverityLevel());
    }
}
