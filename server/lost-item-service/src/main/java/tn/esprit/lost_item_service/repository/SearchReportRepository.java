package tn.esprit.lost_item_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.lost_item_service.entity.ReportStatus;
import tn.esprit.lost_item_service.entity.SearchReport;
import tn.esprit.lost_item_service.entity.SearchResult;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SearchReportRepository extends JpaRepository<SearchReport, Long> {

    List<SearchReport> findByLostItemIdOrderBySearchDateDesc(Long lostItemId);

    long countByLostItemIdAndStatus(Long lostItemId, ReportStatus status);

    boolean existsByLostItemIdAndSearchDate(Long lostItemId, LocalDate searchDate);

    List<SearchReport> findByLostItemIdAndStatus(Long lostItemId, ReportStatus status);

    List<SearchReport> findByLostItemId(Long lostItemId);

    // ── Advanced search log queries ───────────────────────────────────────────

    List<SearchReport> findByReportedByOrderBySearchDateDesc(Long reportedBy);

    List<SearchReport> findBySearchResultOrderBySearchDateDesc(SearchResult searchResult);

    List<SearchReport> findByStatusOrderBySearchDateDesc(ReportStatus status);

    @Query("SELECT r FROM SearchReport r WHERE r.searchDate BETWEEN :from AND :to ORDER BY r.searchDate DESC")
    List<SearchReport> findByDateRange(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    @Query("SELECT r FROM SearchReport r WHERE r.lostItemId = :lostItemId AND r.searchDate BETWEEN :from AND :to ORDER BY r.searchDate DESC")
    List<SearchReport> findByLostItemIdAndDateRange(
            @Param("lostItemId") Long lostItemId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    @Query("SELECT r FROM SearchReport r WHERE LOWER(r.locationSearched) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY r.searchDate DESC")
    List<SearchReport> findByLocationKeyword(@Param("keyword") String keyword);

    @Query("SELECT r FROM SearchReport r WHERE r.reportedBy = :reportedBy AND r.searchDate BETWEEN :from AND :to ORDER BY r.searchDate DESC")
    List<SearchReport> findByReporterAndDateRange(
            @Param("reportedBy") Long reportedBy,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    // ── Statistics ────────────────────────────────────────────────────────────

    @Query("SELECT r.searchResult, COUNT(r) FROM SearchReport r GROUP BY r.searchResult")
    List<Object[]> countGroupedByResult();

    @Query("SELECT r.status, COUNT(r) FROM SearchReport r GROUP BY r.status")
    List<Object[]> countGroupedByStatus();

    @Query("SELECT r.lostItemId, COUNT(r) FROM SearchReport r GROUP BY r.lostItemId ORDER BY COUNT(r) DESC")
    List<Object[]> countGroupedByLostItem();

    @Query("SELECT r.reportedBy, COUNT(r) FROM SearchReport r GROUP BY r.reportedBy ORDER BY COUNT(r) DESC")
    List<Object[]> countGroupedByReporter();

    long countBySearchResult(SearchResult searchResult);

    long countByLostItemId(Long lostItemId);

    List<SearchReport> findByLostItemIdIn(java.util.Collection<Long> lostItemIds);
}
