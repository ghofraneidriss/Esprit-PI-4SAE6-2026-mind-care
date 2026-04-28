package tn.esprit.lost_item_service.dto;

import jakarta.validation.constraints.Size;
import lombok.*;
import tn.esprit.lost_item_service.Entity.ReportStatus;
import tn.esprit.lost_item_service.Entity.SearchResult;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSearchReportRequest {

    private LocalDate searchDate;

    @Size(max = 255, message = "Location searched cannot exceed 255 characters")
    private String locationSearched;

    private SearchResult searchResult;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;

    private ReportStatus status;
}
