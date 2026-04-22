package tn.esprit.recommendation_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.recommendation_service.entity.PatientStats;

public interface PatientStatsRepository extends JpaRepository<PatientStats, Long> {
}

