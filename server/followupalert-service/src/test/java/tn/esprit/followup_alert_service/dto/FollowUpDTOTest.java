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
    @DisplayName("FollowUpRequestDTO - Create and getters")
    void testFollowUpRequestDTO() {
        FollowUpRequestDTO dto = new FollowUpRequestDTO();
        dto.setPatientId(100L);
        dto.setCaregiverId(50L);
        dto.setFollowUpDate(LocalDate.now());
        dto.setCognitiveScore(25);
        dto.setMood(MoodState.HAPPY);
        dto.setEating(IndependenceLevel.INDEPENDENT);

        assertThat(dto.getPatientId()).isEqualTo(100L);
        assertThat(dto.getCaregiverId()).isEqualTo(50L);
        assertThat(dto.getCognitiveScore()).isEqualTo(25);
        assertThat(dto.getMood()).isEqualTo(MoodState.HAPPY);
    }

    @Test
    @DisplayName("FollowUpResponseDTO - Create and getters")
    void testFollowUpResponseDTO() {
        FollowUpResponseDTO dto = new FollowUpResponseDTO();
        dto.setId(1L);
        dto.setPatientId(100L);
        dto.setCaregiverId(50L);
        dto.setMood(MoodState.HAPPY);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getPatientId()).isEqualTo(100L);
        assertThat(dto.getCaregiverId()).isEqualTo(50L);
    }

    @Test
    @DisplayName("FollowUpResponseDTO.fromEntity - Convert FollowUp to DTO")
    void testFollowUpResponseDTOFromEntity() {
        FollowUp followUp = new FollowUp();
        followUp.setId(1L);
        followUp.setPatientId(100L);
        followUp.setCaregiverId(50L);
        followUp.setFollowUpDate(LocalDate.now());
        followUp.setCognitiveScore(25);
        followUp.setMood(MoodState.HAPPY);
        followUp.setEating(IndependenceLevel.INDEPENDENT);
        followUp.setDressing(IndependenceLevel.NEEDS_ASSISTANCE);
        followUp.setMobility(IndependenceLevel.INDEPENDENT);
        followUp.setHoursSlept(7);
        followUp.setSleepQuality(SleepQuality.GOOD);
        followUp.setCreatedAt(LocalDateTime.now());
        followUp.setUpdatedAt(LocalDateTime.now());

        FollowUpResponseDTO dto = FollowUpResponseDTO.fromEntity(followUp);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getPatientId()).isEqualTo(100L);
        assertThat(dto.getCaregiverId()).isEqualTo(50L);
        assertThat(dto.getCognitiveScore()).isEqualTo(25);
        assertThat(dto.getMood()).isEqualTo(MoodState.HAPPY);
    }
}
