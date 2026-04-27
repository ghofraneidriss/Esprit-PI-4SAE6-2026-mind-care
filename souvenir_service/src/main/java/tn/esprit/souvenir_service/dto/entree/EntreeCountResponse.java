package tn.esprit.souvenir_service.dto.entree;

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
public class EntreeCountResponse {

    private Long patientId;
    private Long totalEntrees;
}
