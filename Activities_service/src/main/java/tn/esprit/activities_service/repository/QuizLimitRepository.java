package tn.esprit.activities_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.activities_service.entity.QuizLimit;

import java.util.Optional;

@Repository
public interface QuizLimitRepository extends JpaRepository<QuizLimit, Long> {

    Optional<QuizLimit> findByPatientId(Long patientId);

    void deleteByPatientId(Long patientId);
}
