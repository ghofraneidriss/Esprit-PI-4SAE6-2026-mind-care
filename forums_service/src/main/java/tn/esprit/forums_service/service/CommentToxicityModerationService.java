package tn.esprit.forums_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.regex.Pattern;

/**
 * Calls Hugging Face Inference API (hosted model) to score comment text for toxicity.
 * No static word list: the model infers from context. English-centric (toxic-bert).
 * Base URL must use the current router host ({@code router.huggingface.co/hf-inference}); the legacy
 * {@code api-inference.huggingface.co} endpoint returns HTTP 410.
 */
@Slf4j
@Service
public class CommentToxicityModerationService {

    private static final int MAX_INPUT_CHARS = 1500;

    /**
     * Fallback when HF is down or returns an error: obvious profanity (EN/FR) without external calls.
     * Uses word boundaries where possible to limit false positives.
     */
    private static final Pattern[] LOCAL_PROFANITY = new Pattern[]{
            Pattern.compile("(?iu)\\bf+u+c+k+(?:ing|ed|er)?\\b"),
            Pattern.compile("(?iu)\\bshit\\b"),
            Pattern.compile("(?iu)\\bbitch(?:es)?\\b"),
            Pattern.compile("(?iu)\\basshole\\b"),
            Pattern.compile("(?iu)\\bcunt\\b"),
            Pattern.compile("(?iu)\\bdick\\b"),
            Pattern.compile("(?iu)\\bcock\\b"),
            Pattern.compile("(?iu)\\bslut\\b"),
            Pattern.compile("(?iu)\\bmerde\\b"),
            Pattern.compile("(?iu)\\bputain\\b"),
            Pattern.compile("(?iu)\\bconnard\\b"),
            Pattern.compile("(?iu)\\bsalope\\b"),
            Pattern.compile("(?iu)\\bfoutre\\b"),
            Pattern.compile("(?iu)\\bencul[eé]\\w*\\b"),
            Pattern.compile("(?iu)\\b(nigger|nigga|niggas)\\b"),
    };

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${mindcare.forum.moderation.enabled:false}")
    private boolean moderationEnabled;

    @Value("${mindcare.forum.moderation.huggingface.api-token:}")
    private String apiToken;

    @Value("${mindcare.forum.moderation.huggingface.api-base:https://router.huggingface.co/hf-inference/models}")
    private String apiBase;

    @Value("${mindcare.forum.moderation.huggingface.model-id:unitary/toxic-bert}")
    private String modelId;

    @Value("${mindcare.forum.moderation.score-threshold:0.55}")
    private double scoreThreshold;

    @Value("${mindcare.forum.moderation.masked-text:[Comment removed - does not meet community standards.]}")
    private String maskedText;

    @Value("${mindcare.forum.moderation.local-fallback.enabled:true}")
    private boolean localFallbackEnabled;

    public CommentToxicityModerationService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Returns {@code raw} unchanged when moderation is off, API is missing, or the call fails.
     * When the model scores above the threshold, returns {@link #maskedText}.
     */
    public String moderateIfNeeded(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        if (localFallbackEnabled && matchesLocalProfanity(raw)) {
            return maskedText;
        }
        if (!moderationEnabled) {
            return raw;
        }
        if (apiToken == null || apiToken.isBlank()) {
            log.warn("Forum moderation enabled but mindcare.forum.moderation.huggingface.api-token is empty");
            return raw;
        }
        try {
            if (isToxicByModel(raw)) {
                return maskedText;
            }
        } catch (Exception e) {
            log.warn("Toxicity check failed, leaving comment as-is: {}", e.getMessage());
        }
        return raw;
    }

    /**
     * Masks profanity for API responses (including rows stored before moderation worked).
     * Does not call Hugging Face — uses the same local list as {@link #moderateIfNeeded(String)} for consistency.
     */
    public String maskContentForDisplay(String stored) {
        if (stored == null || stored.isBlank()) {
            return stored;
        }
        if (localFallbackEnabled && matchesLocalProfanity(stored)) {
            return maskedText;
        }
        return stored;
    }

    private boolean matchesLocalProfanity(String raw) {
        for (Pattern p : LOCAL_PROFANITY) {
            if (p.matcher(raw).find()) {
                return true;
            }
        }
        return false;
    }

    private boolean isToxicByModel(String text) throws Exception {
        String truncated = text.length() > MAX_INPUT_CHARS ? text.substring(0, MAX_INPUT_CHARS) : text;
        String base = apiBase.endsWith("/") ? apiBase.substring(0, apiBase.length() - 1) : apiBase;
        String url = base + "/" + modelId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiToken.trim());

        String jsonBody = objectMapper.createObjectNode().put("inputs", truncated).toString();
        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        String body;
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
            body = response.getBody();
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().value() == 503) {
                try {
                    Thread.sleep(12000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
                ResponseEntity<String> retry = restTemplate.postForEntity(url, entity, String.class);
                body = retry.getBody();
            } else {
                int code = ex.getStatusCode().value();
                if (code == 410) {
                    log.warn("HuggingFace HTTP 410 (inference host deprecated). Set mindcare.forum.moderation.huggingface.api-base to https://router.huggingface.co/hf-inference/models — {}",
                            truncate(ex.getResponseBodyAsString(), 300));
                } else {
                    log.warn("HuggingFace HTTP {}: {}", code, truncate(ex.getResponseBodyAsString(), 300));
                }
                return false;
            }
        } catch (RestClientException ex) {
            log.warn("HuggingFace request failed: {}", ex.getMessage());
            return false;
        }

        if (body == null || body.isBlank()) {
            return false;
        }
        return scoresAboveThreshold(body);
    }

    private boolean scoresAboveThreshold(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        if (root.isObject() && root.has("error")) {
            log.warn("HuggingFace API error payload: {}", truncate(root.toString(), 400));
            return false;
        }
        double maxBad = maxToxicScoreFromPayload(root);
        return maxBad >= scoreThreshold;
    }

    /**
     * Supports {@code unitary/toxic-bert} shape {@code [[{label,score},...]]} and flat {@code [{label,score},...]}.
     */
    private double maxToxicScoreFromPayload(JsonNode root) {
        double max = 0;
        if (root.isArray()) {
            for (JsonNode outer : root) {
                max = Math.max(max, maxScoresInLabelsArray(outer));
            }
        }
        return max;
    }

    private double maxScoresInLabelsArray(JsonNode node) {
        if (node == null || node.isNull()) {
            return 0;
        }
        if (node.isArray()) {
            double m = 0;
            for (JsonNode item : node) {
                m = Math.max(m, maxScoresInLabelsArray(item));
            }
            return m;
        }
        if (node.isObject() && node.has("score")) {
            return node.path("score").asDouble(0);
        }
        return 0;
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        String t = s.replace("\n", " ");
        return t.length() <= max ? t : t.substring(0, max) + "…";
    }
}
