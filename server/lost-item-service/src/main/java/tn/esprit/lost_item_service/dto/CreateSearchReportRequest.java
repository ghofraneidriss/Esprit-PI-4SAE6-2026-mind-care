package tn.esprit.lost_item_service.dto;

import lombok.*;
import tn.esprit.lost_item_service.entity.SearchResult;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSearchReportRequest {

    private Long lostItemId;

    private Long reportedBy;

    private LocalDate searchDate;

    private String locationSearched;

    private SearchResult searchResult;

    private String notes;
}
