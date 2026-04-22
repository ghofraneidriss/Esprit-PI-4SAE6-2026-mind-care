package tn.esprit.activities_service.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Health Check", description = "API pour vérifier l'état du service")
public class HealthController {
    
    @Operation(summary = "Vérifier l'état du service", description = "Retourne l'état de santé du service Activities")
    @GetMapping("/health")
    public String health() {
        return "Activities Service is running!";
    }
}
