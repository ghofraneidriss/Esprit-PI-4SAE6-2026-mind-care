package tn.esprit.microservice.reclamationms.service;

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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tn.esprit.microservice.reclamationms.entity.Reclamation;
import tn.esprit.microservice.reclamationms.entity.ReclamationClassificationLevel;
import tn.esprit.microservice.reclamationms.exception.ResourceNotFoundException;
import tn.esprit.microservice.reclamationms.repository.ReclamationRepository;

@Service
public class ReclamationService {

    private static final int QR_SIZE = 320;
    private static final int HIGH_KEYWORD_SCORE = 75;
    private static final int MEDIUM_KEYWORD_SCORE = 40;
    private static final int LOW_KEYWORD_SCORE = 10;

    private final ReclamationRepository reclamationRepository;
    private final ObjectMapper objectMapper;
    private final Set<String> highKeywords;
    private final Set<String> mediumKeywords;
    private final Set<String> lowKeywords;
    private final int highThreshold;
    private final int mediumThreshold;

    public ReclamationService(
            ReclamationRepository reclamationRepository,
            ObjectMapper objectMapper,
            @Value("${reclamation.classification.high-keywords:retard intervention,urgence,danger,accident,panne totale,service indisponible,impossible d utiliser,bloque}")
            String highKeywordsConfig,
            @Value("${reclamation.classification.medium-keywords:bug,erreur,deconnexion,paiement,lent,instable,retard,interruption}")
            String mediumKeywordsConfig,
            @Value("${reclamation.classification.low-keywords:gps ne marche pas,gps,suggestion,affichage}")
            String lowKeywordsConfig,
            @Value("${reclamation.classification.high-threshold:70}")
            int highThreshold,
            @Value("${reclamation.classification.medium-threshold:35}")
            int mediumThreshold
    ) {
        this.reclamationRepository = reclamationRepository;
        this.objectMapper = objectMapper;
        this.highKeywords = parseKeywordConfig(highKeywordsConfig);
        this.mediumKeywords = parseKeywordConfig(mediumKeywordsConfig);
        this.lowKeywords = parseKeywordConfig(lowKeywordsConfig);
        this.highThreshold = Math.max(0, Math.min(100, highThreshold));
        this.mediumThreshold = Math.max(0, Math.min(this.highThreshold, mediumThreshold));
    }

    public List<Reclamation> findAll() {
        return reclamationRepository.findAll();
    }

    public List<Reclamation> findByClassificationLevel(ReclamationClassificationLevel level) {
        return reclamationRepository.findByClassificationLevelOrderByCreatedAtDesc(level);
    }

    public ReclamationRatingSummary getRatingSummary() {
        return buildRatingSummary(reclamationRepository.findAll());
    }

    public List<ReclamationCourseRatingStats> getCourseRatingStats() {
        return buildCourseRatingStats(reclamationRepository.findAll());
    }

    public List<ReclamationCourseRatingStats> getCourseRanking(int top) {
        int safeTop = Math.max(top, 1);
        List<ReclamationCourseRatingStats> stats = buildCourseRatingStats(reclamationRepository.findAll());
        stats.sort(
                Comparator.comparingDouble(ReclamationCourseRatingStats::averageRating).reversed()
                        .thenComparing(Comparator.comparingLong(ReclamationCourseRatingStats::reclamationCount).reversed())
                        .thenComparing(ReclamationCourseRatingStats::courseTitle)
        );
        if (stats.size() <= safeTop) {
            return stats;
        }
        return new ArrayList<>(stats.subList(0, safeTop));
    }

    public Reclamation findById(Long id) {
        return reclamationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reclamation with id " + id + " not found"));
    }

    public Reclamation create(Reclamation reclamation) {
        reclamation.setRating(normalizeRating(reclamation.getRating()));
        applyAutoClassification(reclamation);
        return reclamationRepository.save(reclamation);
    }

