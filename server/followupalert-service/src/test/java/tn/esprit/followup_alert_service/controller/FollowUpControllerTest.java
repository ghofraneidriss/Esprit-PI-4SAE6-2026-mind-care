package tn.esprit.followup_alert_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.followup_alert_service.dto.FollowUpRequestDTO;
import tn.esprit.followup_alert_service.entity.FollowUp;
import tn.esprit.followup_alert_service.entity.IndependenceLevel;
import tn.esprit.followup_alert_service.entity.MoodState;
import tn.esprit.followup_alert_service.entity.SleepQuality;
import tn.esprit.followup_alert_service.service.FollowUpService;

import java.time.LocalDate;
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

@WebMvcTest(FollowUpController.class)
@DisplayName("FollowUpController Tests")
class FollowUpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FollowUpService followUpService;

    @Autowired
    private ObjectMapper objectMapper;

    private FollowUp followUp;
    private FollowUpRequestDTO followUpRequestDTO;

    @BeforeEach
    void setUp() {
        followUp = new FollowUp();
        followUp.setId(1L);
        followUp.setPatientId(100L);
        followUp.setCaregiverId(50L);
        followUp.setFollowUpDate(LocalDate.now());
        followUp.setCognitiveScore(25);
        followUp.setMood(MoodState.HAPPY);
        followUp.setAgitationObserved(false);
        followUp.setConfusionObserved(false);
        followUp.setEating(IndependenceLevel.INDEPENDENT);
        followUp.setDressing(IndependenceLevel.NEEDS_ASSISTANCE);
        followUp.setMobility(IndependenceLevel.INDEPENDENT);
        followUp.setHoursSlept(7);
        followUp.setSleepQuality(SleepQuality.GOOD);
        followUp.setNotes("Patient doing well");
        followUp.setVitalSigns("BP: 120/80, HR: 72");
        followUp.setCreatedAt(LocalDateTime.now());
        followUp.setUpdatedAt(LocalDateTime.now());

        followUpRequestDTO = new FollowUpRequestDTO();
        followUpRequestDTO.setPatientId(100L);
        followUpRequestDTO.setCaregiverId(50L);
        followUpRequestDTO.setFollowUpDate(LocalDate.now());
        followUpRequestDTO.setCognitiveScore(25);
        followUpRequestDTO.setMood(MoodState.HAPPY);
        followUpRequestDTO.setAgitationObserved(false);
        followUpRequestDTO.setConfusionObserved(false);
        followUpRequestDTO.setEating(IndependenceLevel.INDEPENDENT);
        followUpRequestDTO.setDressing(IndependenceLevel.NEEDS_ASSISTANCE);
        followUpRequestDTO.setMobility(IndependenceLevel.INDEPENDENT);
        followUpRequestDTO.setHoursSlept(7);
        followUpRequestDTO.setSleepQuality(SleepQuality.GOOD);
        followUpRequestDTO.setNotes("Patient doing well");
        followUpRequestDTO.setVitalSigns("BP: 120/80, HR: 72");
    }

    @Test
    @DisplayName("POST /api/followups - Create followup successfully")
    void testCreateFollowUp() throws Exception {
        when(followUpService.createFollowUp(any(FollowUp.class))).thenReturn(followUp);

        mockMvc.perform(post("/api/followups")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(followUpRequestDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.patientId").value(100L))
                .andExpect(jsonPath("$.mood").value("HAPPY"));

        verify(followUpService, times(1)).createFollowUp(any(FollowUp.class));
    }

    @Test
    @DisplayName("GET /api/followups - Get all followups")
    void testGetAllFollowUps() throws Exception {
        when(followUpService.getAllFollowUps()).thenReturn(List.of(followUp));

        mockMvc.perform(get("/api/followups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].patientId").value(100L));

        verify(followUpService, times(1)).getAllFollowUps();
    }

    @Test
    @DisplayName("GET /api/followups/{id} - Get followup by ID")
    void testGetFollowUpById() throws Exception {
        when(followUpService.getFollowUpById(1L)).thenReturn(followUp);

        mockMvc.perform(get("/api/followups/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.cognitiveScore").value(25));

        verify(followUpService, times(1)).getFollowUpById(1L);
    }

    @Test
    @DisplayName("GET /api/followups/patient/{patientId} - Get followups by patient")
    void testGetFollowUpsByPatientId() throws Exception {
        when(followUpService.getFollowUpsByPatientId(100L)).thenReturn(List.of(followUp));

        mockMvc.perform(get("/api/followups/patient/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].patientId").value(100L));

        verify(followUpService, times(1)).getFollowUpsByPatientId(100L);
    }

    @Test
    @DisplayName("GET /api/followups/caregiver/{caregiverId} - Get followups by caregiver")
    void testGetFollowUpsByCaregiverId() throws Exception {
        when(followUpService.getFollowUpsByCaregiverId(50L)).thenReturn(List.of(followUp));

        mockMvc.perform(get("/api/followups/caregiver/50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].caregiverId").value(50L));

        verify(followUpService, times(1)).getFollowUpsByCaregiverId(50L);
    }

    @Test
    @DisplayName("PUT /api/followups/{id} - Update followup")
    void testUpdateFollowUp() throws Exception {
        when(followUpService.updateFollowUp(anyLong(), any(FollowUp.class))).thenReturn(followUp);

        mockMvc.perform(put("/api/followups/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(followUpRequestDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(followUpService, times(1)).updateFollowUp(anyLong(), any(FollowUp.class));
    }

    @Test
    @DisplayName("DELETE /api/followups/{id} - Delete followup")
    void testDeleteFollowUp() throws Exception {
        mockMvc.perform(delete("/api/followups/1"))
                .andExpect(status().isNoContent());

        verify(followUpService, times(1)).deleteFollowUp(1L);
    }

    @Test
    @DisplayName("GET /api/followups/patient/{patientId}/cognitive-decline - Detect cognitive decline")
    void testDetectCognitiveDecline() throws Exception {
        when(followUpService.detectCognitiveDecline(100L)).thenReturn(false);

        mockMvc.perform(get("/api/followups/patient/100/cognitive-decline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(100L))
                .andExpect(jsonPath("$.cognitiveDecline").value(false));

        verify(followUpService, times(1)).detectCognitiveDecline(100L);
    }

    @Test
    @DisplayName("GET /api/followups/patient/{patientId}/risk - Get patient risk")
    void testGetPatientRisk() throws Exception {
        Map<String, Object> riskData = Map.of("patientId", 100L, "riskScore", 35);
        when(followUpService.calculatePatientRisk(100L)).thenReturn(riskData);

        mockMvc.perform(get("/api/followups/patient/100/risk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskScore").value(35));

        verify(followUpService, times(1)).calculatePatientRisk(100L);
    }

    @Test
    @DisplayName("GET /api/followups/statistics - Get global statistics")
    void testGetStatistics() throws Exception {
        Map<String, Object> stats = Map.of("totalFollowups", 150, "averageCognitiveScore", 22);
        when(followUpService.getStatistics()).thenReturn(stats);

        mockMvc.perform(get("/api/followups/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFollowups").value(150));

        verify(followUpService, times(1)).getStatistics();
    }

    @Test
    @DisplayName("GET /api/followups/statistics/patient/{patientId} - Get patient statistics")
    void testGetStatisticsByPatient() throws Exception {
        Map<String, Object> stats = Map.of("patientId", 100L, "followupCount", 5);
        when(followUpService.getStatisticsByPatient(100L)).thenReturn(stats);

        mockMvc.perform(get("/api/followups/statistics/patient/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(100L));

        verify(followUpService, times(1)).getStatisticsByPatient(100L);
    }
}
