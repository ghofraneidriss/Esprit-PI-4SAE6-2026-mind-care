package tn.esprit.microservice.feedbackms.controller;

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
import tn.esprit.microservice.feedbackms.entity.Feedback;
import tn.esprit.microservice.feedbackms.service.FeedbackService;

@RestController
@RequestMapping("/feedbacks")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @GetMapping
    public List<Feedback> getAll() {
        return feedbackService.findAll();
    }

    @GetMapping("/{id}")
    public Feedback getById(@PathVariable Long id) {
        return feedbackService.findById(id);
    }

    @PostMapping
    public ResponseEntity<Feedback> create(@RequestBody Feedback feedback) {
        return ResponseEntity.ok(feedbackService.create(feedback));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Feedback> update(@PathVariable Long id, @RequestBody Feedback feedback) {
        return ResponseEntity.ok(feedbackService.update(id, feedback));
    }

    @GetMapping("/approved")
    public List<Feedback> getApproved() {
        return feedbackService.findApproved();
    }

    @GetMapping("/blocked")
    public List<Feedback> getBlocked() {
        return feedbackService.findBlocked();
    }

    @GetMapping("/ratings/summary")
    public ResponseEntity<FeedbackService.RatingSummary> getRatingSummary() {
        return ResponseEntity.ok(feedbackService.getRatingSummary());
    }

    @GetMapping("/ratings/by-course")
    public ResponseEntity<List<FeedbackService.CourseRatingStats>> getRatingsByCourse() {
        return ResponseEntity.ok(feedbackService.getCourseRatingStats());
    }

    @GetMapping("/ratings/ranking")
    public ResponseEntity<List<FeedbackService.CourseRatingStats>> getRatingsRanking(
            @RequestParam(defaultValue = "5") int top
    ) {
        return ResponseEntity.ok(feedbackService.getCourseRanking(top));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        feedbackService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/{id}/lecture", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> lecture(@PathVariable Long id) {
        return ResponseEntity.ok(feedbackService.buildLectureText(id));
    }

    @PostMapping("/dictation")
    public ResponseEntity<FeedbackService.FeedbackDictationResult> processDictation(
            @RequestBody DictationRequest request) {
        return ResponseEntity.ok(feedbackService.processDictation(request.text()));
    }

    @PostMapping("/moderation/analyze")
    public ResponseEntity<FeedbackService.ModerationPreview> analyzeModeration(
            @RequestBody ModerationAnalyzeRequest request) {
        return ResponseEntity.ok(feedbackService.analyzeDraft(request.title(), request.comment()));
    }

    @GetMapping(value = "/{id}/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getQrCode(@PathVariable Long id) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(feedbackService.generateQrCode(id));
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long id) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.attachment().filename("feedback-" + id + ".pdf").build()
        );
        return ResponseEntity.ok()
                .headers(headers)
                .body(feedbackService.generatePdf(id));
    }

    public record DictationRequest(String text) {
    }

    public record ModerationAnalyzeRequest(String title, String comment) {
    }
}
