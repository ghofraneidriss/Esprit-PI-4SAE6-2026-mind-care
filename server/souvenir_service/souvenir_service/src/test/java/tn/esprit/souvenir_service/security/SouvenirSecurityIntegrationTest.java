package tn.esprit.souvenir_service.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import tn.esprit.souvenir_service.config.SecurityConfig;
import tn.esprit.souvenir_service.controller.EntreeSouvenirController;
import tn.esprit.souvenir_service.controller.SystemController;
import tn.esprit.souvenir_service.dto.entree.EntreeSouvenirResponse;
import tn.esprit.souvenir_service.enums.ThemeCulturel;
import tn.esprit.souvenir_service.service.EntreeSouvenirService;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = {EntreeSouvenirController.class, SystemController.class},
        properties = {
                "eureka.client.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.main.lazy-initialization=true"
        }
)
@Import(SecurityConfig.class)
class SouvenirSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EntreeSouvenirService entreeSouvenirService;

    @Test
    void protectedEndpoint_shouldReturnUnauthorized_withoutCredentials() throws Exception {
        mockMvc.perform(get("/api/souvenirs/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_shouldReturnOk_withValidBasicAuth() throws Exception {
        EntreeSouvenirResponse response = EntreeSouvenirResponse.builder()
                .id(1L)
                .patientId(10L)
                .texte("Souvenir")
                .mediaType(tn.esprit.souvenir_service.enums.MediaType.IMAGE)
                .themeCulturel(ThemeCulturel.VOYAGE)
                .build();
        when(entreeSouvenirService.getEntreeById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/souvenirs/1")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("devops", "devops123"))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    void publicPingEndpoint_shouldRemainAccessible_withoutCredentials() throws Exception {
        mockMvc.perform(get("/api/system/ping").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Souvenir service is running."));
    }
}
