package tn.esprit.microservice.reclamationms.controller;

import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.microservice.reclamationms.entity.ReclamationClassificationLevel;
import tn.esprit.microservice.reclamationms.entity.Reclamation;
import tn.esprit.microservice.reclamationms.service.ReclamationService;

@RestController
@RequestMapping("/reclamations")
public class ReclamationController {

    private final ReclamationService reclamationService;

    public ReclamationController(ReclamationService reclamationService) {
        this.reclamationService = reclamationService;
    }

    @GetMapping
    public List<Reclamation> getAll() {
        return reclamationService.findAll();
    }

    @GetMapping("/{id}")
    public Reclamation getById(@PathVariable Long id) {
        return reclamationService.findById(id);
    }

    @PostMapping
    public ResponseEntity<Reclamation> create(@RequestBody Reclamation reclamation) {
        return ResponseEntity.ok(reclamationService.create(reclamation));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Reclamation> update(@PathVariable Long id, @RequestBody Reclamation reclamation) {
        return ResponseEntity.ok(reclamationService.update(id, reclamation));
    }

    @GetMapping("/classification/{level}")
    public List<Reclamation> getByClassification(@PathVariable ReclamationClassificationLevel level) {
        return reclamationService.findByClassificationLevel(level);
    }

    @PostMapping("/classification/analyze")
    public ResponseEntity<ReclamationService.ClassificationPreview> analyzeClassification(
            @RequestBody ClassificationAnalyzeRequest request) {
        return ResponseEntity.ok(reclamationService.analyzeClassification(request.subject(), request.description()));
    }

    @GetMapping("/ratings/summary")
    public ResponseEntity<ReclamationService.ReclamationRatingSummary> getRatingSummary() {
        return ResponseEntity.ok(reclamationService.getRatingSummary());
    }

    @GetMapping("/ratings/by-course")
    public ResponseEntity<List<ReclamationService.ReclamationCourseRatingStats>> getRatingsByCourse() {
        return ResponseEntity.ok(reclamationService.getCourseRatingStats());
    }

    @GetMapping("/ratings/ranking")
    public ResponseEntity<List<ReclamationService.ReclamationCourseRatingStats>> getRatingsRanking(
            @RequestParam(defaultValue = "5") int top
    ) {
        return ResponseEntity.ok(reclamationService.getCourseRanking(top));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        reclamationService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/{id}/lecture", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> lecture(@PathVariable Long id) {
        return ResponseEntity.ok(reclamationService.buildLectureText(id));
    }

    @PostMapping("/dictation")
    public ResponseEntity<ReclamationService.ReclamationDictationResult> processDictation(
            @RequestBody DictationRequest request) {
        return ResponseEntity.ok(reclamationService.processDictation(request.text()));
    }

    @GetMapping(value = "/{id}/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getQrCode(@PathVariable Long id) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(reclamationService.generateQrCode(id));
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long id) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment().filename("reclamation-" + id + ".pdf").build()
        );
        return ResponseEntity.ok()
                .headers(headers)
                .body(reclamationService.generatePdf(id));
    }

    public record DictationRequest(String text) {
    }

    public record ClassificationAnalyzeRequest(String subject, String description) {
    }
}
