package tn.esprit.movement_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Calls users-service REST API ({@code /api/users}) for alert recipient resolution.
 */
@Service
public class UsersClient {

    private static final Logger log = LoggerFactory.getLogger(UsersClient.class);

    private final RestTemplate restTemplate = new RestTemplate();

    /** Example: http://localhost:8081/api */
    @Value("${users.service.base-url:http://localhost:8081/api}")
    private String usersBaseUrl;

    public List<String> getDoctorEmails() {
        List<UserApiDto> users = getAllUsers();
        return users.stream()
                .filter(u -> "DOCTOR".equalsIgnoreCase(u.getRole()))
                .map(UserApiDto::getEmail)
                .filter(this::isValidEmail)
                .map(String::trim)
                .distinct()
                .toList();
    }

    /**
     * Emails of the caregiver and volunteer linked to this patient (from patient row).
     */
    public List<String> getCareNetworkEmailsForPatient(Long patientUserId) {
        Set<String> emails = new LinkedHashSet<>();
        Optional<UserApiDto> patient = getUserById(patientUserId);
        if (patient.isEmpty()) {
            return Collections.emptyList();
        }

        UserApiDto p = patient.get();
        if (p.getCaregiverId() != null) {
            getUserById(p.getCaregiverId())
                    .filter(u -> isValidEmail(u.getEmail()))
                    .ifPresent(u -> emails.add(u.getEmail().trim()));
        }
        if (p.getVolunteerId() != null) {
            getUserById(p.getVolunteerId())
                    .filter(u -> isValidEmail(u.getEmail()))
                    .ifPresent(u -> emails.add(u.getEmail().trim()));
        }

        return new ArrayList<>(emails);
    }

    /**
     * User ids (caregiver + volunteer) linked to this patient — for in-app CVP notifications.
     */
    public List<Long> getCareNetworkUserIdsForPatient(Long patientUserId) {
        List<Long> ids = new ArrayList<>();
        Optional<UserApiDto> patient = getUserById(patientUserId);
        if (patient.isEmpty()) {
            return ids;
        }
        UserApiDto p = patient.get();
        if (p.getCaregiverId() != null) {
            ids.add(p.getCaregiverId());
        }
        if (p.getVolunteerId() != null) {
            ids.add(p.getVolunteerId());
        }
        return ids.stream().distinct().toList();
    }

    public String getPatientDisplayName(Long patientUserId) {
        Optional<UserApiDto> o = getUserById(patientUserId);
        if (o.isEmpty()) {
            return "Patient";
        }
        UserApiDto u = o.get();
        String fn = u.getFirstName() != null ? u.getFirstName().trim() : "";
        String ln = u.getLastName() != null ? u.getLastName().trim() : "";
        String name = (fn + " " + ln).trim();
        return name.isEmpty() ? "Patient" : name;
    }

    /**
     * Creates a MindCare in-app notification (users-service). {@code mapsUrl} is stored in {@code snippet} for deep-link.
     */
    public void postInAppNotification(
            Long targetUserId,
            String message,
            String type,
            String eventKind,
            String actorName,
            Long actorUserId,
            String mapsUrl) {
        String url = normalizeUsersBase(usersBaseUrl) + "/users/notifications";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("userId", targetUserId);
            body.put("message", message);
            body.put("type", type);
            body.put("eventKind", eventKind);
            body.put("actorName", actorName);
            body.put("actorUserId", actorUserId);
            body.put("snippet", mapsUrl);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(url, entity, Object.class);
        } catch (RestClientException ex) {
            log.warn("Failed to POST in-app notification for user {}: {}", targetUserId, ex.getMessage());
        }
    }

    private static String normalizeUsersBase(String base) {
        if (base == null || base.isBlank()) {
            return "http://localhost:8081/api";
        }
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    private List<UserApiDto> getAllUsers() {
        try {
            String url = usersBaseUrl + "/users";
            ResponseEntity<List<UserApiDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<UserApiDto>>() {}
            );
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (RestClientException ex) {
            return Collections.emptyList();
        }
    }

    private Optional<UserApiDto> getUserById(Long userId) {
        try {
            String url = usersBaseUrl + "/users/" + userId;
            ResponseEntity<UserApiDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<UserApiDto>() {}
            );
            return Optional.ofNullable(response.getBody());
        } catch (RestClientException ex) {
            return Optional.empty();
        }
    }

    private boolean isValidEmail(String value) {
        if (value == null) {
            return false;
        }
        String v = value.trim();
        if (v.isBlank()) {
            return false;
        }
        int atIndex = v.indexOf('@');
        int dotIndex = v.lastIndexOf('.');
        return atIndex > 0 && dotIndex > atIndex + 1 && dotIndex < v.length() - 1;
    }

    /** JSON shape from users-service {@code User} entity. */
    public static class UserApiDto {
        private Long userId;
        private String email;
        private String role;
        private String firstName;
        private String lastName;
        private Long caregiverId;
        private Long volunteerId;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public Long getCaregiverId() {
            return caregiverId;
        }

        public void setCaregiverId(Long caregiverId) {
            this.caregiverId = caregiverId;
        }

        public Long getVolunteerId() {
            return volunteerId;
        }

        public void setVolunteerId(Long volunteerId) {
            this.volunteerId = volunteerId;
        }
    }
}
