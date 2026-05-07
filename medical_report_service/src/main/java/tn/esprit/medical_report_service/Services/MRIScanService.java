package tn.esprit.medical_report_service.Services;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import tn.esprit.medical_report_service.DTOs.MRIScanDTO;
import tn.esprit.medical_report_service.Enteties.MRIScan;
import tn.esprit.medical_report_service.Enteties.MedicalReport;
import tn.esprit.medical_report_service.Exceptions.ResourceNotFoundException;
import tn.esprit.medical_report_service.Mappers.MRIScanMapper;
import tn.esprit.medical_report_service.Repositories.MRIScanRepository;
import tn.esprit.medical_report_service.Repositories.MedicalReportRepository;

@Service
@AllArgsConstructor
public class MRIScanService implements IMRIScan {

    private final MRIScanRepository mriScanRepository;
    private final MedicalReportRepository medicalReportRepository;
    private final MRIScanMapper mriScanMapper;

    @Override
    public MRIScanDTO addMRIScan(MRIScanDTO mriScanDTO, Long medicalReportId) {
        MedicalReport report = medicalReportRepository.findById(medicalReportId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("MedicalReport not found with id: " + medicalReportId));

        MRIScan mriScan = mriScanMapper.toEntity(mriScanDTO);
        mriScan.setMedicalReport(report);
        mriScan = mriScanRepository.save(mriScan);
        return mriScanMapper.toDTO(mriScan);
    }

    @Override
    public MRIScanDTO updateMRIScan(Long id, MRIScanDTO mriScanDTO) {
        MRIScan existingScan = mriScanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MRIScan not found with id: " + id));

        mriScanDTO.setId(id);
        MRIScan mriScan = mriScanMapper.toEntity(mriScanDTO);

        // Preserve report link
        mriScan.setMedicalReport(existingScan.getMedicalReport());

        mriScan = mriScanRepository.save(mriScan);
        return mriScanMapper.toDTO(mriScan);
    }

    @Override
    public void deleteMRIScan(Long id) {
        MRIScan scan = mriScanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MRIScan not found with id: " + id));
        mriScanRepository.delete(scan);
    }

    @Override
    public MRIScanDTO getMRIScanById(Long id) {
        MRIScan scan = mriScanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MRIScan not found with id: " + id));
        return mriScanMapper.toDTO(scan);
    }

    @Override
    public Page<MRIScanDTO> getMRIScansByPatientId(Long patientId, Pageable pageable) {
        return mriScanRepository.findByPatientId(patientId, pageable)
                .map(mriScanMapper::toDTO);
    }
}
