package tn.esprit.souvenir_service.dto.entree;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.esprit.souvenir_service.enums.MediaType;
import tn.esprit.souvenir_service.enums.ThemeCulturel;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntreeSouvenirResponse {

    private Long id;
    private Long patientId;
    private Long doctorId;
    private Long caregiverId;
    private String infosCaregiver;
    private String texte;
    private MediaType mediaType;
    private String mediaUrl;
    private String mediaTitle;
    private String expectedSpeakerName;
    private String expectedSpeakerRelation;
    private String patientGuessSpeakerName;
    private Boolean voiceRecognized;
    private ThemeCulturel themeCulturel;
    private Boolean traitee;
    private Integer importance;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
