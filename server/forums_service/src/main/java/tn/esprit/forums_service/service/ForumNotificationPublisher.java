package tn.esprit.forums_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pushes rows into users-service notifications (same contract as {@code NotificationController#create}).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ForumNotificationPublisher {

    private final RestTemplate restTemplate;

    @Value("${mindcare.users.notifications-url:http://localhost:8081/api/users/notifications}")
    private String notificationsUrl;

    public void notifyUser(Long userId, String message, String type, Long postId) {
        notifyEnriched(userId, message, type, postId, null, null, null, null, null);
    }

    /**
     * @param eventKind see {@code Notification.eventKind} (FORUM_*)
     * @param actorName resolved display name (French UI)
     * @param postTitle short title
     * @param snippet optional excerpt
     * @param actorUserId optional for deduplication on client
     */
    public void notifyEnriched(
            Long userId,
            String message,
            String type,
            Long postId,
            String eventKind,
            String actorName,
            String postTitle,
            String snippet,
            Long actorUserId) {
        if (userId == null || userId <= 0) {
            return;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("userId", userId);
            body.put("message", message);
            body.put("type", type != null ? type : "INFO");
            if (postId != null) {
                body.put("postId", postId);
            }
            if (eventKind != null && !eventKind.isBlank()) {
                body.put("eventKind", eventKind);
            }
            if (actorName != null && !actorName.isBlank()) {
                body.put("actorName", actorName);
            }
            if (postTitle != null && !postTitle.isBlank()) {
                body.put("postTitle", postTitle);
            }
            if (snippet != null && !snippet.isBlank()) {
                body.put("snippet", snippet);
            }
            if (actorUserId != null && actorUserId > 0) {
                body.put("actorUserId", actorUserId);
            }
            restTemplate.postForEntity(notificationsUrl, new HttpEntity<>(body, headers), Void.class);
        } catch (Exception e) {
            log.warn("Could not send forum notification to user {}: {}", userId, e.getMessage());
        }
    }
}
