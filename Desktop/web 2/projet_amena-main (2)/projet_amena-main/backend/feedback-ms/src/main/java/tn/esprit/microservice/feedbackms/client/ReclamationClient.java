package tn.esprit.microservice.feedbackms.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "reclamation-ms", path = "/reclamations")
public interface ReclamationClient {

    @PostMapping
    ReclamationResponse createReclamation(@RequestBody ReclamationCreateRequest request);

    record ReclamationCreateRequest(String userName, String userEmail, String subject,
                                    String imageUrl, Integer rating, String description, String status) {
    }

    record ReclamationResponse(Long id) {
    }
}
