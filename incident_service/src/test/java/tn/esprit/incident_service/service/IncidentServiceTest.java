package tn.esprit.incident_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.incident_service.entity.Incident;
import tn.esprit.incident_service.entity.IncidentComment;
import tn.esprit.incident_service.entity.IncidentType;
import tn.esprit.incident_service.enums.IncidentStatus;
import tn.esprit.incident_service.enums.SeverityLevel;
import tn.esprit.incident_service.repository.IncidentCommentRepository;
import tn.esprit.incident_service.repository.IncidentRepository;
import tn.esprit.incident_service.repository.IncidentTypeRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private IncidentTypeRepository incidentTypeRepository;

    @Mock
    private IncidentCommentRepository incidentCommentRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private IncidentService incidentService;

    private Incident incident;
    private IncidentType incidentType;

    @BeforeEach
    public void setUp() {
        incidentType = new IncidentType();
        incidentType.setId(1L);
        incidentType.setName("Fall");

        incident = new Incident();
        incident.setId(1L);
        incident.setPatientId(100L);
        incident.setDescription("Patient fell");
        incident.setStatus(IncidentStatus.OPEN);
        incident.setSeverityLevel(SeverityLevel.HIGH);
        incident.setIncidentType(incidentType);
    }

    @Test
    public void testGetAllActiveIncidents() {
        when(incidentRepository.findByStatusNot(IncidentStatus.RESOLVED)).thenReturn(Arrays.asList(incident));

        List<Incident> result = incidentService.getAllActiveIncidents();

        assertEquals(1, result.size());
        verify(incidentRepository, times(1)).findByStatusNot(IncidentStatus.RESOLVED);
    }

    @Test
    public void testGetActiveIncidentsByPatient() {
        when(incidentRepository.findByPatientIdAndStatusNot(100L, IncidentStatus.RESOLVED)).thenReturn(Arrays.asList(incident));

        List<Incident> result = incidentService.getActiveIncidentsByPatient(100L);

        assertEquals(1, result.size());
    }

    @Test
    public void testCreateIncident_caregiverSource_noEmail() {
        incident.setSource("CAREGIVER");

        when(incidentRepository.save(any(Incident.class))).thenReturn(incident);

        Incident result = incidentService.createIncident(incident);

        assertNotNull(result);
        verify(incidentRepository, times(1)).save(any(Incident.class));
    }

    @Test
    public void testUpdateIncident_found() {
        Incident updated = new Incident();
        updated.setDescription("Updated description");
        updated.setStatus(IncidentStatus.IN_PROGRESS);
        updated.setSeverityLevel(SeverityLevel.LOW);

        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any(Incident.class))).thenReturn(incident);

        Incident result = incidentService.updateIncident(1L, updated);

        assertNotNull(result);
        verify(incidentRepository, times(1)).save(any(Incident.class));
    }

    @Test
    public void testUpdateIncident_notFound_throwsException() {
        when(incidentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> incidentService.updateIncident(99L, incident));
    }

    @Test
    public void testUpdateIncidentStatus() {
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any(Incident.class))).thenReturn(incident);

        Incident result = incidentService.updateIncidentStatus(1L, IncidentStatus.RESOLVED);

        assertNotNull(result);
        verify(incidentRepository, times(1)).save(any(Incident.class));
    }

    @Test
    public void testDeleteIncident() {
        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        doNothing().when(incidentRepository).delete(any(Incident.class));

        incidentService.deleteIncident(1L);

        verify(incidentRepository, times(1)).delete(any(Incident.class));
    }

    @Test
    public void testGetAllIncidentTypes() {
        when(incidentTypeRepository.findAll()).thenReturn(Arrays.asList(incidentType));

        List<IncidentType> result = incidentService.getAllIncidentTypes();

        assertEquals(1, result.size());
    }

    @Test
    public void testAddComment() {
        IncidentComment comment = new IncidentComment();
        comment.setContent("Test comment");

        when(incidentRepository.findById(1L)).thenReturn(Optional.of(incident));
        when(incidentCommentRepository.save(any(IncidentComment.class))).thenReturn(comment);

        IncidentComment result = incidentService.addComment(1L, comment);

        assertNotNull(result);
        verify(incidentCommentRepository, times(1)).save(any(IncidentComment.class));
    }

    @Test
    public void testAddComment_incidentNotFound_throwsException() {
        IncidentComment comment = new IncidentComment();

        when(incidentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> incidentService.addComment(99L, comment));
    }

    @Test
    public void testDeleteComment() {
        IncidentComment comment = new IncidentComment();
        comment.setId(1L);

        when(incidentCommentRepository.findById(1L)).thenReturn(Optional.of(comment));
        doNothing().when(incidentCommentRepository).delete(any(IncidentComment.class));

        incidentService.deleteComment(1L);

        verify(incidentCommentRepository, times(1)).delete(any(IncidentComment.class));
    }

    @Test
    public void testGetPatientIncidentsHistory() {
        when(incidentRepository.findByPatientId(100L)).thenReturn(Arrays.asList(incident));

        List<Incident> result = incidentService.getPatientIncidentsHistory(100L);

        assertEquals(1, result.size());
    }
}
