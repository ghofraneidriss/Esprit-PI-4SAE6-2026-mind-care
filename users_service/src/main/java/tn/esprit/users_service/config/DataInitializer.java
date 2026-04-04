package tn.esprit.users_service.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import tn.esprit.users_service.entity.Role;
import tn.esprit.users_service.entity.User;
import tn.esprit.users_service.repository.UserRepository;

import java.util.List;

/**
 * Seeds the alzheimer_db with demo users on first run.
 * Runs only when the users table is empty.
 * All passwords are:  test1234
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() > 0) {
            log.info("[DataInitializer] Users table already populated — skipping seed.");
            return;
        }

        String pwd = passwordEncoder.encode("test1234");

        List<User> users = List.of(
            // ── Admin ──────────────────────────────────────────────────────────
            User.builder()
                .firstName("Amira").lastName("Boukadida")
                .email("admin@mindcare.tn").password(pwd)
                .phone("+216 70 000 001").role(Role.ADMIN)
                .build(),

            // ── Doctor ─────────────────────────────────────────────────────────
            User.builder()
                .firstName("Dr. Karim").lastName("Messaoudi")
                .email("doctor@mindcare.tn").password(pwd)
                .phone("+216 70 000 002").role(Role.DOCTOR)
                .build(),

            // ── Caregivers ─────────────────────────────────────────────────────
            User.builder()
                .firstName("Sana").lastName("Trabelsi")
                .email("caregiver1@mindcare.tn").password(pwd)
                .phone("+216 70 000 003").role(Role.CAREGIVER)
                .build(),

            User.builder()
                .firstName("Yassine").lastName("Haddad")
                .email("caregiver2@mindcare.tn").password(pwd)
                .phone("+216 70 000 004").role(Role.CAREGIVER)
                .build(),

            // ── Patients ───────────────────────────────────────────────────────
            User.builder()
                .firstName("Mohamed").lastName("Gharbi")
                .email("patient1@mindcare.tn").password(pwd)
                .phone("+216 70 000 005").role(Role.PATIENT)
                .build(),

            User.builder()
                .firstName("Fatma").lastName("Riahi")
                .email("patient2@mindcare.tn").password(pwd)
                .phone("+216 70 000 006").role(Role.PATIENT)
                .build(),

            User.builder()
                .firstName("Hedi").lastName("Zouari")
                .email("patient3@mindcare.tn").password(pwd)
                .phone("+216 70 000 007").role(Role.PATIENT)
                .build()
        );

        userRepository.saveAll(users);
        log.info("[DataInitializer] Seeded {} users (password for all: test1234)", users.size());
        users.forEach(u -> log.info("  {} {} | {} | {}", u.getFirstName(), u.getLastName(), u.getEmail(), u.getRole()));
    }
}
