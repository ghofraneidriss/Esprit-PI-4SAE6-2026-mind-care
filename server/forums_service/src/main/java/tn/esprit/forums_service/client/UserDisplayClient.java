package tn.esprit.forums_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Resolves {@code userId} → display name (first + last) via users-service REST API.
 */
@Service
public class UserDisplayClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${users.service.base-url:http://localhost:8081/api}")
    private String usersBaseUrl;

    /**
     * Batch resolve display names. Unknown ids fall back to {@code "User " + id}.
     */
    public Map<Long, String> resolveDisplayNames(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        Set<Long> need = userIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (need.isEmpty()) {
            return Map.of();
        }

        Map<Long, String> out = new LinkedHashMap<>();
        List<UserApiRow> rows = fetchAllUsers();
        for (UserApiRow u : rows) {
            if (u.getUserId() != null && need.contains(u.getUserId())) {
                out.put(u.getUserId(), formatName(u));
            }
        }
        for (Long id : need) {
            out.putIfAbsent(id, "User " + id);
        }
        return out;
    }

    private List<UserApiRow> fetchAllUsers() {
        try {
            String url = usersBaseUrl + "/users";
            ResponseEntity<List<UserApiRow>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<UserApiRow>>() {}
            );
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (RestClientException ex) {
            return List.of();
        }
    }

    private static String formatName(UserApiRow u) {
        String fn = u.getFirstName() != null ? u.getFirstName().trim() : "";
        String ln = u.getLastName() != null ? u.getLastName().trim() : "";
        String full = (fn + " " + ln).trim();
        if (!full.isEmpty()) {
            return full;
        }
        if (u.getEmail() != null && !u.getEmail().isBlank()) {
            return u.getEmail().trim();
        }
        return u.getUserId() != null ? "User " + u.getUserId() : "Member";
    }

    /** JSON subset of users-service {@code User} entity. */
    public static class UserApiRow {
        private Long userId;
        private String firstName;
        private String lastName;
        private String email;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
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

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}
