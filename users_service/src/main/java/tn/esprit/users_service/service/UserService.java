package tn.esprit.users_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.users_service.entity.PasswordResetToken;
import tn.esprit.users_service.entity.Role;
import tn.esprit.users_service.entity.User;
import tn.esprit.users_service.repository.PasswordResetTokenRepository;
import tn.esprit.users_service.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository tokenRepository;
    private final JavaMailSender mailSender;


    public User registerUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already in use");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        // Default role if not provided
        if (user.getRole() == null) {
            user.setRole(Role.PATIENT);
        }
        return userRepository.save(user);
    }

    public User updateUser(Long id, User userDetails) {
        User user = getUserById(id);
        
        user.setFirstName(userDetails.getFirstName());
        user.setLastName(userDetails.getLastName());
        user.setPhone(userDetails.getPhone());
        
        // Only update password if provided and not empty
        if (userDetails.getPassword() != null && !userDetails.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(userDetails.getPassword()));
        }

        // Only Admin can update role (assuming logic handled in controller/security, 
        // but here we just update if passed, or we could restrict it)
        if (userDetails.getRole() != null) {
            user.setRole(userDetails.getRole());
        }

        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found");
        }
        userRepository.deleteById(id);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User login(String email, String password, Role role) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }
        if (user.getRole() != role) {
            throw new RuntimeException("Invalid role for this account");
        }
        return user;
    }

    // ── Forgot Password ───────────────────────────────────────────────────────

    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No account found with this email address."));

        // Delete any existing token for this email
        tokenRepository.deleteByEmail(email);

        // Generate a secure token valid for 30 minutes
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .email(email)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .used(false)
                .build();
        tokenRepository.save(resetToken);

        // Send email
        String resetLink = "http://localhost:4200/auth/new-password-cover?token=" + token;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("MindCare — Password Reset Request");
        message.setText(
            "Hello " + user.getFirstName() + ",\n\n" +
            "We received a request to reset your MindCare account password.\n\n" +
            "Click the link below to set a new password (valid for 30 minutes):\n" +
            resetLink + "\n\n" +
            "If you did not request this, please ignore this email.\n\n" +
            "— The MindCare Team"
        );

        try {
            mailSender.send(message);
            log.info("Password reset email sent to {}", email);
        } catch (Exception e) {
            log.error("Failed to send reset email to {}: {}", email, e.getMessage());
            throw new RuntimeException("Failed to send reset email. Please try again later.");
        }
    }

    // ── Reset Password ────────────────────────────────────────────────────────

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset link."));

        if (resetToken.isUsed()) {
            throw new RuntimeException("This reset link has already been used.");
        }
        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("This reset link has expired. Please request a new one.");
        }

        User user = userRepository.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found."));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        log.info("Password successfully reset for {}", resetToken.getEmail());
    }
}
