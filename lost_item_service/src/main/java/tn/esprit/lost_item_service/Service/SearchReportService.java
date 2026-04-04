package tn.esprit.lost_item_service.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.lost_item_service.Entity.ReportStatus;
import tn.esprit.lost_item_service.Entity.SearchReport;
import tn.esprit.lost_item_service.Exception.DuplicateReportException;
import tn.esprit.lost_item_service.Repository.SearchReportRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchReportService {

    private final SearchReportRepository searchReportRepository;

    public SearchReport createSearchReport(SearchReport report) {
        // Sprint 2 - Feature 7: validate one search report per lost item per day
        if (searchReportRepository.existsByLostItemIdAndSearchDate(report.getLostItemId(), report.getSearchDate())) {
            throw new DuplicateReportException(
                "A search report already exists for lost item id=" + report.getLostItemId()
                + " on " + report.getSearchDate() + ". Only one report per item per day is allowed."
            );
        }
        log.info("Creating search report for lost item id={}", report.getLostItemId());
        return searchReportRepository.save(report);
    }

    public List<SearchReport> getSearchReportsByLostItemId(Long lostItemId) {
        return searchReportRepository.findByLostItemIdOrderBySearchDateDesc(lostItemId);
    }

    public SearchReport getSearchReportById(Long id) {
        return searchReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Search report not found with id: " + id));
    }

    public SearchReport updateSearchReport(Long id, SearchReport updated) {
        SearchReport existing = getSearchReportById(id);
        existing.setLostItemId(updated.getLostItemId());
        existing.setReportedBy(updated.getReportedBy());
        existing.setSearchDate(updated.getSearchDate());
        existing.setLocationSearched(updated.getLocationSearched());
        existing.setSearchResult(updated.getSearchResult());
        existing.setNotes(updated.getNotes());
        existing.setStatus(updated.getStatus());
        return searchReportRepository.save(existing);
    }

    public void deleteSearchReport(Long id) {
        if (!searchReportRepository.existsById(id)) {
            throw new RuntimeException("Search report not found with id: " + id);
        }
        searchReportRepository.deleteById(id);
    }

    public long getOpenReportsCount(Long lostItemId) {
        return searchReportRepository.countByLostItemIdAndStatus(lostItemId, ReportStatus.OPEN);
    }
}
