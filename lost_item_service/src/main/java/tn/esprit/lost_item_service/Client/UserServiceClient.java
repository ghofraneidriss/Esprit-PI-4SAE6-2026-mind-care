package tn.esprit.lost_item_service.Client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class UserServiceClient {

    private final RestTemplate restTemplate;
    private final String usersServiceUrl;

    public UserServiceClient(
            RestTemplate restTemplate,
            @Value("${services.users-service.url:http://localhost:8082}") String usersServiceUrl
    ) {
        this.restTemplate = restTemplate;
        this.usersServiceUrl = usersServiceUrl;
    }

    /**
     * Fetches a user by ID from the users-service.
     * Returns Optional.empty() if the user is not found or the service is unavailable.
     */
    public Optional<Map<String, Object>> getUserById(Long userId) {
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    usersServiceUrl + "/api/users/" + userId,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );
            return Optional.ofNullable(response.getBody());
        } catch (RestClientException e) {
            log.warn("Could not fetch user id={} from users-service: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Fetches all users from the users-service.
     * Returns empty list if the service is unavailable.
     */
    public List<Map<String, Object>> getAllUsers() {
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    usersServiceUrl + "/api/users",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );
            return response.getBody() != null ? response.getBody() : Collections.emptyList();
        } catch (RestClientException e) {
            log.warn("Could not fetch users from users-service: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Verifies if a user with the given ID exists.
     */
    public boolean userExists(Long userId) {
        return getUserById(userId).isPresent();
    }
}
