package tn.esprit.followup_alert_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.followup_alert_service.entity.FollowUp;
import tn.esprit.followup_alert_service.entity.IndependenceLevel;
import tn.esprit.followup_alert_service.entity.MoodState;
import tn.esprit.followup_alert_service.entity.SleepQuality;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FollowUpResponseDTO {

    private Long id;
    private Long patientId;
    private Long caregiverId;
    private LocalDate followUpDate;
    private Integer cognitiveScore;
    private MoodState mood;
    private Boolean agitationObserved;
    private Boolean confusionObserved;
    private IndependenceLevel eating;
    private IndependenceLevel dressing;
    private IndependenceLevel mobility;
    private Integer hoursSlept;
    private SleepQuality sleepQuality;
    private String notes;
    private String vitalSigns;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static FollowUpResponseDTO fromEntity(FollowUp followUp) {
        FollowUpResponseDTO dto = new FollowUpResponseDTO();
        dto.setId(followUp.getId());
        dto.setPatientId(followUp.getPatientId());
        dto.setCaregiverId(followUp.getCaregiverId());
        dto.setFollowUpDate(followUp.getFollowUpDate());
        dto.setCognitiveScore(followUp.getCognitiveScore());
        dto.setMood(followUp.getMood());
        dto.setAgitationObserved(followUp.getAgitationObserved());
        dto.setConfusionObserved(followUp.getConfusionObserved());
        dto.setEating(followUp.getEating());
        dto.setDressing(followUp.getDressing());
        dto.setMobility(followUp.getMobility());
        dto.setHoursSlept(followUp.getHoursSlept());
        dto.setSleepQuality(followUp.getSleepQuality());
        dto.setNotes(followUp.getNotes());
        dto.setVitalSigns(followUp.getVitalSigns());
        dto.setCreatedAt(followUp.getCreatedAt());
        dto.setUpdatedAt(followUp.getUpdatedAt());
        return dto;
    }
}
