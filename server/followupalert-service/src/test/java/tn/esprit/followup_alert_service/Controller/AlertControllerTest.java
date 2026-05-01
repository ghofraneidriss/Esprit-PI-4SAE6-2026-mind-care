package tn.esprit.followup_alert_service.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.followup_alert_service.dto.AlertRequestDTO;
import tn.esprit.followup_alert_service.dto.AlertResponseDTO;
import tn.esprit.followup_alert_service.Entity.Alert;
import tn.esprit.followup_alert_service.Entity.AlertLevel;
import tn.esprit.followup_alert_service.Entity.AlertStatus;
import tn.esprit.followup_alert_service.Service.AlertService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AlertController.class)
@DisplayName("AlertController Tests")
class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AlertService alertService;

    @Autowired
    private ObjectMapper objectMapper;

    private Alert alert;
    private AlertRequestDTO alertRequestDTO;

    @BeforeEach
    void setUp() {
        alert = new Alert();
        alert.setId(1L);
        alert.setPatientId(100L);
        alert.setTitle("High Blood Pressure");
        alert.setDescription("Patient has elevated BP");
        alert.setLevel(AlertLevel.HIGH);
        alert.setStatus(AlertStatus.NEW);
        alert.setCreatedAt(LocalDateTime.now());

        alertRequestDTO = new AlertRequestDTO();
        alertRequestDTO.setPatientId(100L);
        alertRequestDTO.setTitle("High Blood Pressure");
        alertRequestDTO.setDescription("Patient has elevated BP");
        alertRequestDTO.setLevel(AlertLevel.HIGH);
    }

    @Test
    @DisplayName("POST /api/alerts - Create alert successfully")
    void testCreateAlert() throws Exception {
        when(alertService.createAlert(any(Alert.class))).thenReturn(alert);

        mockMvc.perform(post("/api/alerts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(alertRequestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("High Blood Pressure"))
                .andExpect(jsonPath("$.level").value("HIGH"));

        verify(alertService, times(1)).createAlert(any(Alert.class));
    }

    @Test
    @DisplayName("GET /api/alerts - Get all alerts")
    void testGetAllAlerts() throws Exception {
        when(alertService.getAllAlerts()).thenReturn(List.of(alert));

        mockMvc.perform(get("/api/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].title").value("High Blood Pressure"));

        verify(alertService, times(1)).getAllAlerts();
    }

    @Test
    @DisplayName("GET /api/alerts/{id} - Get alert by ID")
    void testGetAlertById() throws Exception {
        when(alertService.getAlertById(1L)).thenReturn(alert);

        mockMvc.perform(get("/api/alerts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("High Blood Pressure"));

        verify(alertService, times(1)).getAlertById(1L);
    }

    @Test
    @DisplayName("GET /api/alerts/patient/{patientId} - Get alerts by patient")
    void testGetAlertsByPatientId() throws Exception {
        when(alertService.getAlertsByPatientId(100L)).thenReturn(List.of(alert));

        mockMvc.perform(get("/api/alerts/patient/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].patientId").value(100L));

        verify(alertService, times(1)).getAlertsByPatientId(100L);
    }

    @Test
    @DisplayName("GET /api/alerts/level/{level} - Get alerts by level")
    void testGetAlertsByLevel() throws Exception {
        when(alertService.getAlertsByLevel(AlertLevel.HIGH)).thenReturn(List.of(alert));

        mockMvc.perform(get("/api/alerts/level/HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].level").value("HIGH"));

        verify(alertService, times(1)).getAlertsByLevel(AlertLevel.HIGH);
    }

    @Test
    @DisplayName("GET /api/alerts/status/{status} - Get alerts by status")
    void testGetAlertsByStatus() throws Exception {
        when(alertService.getAlertsByStatus(AlertStatus.NEW)).thenReturn(List.of(alert));

        mockMvc.perform(get("/api/alerts/status/NEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("NEW"));

        verify(alertService, times(1)).getAlertsByStatus(AlertStatus.NEW);
    }

    @Test
    @DisplayName("GET /api/alerts/critical/new - Get critical new alerts")
    void testGetCriticalNewAlerts() throws Exception {
        when(alertService.getCriticalNewAlerts()).thenReturn(List.of(alert));

        mockMvc.perform(get("/api/alerts/critical/new"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].level").value("HIGH"));

        verify(alertService, times(1)).getCriticalNewAlerts();
    }

    @Test
    @DisplayName("PATCH /api/alerts/{id}/view - Mark alert as viewed")
    void testMarkAsViewed() throws Exception {
        alert.setViewedAt(LocalDateTime.now());
        when(alertService.markAsViewed(1L)).thenReturn(alert);

        mockMvc.perform(patch("/api/alerts/1/view"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(alertService, times(1)).markAsViewed(1L);
    }

    @Test
    @DisplayName("PATCH /api/alerts/{id}/resolve - Resolve alert")
    void testResolveAlert() throws Exception {
        alert.setStatus(AlertStatus.RESOLVED);
        when(alertService.resolveAlert(1L)).thenReturn(alert);

        mockMvc.perform(patch("/api/alerts/1/resolve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        verify(alertService, times(1)).resolveAlert(1L);
    }

    @Test
    @DisplayName("PUT /api/alerts/{id} - Update alert")
    void testUpdateAlert() throws Exception {
        when(alertService.updateAlert(anyLong(), any(Alert.class))).thenReturn(alert);

        mockMvc.perform(put("/api/alerts/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(alertRequestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(alertService, times(1)).updateAlert(anyLong(), any(Alert.class));
    }

    @Test
    @DisplayName("DELETE /api/alerts/{id} - Delete alert")
    void testDeleteAlert() throws Exception {
        mockMvc.perform(delete("/api/alerts/1"))
                .andExpect(status().isNoContent());

        verify(alertService, times(1)).deleteAlert(1L);
    }

    @Test
    @DisplayName("PATCH /api/alerts/{id}/escalate - Escalate alert")
    void testEscalateAlert() throws Exception {
        alert.setLevel(AlertLevel.CRITICAL);
        when(alertService.escalateAlert(1L)).thenReturn(alert);

        mockMvc.perform(patch("/api/alerts/1/escalate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value("CRITICAL"));

        verify(alertService, times(1)).escalateAlert(1L);
    }

    @Test
    @DisplayName("PATCH /api/alerts/patient/{patientId}/resolve-all - Resolve all for patient")
    void testResolveAllByPatient() throws Exception {
        when(alertService.resolveAllByPatient(100L)).thenReturn(2);

        mockMvc.perform(patch("/api/alerts/patient/100/resolve-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolved").value(2))
                .andExpect(jsonPath("$.patientId").value(100L));

        verify(alertService, times(1)).resolveAllByPatient(100L);
    }

    @Test
    @DisplayName("GET /api/alerts/statistics - Get alert statistics")
    void testGetStatistics() throws Exception {
        Map<String, Object> stats = Map.of("total", 10, "new", 3, "critical", 1);
        when(alertService.getStatistics()).thenReturn(stats);

        mockMvc.perform(get("/api/alerts/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(10));

        verify(alertService, times(1)).getStatistics();
    }
}
