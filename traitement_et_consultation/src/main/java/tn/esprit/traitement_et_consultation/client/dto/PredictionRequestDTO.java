package tn.esprit.traitement_et_consultation.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictionRequestDTO {
    private int age;
    private int mmse;
    private int isSmoker;
    private int drinksAlcohol;
    private int physicalActivity;
    private int familyHistory;
    private int hypertension;
    private int type2Diabetes;
    private int hypercholesterolemia;
    private int sleepDisorders;
}
