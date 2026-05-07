package tn.esprit.medical_report_service.Repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.medical_report_service.Enteties.MRIScan;

@Repository
public interface MRIScanRepository extends JpaRepository<MRIScan, Long> {
    Page<MRIScan> findByPatientId(Long patientId, Pageable pageable);
}