    public Reclamation update(Long id, Reclamation request) {
        Reclamation reclamation = findById(id);
        reclamation.setUserName(request.getUserName());
        reclamation.setUserEmail(request.getUserEmail());
        reclamation.setSubject(request.getSubject());
        reclamation.setImageUrl(request.getImageUrl());
        reclamation.setRating(normalizeRating(request.getRating()));
        reclamation.setDescription(request.getDescription());
        reclamation.setStatus(request.getStatus());
        applyAutoClassification(reclamation);
        return reclamationRepository.save(reclamation);
    }

    public void delete(Long id) {
        Reclamation reclamation = findById(id);
        reclamationRepository.delete(reclamation);
    }

    public ClassificationPreview analyzeClassification(String subject, String description) {
        return runClassificationModel(subject, description);
    }

    public String buildLectureText(Long id) {
        Reclamation reclamation = findById(id);
        return String.format(
                Locale.ROOT,
            "Reclamation de %s. Sujet: %s. Description: %s. Note: %s sur 5. Statut: %s. Classification: %s.",
                safe(reclamation.getUserName()),
                safe(reclamation.getSubject()),
                safe(reclamation.getDescription()),
            reclamation.getRating() == null ? "-" : reclamation.getRating(),
                reclamation.getStatus() == null ? "-" : reclamation.getStatus().name(),
                reclamation.getClassificationLevel() == null ? "-" : reclamation.getClassificationLevel().name()
        );
    }

    public ReclamationDictationResult processDictation(String transcript) {
        String cleanedText = normalizeTranscript(transcript);
        if (cleanedText.isEmpty()) {
            return new ReclamationDictationResult("", "", "");
        }

        int splitIndex = cleanedText.indexOf(". ");
        String suggestedSubject;
        String suggestedDescription;

        if (splitIndex > 0) {
            suggestedSubject = cleanedText.substring(0, splitIndex + 1).trim();
            suggestedDescription = cleanedText.substring(splitIndex + 2).trim();
        } else {
            suggestedSubject = cleanedText.length() > 80
                    ? cleanedText.substring(0, 80).trim() + "..."
                    : cleanedText;
            suggestedDescription = cleanedText;
        }

        return new ReclamationDictationResult(cleanedText, suggestedSubject, suggestedDescription);
    }

    public byte[] generateQrCode(Long id) {
        Reclamation reclamation = findById(id);
        return encodeQrCode(buildQrPayload(reclamation));
    }

    public byte[] generatePdf(Long id) {
        Reclamation reclamation = findById(id);
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 16);
                content.newLineAtOffset(50, 780);
                content.showText("Reclamation");
                content.newLineAtOffset(0, -26);
                content.setFont(PDType1Font.HELVETICA, 12);

