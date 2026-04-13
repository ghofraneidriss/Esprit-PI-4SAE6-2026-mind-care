package tn.esprit.microservice.feedbackms.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tn.esprit.microservice.feedbackms.client.ReclamationClient;
import tn.esprit.microservice.feedbackms.entity.Feedback;
import tn.esprit.microservice.feedbackms.entity.FeedbackModerationStatus;
import tn.esprit.microservice.feedbackms.exception.ResourceNotFoundException;
import tn.esprit.microservice.feedbackms.repository.FeedbackRepository;

@Service
public class FeedbackService {

    private static final int QR_SIZE = 320;
    private static final Pattern URL_PATTERN = Pattern.compile("(https?://\\S+|www\\.\\S+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern REPEATED_PUNCTUATION_PATTERN = Pattern.compile("([!?.,])\\1{2,}");

    private final FeedbackRepository feedbackRepository;
    private final ObjectMapper objectMapper;
    private final Set<String> blockedWords;
    private final int autoBlockThreshold;
    private final ReclamationClient reclamationClient;
    private final boolean autoReclamationEnabled;
    private final int negativeRatingThreshold;

    public FeedbackService(
            FeedbackRepository feedbackRepository,
            ObjectMapper objectMapper,
            @Value("${feedback.moderation.blocked-words:insulte,haine,raciste,violence,arnaque,spam}")
            String blockedWordsConfig,
            @Value("${feedback.moderation.auto-block-threshold:70}")
            int autoBlockThreshold,
            ReclamationClient reclamationClient,
            @Value("${feedback.auto-reclamation.enabled:true}")
            boolean autoReclamationEnabled,
            @Value("${feedback.auto-reclamation.negative-threshold:2}")
            int negativeRatingThreshold
    ) {
        this.feedbackRepository = feedbackRepository;
        this.objectMapper = objectMapper;
        this.blockedWords = parseBlockedWords(blockedWordsConfig);
        this.autoBlockThreshold = Math.max(0, Math.min(autoBlockThreshold, 100));
        this.reclamationClient = reclamationClient;
        this.autoReclamationEnabled = autoReclamationEnabled;
        this.negativeRatingThreshold = Math.max(1, Math.min(negativeRatingThreshold, 5));
    }

    public List<Feedback> findAll() {
        return feedbackRepository.findAll();
    }

    public List<Feedback> findApproved() {
        return feedbackRepository.findByModerationStatusOrderByCreatedAtDesc(FeedbackModerationStatus.APPROVED);
    }

    public List<Feedback> findBlocked() {
        return feedbackRepository.findByModerationStatusOrderByCreatedAtDesc(FeedbackModerationStatus.REJECTED);
    }

    public RatingSummary getRatingSummary() {
        return buildRatingSummary(feedbackRepository.findAll());
    }

    public List<CourseRatingStats> getCourseRatingStats() {
        return buildCourseRatingStats(feedbackRepository.findAll());
    }

    public List<CourseRatingStats> getCourseRanking(int top) {
        int safeTop = Math.max(top, 1);
        List<CourseRatingStats> stats = buildCourseRatingStats(feedbackRepository.findAll());
        stats.sort(
                Comparator.comparingDouble(CourseRatingStats::averageRating).reversed()
                        .thenComparing(Comparator.comparingLong(CourseRatingStats::feedbackCount).reversed())
                        .thenComparing(CourseRatingStats::courseTitle)
        );
        if (stats.size() <= safeTop) {
            return stats;
        }
        return new ArrayList<>(stats.subList(0, safeTop));
    }

    public Feedback findById(Long id) {
        return feedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Feedback with id " + id + " not found"));
    }

    public Feedback create(Feedback feedback) {
        feedback.setRating(normalizeRating(feedback.getRating()));
        applyAutoModeration(feedback);
        applyAutoReclamation(feedback);
        return feedbackRepository.save(feedback);
    }

    public Feedback update(Long id, Feedback request) {
        Feedback feedback = findById(id);
        feedback.setUserName(request.getUserName());
        feedback.setUserEmail(request.getUserEmail());
        feedback.setTitle(request.getTitle());
        feedback.setImageUrl(request.getImageUrl());
        feedback.setRating(normalizeRating(request.getRating()));
        feedback.setComment(request.getComment());
        applyAutoModeration(feedback);
        applyAutoReclamation(feedback);
        return feedbackRepository.save(feedback);
    }

    public void delete(Long id) {
        Feedback feedback = findById(id);
        feedbackRepository.delete(feedback);
    }

    public ModerationPreview analyzeDraft(String title, String comment) {
        return runModerationModel(title, comment);
    }

    public String buildLectureText(Long id) {
        Feedback feedback = findById(id);
        String rating = feedback.getRating() == null ? "-" : feedback.getRating().toString();
        return String.format(
                Locale.ROOT,
                "Feedback de %s. Titre: %s. Commentaire: %s. Note: %s sur 5.",
                safe(feedback.getUserName()),
                safe(feedback.getTitle()),
                safe(feedback.getComment()),
                rating
        );
    }

    public FeedbackDictationResult processDictation(String transcript) {
        String cleanedText = normalizeTranscript(transcript);
        if (cleanedText.isEmpty()) {
            return new FeedbackDictationResult("", "", "");
        }

        int splitIndex = cleanedText.indexOf(". ");
        String suggestedTitle;
        String suggestedComment;

        if (splitIndex > 0) {
            suggestedTitle = cleanedText.substring(0, splitIndex + 1).trim();
            suggestedComment = cleanedText.substring(splitIndex + 2).trim();
        } else {
            suggestedTitle = cleanedText.length() > 70
                    ? cleanedText.substring(0, 70).trim() + "..."
                    : cleanedText;
            suggestedComment = cleanedText;
        }

        return new FeedbackDictationResult(cleanedText, suggestedTitle, suggestedComment);
    }

    public byte[] generateQrCode(Long id) {
        Feedback feedback = findById(id);
        return encodeQrCode(buildQrPayload(feedback));
    }

    public byte[] generatePdf(Long id) {
        Feedback feedback = findById(id);
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 16);
                content.newLineAtOffset(50, 780);
                content.showText("Feedback");
                content.newLineAtOffset(0, -26);
                content.setFont(PDType1Font.HELVETICA, 12);

                for (String line : buildPdfLines(feedback)) {
                    for (String wrappedLine : wrapText(line, 92)) {
                        content.showText(wrappedLine);
                        content.newLineAtOffset(0, -16);
                    }
                }
                content.endText();
            }

