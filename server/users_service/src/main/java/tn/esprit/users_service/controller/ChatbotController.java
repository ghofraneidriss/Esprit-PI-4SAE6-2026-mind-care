package tn.esprit.users_service.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import tn.esprit.users_service.entity.Role;
import tn.esprit.users_service.repository.UserRepository;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users/chatbot")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4210"})
@RequiredArgsConstructor
@Slf4j
public class ChatbotController {

    private final RestTemplate restTemplate;
    private final UserRepository userRepository;

    @Value("${groq.api.key:YOUR_GROQ_API_KEY_HERE}")
    private String groqApiKey;

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    private static final String SYSTEM_PROMPT_BASE =
            "You are MindCare Admin Assistant — an intelligent assistant for the MindCare platform, " +
            "a system that helps manage Alzheimer's patients, caregivers, incidents, and medical staff.\n\n" +
            "You have access to LIVE DATA from the platform (injected below). " +
            "Use this data to answer admin questions accurately and concisely.\n" +
            "When asked about counts, stats, or lists — always use the provided data, never guess.\n" +
            "You can also answer general questions about Alzheimer's care, platform usage, and best practices.\n" +
            "Keep answers short and professional.\n\n";

    // Internal service URLs (direct, not via gateway)
    private static final String INCIDENT_SERVICE = "http://localhost:8087";
    private static final String FORUM_SERVICE    = "http://localhost:8086";

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, String> body) {
        String userMessage = body.getOrDefault("message", "").trim();
        if (userMessage.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message cannot be empty"));
        }

        String fullSystemPrompt = SYSTEM_PROMPT_BASE + buildLiveContext();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + groqApiKey);

        Map<String, Object> requestBody = Map.of(
                "model", "llama-3.1-8b-instant",
                "messages", List.of(
                        Map.of("role", "system", "content", fullSystemPrompt),
                        Map.of("role", "user",   "content", userMessage)
                ),
                "temperature", 0.4,
                "max_tokens", 512
        );

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    GROQ_URL, new HttpEntity<>(requestBody, headers), Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody != null && responseBody.containsKey("choices")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (!choices.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> message = (Map<String, String>) choices.get(0).get("message");
                    return ResponseEntity.ok(Map.of("reply", message.get("content")));
                }
            }
            return ResponseEntity.ok(Map.of("reply", "Sorry, I could not generate a response."));

        } catch (Exception e) {
            log.error("[Chatbot] Groq call failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "Chatbot service unavailable: " + e.getMessage()));
        }
    }

    // Build real-time context from all microservices
    private String buildLiveContext() {
        StringBuilder ctx = new StringBuilder("=== LIVE PLATFORM DATA ===\n");

        // 1. USERS — local DB, always available
        try {
            long total      = userRepository.count();
            long patients   = userRepository.countByRole(Role.PATIENT);
            long caregivers = userRepository.countByRole(Role.CAREGIVER);
            long doctors    = userRepository.countByRole(Role.DOCTOR);
            long admins     = userRepository.countByRole(Role.ADMIN);
            long unassigned = userRepository.findAll().stream()
                    .filter(u -> u.getRole() == Role.PATIENT && u.getCaregiverId() == null)
                    .count();

            ctx.append("USERS:\n")
               .append("  Total: ").append(total).append("\n")
               .append("  Patients: ").append(patients).append("\n")
               .append("  Caregivers: ").append(caregivers).append("\n")
               .append("  Doctors: ").append(doctors).append("\n")
               .append("  Admins: ").append(admins).append("\n")
               .append("  Patients without assigned caregiver: ").append(unassigned).append("\n");
        } catch (Exception e) {
            ctx.append("USERS: unavailable\n");
        }

        // 2. INCIDENTS — incident_service
        try {
            List<?> active  = restTemplate.getForObject(INCIDENT_SERVICE + "/api/incidents", List.class);
            List<?> history = restTemplate.getForObject(INCIDENT_SERVICE + "/api/incidents/history", List.class);
            List<?> types   = restTemplate.getForObject(INCIDENT_SERVICE + "/api/incident-types", List.class);

            long open = 0, inProgress = 0, resolved = 0;
            if (active != null) {
                for (Object item : active) {
                    if (item instanceof Map) {
                        String status = String.valueOf(((Map<?,?>)item).get("status"));
                        if ("OPEN".equals(status))             open++;
                        else if ("IN_PROGRESS".equals(status)) inProgress++;
                        else if ("RESOLVED".equals(status))    resolved++;
                    }
                }
            }

            ctx.append("INCIDENTS:\n")
               .append("  Active incidents: ").append(active != null ? active.size() : 0).append("\n")
               .append("    OPEN: ").append(open).append("\n")
               .append("    IN_PROGRESS: ").append(inProgress).append("\n")
               .append("    RESOLVED: ").append(resolved).append("\n")
               .append("  Total history (incl. deleted): ").append(history != null ? history.size() : 0).append("\n")
               .append("  Incident types/categories: ").append(types != null ? types.size() : 0).append("\n");
        } catch (Exception e) {
            ctx.append("INCIDENTS: unavailable\n");
        }

        // 3. FORUM — forum_service
        try {
            List<?> categories = restTemplate.getForObject(FORUM_SERVICE + "/api/categories", List.class);
            List<?> posts      = restTemplate.getForObject(FORUM_SERVICE + "/api/posts", List.class);

            ctx.append("FORUM:\n")
               .append("  Categories: ").append(categories != null ? categories.size() : 0).append("\n")
               .append("  Posts: ").append(posts != null ? posts.size() : 0).append("\n");
        } catch (Exception e) {
            ctx.append("FORUM: unavailable\n");
        }

        ctx.append("==========================\n");
        return ctx.toString();
    }
}
