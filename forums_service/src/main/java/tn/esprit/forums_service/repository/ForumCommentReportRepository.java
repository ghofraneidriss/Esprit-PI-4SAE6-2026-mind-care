package tn.esprit.forums_service.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.forums_service.entity.ForumCommentReport;
import tn.esprit.forums_service.entity.ReportStatus;

import java.util.List;

public interface ForumCommentReportRepository extends JpaRepository<ForumCommentReport, Long> {

    List<ForumCommentReport> findByStatusAndPostAuthorIdOrderByCreatedAtDesc(ReportStatus status, Long postAuthorId);

    List<ForumCommentReport> findByPostAuthorIdOrderByCreatedAtDesc(Long postAuthorId, Pageable pageable);
}
