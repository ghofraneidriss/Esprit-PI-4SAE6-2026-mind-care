package tn.esprit.forums_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import tn.esprit.forums_service.entity.ModerationAction;

@Data
public class ResolveReportRequest {
    @NotNull
    private Long doctorId;

    @NotNull
    private ModerationAction action;
}
