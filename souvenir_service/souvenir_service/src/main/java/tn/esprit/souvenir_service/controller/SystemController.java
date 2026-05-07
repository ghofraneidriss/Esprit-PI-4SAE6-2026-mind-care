package tn.esprit.souvenir_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping
public class SystemController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("service", "souvenir_service");
        body.put("status", "UP");
        body.put("timestamp", LocalDateTime.now());
        body.put("swagger", "/swagger-ui.html");
        body.put("apiDocs", "/api-docs");
        return ResponseEntity.ok(body);
    }

    @GetMapping("/swagger")
    public ResponseEntity<Void> swaggerRedirect() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create("/swagger-ui.html"))
                .build();
    }

    @GetMapping("/api/system/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", "Souvenir service is running.");
        body.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(body);
    }
}
