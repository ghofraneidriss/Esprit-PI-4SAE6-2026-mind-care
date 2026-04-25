package tn.esprit.lost_item_service.DTO;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.lost_item_service.Entity.ReportStatus;
import tn.esprit.lost_item_service.Entity.SearchResult;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for SearchReport.
 * Contains only safe fields to expose in REST API responses.
 * Excludes internal sensitive data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchReportDTO {

    private Long id;
    private Long lostItemId;
    private Long reportedBy;
    private LocalDate searchDate;
    private String locationSearched;
    private SearchResult searchResult;
    private String notes;
    private ReportStatus status;
    private LocalDateTime createdAt;

}
