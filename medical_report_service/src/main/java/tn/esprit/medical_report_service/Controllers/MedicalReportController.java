package tn.esprit.medical_report_service.Controllers;

import lombok.AllArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import tn.esprit.medical_report_service.Enteties.MedicalReport;
import tn.esprit.medical_report_service.Services.GcsReportStorageService;
import tn.esprit.medical_report_service.Services.IMedicalReport;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import tn.esprit.medical_report_service.Services.CloudinaryStorageService;

import tn.esprit.medical_report_service.DTOs.MedicalReportDTO;

@RestController
@RequestMapping("/api/medical-reports")
@AllArgsConstructor
public class MedicalReportController {

    private final GcsReportStorageService gcsReportStorageService;
    private final CloudinaryStorageService cloudinaryStorageService;
    private final IMedicalReport medicalReportService;

    @PostMapping
    public MedicalReportDTO addMedicalReport(@Valid @RequestBody MedicalReport medicalReport) {
        return medicalReportService.addMedicalReport(medicalReport);
    }

    @PutMapping
    public MedicalReportDTO updateMedicalReport(@Valid @RequestBody MedicalReport medicalReport) {
        return medicalReportService.updateMedicalReport(medicalReport);
    }

    @DeleteMapping("/{id}")
    public void deleteMedicalReport(@PathVariable Long id) {
        medicalReportService.deleteMedicalReport(id);
    }

    @GetMapping("/{id}")
    public MedicalReportDTO getMedicalReportById(@PathVariable Long id) {
        return medicalReportService.getMedicalReportById(id);
    }

    @GetMapping
    public List<MedicalReportDTO> getAllMedicalReports() {
        return medicalReportService.getAllMedicalReports();
    }

    @GetMapping("/patient/{patientId}")
    public List<MedicalReportDTO> getReportsByPatientId(@PathVariable Long patientId) {
        return medicalReportService.getReportsByPatientId(patientId);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() == null ? "Validation error" : error.getDefaultMessage())
                .orElse("Validation error");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", message));
    }

    @GetMapping({ "/{id}/pdf", "/{id}/export" })
    public ResponseEntity<byte[]> downloadMedicalReportPdf(@PathVariable Long id) {

        byte[] pdfBytes = medicalReportService.exportUploadAndNotify(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=medical_report_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/gcs-test")
    public ResponseEntity<Map<String, String>> testGcs() {

        byte[] testPdf = "Hello GCS".getBytes(); // for test (not real pdf)
        String objectName = "tests/test_" + System.currentTimeMillis() + ".pdf";

        String url = gcsReportStorageService.uploadPdfAndGetSignedUrl(testPdf, objectName);

        return ResponseEntity.ok(Map.of(
                "objectName", objectName,
                "signedUrl", url));
    }

    @GetMapping("/cloudinary-test")
    public ResponseEntity<Map<String, String>> testCloudinary() {
        byte[] testPdf = "Hello Cloudinary".getBytes();
        String fileName = "test_" + System.currentTimeMillis() + ".pdf";
        String folder = "test-reports";
        String tags = "test,manual-test";
        String url = cloudinaryStorageService.uploadPdf(testPdf, fileName, folder, tags);

        return ResponseEntity.ok(Map.of(
                "fileName", fileName,
                "url", url));
    }
}
