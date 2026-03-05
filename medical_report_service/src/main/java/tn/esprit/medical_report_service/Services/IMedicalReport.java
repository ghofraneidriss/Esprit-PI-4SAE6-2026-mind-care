package tn.esprit.medical_report_service.Services;

import tn.esprit.medical_report_service.DTOs.MedicalReportDTO;
import tn.esprit.medical_report_service.Enteties.MedicalReport;
import java.util.List;

public interface IMedicalReport {
    MedicalReportDTO addMedicalReport(MedicalReport medicalReport);

    MedicalReportDTO updateMedicalReport(MedicalReport medicalReport);

    void deleteMedicalReport(Long id);

    MedicalReportDTO getMedicalReportById(Long id);

    List<MedicalReportDTO> getAllMedicalReports();

    List<MedicalReportDTO> getReportsByPatientId(Long patientId);

    byte[] exportUploadAndNotify(Long id);
}
