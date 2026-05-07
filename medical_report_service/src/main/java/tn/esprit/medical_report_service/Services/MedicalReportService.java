package tn.esprit.medical_report_service.Services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.medical_report_service.Clients.UserServiceClient;
import tn.esprit.medical_report_service.DTOs.UserDTO;
import tn.esprit.medical_report_service.Enteties.File;
import tn.esprit.medical_report_service.Enteties.MedicalReport;
import tn.esprit.medical_report_service.Repositories.FileRepository;
import tn.esprit.medical_report_service.Repositories.MedicalReportRepository;

import tn.esprit.medical_report_service.Mappers.MedicalReportMapper;
import tn.esprit.medical_report_service.DTOs.MedicalReportDTO;
import java.util.stream.Collectors;
import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
@Transactional
public class MedicalReportService implements IMedicalReport {

    private final MedicalReportRepository medicalReportRepository;
    private final FileRepository fileRepository;
    private final UserServiceClient userServiceClient;
    private final PdfService pdfService;
    private final CloudinaryStorageService cloudinaryStorageService;
    private final EmailService emailService;
    private final MedicalReportMapper medicalReportMapper;

    @Override
    public MedicalReportDTO addMedicalReport(MedicalReport medicalReport) {
        enrichAndValidateParticipants(medicalReport);

        // Map containing files that actually exist in DB
        if (medicalReport.getFiles() != null && !medicalReport.getFiles().isEmpty()) {
            List<File> managedFiles = medicalReport.getFiles().stream()
                    .filter(f -> f.getFileid() != null)
                    .map(f -> fileRepository.findById(f.getFileid()).orElse(null))
                    .filter(f -> f != null)
                    .collect(Collectors.toList());

            // Clear the "detached"/unmanaged files and set the managed ones
            medicalReport.setFiles(managedFiles);

            // Establish bidirectional link
            for (File file : managedFiles) {
                file.setMedicalReport(medicalReport);
            }
        }

        MedicalReport saved = medicalReportRepository.save(medicalReport);
        return medicalReportMapper.toDTO(saved);
    }

    @Override
    public MedicalReportDTO updateMedicalReport(MedicalReport medicalReport) {
        if (medicalReport.getReportid() == null) {
            throw new IllegalArgumentException("Report ID is required for update");
        }
        MedicalReport existing = medicalReportRepository.findById(medicalReport.getReportid())
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        enrichAndValidateParticipants(medicalReport);

        // Update basic fields if provided
        if (medicalReport.getTitle() != null && !medicalReport.getTitle().isBlank()) {
            existing.setTitle(medicalReport.getTitle());
        }
        if (medicalReport.getDescription() != null && !medicalReport.getDescription().isBlank()) {
            existing.setDescription(medicalReport.getDescription());
        }
        if (medicalReport.getDiagnosis() != null) {
            existing.setDiagnosis(medicalReport.getDiagnosis());
        }
        if (medicalReport.getRiskLevel() != null) {
            existing.setRiskLevel(medicalReport.getRiskLevel());
        }
        if (medicalReport.getApprovedAt() != null) {
            existing.setApprovedAt(medicalReport.getApprovedAt());
        }
        if (medicalReport.getStatus() != null) {
            existing.setStatus(medicalReport.getStatus());
        }

        // Handle files during update
        if (medicalReport.getFiles() != null) {
            // First, untie all files currently linked to this report
            if (existing.getFiles() != null) {
                for (File f : existing.getFiles()) {
                    f.setMedicalReport(null);
                }
                existing.getFiles().clear();
            }

            // Link new selection of files
            List<File> managedFiles = medicalReport.getFiles().stream()
                    .filter(f -> f.getFileid() != null)
                    .map(f -> fileRepository.findById(f.getFileid()).orElse(null))
                    .filter(f -> f != null)
                    .collect(Collectors.toList());

            existing.getFiles().addAll(managedFiles);
            for (File file : managedFiles) {
                file.setMedicalReport(existing);
            }
        }

        MedicalReport updated = medicalReportRepository.save(existing);
        return medicalReportMapper.toDTO(updated);
    }

    @Override
    public void deleteMedicalReport(Long id) {
        medicalReportRepository.deleteById(id);
    }

    @Override
    public MedicalReportDTO getMedicalReportById(Long id) {
        return medicalReportRepository.findById(id).map(medicalReportMapper::toDTO).orElse(null);
    }

