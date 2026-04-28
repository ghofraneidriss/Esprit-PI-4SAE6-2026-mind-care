package tn.esprit.lost_item_service.dto;

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

    private String locationSearched;

    private SearchResult searchResult;

    private String notes;

    private ReportStatus status;
}
