package tn.esprit.medical_report_service.Services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tn.esprit.medical_report_service.DTOs.MRIScanDTO;

public interface IMRIScan {
    MRIScanDTO addMRIScan(MRIScanDTO mriScanDTO, Long medicalReportId);

    MRIScanDTO updateMRIScan(Long id, MRIScanDTO mriScanDTO);

    void deleteMRIScan(Long id);

    MRIScanDTO getMRIScanById(Long id);

    Page<MRIScanDTO> getMRIScansByPatientId(Long patientId, Pageable pageable);
}
