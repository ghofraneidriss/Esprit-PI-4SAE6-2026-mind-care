package tn.esprit.followup_alert_service.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import tn.esprit.followup_alert_service.Entity.FollowUp;
import tn.esprit.followup_alert_service.Entity.IndependenceLevel;
import tn.esprit.followup_alert_service.Entity.MoodState;
import tn.esprit.followup_alert_service.Entity.SleepQuality;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FollowUp DTO Tests")
class FollowUpDTOTest {

    @Test
    @DisplayName("FollowUpRequestDTO - Create and all getters")
    void testFollowUpRequestDTO() {
        FollowUpRequestDTO dto = new FollowUpRequestDTO();
        dto.setPatientId(100L);
        dto.setCaregiverId(50L);
        LocalDate date = LocalDate.now();
        dto.setFollowUpDate(date);
        dto.setCognitiveScore(25);
        dto.setMood(MoodState.HAPPY);
        dto.setEating(IndependenceLevel.INDEPENDENT);
        dto.setDressing(IndependenceLevel.NEEDS_ASSISTANCE);
        dto.setMobility(IndependenceLevel.INDEPENDENT);
        dto.setHoursSlept(7);
        dto.setSleepQuality(SleepQuality.GOOD);
        dto.setAgitationObserved(false);
        dto.setConfusionObserved(true);
        dto.setNotes("Test notes");
        dto.setVitalSigns("BP: 120/80");

        assertThat(dto.getPatientId()).isEqualTo(100L);
        assertThat(dto.getCaregiverId()).isEqualTo(50L);
        assertThat(dto.getFollowUpDate()).isEqualTo(date);
        assertThat(dto.getCognitiveScore()).isEqualTo(25);
        assertThat(dto.getMood()).isEqualTo(MoodState.HAPPY);
        assertThat(dto.getEating()).isEqualTo(IndependenceLevel.INDEPENDENT);
        assertThat(dto.getDressing()).isEqualTo(IndependenceLevel.NEEDS_ASSISTANCE);
        assertThat(dto.getMobility()).isEqualTo(IndependenceLevel.INDEPENDENT);
        assertThat(dto.getHoursSlept()).isEqualTo(7);
        assertThat(dto.getSleepQuality()).isEqualTo(SleepQuality.GOOD);
        assertThat(dto.getAgitationObserved()).isFalse();
        assertThat(dto.getConfusionObserved()).isTrue();
        assertThat(dto.getNotes()).isEqualTo("Test notes");
        assertThat(dto.getVitalSigns()).isEqualTo("BP: 120/80");
    }

    @Test
    @DisplayName("FollowUpRequestDTO - With null and default values")
    void testFollowUpRequestDTOWithNulls() {
        FollowUpRequestDTO dto = new FollowUpRequestDTO();
        dto.setPatientId(null);
        dto.setCaregiverId(null);
        dto.setFollowUpDate(null);
        dto.setCognitiveScore(null);
        dto.setMood(null);
        dto.setNotes(null);

        assertThat(dto.getPatientId()).isNull();
        assertThat(dto.getCaregiverId()).isNull();
        assertThat(dto.getFollowUpDate()).isNull();
        assertThat(dto.getCognitiveScore()).isNull();
        assertThat(dto.getMood()).isNull();
        assertThat(dto.getNotes()).isNull();
    }

    @Test
    @DisplayName("FollowUpRequestDTO - All mood states")
    void testFollowUpRequestDTOAllMoods() {
        for (MoodState mood : MoodState.values()) {
            FollowUpRequestDTO dto = new FollowUpRequestDTO();
            dto.setMood(mood);
            assertThat(dto.getMood()).isEqualTo(mood);
        }
    }

    @Test
    @DisplayName("FollowUpRequestDTO - All independence levels")
    void testFollowUpRequestDTOAllIndependenceLevels() {
        for (IndependenceLevel level : IndependenceLevel.values()) {
            FollowUpRequestDTO dto = new FollowUpRequestDTO();
            dto.setEating(level);
            assertThat(dto.getEating()).isEqualTo(level);
        }
    }

    @Test
    @DisplayName("FollowUpRequestDTO - All sleep qualities")
    void testFollowUpRequestDTOAllSleepQualities() {
        for (SleepQuality quality : SleepQuality.values()) {
            FollowUpRequestDTO dto = new FollowUpRequestDTO();
            dto.setSleepQuality(quality);
            assertThat(dto.getSleepQuality()).isEqualTo(quality);
        }
    }

