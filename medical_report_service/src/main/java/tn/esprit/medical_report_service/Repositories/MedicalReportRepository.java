package tn.esprit.medical_report_service.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.medical_report_service.Enteties.MedicalReport;

@Repository
public interface MedicalReportRepository extends JpaRepository<MedicalReport, Long> {
    java.util.List<MedicalReport> findByPatientid(Long patientid);
}
