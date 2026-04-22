package tn.esprit.users_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.users_service.dto.LoginRequest;
import tn.esprit.users_service.entity.User;
import tn.esprit.users_service.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<User> registerUser(@Valid @RequestBody User user) {
        return ResponseEntity.ok(userService.registerUser(user));
    }

    @PostMapping("/login")
    public ResponseEntity<User> login(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(userService.login(loginRequest.email(), loginRequest.password(), loginRequest.role()));
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        return ResponseEntity.ok(userService.updateUser(id, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // ── Password Reset ────────────────────────────────────────────────────────

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@RequestBody java.util.Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body("Email is required.");
        }
        userService.forgotPassword(email);
        return ResponseEntity.ok("Password reset email sent. Please check your inbox.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody java.util.Map<String, String> body) {
        String token    = body.get("token");
        String password = body.get("password");
        if (token == null || token.isBlank() || password == null || password.isBlank()) {
            return ResponseEntity.badRequest().body("Token and password are required.");
        }
        userService.resetPassword(token, password);
        return ResponseEntity.ok("Password has been reset successfully. You can now log in.");
    }
}
