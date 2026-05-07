package tn.esprit.users_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Projection minimale pour l’inscription (liste déroulante) — évite de charger les hash mots de passe et champs inutiles.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PatientSummaryDTO {
    private Long userId;
    private String firstName;
    private String lastName;
    private String email;
}
