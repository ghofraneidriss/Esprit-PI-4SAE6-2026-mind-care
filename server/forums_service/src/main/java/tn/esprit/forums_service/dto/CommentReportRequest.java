package tn.esprit.forums_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentReportRequest {
    @NotNull
    private Long reporterUserId;

    private String reason;
}