    @Override
    public List<MedicalReportDTO> getAllMedicalReports() {
        return medicalReportRepository.findAll().stream()
                .map(medicalReportMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<MedicalReportDTO> getReportsByPatientId(Long patientId) {
        return medicalReportRepository.findByPatientid(patientId).stream()
                .map(medicalReportMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public byte[] exportUploadAndNotify(Long id) {
        MedicalReport report = medicalReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report not found id=" + id));

        byte[] pdfBytes = pdfService.generateMedicalReportPdf(report);

        Long patientId = report.getPatientid();
        String cloudinaryUrl = null;
        try {
            String fileName = "report_" + id + ".pdf";
            String folder = "patient-reports/patient_" + patientId;
            String patientName = report.getPatientName() != null ? report.getPatientName() : "Patient";
            String docName = report.getDoctorName() != null ? report.getDoctorName() : "Doctor";
            String tags = "patient:" + patientName + ",doctor:" + docName + ",report_id:" + id;

            cloudinaryUrl = cloudinaryStorageService.uploadPdf(pdfBytes, fileName, folder, tags);
            log.info("Cloudinary upload succeeded for report {} in folder {} with tags {}", id, folder, tags);

            // Save the URL to DB for later retrieval
            report.setReportUrl(cloudinaryUrl);
            medicalReportRepository.save(report);

        } catch (Exception cloudinaryEx) {
            log.error("Cloudinary upload failed for report {}: {}", id, cloudinaryEx.getMessage());
        }

        UserDTO user = null;
        try {
            user = userServiceClient.getUserById(patientId);
        } catch (Exception userEx) {
            log.warn("Failed to load patient {} for report {}: {}", patientId, id, userEx.getMessage());
        }

        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            log.warn("Skipping mail: patient email not found for patientId={}", patientId);
            return pdfBytes;
        }

        String doctorName = report.getDoctorName() != null ? report.getDoctorName() : "Your Doctor";
        String doctorEmail = report.getDoctorEmail() != null ? report.getDoctorEmail() : "unknown@example.com";

        // MINDCARE PATIENT PORTAL LINK (Directs patient to our app's login/dashboard)
        String appPatientPortalLink = "http://localhost:4200/reports";

        try {
            if (cloudinaryUrl != null) {
                emailService.sendReportLink(user.getEmail(), id, appPatientPortalLink, doctorName, doctorEmail,
                        report.getPatientName());
            } else {
                emailService.sendReportAttachment(user.getEmail(), id, pdfBytes, doctorName, doctorEmail);
            }
        } catch (Exception mailEx) {
            log.warn("Email notification failed for report {}: {}", id, mailEx.getMessage());
        }

        return pdfBytes;
    }

    private void enrichAndValidateParticipants(MedicalReport medicalReport) {
        if (medicalReport.getPatientid() == null) {
            throw new IllegalArgumentException("patientid is required");
        }
        if (medicalReport.getDoctorid() == null) {
            throw new IllegalArgumentException("doctorid is required");
        }

        UserDTO patient = userServiceClient.getUserById(medicalReport.getPatientid());
        UserDTO doctor = userServiceClient.getUserById(medicalReport.getDoctorid());

        if (patient == null || patient.getUserId() == null) {
            throw new IllegalArgumentException("Patient not found");
        }
        if (doctor == null || doctor.getUserId() == null) {
            throw new IllegalArgumentException("Doctor not found");
        }

        String patientRole = normalizeRole(patient.getRole());
        String doctorRole = normalizeRole(doctor.getRole());

        if (!"PATIENT".equals(patientRole)) {
            throw new IllegalArgumentException("Selected patientid is not a PATIENT");
        }
        if (!"DOCTOR".equals(doctorRole)) {
            throw new IllegalArgumentException("Selected doctorid is not a DOCTOR");
        }

        medicalReport.setPatientName(formatFullName(patient.getFirstName(), patient.getLastName()));
        medicalReport.setDoctorName(formatFullName(doctor.getFirstName(), doctor.getLastName()));
        if (medicalReport.getDoctorEmail() == null || medicalReport.getDoctorEmail().isBlank()) {
            medicalReport.setDoctorEmail(doctor.getEmail());
        }
    }

    private String normalizeRole(String role) {
        return role == null ? "" : role.trim().toUpperCase().replace("ROLE_", "");
    }

    private String formatFullName(String firstName, String lastName) {
        String safeFirst = firstName == null ? "" : firstName.trim();
        String safeLast = lastName == null ? "" : lastName.trim();
        return (safeFirst + " " + safeLast).trim();
    }
}
