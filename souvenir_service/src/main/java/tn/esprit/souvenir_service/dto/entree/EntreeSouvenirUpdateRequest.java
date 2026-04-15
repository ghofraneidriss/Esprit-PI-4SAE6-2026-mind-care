package tn.esprit.souvenir_service.dto.entree;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.esprit.souvenir_service.enums.MediaType;
import tn.esprit.souvenir_service.enums.ThemeCulturel;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntreeSouvenirUpdateRequest {

    private Long doctorId;

    private Long caregiverId;

    private String infosCaregiver;

    @NotBlank(message = "texte is required")
    private String texte;

    @NotNull(message = "mediaType is required")
    private MediaType mediaType;

    @Size(max = 500, message = "mediaUrl max length is 500")
    private String mediaUrl;

    @Size(max = 255, message = "mediaTitle max length is 255")
    private String mediaTitle;

    @Size(max = 255, message = "expectedSpeakerName max length is 255")
    private String expectedSpeakerName;

    @Size(max = 255, message = "expectedSpeakerRelation max length is 255")
    private String expectedSpeakerRelation;

    @NotNull(message = "themeCulturel is required")
    private ThemeCulturel themeCulturel;

    @Min(value = 1, message = "importance min is 1")
    @Max(value = 10, message = "importance max is 10")
    private Integer importance;
}
