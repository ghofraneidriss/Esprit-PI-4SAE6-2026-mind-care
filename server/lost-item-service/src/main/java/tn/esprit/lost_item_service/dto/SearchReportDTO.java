package tn.esprit.lost_item_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.lost_item_service.entity.ReportStatus;
import tn.esprit.lost_item_service.entity.SearchResult;

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
