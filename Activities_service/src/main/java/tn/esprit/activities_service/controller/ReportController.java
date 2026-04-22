package tn.esprit.activities_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import tn.esprit.activities_service.service.ReportService;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports", description = "PDF patient reports (MindCare CVP)")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Operation(summary = "Generate patient PDF report",
               description = "Full A4 report: history, domain performance, clinical interpretation, recommendations")
    @GetMapping("/patient/{patientId}/pdf")
    public ResponseEntity<byte[]> generatePatientPdf(
            @Parameter(description = "Patient ID") @PathVariable("patientId") Long patientId) {
        try {
            byte[] pdf = reportService.generatePatientReport(patientId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.builder("attachment")
                    .filename("mindcare-cvp-patient-report-" + patientId + ".pdf")
                    .build());
            headers.setContentLength(pdf.length);

            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
