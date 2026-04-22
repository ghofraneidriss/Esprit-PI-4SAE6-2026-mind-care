package tn.esprit.lost_item_service.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "search_report")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private Long lostItemId;

    @NotNull
    private Long reportedBy;

    @NotNull
    private LocalDate searchDate;

    private String locationSearched;

    @Enumerated(EnumType.STRING)
    private SearchResult searchResult;

    @Size(max = 2000)
    private String notes;

    @Enumerated(EnumType.STRING)
    private ReportStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = ReportStatus.OPEN;
        if (searchResult == null) searchResult = SearchResult.NOT_FOUND;
    }

    @PreUpdate
    protected void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
