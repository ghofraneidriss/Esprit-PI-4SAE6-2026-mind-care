export const environment = {
    production: false,
    /**
     * Base API path for services behind the gateway (forums, activities, …).
     */
    apiUrl: '/api',
    /**
     * Dev: call activities-service on :8084 directly (quiz, performance, PDF reports, …).
     * Avoids 503 from the gateway when Eureka does not resolve `activities-service`.
     * Requires CORS on activities-service (already allows http://localhost:4200).
     * Set false in production if the app is served behind the same origin as /api only.
     */
    useActivitiesServiceDirect: true,
    activitiesServiceBaseUrl: 'http://localhost:8084/api',
    /**
     * API Gateway (localization, movement, forums, …). Use with Eureka-registered services.
     */
    gatewayBaseUrl: 'http://localhost:8080/api',
    /**
     * When true, AuthService calls users-service directly (port 8081). Avoids 503 if the gateway
     * returns 503 without Eureka. Requires CORS on users-service (enabled in CorsConfig).
     */
    useUsersServiceDirect: true,
    usersServiceBaseUrl: 'http://localhost:8081/api',
    /**
     * Dev: forums-service direct on 8082 — same idea as incident: avoids gateway/Eureka latency for /api/categories, /api/posts, …
     * Requires CORS on forums-service (already allows http://localhost:4200).
     */
    useForumsServiceDirect: true,
    forumsServiceBaseUrl: 'http://localhost:8082/api',
    /**
     * Dev: incident service direct on 8083 — faster when the gateway is down.
     * Set useIncidentDirect: false when everything goes through the gateway (8080) in prod.
     */
    incidentApiUrl: 'http://localhost:8083/api',
    useIncidentDirect: true,
    /**
     * Dev: safe zones via localization-service (8085), positions/alerts via movement-service (8086).
     * Avoids failure when Eureka or the gateway (8080) are not running. Set to false in prod if all traffic uses the gateway.
     */
    useMovementLocalizationDirect: true,
    localizationServiceBaseUrl: 'http://localhost:8085/api',
    movementServiceBaseUrl: 'http://localhost:8086/api'
};

/** Base URL for the activities microservice (quiz, game results, performance, reports, …). */
export function activitiesApiBase(): string {
    if (!environment.production && environment.useActivitiesServiceDirect) {
        return environment.activitiesServiceBaseUrl;
    }
    return environment.apiUrl;
}

/** Base URL for forums-service (categories, posts, comments, staff dashboard). */
export function forumsApiBase(): string {
    if (!environment.production && environment.useForumsServiceDirect) {
        return environment.forumsServiceBaseUrl.replace(/\/$/, '');
    }
    return environment.apiUrl;
}

/**
 * Backend exposes stored images as `/api/photo-activities/{id}/image`. `<img src>` would otherwise
 * request `localhost:4200/api/...` (gateway 503). Rewrites to the same base as {@link activitiesApiBase}.
 */
export function resolveActivitiesMediaUrl(url: string | undefined | null): string {
    if (url == null) return '';
    const u = String(url).trim();
    if (!u) return '';
    if (u.startsWith('http://') || u.startsWith('https://')) return u;
    if (u.startsWith('/api/')) {
        return activitiesApiBase() + u.slice('/api'.length);
    }
    return u;
}
