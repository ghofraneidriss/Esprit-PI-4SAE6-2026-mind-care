package tn.esprit.recommendation_service.dto.puzzle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SouvenirSourceSummary {

    private Long id;
    private Long patientId;
    private String mediaType;
    private String mediaUrl;
    private String mediaTitle;
    private String texte;
}
