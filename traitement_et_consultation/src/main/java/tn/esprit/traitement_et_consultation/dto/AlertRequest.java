package tn.esprit.traitement_et_consultation.dto;

import lombok.Data;

@Data
public class AlertRequest {
    private String email;
    private String subject;
    private String message;
}
