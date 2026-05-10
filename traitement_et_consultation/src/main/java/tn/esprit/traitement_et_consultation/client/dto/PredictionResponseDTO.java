package tn.esprit.traitement_et_consultation.client.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PredictionResponseDTO {
    private Boolean isSick;
    private Double diseasePercentage;
    private Integer cluster;
}