            document.save(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to generate feedback PDF", ex);
        }
    }

    private List<String> buildPdfLines(Feedback feedback) {
        List<String> lines = new ArrayList<>();
        lines.add("ID: " + (feedback.getId() == null ? "-" : feedback.getId()));
        lines.add("Nom: " + safe(feedback.getUserName()));
        lines.add("Email: " + safe(feedback.getUserEmail()));
        lines.add("Titre: " + safe(feedback.getTitle()));
        lines.add("Image URL: " + safe(feedback.getImageUrl()));
        lines.add("Note: " + (feedback.getRating() == null ? "-" : feedback.getRating() + "/5"));
        lines.add("Moderation status: " + (feedback.getModerationStatus() == null
                ? "-"
                : feedback.getModerationStatus().name()));
        lines.add("Moderation score: " + (feedback.getModerationScore() == null ? "-" : feedback.getModerationScore()));
        lines.add("Moderation flagged: " + (feedback.getModerationFlagged() == null ? "-" : feedback.getModerationFlagged()));
        lines.add("Blocked words: " + safe(feedback.getBlockedWords()));
        lines.add("Moderation note: " + safe(feedback.getModerationNote()));
        lines.add("Auto reclamation created: " + (feedback.getAutoReclamationCreated() == null ? "-" : feedback.getAutoReclamationCreated()));
        lines.add("Linked reclamation id: " + (feedback.getLinkedReclamationId() == null ? "-" : feedback.getLinkedReclamationId()));
        lines.add("Auto reclamation status: " + safe(feedback.getAutoReclamationStatus()));
        lines.add("Commentaire: " + safe(feedback.getComment()));
        lines.add("Date creation: " + (feedback.getCreatedAt() == null ? "-" : feedback.getCreatedAt()));
        return lines;
    }

    private String buildQrPayload(Feedback feedback) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", feedback.getId());
        payload.put("userName", feedback.getUserName());
        payload.put("userEmail", feedback.getUserEmail());
        payload.put("title", feedback.getTitle());
        payload.put("rating", feedback.getRating());
        payload.put("comment", feedback.getComment());
        payload.put("moderationStatus", feedback.getModerationStatus());
        payload.put("moderationScore", feedback.getModerationScore());
        payload.put("moderationFlagged", feedback.getModerationFlagged());
        payload.put("blockedWords", feedback.getBlockedWords());
        payload.put("moderationNote", feedback.getModerationNote());
        payload.put("autoReclamationCreated", feedback.getAutoReclamationCreated());
        payload.put("linkedReclamationId", feedback.getLinkedReclamationId());
        payload.put("autoReclamationStatus", feedback.getAutoReclamationStatus());
        payload.put("createdAt", feedback.getCreatedAt());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize feedback QR payload", ex);
        }
    }

    private void applyAutoReclamation(Feedback feedback) {
        if (!autoReclamationEnabled) {
            if (feedback.getLinkedReclamationId() != null) {
                feedback.setAutoReclamationCreated(true);
                feedback.setAutoReclamationStatus("DISABLED_ALREADY_LINKED");
            } else {
                feedback.setAutoReclamationCreated(false);
                feedback.setAutoReclamationStatus("DISABLED");
            }
            return;
        }

        int rating = normalizeRating(feedback.getRating());
        feedback.setRating(rating);

        if (rating > negativeRatingThreshold) {
            if (feedback.getLinkedReclamationId() != null) {
                feedback.setAutoReclamationCreated(true);
                feedback.setAutoReclamationStatus("ALREADY_CREATED");
            } else {
                feedback.setAutoReclamationCreated(false);
                feedback.setLinkedReclamationId(null);
                feedback.setAutoReclamationStatus("SKIPPED_NOT_NEGATIVE");
            }
            return;
        }

        if (feedback.getLinkedReclamationId() != null) {
            feedback.setAutoReclamationCreated(true);
            feedback.setAutoReclamationStatus("ALREADY_CREATED");
            return;
        }

        try {
            ReclamationClient.ReclamationResponse response =
                    reclamationClient.createReclamation(buildAutoReclamationPayload(feedback));
            if (response != null && response.id() != null) {
                feedback.setAutoReclamationCreated(true);
                feedback.setLinkedReclamationId(response.id());
                feedback.setAutoReclamationStatus("CREATED");
                return;
            }

            feedback.setAutoReclamationCreated(false);
            feedback.setLinkedReclamationId(null);
            feedback.setAutoReclamationStatus("FAILED_EMPTY_RESPONSE");
        } catch (Exception ex) {
            feedback.setAutoReclamationCreated(false);
            feedback.setLinkedReclamationId(null);
            feedback.setAutoReclamationStatus("FAILED: " + abbreviate(safe(ex.getMessage()), 180));
        }
    }

    private ReclamationClient.ReclamationCreateRequest buildAutoReclamationPayload(Feedback feedback) {
        return new ReclamationClient.ReclamationCreateRequest(
                feedback.getUserName(),
                feedback.getUserEmail(),
                mapForumTitleToReclamationSubject(feedback.getTitle()),
                feedback.getImageUrl(),
                normalizeRating(feedback.getRating()),
                buildAutoReclamationDescription(feedback),
                "OPEN"
        );
    }

    private String mapForumTitleToReclamationSubject(String forumTitle) {
        String normalizedTitle = normalizeTranscript(forumTitle);
        if (normalizedTitle.isEmpty()) {
            return "Feedback negatif - sujet non renseigne";
        }
        return abbreviate(normalizedTitle, 255);
    }

    private String buildAutoReclamationDescription(Feedback feedback) {
        String description = "Reclamation auto depuis feedback negatif. Commentaire: "
                + safe(feedback.getComment())
                + ". Note: "
                + normalizeRating(feedback.getRating())
                + "/5.";
        return abbreviate(normalizeTranscript(description), 255);
    }

    private String abbreviate(String value, int maxLength) {
        String normalized = normalizeTranscript(value);
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength - 3).trim() + "...";
    }

    private RatingSummary buildRatingSummary(List<Feedback> feedbacks) {
        long feedbackCount = feedbacks.size();
        long ratedFeedbackCount = 0;
        long ratingSum = 0;

        for (Feedback feedback : feedbacks) {
            Integer rating = feedback.getRating();
            if (rating == null) {
                continue;
            }
            ratedFeedbackCount++;
            ratingSum += rating;
        }

        double averageRating = ratedFeedbackCount == 0 ? 0d : roundRating((double) ratingSum / ratedFeedbackCount);
        int rankedCourseCount = buildCourseRatingStats(feedbacks).size();
        return new RatingSummary(averageRating, feedbackCount, ratedFeedbackCount, rankedCourseCount);
    }

    private List<CourseRatingStats> buildCourseRatingStats(List<Feedback> feedbacks) {
        Map<String, CourseRatingAccumulator> accumulators = new LinkedHashMap<>();

        for (Feedback feedback : feedbacks) {
            String courseTitle = normalizeTranscript(feedback.getTitle());
            if (courseTitle.isEmpty()) {
                courseTitle = "Cours non renseigne";
            }

            CourseRatingAccumulator accumulator = accumulators.computeIfAbsent(
                    courseTitle,
                    CourseRatingAccumulator::new
            );
            accumulator.feedbackCount++;

            Integer rating = feedback.getRating();
            if (rating == null) {
                continue;
            }

            accumulator.ratedFeedbackCount++;
            accumulator.ratingSum += rating;
        }

        List<CourseRatingStats> stats = new ArrayList<>();
        for (CourseRatingAccumulator accumulator : accumulators.values()) {
            double average = accumulator.ratedFeedbackCount == 0
                    ? 0d
                    : roundRating((double) accumulator.ratingSum / accumulator.ratedFeedbackCount);
            stats.add(new CourseRatingStats(accumulator.courseTitle, average, accumulator.feedbackCount));
        }
        return stats;
    }

    private double roundRating(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }

    private void applyAutoModeration(Feedback feedback) {
        ModerationPreview preview = runModerationModel(feedback.getTitle(), feedback.getComment());
        feedback.setModerationStatus(preview.flagged() ? FeedbackModerationStatus.REJECTED : FeedbackModerationStatus.APPROVED);
        feedback.setModerationScore(preview.score());
        feedback.setModerationFlagged(preview.flagged());
        feedback.setBlockedWords(preview.blockedWords().isEmpty() ? "" : String.join(", ", preview.blockedWords()));
        feedback.setModerationNote(buildAutoModerationNote(preview));
        feedback.setModeratedBy("AUTO_MODEL");
        feedback.setModeratedAt(LocalDateTime.now());
    }

    private String buildAutoModerationNote(ModerationPreview preview) {
        return "Auto moderation -> decision="
                + (preview.flagged() ? "BLOCKED" : "APPROVED")
                + ", score="
                + preview.score()
                + ", threshold="
                + autoBlockThreshold
                + ", reason="
                + preview.reason();
    }

    private ModerationPreview runModerationModel(String title, String comment) {
        String mergedText = normalizeTranscript(title) + " " + normalizeTranscript(comment);
        String normalizedForModel = normalizeForMatching(mergedText);
        List<String> matchedWords = findBlockedWords(normalizedForModel);

        int score = 0;
        score += matchedWords.size() * 45;
        if (URL_PATTERN.matcher(mergedText).find()) {
            score += 10;
        }
        if (REPEATED_PUNCTUATION_PATTERN.matcher(mergedText).find()) {
            score += 12;
        }
        if (containsSpamPattern(normalizedForModel)) {
            score += 20;
        }

        double upperCaseRatio = calculateUpperCaseRatio(mergedText);
        if (upperCaseRatio > 0.45d) {
            score += 8;
        }

        if (normalizeTranscript(comment).length() < 8) {
            score += 7;
        }

        score = Math.min(score, 100);

        boolean hasBlockedWords = !matchedWords.isEmpty();
        boolean highRiskScore = score >= autoBlockThreshold;
        boolean flagged = hasBlockedWords || highRiskScore;

        String recommendation = flagged ? "BLOCK" : "ALLOW";

        String reason;
        if (hasBlockedWords && highRiskScore) {
            reason = "Blocked words detected and score above threshold";
        } else if (hasBlockedWords) {
            reason = "Blocked words detected: " + String.join(", ", matchedWords);
        } else if (highRiskScore) {
            reason = "Score " + score + " is above threshold " + autoBlockThreshold;
        } else {
            reason = "Score below threshold and no blocked words";
        }

        return new ModerationPreview(score, flagged, matchedWords, recommendation, reason);
    }

    private Set<String> parseBlockedWords(String blockedWordsConfig) {
        Set<String> parsed = new LinkedHashSet<>();
        Arrays.stream(blockedWordsConfig.split(","))
                .map(this::normalizeForMatching)
                .map(String::trim)
                .filter(word -> !word.isEmpty())
                .forEach(parsed::add);
        return parsed;
    }

    private List<String> findBlockedWords(String normalizedText) {
        List<String> matches = new ArrayList<>();
        for (String word : blockedWords) {
            if (word.contains(" ")) {
                if (normalizedText.contains(word)) {
                    matches.add(word);
                }
                continue;
            }

            Pattern wordPattern = Pattern.compile("\\b" + Pattern.quote(word) + "\\b");
            if (wordPattern.matcher(normalizedText).find()) {
                matches.add(word);
            }
        }
        return matches;
    }

    private boolean containsSpamPattern(String normalizedText) {
        return normalizedText.contains("argent facile")
                || normalizedText.contains("clique ici")
                || normalizedText.contains("offre limitee")
                || normalizedText.contains("gagner vite");
    }

    private double calculateUpperCaseRatio(String text) {
        int letters = 0;
        int uppercase = 0;
        for (char c : text.toCharArray()) {
            if (Character.isLetter(c)) {
                letters++;
                if (Character.isUpperCase(c)) {
                    uppercase++;
                }
            }
        }
        if (letters == 0) {
            return 0d;
        }
        return (double) uppercase / letters;
    }

    private String normalizeForMatching(String value) {
        String normalized = normalizeTranscript(value);
        String withoutAccents = Normalizer.normalize(normalized, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccents
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private byte[] encodeQrCode(String payload) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            BitMatrix matrix = new MultiFormatWriter().encode(payload, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return output.toByteArray();
        } catch (WriterException | IOException ex) {
            throw new IllegalStateException("Unable to generate feedback QR code", ex);
        }
    }

    private List<String> wrapText(String text, int maxLineLength) {
        String normalized = normalizeTranscript(text);
        if (normalized.isEmpty()) {
            return List.of("-");
        }

        List<String> wrappedLines = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();
        for (String word : normalized.split(" ")) {
            if (currentLine.isEmpty()) {
                currentLine.append(word);
                continue;
            }

            if (currentLine.length() + 1 + word.length() > maxLineLength) {
                wrappedLines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            } else {
                currentLine.append(' ').append(word);
            }
        }

        if (!currentLine.isEmpty()) {
            wrappedLines.add(currentLine.toString());
        }
        return wrappedLines;
    }

    private String normalizeTranscript(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private String safe(String value) {
        String normalized = normalizeTranscript(value);
        return normalized.isEmpty() ? "-" : normalized;
    }

    private int normalizeRating(Integer rating) {
        if (rating == null) {
            return 3;
        }
        return Math.max(1, Math.min(rating, 5));
    }

    private static final class CourseRatingAccumulator {
        private final String courseTitle;
        private long feedbackCount;
        private long ratedFeedbackCount;
        private long ratingSum;

        private CourseRatingAccumulator(String courseTitle) {
            this.courseTitle = courseTitle;
        }
    }

    public record RatingSummary(double averageRating, long feedbackCount, long ratedFeedbackCount,
                                int rankedCourseCount) {
    }

    public record CourseRatingStats(String courseTitle, double averageRating, long feedbackCount) {
    }

    public record FeedbackDictationResult(String cleanedText, String suggestedTitle, String suggestedComment) {
    }

    public record ModerationPreview(int score, boolean flagged, List<String> blockedWords,
                                    String recommendation, String reason) {
    }
}
