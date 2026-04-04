package tn.esprit.lost_item_service.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.lost_item_service.Entity.ReportStatus;
import tn.esprit.lost_item_service.Entity.SearchReport;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SearchReportRepository extends JpaRepository<SearchReport, Long> {

    List<SearchReport> findByLostItemIdOrderBySearchDateDesc(Long lostItemId);

    long countByLostItemIdAndStatus(Long lostItemId, ReportStatus status);

    boolean existsByLostItemIdAndSearchDate(Long lostItemId, LocalDate searchDate);

    List<SearchReport> findByLostItemIdAndStatus(Long lostItemId, ReportStatus status);

    List<SearchReport> findByLostItemId(Long lostItemId);
}