                for (String line : buildPdfLines(reclamation)) {
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
            throw new IllegalStateException("Unable to generate reclamation PDF", ex);
        }
    }

    private List<String> buildPdfLines(Reclamation reclamation) {
        List<String> lines = new ArrayList<>();
        lines.add("ID: " + (reclamation.getId() == null ? "-" : reclamation.getId()));
        lines.add("Nom: " + safe(reclamation.getUserName()));
        lines.add("Email: " + safe(reclamation.getUserEmail()));
        lines.add("Sujet: " + safe(reclamation.getSubject()));
        lines.add("Image URL: " + safe(reclamation.getImageUrl()));
        lines.add("Note: " + (reclamation.getRating() == null ? "-" : reclamation.getRating() + "/5"));
        lines.add("Statut: " + (reclamation.getStatus() == null ? "-" : reclamation.getStatus().name()));
        lines.add("Classification: " + (reclamation.getClassificationLevel() == null
            ? "-"
            : reclamation.getClassificationLevel().name()));
        lines.add("Classification score: " + (reclamation.getClassificationScore() == null
            ? "-"
            : reclamation.getClassificationScore()));
        lines.add("Classification keywords: " + safe(reclamation.getClassificationKeywords()));
        lines.add("Classification reason: " + safe(reclamation.getClassificationReason()));
        lines.add("Description: " + safe(reclamation.getDescription()));
        lines.add("Date creation: " + (reclamation.getCreatedAt() == null ? "-" : reclamation.getCreatedAt()));
        return lines;
    }

    private String buildQrPayload(Reclamation reclamation) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", reclamation.getId());
        payload.put("userName", reclamation.getUserName());
        payload.put("userEmail", reclamation.getUserEmail());
        payload.put("subject", reclamation.getSubject());
        payload.put("rating", reclamation.getRating());
        payload.put("status", reclamation.getStatus());
        payload.put("classificationLevel", reclamation.getClassificationLevel());
        payload.put("classificationScore", reclamation.getClassificationScore());
        payload.put("classificationKeywords", reclamation.getClassificationKeywords());
        payload.put("classificationReason", reclamation.getClassificationReason());
        payload.put("description", reclamation.getDescription());
        payload.put("createdAt", reclamation.getCreatedAt());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize reclamation QR payload", ex);
        }
    }

    private void applyAutoClassification(Reclamation reclamation) {
        ClassificationPreview preview = runClassificationModel(reclamation.getSubject(), reclamation.getDescription());
        reclamation.setClassificationLevel(preview.level());
        reclamation.setClassificationScore(preview.score());
        reclamation.setClassificationKeywords(
                preview.matchedKeywords().isEmpty() ? "" : String.join(", ", preview.matchedKeywords())
        );
        reclamation.setClassificationReason(buildAutoClassificationReason(preview));
        reclamation.setClassifiedBy("AUTO_CLASSIFIER");
        reclamation.setClassifiedAt(LocalDateTime.now());
    }

    private String buildAutoClassificationReason(ClassificationPreview preview) {
        return "Auto classification -> level="
                + preview.level().name()
                + ", score="
                + preview.score()
                + ", thresholdMedium="
                + mediumThreshold
                + ", thresholdHigh="
                + highThreshold
                + ", reason="
                + preview.reason();
    }

    private ClassificationPreview runClassificationModel(String subject, String description) {
        String mergedText = normalizeTranscript(subject) + " " + normalizeTranscript(description);
        String normalizedText = normalizeForMatching(mergedText);

        List<String> matchedKeywords = new ArrayList<>();
        List<String> reasons = new ArrayList<>();

        int score = 0;
        int highAdded = addKeywordScore(normalizedText, highKeywords, HIGH_KEYWORD_SCORE, matchedKeywords);
        int mediumAdded = addKeywordScore(normalizedText, mediumKeywords, MEDIUM_KEYWORD_SCORE, matchedKeywords);
        int lowAdded = addKeywordScore(normalizedText, lowKeywords, LOW_KEYWORD_SCORE, matchedKeywords);

        score += highAdded + mediumAdded + lowAdded;

        if (highAdded > 0) {
            reasons.add("high impact keywords detected");
        }
        if (mediumAdded > 0) {
            reasons.add("service quality issue keywords detected");
        }
        if (lowAdded > 0) {
            reasons.add("minor issue keywords detected");
        }

        if (normalizedText.contains("urgent") || normalizedText.contains("immediat")) {
            score += 15;
            reasons.add("urgent context detected");
        }

        if (normalizedText.contains("pas de reponse") || normalizedText.contains("aucune reponse")) {
            score += 20;
            reasons.add("no response context detected");
        }

        if (normalizedText.contains("depuis")
                && (normalizedText.contains("jour") || normalizedText.contains("semaine"))) {
            score += 10;
            reasons.add("delay duration context detected");
        }

        score = Math.min(score, 100);

        ReclamationClassificationLevel level;
        if (score >= highThreshold) {
            level = ReclamationClassificationLevel.FORT;
        } else if (score >= mediumThreshold) {
            level = ReclamationClassificationLevel.MOYEN;
        } else {
            level = ReclamationClassificationLevel.FAIBLE;
        }

        String reason;
        if (reasons.isEmpty()) {
            reason = "No critical signal detected";
        } else {
            reason = String.join(", ", reasons);
        }

        if (!matchedKeywords.isEmpty()) {
            reason = reason + " | keywords=" + String.join(", ", matchedKeywords);
        }

        return new ClassificationPreview(level, score, matchedKeywords, reason);
    }

    private int addKeywordScore(String normalizedText, Set<String> keywords, int increment, List<String> matches) {
        int added = 0;
        for (String keyword : keywords) {
            if (keyword.isEmpty()) {
                continue;
            }
            if (!normalizedText.contains(keyword)) {
                continue;
            }
            if (!matches.contains(keyword)) {
                matches.add(keyword);
            }
            added += increment;
        }
        return added;
    }

    private Set<String> parseKeywordConfig(String config) {
        Set<String> parsed = new LinkedHashSet<>();
        Arrays.stream(config.split(","))
                .map(this::normalizeForMatching)
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .forEach(parsed::add);
        return parsed;
    }

    private ReclamationRatingSummary buildRatingSummary(List<Reclamation> reclamations) {
        long reclamationCount = reclamations.size();
        long ratedReclamationCount = 0;
        long ratingSum = 0;

        for (Reclamation reclamation : reclamations) {
            Integer rating = reclamation.getRating();
            if (rating == null) {
                continue;
            }
            ratedReclamationCount++;
            ratingSum += rating;
        }

        double averageRating = ratedReclamationCount == 0
                ? 0d
                : roundRating((double) ratingSum / ratedReclamationCount);
        int rankedCourseCount = buildCourseRatingStats(reclamations).size();

        return new ReclamationRatingSummary(averageRating, reclamationCount, ratedReclamationCount, rankedCourseCount);
    }

    private List<ReclamationCourseRatingStats> buildCourseRatingStats(List<Reclamation> reclamations) {
        Map<String, ReclamationCourseAccumulator> accumulators = new LinkedHashMap<>();

        for (Reclamation reclamation : reclamations) {
            String courseTitle = normalizeTranscript(reclamation.getSubject());
            if (courseTitle.isEmpty()) {
                courseTitle = "Cours non renseigne";
            }

            ReclamationCourseAccumulator accumulator = accumulators.computeIfAbsent(
                    courseTitle,
                    ReclamationCourseAccumulator::new
            );
            accumulator.reclamationCount++;

            Integer rating = reclamation.getRating();
            if (rating == null) {
                continue;
            }

            accumulator.ratedReclamationCount++;
            accumulator.ratingSum += rating;
        }

        List<ReclamationCourseRatingStats> stats = new ArrayList<>();
        for (ReclamationCourseAccumulator accumulator : accumulators.values()) {
            double average = accumulator.ratedReclamationCount == 0
                    ? 0d
                    : roundRating((double) accumulator.ratingSum / accumulator.ratedReclamationCount);
            stats.add(new ReclamationCourseRatingStats(accumulator.courseTitle, average, accumulator.reclamationCount));
        }
        return stats;
    }

    private double roundRating(double value) {
        return Math.round(value * 100.0d) / 100.0d;
    }

    private byte[] encodeQrCode(String payload) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            BitMatrix matrix = new MultiFormatWriter().encode(payload, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return output.toByteArray();
        } catch (WriterException | IOException ex) {
            throw new IllegalStateException("Unable to generate reclamation QR code", ex);
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

    private static final class ReclamationCourseAccumulator {
        private final String courseTitle;
        private long reclamationCount;
        private long ratedReclamationCount;
        private long ratingSum;

        private ReclamationCourseAccumulator(String courseTitle) {
            this.courseTitle = courseTitle;
        }
    }

    public record ReclamationRatingSummary(double averageRating, long reclamationCount, long ratedReclamationCount,
                                           int rankedCourseCount) {
    }

    public record ReclamationCourseRatingStats(String courseTitle, double averageRating, long reclamationCount) {
    }

    public record ReclamationDictationResult(String cleanedText, String suggestedSubject,
                                             String suggestedDescription) {
    }

    public record ClassificationPreview(ReclamationClassificationLevel level, int score,
                                        List<String> matchedKeywords, String reason) {
    }
}
