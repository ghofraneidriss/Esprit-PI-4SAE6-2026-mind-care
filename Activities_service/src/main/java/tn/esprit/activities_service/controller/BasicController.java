package tn.esprit.activities_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class BasicController {
    
    @GetMapping("/")
    public ResponseEntity<String> home() {
        return ResponseEntity.ok("Backend is running!");
    }
    
    @GetMapping("/api")
    public ResponseEntity<String> api() {
        return ResponseEntity.ok("API is working!");
    }
}
