package tn.esprit.incident_service.controller;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.incident_service.dto.IncidentStatsDTO;
import tn.esprit.incident_service.dto.PatientStatsDTO;
import tn.esprit.incident_service.entity.Incident;
import tn.esprit.incident_service.entity.IncidentComment;
import tn.esprit.incident_service.entity.IncidentType;
import tn.esprit.incident_service.service.EmailService;
import tn.esprit.incident_service.service.IncidentService;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4210"})
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;
    private final EmailService emailService;

    // --- TEST EMAIL ---
    @GetMapping("/test-email")
    public ResponseEntity<String> testEmail() {
        String result = emailService.sendTestEmail();
        return ResponseEntity.ok(result);
    }

    // --- INCIDENTS ---

    @GetMapping("/incidents")
    public ResponseEntity<List<Incident>> getAllActiveIncidents() {
        return ResponseEntity.ok(incidentService.getAllActiveIncidents());
    }

    @GetMapping("/incidents/history")
    public ResponseEntity<List<Incident>> getHistory() {
        return ResponseEntity.ok(incidentService.getAllHistory());
    }

    @GetMapping("/incidents/patient/{patientId}")
    public ResponseEntity<List<Incident>> getByPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(incidentService.getActiveIncidentsByPatient(patientId));
    }

    @GetMapping("/incidents/patient/{patientId}/history")
    public ResponseEntity<List<Incident>> getByPatientHistory(@PathVariable Long patientId) {
        return ResponseEntity.ok(incidentService.getPatientIncidentsHistory(patientId));
    }

    @GetMapping("/incidents/caregiver/{caregiverId}")
    public ResponseEntity<List<Incident>> getByCaregiver(@PathVariable Long caregiverId) {
        return ResponseEntity.ok(incidentService.getActiveIncidentsByCaregiver(caregiverId));
    }

    @GetMapping("/incidents/reported")
    public ResponseEntity<List<Incident>> getReportedIncidents() {
        return ResponseEntity.ok(incidentService.getCaregiverReportedIncidents());
    }

    @PostMapping("/incidents")
    public ResponseEntity<Incident> createIncident(@RequestBody Incident incident) {
        // En vrai, on mapperait DTO -> Entity ici
        return new ResponseEntity<>(incidentService.createIncident(incident), HttpStatus.CREATED);
    }

    @PutMapping("/incidents/{id}")
    public ResponseEntity<Incident> updateIncident(@PathVariable Long id, @RequestBody Incident incident) {
        return ResponseEntity.ok(incidentService.updateIncident(id, incident));
    }

    @PatchMapping("/incidents/{id}/status")
    public ResponseEntity<Incident> updateIncidentStatus(@PathVariable Long id, @RequestBody java.util.Map<String, String> body) {
        return ResponseEntity.ok(incidentService.updateIncidentStatus(id, body.get("status")));
    }

    @DeleteMapping("/incidents/{id}")
    public ResponseEntity<Void> deleteIncident(@PathVariable Long id) {
        incidentService.deleteIncident(id);
        return ResponseEntity.noContent().build();
    }

    // --- INCIDENT TYPES ---

    @GetMapping("/incident-types")
    public ResponseEntity<List<IncidentType>> getAllTypes() {
        return ResponseEntity.ok(incidentService.getAllIncidentTypes());
    }

    @PostMapping("/incident-types")
    public ResponseEntity<IncidentType> createType(@RequestBody IncidentType type) {
        return new ResponseEntity<>(incidentService.createIncidentType(type), HttpStatus.CREATED);
    }

    @PutMapping("/incident-types/{id}")
    public ResponseEntity<IncidentType> updateType(@PathVariable Long id, @RequestBody IncidentType type) {
        return ResponseEntity.ok(incidentService.updateIncidentType(id, type));
    }

    @DeleteMapping("/incident-types/{id}")
    public ResponseEntity<Void> deleteType(@PathVariable Long id) {
        incidentService.deleteIncidentType(id);
        return ResponseEntity.noContent().build();
    }

    // --- COMMENTS ---

    @GetMapping("/incidents/{id}/comments")
    public ResponseEntity<List<IncidentComment>> getComments(@PathVariable Long id) {
        return ResponseEntity.ok(incidentService.getCommentsByIncident(id));
    }

    @PostMapping("/incidents/{id}/comments")
    public ResponseEntity<IncidentComment> addComment(@PathVariable Long id, @RequestBody IncidentComment comment) {
        return new ResponseEntity<>(incidentService.addComment(id, comment), HttpStatus.CREATED);
    }

    @DeleteMapping("/incidents/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        incidentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }

    // --- STATS ---
    @GetMapping("/incidents/stats")
    public ResponseEntity<IncidentStatsDTO> getStats() {
        return ResponseEntity.ok(incidentService.getStats());
    }

    // --- PATIENT STATS ---
    @GetMapping("/incidents/patient-stats")
    public ResponseEntity<List<PatientStatsDTO>> getPatientStats() {
        return ResponseEntity.ok(incidentService.getPatientStats());
    }

    @GetMapping("/incidents/patient-stats/{patientId}")
    public ResponseEntity<PatientStatsDTO> getPatientStatsById(@PathVariable Long patientId) {
        return ResponseEntity.ok(incidentService.getPatientStatsById(patientId));
    }

    // --- SEND PATIENT STATS BY EMAIL (with PDF) ---
    @PostMapping("/incidents/patient-stats/{patientId}/send-email")
    public ResponseEntity<java.util.Map<String, String>> sendPatientStatsByEmail(
            @PathVariable Long patientId,
            @RequestBody java.util.Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "L'adresse e-mail est requise"));
        }
        PatientStatsDTO stats = incidentService.getPatientStatsById(patientId);
        byte[] pdfBytes = generatePatientStatsPdf(stats);
        emailService.sendPatientStatsEmail(email, stats, pdfBytes);
        return ResponseEntity.ok(java.util.Map.of("message", "Statistiques envoyées à " + email + " avec le rapport PDF"));
    }

    /**
     * Génère un PDF avec les statistiques du patient.
     */
    private byte[] generatePatientStatsPdf(PatientStatsDTO stats) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            // Fonts
            Font titleFont   = new Font(Font.HELVETICA, 22, Font.BOLD, new java.awt.Color(30, 58, 95));
            Font subtitleFont = new Font(Font.HELVETICA, 12, Font.NORMAL, new java.awt.Color(100, 116, 139));
            Font sectionFont  = new Font(Font.HELVETICA, 14, Font.BOLD, new java.awt.Color(30, 41, 59));
            Font labelFont    = new Font(Font.HELVETICA, 11, Font.NORMAL, new java.awt.Color(100, 116, 139));
            Font valueFont    = new Font(Font.HELVETICA, 14, Font.BOLD, new java.awt.Color(30, 41, 59));
            Font smallFont    = new Font(Font.HELVETICA, 10, Font.NORMAL, new java.awt.Color(148, 163, 184));

            // Risk colors
            java.awt.Color riskColor;
            String riskLabel;
            switch (stats.getRiskLevel()) {
                case "CRITICAL": riskColor = new java.awt.Color(239, 68, 68);  riskLabel = "CRITIQUE"; break;
                case "HIGH":     riskColor = new java.awt.Color(249, 115, 22); riskLabel = "ÉLEVÉ"; break;
                case "MODERATE": riskColor = new java.awt.Color(234, 179, 8);  riskLabel = "MODÉRÉ"; break;
                default:         riskColor = new java.awt.Color(34, 197, 94);  riskLabel = "FAIBLE"; break;
            }

            // ─── Title ───
            Paragraph title = new Paragraph("MindCare — Rapport Statistique", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);

            Paragraph sub = new Paragraph("Généré le " + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")), subtitleFont);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(24);
            doc.add(sub);

            // ─── Separator ───
            doc.add(new Paragraph("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", smallFont));
            doc.add(Chunk.NEWLINE);

            // ─── Patient info ───
            Paragraph patientSection = new Paragraph("Patient : " + stats.getPatientName(), sectionFont);
            patientSection.setSpacingAfter(4);
            doc.add(patientSection);
            doc.add(new Paragraph("ID Patient : " + stats.getPatientId(), labelFont));
            doc.add(Chunk.NEWLINE);

            // ─── Score & Risk ───
            Font scoreFont = new Font(Font.HELVETICA, 36, Font.BOLD, riskColor);
            Paragraph scorePara = new Paragraph();
            scorePara.setAlignment(Element.ALIGN_CENTER);
            scorePara.add(new Chunk(String.valueOf(stats.getSeverityScore()), scoreFont));
            scorePara.add(new Chunk(" / 100", subtitleFont));
            doc.add(scorePara);

            Font riskFont = new Font(Font.HELVETICA, 13, Font.BOLD, riskColor);
            Paragraph riskPara = new Paragraph("Niveau de risque : " + riskLabel, riskFont);
            riskPara.setAlignment(Element.ALIGN_CENTER);
            riskPara.setSpacingAfter(20);
            doc.add(riskPara);

            // ─── Key Metrics Table ───
            Paragraph metricsTitle = new Paragraph("Résumé des incidents", sectionFont);
            metricsTitle.setSpacingAfter(10);
            doc.add(metricsTitle);

            PdfPTable metricsTable = new PdfPTable(4);
            metricsTable.setWidthPercentage(100);
            metricsTable.setSpacingAfter(20);

            addMetricCell(metricsTable, "Total", String.valueOf(stats.getTotalIncidents()), labelFont, valueFont);
            addMetricCell(metricsTable, "Actifs", String.valueOf(stats.getActiveIncidents()), labelFont, new Font(Font.HELVETICA, 14, Font.BOLD, new java.awt.Color(234, 88, 12)));
            addMetricCell(metricsTable, "Résolus", String.valueOf(stats.getResolvedIncidents()), labelFont, new Font(Font.HELVETICA, 14, Font.BOLD, new java.awt.Color(22, 163, 74)));
            addMetricCell(metricsTable, "Jrs moy.", stats.getAvgDaysBetween() > 0 ? String.format("%.1f", stats.getAvgDaysBetween()) : "—", labelFont, new Font(Font.HELVETICA, 14, Font.BOLD, new java.awt.Color(37, 99, 235)));
            doc.add(metricsTable);

            // ─── Severity Breakdown Table ───
            Paragraph sevTitle = new Paragraph("Répartition par sévérité", sectionFont);
            sevTitle.setSpacingAfter(10);
            doc.add(sevTitle);

            PdfPTable sevTable = new PdfPTable(2);
            sevTable.setWidthPercentage(60);
            sevTable.setHorizontalAlignment(Element.ALIGN_LEFT);
            sevTable.setWidths(new float[]{3, 1});

            java.util.Map<String, Long> bySev = stats.getBySeverity() != null ? stats.getBySeverity() : java.util.Map.of();
            addSeverityRow(sevTable, "Critique", bySev.getOrDefault("CRITICAL", 0L), new java.awt.Color(239, 68, 68), labelFont);
            addSeverityRow(sevTable, "Élevé", bySev.getOrDefault("HIGH", 0L), new java.awt.Color(249, 115, 22), labelFont);
            addSeverityRow(sevTable, "Moyen", bySev.getOrDefault("MEDIUM", 0L), new java.awt.Color(234, 179, 8), labelFont);
            addSeverityRow(sevTable, "Faible", bySev.getOrDefault("LOW", 0L), new java.awt.Color(34, 197, 94), labelFont);
            doc.add(sevTable);

            doc.add(Chunk.NEWLINE);
            doc.add(new Paragraph("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", smallFont));
            Paragraph footer = new Paragraph("Ce rapport a été généré automatiquement par la plateforme MindCare.", smallFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(8);
            doc.add(footer);

            doc.close();
            return baos.toByteArray();

        } catch (Exception e) {
            //log.error("❌ Erreur lors de la génération du PDF de stats : {}", e.getMessage());
            return new byte[0];
        }
    }

    private void addMetricCell(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(new java.awt.Color(226, 232, 240));
        cell.setPadding(10);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        Paragraph p = new Paragraph();
        p.setAlignment(Element.ALIGN_CENTER);
        p.add(new Chunk(value + "\n", valueFont));
        p.add(new Chunk(label, labelFont));
        cell.addElement(p);
        table.addCell(cell);
    }

    private void addSeverityRow(PdfPTable table, String label, long count, java.awt.Color color, Font labelFont) {
        Font colorValueFont = new Font(Font.HELVETICA, 12, Font.BOLD, color);

        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(Rectangle.BOTTOM);
        labelCell.setBorderColor(new java.awt.Color(241, 245, 249));
        labelCell.setPadding(8);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(String.valueOf(count), colorValueFont));
        valueCell.setBorder(Rectangle.BOTTOM);
        valueCell.setBorderColor(new java.awt.Color(241, 245, 249));
        valueCell.setPadding(8);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);
    }

    // --- PDF REPORT ---
    @GetMapping("/incidents/report")
    public ResponseEntity<byte[]> generateReport(@RequestParam(required = false) Long patientId) {
        List<Incident> incidents = incidentService.getIncidentsForReport(patientId);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            Font headerFont = new Font(Font.HELVETICA, 11, Font.BOLD);
            Font bodyFont = new Font(Font.HELVETICA, 10);

            doc.add(new Paragraph("MindCare — Incident Report", titleFont));
            doc.add(new Paragraph(patientId != null ? "Patient ID: " + patientId : "All Patients", bodyFont));
            doc.add(new Paragraph("Generated: " + java.time.LocalDateTime.now().format(fmt), bodyFont));
            doc.add(Chunk.NEWLINE);

            for (Incident i : incidents) {
                doc.add(new Paragraph("Incident #" + i.getId(), headerFont));
                doc.add(new Paragraph("Type: " + (i.getType() != null ? i.getType().getName() : "N/A"), bodyFont));
                doc.add(new Paragraph("Severity: " + (i.getSeverityLevel() != null ? i.getSeverityLevel() : "N/A"), bodyFont));
                doc.add(new Paragraph("Status: " + (i.getStatus() != null ? i.getStatus() : "N/A"), bodyFont));
                doc.add(new Paragraph("Date: " + (i.getIncidentDate() != null ? i.getIncidentDate().format(fmt) : "N/A"), bodyFont));
                doc.add(new Paragraph("Description: " + (i.getDescription() != null ? i.getDescription() : ""), bodyFont));
                doc.add(new Paragraph("─────────────────────────────────────────", bodyFont));
            }

            doc.close();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "incident-report.pdf");
            return ResponseEntity.ok().headers(headers).body(baos.toByteArray());

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
