package tn.esprit.medical_report_service.Repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.medical_report_service.Enteties.AIResult;
import tn.esprit.medical_report_service.Enteties.RiskLevel;

@Repository
public interface AIResultRepository extends JpaRepository<AIResult, Long> {
    Page<AIResult> findByRiskLevel(RiskLevel riskLevel, Pageable pageable);
}
