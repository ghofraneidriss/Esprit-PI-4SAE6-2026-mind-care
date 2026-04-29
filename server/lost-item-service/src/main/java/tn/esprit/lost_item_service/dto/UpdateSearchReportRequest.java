package tn.esprit.lost_item_service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import tn.esprit.lost_item_service.entity.ReportStatus;
import tn.esprit.lost_item_service.entity.SearchResult;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpdateSearchReportRequest {

    private Long id;

    private Long lostItemId;

    private Long reportedBy;

    private LocalDate searchDate;

    private String locationSearched;

    private SearchResult searchResult;

    private String notes;

    private ReportStatus status;

    private LocalDate createdAt;
}