    @Test
    @DisplayName("FollowUpResponseDTO - Create and all getters")
    void testFollowUpResponseDTO() {
        FollowUpResponseDTO dto = new FollowUpResponseDTO();
        dto.setId(1L);
        dto.setPatientId(100L);
        dto.setCaregiverId(50L);
        LocalDate date = LocalDate.now();
        dto.setFollowUpDate(date);
        dto.setMood(MoodState.HAPPY);
        dto.setCognitiveScore(25);
        LocalDateTime now = LocalDateTime.now();
        dto.setCreatedAt(now);
        dto.setUpdatedAt(now);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getPatientId()).isEqualTo(100L);
        assertThat(dto.getCaregiverId()).isEqualTo(50L);
        assertThat(dto.getFollowUpDate()).isEqualTo(date);
        assertThat(dto.getMood()).isEqualTo(MoodState.HAPPY);
        assertThat(dto.getCognitiveScore()).isEqualTo(25);
        assertThat(dto.getCreatedAt()).isEqualTo(now);
        assertThat(dto.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("FollowUpResponseDTO.fromEntity - Convert FollowUp to DTO")
    void testFollowUpResponseDTOFromEntity() {
        FollowUp followUp = new FollowUp();
        followUp.setId(1L);
        followUp.setPatientId(100L);
        followUp.setCaregiverId(50L);
        LocalDate date = LocalDate.now();
        followUp.setFollowUpDate(date);
        followUp.setCognitiveScore(25);
        followUp.setMood(MoodState.HAPPY);
        followUp.setEating(IndependenceLevel.INDEPENDENT);
        followUp.setDressing(IndependenceLevel.NEEDS_ASSISTANCE);
        followUp.setMobility(IndependenceLevel.INDEPENDENT);
        followUp.setHoursSlept(7);
        followUp.setSleepQuality(SleepQuality.GOOD);
        followUp.setAgitationObserved(false);
        followUp.setConfusionObserved(false);
        followUp.setNotes("Test notes");
        followUp.setVitalSigns("BP: 120/80");
        LocalDateTime now = LocalDateTime.now();
        followUp.setCreatedAt(now);
        followUp.setUpdatedAt(now);

        FollowUpResponseDTO dto = FollowUpResponseDTO.fromEntity(followUp);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getPatientId()).isEqualTo(100L);
        assertThat(dto.getCaregiverId()).isEqualTo(50L);
        assertThat(dto.getFollowUpDate()).isEqualTo(date);
        assertThat(dto.getCognitiveScore()).isEqualTo(25);
        assertThat(dto.getMood()).isEqualTo(MoodState.HAPPY);
        assertThat(dto.getEating()).isEqualTo(IndependenceLevel.INDEPENDENT);
        assertThat(dto.getDressing()).isEqualTo(IndependenceLevel.NEEDS_ASSISTANCE);
        assertThat(dto.getMobility()).isEqualTo(IndependenceLevel.INDEPENDENT);
        assertThat(dto.getHoursSlept()).isEqualTo(7);
        assertThat(dto.getSleepQuality()).isEqualTo(SleepQuality.GOOD);
        assertThat(dto.getNotes()).isEqualTo("Test notes");
        assertThat(dto.getVitalSigns()).isEqualTo("BP: 120/80");
        assertThat(dto.getCreatedAt()).isEqualTo(now);
        assertThat(dto.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("FollowUpResponseDTO.fromEntity - With null fields")
    void testFollowUpResponseDTOFromEntityWithNulls() {
        FollowUp followUp = new FollowUp();
        followUp.setId(2L);
        followUp.setPatientId(null);
        followUp.setCaregiverId(null);
        followUp.setFollowUpDate(null);
        followUp.setCognitiveScore(null);
        followUp.setMood(null);
        followUp.setEating(null);
        followUp.setDressing(null);
        followUp.setMobility(null);
        followUp.setHoursSlept(null);
        followUp.setSleepQuality(null);
        followUp.setNotes(null);
        followUp.setVitalSigns(null);
        followUp.setCreatedAt(null);
        followUp.setUpdatedAt(null);

        FollowUpResponseDTO dto = FollowUpResponseDTO.fromEntity(followUp);

        assertThat(dto.getId()).isEqualTo(2L);
        assertThat(dto.getPatientId()).isNull();
        assertThat(dto.getCaregiverId()).isNull();
        assertThat(dto.getFollowUpDate()).isNull();
        assertThat(dto.getCognitiveScore()).isNull();
        assertThat(dto.getMood()).isNull();
        assertThat(dto.getEating()).isNull();
        assertThat(dto.getDressing()).isNull();
        assertThat(dto.getMobility()).isNull();
        assertThat(dto.getHoursSlept()).isNull();
        assertThat(dto.getSleepQuality()).isNull();
        assertThat(dto.getNotes()).isNull();
        assertThat(dto.getVitalSigns()).isNull();
        assertThat(dto.getCreatedAt()).isNull();
        assertThat(dto.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("FollowUpResponseDTO - Edge case with extreme cognitive scores")
    void testFollowUpResponseDTOExtremeValues() {
        FollowUp followUp = new FollowUp();
        followUp.setId(3L);
        followUp.setCognitiveScore(0);
        followUp.setHoursSlept(24);

        FollowUpResponseDTO dto = FollowUpResponseDTO.fromEntity(followUp);

        assertThat(dto.getCognitiveScore()).isEqualTo(0);
        assertThat(dto.getHoursSlept()).isEqualTo(24);
    }

    @Test
    @DisplayName("FollowUpRequestDTO and FollowUpResponseDTO field independence")
    void testFollowUpDTOFieldIndependence() {
        FollowUpRequestDTO requestDTO = new FollowUpRequestDTO();
        requestDTO.setPatientId(50L);
        requestDTO.setCognitiveScore(20);

        FollowUpResponseDTO responseDTO = new FollowUpResponseDTO();
        responseDTO.setPatientId(100L);
        responseDTO.setCognitiveScore(25);
        responseDTO.setId(5L);

        assertThat(requestDTO.getPatientId()).isEqualTo(50L);
        assertThat(responseDTO.getPatientId()).isEqualTo(100L);
        assertThat(requestDTO.getCognitiveScore()).isEqualTo(20);
        assertThat(responseDTO.getCognitiveScore()).isEqualTo(25);
        assertThat(responseDTO.getId()).isEqualTo(5L);
    }
}
