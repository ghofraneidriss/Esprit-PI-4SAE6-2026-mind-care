package tn.esprit.recommendation_service.controller;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SystemControllerTest {

    private final SystemController controller = new SystemController();

    @Test
    void root_shouldReturnServiceMetadata() {
        Map<String, Object> response = controller.root();

        assertThat(response)
                .containsEntry("service", "recommendation_service")
                .containsEntry("status", "UP")
                .containsEntry("docs", "/swagger-ui.html");
        assertThat(response.get("timestamp")).isInstanceOf(LocalDateTime.class);
    }

    @Test
    void health_shouldReturnUpStatus() {
        Map<String, String> response = controller.health();

        assertThat(response).containsEntry("status", "UP");
    }
}
