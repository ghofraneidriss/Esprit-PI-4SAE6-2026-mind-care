package tn.esprit.recommendation_service.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.recommendation_service.config.SecurityConfig;
import tn.esprit.recommendation_service.controller.RecommendationController;
import tn.esprit.recommendation_service.controller.SystemController;
import tn.esprit.recommendation_service.dto.recommendation.RecommendationResponse;
import tn.esprit.recommendation_service.enums.RecommendationStatus;
import tn.esprit.recommendation_service.enums.RecommendationType;
import tn.esprit.recommendation_service.service.RecommendationService;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {RecommendationController.class, SystemController.class})
@Import(SecurityConfig.class)
class RecommendationSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecommendationService recommendationService;

    @Test
    void protectedEndpoint_shouldReturnUnauthorized_withoutCredentials() throws Exception {
        mockMvc.perform(get("/api/recommendations").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_shouldReturnOk_withValidBasicAuth() throws Exception {
        RecommendationResponse response = RecommendationResponse.builder()
                .id(1L)
                .content("Hydrate")
                .type(RecommendationType.DIET)
                .status(RecommendationStatus.ACTIVE)
                .patientId(10L)
                .build();
        when(recommendationService.getAllRecommendations()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/recommendations")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("devops", "devops123"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    void publicHealthEndpoint_shouldRemainAccessible_withoutCredentials() throws Exception {
        mockMvc.perform(get("/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
