package tn.esprit.microservice.volunteer.Util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Utility class to retrieve the authenticated user's information from the
 * Keycloak JWT token.
 */
@Component
public class SecurityUtils {

    /**
     * Get the current authenticated user's Keycloak ID (sub claim).
     * 
     * @return Optional containing the Keycloak user ID or empty if not
     *         authenticated.
     */
    public static Optional<String> getCurrentUserKeycloakId() {
        return getJwt().map(Jwt::getSubject);
    }

    /**
     * Get the current authenticated user's email from the JWT token.
     * 
     * @return Optional containing the user's email or empty if not
     *         found/authenticated.
     */
    public static Optional<String> getCurrentUserEmail() {
        return getJwt().map(jwt -> jwt.getClaimAsString("email"));
    }

    /**
     * Get the current authenticated user's name (preferred_username).
     * 
     * @return Optional containing the preferred username or empty if not
     *         authenticated.
     */
    public static Optional<String> getCurrentUsername() {
        return getJwt().map(jwt -> jwt.getClaimAsString("preferred_username"));
    }

    /**
     * Get a custom claim from the JWT token (e.g. "userId" if added in Keycloak
     * mapper).
     * 
     * @param claimName The name of the claim to retrieve.
     * @return Optional containing the claim value as Object or empty if not found.
     */
    public static Optional<Object> getCustomClaim(String claimName) {
        return getJwt().map(jwt -> jwt.getClaim(claimName));
    }

    /**
     * Helper to get the JWT object from the security context.
     */
    private static Optional<Jwt> getJwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return Optional.of(jwt);
        }
        return Optional.empty();
    }
}
