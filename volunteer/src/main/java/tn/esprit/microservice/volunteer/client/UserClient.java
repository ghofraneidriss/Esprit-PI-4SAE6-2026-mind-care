package tn.esprit.microservice.volunteer.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserClient {

    private final RestTemplate restTemplate;
    private final String userServiceUrl;

    public UserClient(RestTemplate restTemplate,
                      @Value("${users.service.url}") String userServiceUrl) {
        this.restTemplate = restTemplate;
        this.userServiceUrl = userServiceUrl.endsWith("/") ? userServiceUrl.substring(0, userServiceUrl.length() - 1) : userServiceUrl;
    }

    public List<UserSummary> fetchVolunteers() {
        String url = userServiceUrl + "/api/users";
        ResponseEntity<List<UserResponse>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<>() {
                });

        List<UserResponse> users = response.getBody() == null ? List.of() : response.getBody();
        return users.stream()
                .filter(UserResponse::isVolunteer)
                .map(UserResponse::toSummary)
                .collect(Collectors.toList());
    }

    public Optional<UserSummary> fetchVolunteerById(Long userId) {
        String url = userServiceUrl + "/api/users/" + userId;
        try {
            ResponseEntity<UserResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    UserResponse.class
            );
            UserResponse user = response.getBody();
            if (user == null || !user.isVolunteer()) {
                return Optional.empty();
            }
            return Optional.of(user.toSummary());
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw e;
        }
    }

    public record UserSummary(Long userId, String firstName, String lastName, String email, String role, String status, String phone) {
        public String displayName() {
            String first = firstName == null ? "" : firstName.trim();
            String last = lastName == null ? "" : lastName.trim();
            if (first.isEmpty() && last.isEmpty()) {
                return "Volunteer";
            }
            return (first + " " + last).trim();
        }

        public String normalizedEmail() {
            return email == null ? "" : email.toLowerCase(Locale.ROOT);
        }

        public boolean isVolunteer() {
            return role != null && "VOLUNTEER".equalsIgnoreCase(role);
        }

        public boolean isVerified() {
            return status != null && "ONLINE".equalsIgnoreCase(status);
        }
    }

    private record UserResponse(Long userId, String firstName, String lastName, String email, String role, String status, String phone) {
        UserSummary toSummary() {
            return new UserSummary(userId,
                    firstName,
                    lastName,
                    email != null ? email.trim() : null,
                    role,
                    status,
                    phone);
        }

        boolean isVolunteer() {
            return role != null && "VOLUNTEER".equalsIgnoreCase(role);
        }
    }
}
