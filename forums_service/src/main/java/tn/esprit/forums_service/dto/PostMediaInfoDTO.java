package tn.esprit.forums_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostMediaInfoDTO {
    private Long id;
    private int sortOrder;
    private String contentType;
}
