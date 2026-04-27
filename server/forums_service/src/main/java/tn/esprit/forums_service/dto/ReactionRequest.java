package tn.esprit.forums_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import tn.esprit.forums_service.entity.ReactionType;

@Data
public class ReactionRequest {
    @NotNull
    private Long userId;

    @NotNull
    private ReactionType type;
}
