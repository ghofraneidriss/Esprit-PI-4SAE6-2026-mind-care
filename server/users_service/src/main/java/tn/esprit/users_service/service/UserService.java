package tn.esprit.users_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.users_service.dto.PatientSummaryDTO;
import tn.esprit.users_service.entity.Role;
import tn.esprit.users_service.entity.User;
import tn.esprit.users_service.repository.UserRepository;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserService {

    /** Roles allowed for public self-registration (ADMIN is not listed on purpose). */
    private static final Set<Role> SELF_REGISTER_ROLES =
            EnumSet.of(Role.PATIENT, Role.DOCTOR, Role.CAREGIVER, Role.VOLUNTEER);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User registerUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already in use");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        Long assignedPatientId = user.getAssignedPatientId();
        Role requested = user.getRole();
        if (requested == null) {
            user.setRole(Role.PATIENT);
        } else if (SELF_REGISTER_ROLES.contains(requested)) {
            user.setRole(requested);
        } else if (requested == Role.ADMIN) {
            throw new RuntimeException("Cannot self-register as ADMIN");
        } else {
            user.setRole(Role.PATIENT);
        }

        if ((user.getRole() == Role.CAREGIVER || user.getRole() == Role.VOLUNTEER)
                && assignedPatientId == null) {
            throw new RuntimeException("assignedPatientId is required for caregiver and volunteer registration");
        }

        User saved = userRepository.save(user);

        if (assignedPatientId != null && (saved.getRole() == Role.CAREGIVER || saved.getRole() == Role.VOLUNTEER)) {
            User patient = userRepository.findById(assignedPatientId)
                    .orElseThrow(() -> new RuntimeException("Patient not found"));
            if (patient.getRole() != Role.PATIENT) {
                throw new RuntimeException("assignedPatientId must be a PATIENT user");
            }
            if (saved.getRole() == Role.CAREGIVER) {
                patient.setCaregiverId(saved.getUserId());
            } else {
                patient.setVolunteerId(saved.getUserId());
            }
            userRepository.save(patient);
        }

        return saved;
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

        // Update caregiverId / volunteerId (patient assignment)
        user.setCaregiverId(userDetails.getCaregiverId());
        user.setVolunteerId(userDetails.getVolunteerId());

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

    public List<User> getPatientsByCaregiver(Long caregiverId) {
        return userRepository.findByCaregiverId(caregiverId);
    }

    public List<User> getPatientsByVolunteer(Long volunteerId) {
        return userRepository.findByVolunteerId(volunteerId);
    }

    public List<User> getAllPatients() {
        return userRepository.findByRole(Role.PATIENT);
    }

    /** Liste légère pour l’inscription (sans mot de passe ni colonnes inutiles). */
    @Transactional(readOnly = true)
    public List<PatientSummaryDTO> getPatientSummariesForRegistration() {
        return userRepository.findPatientSummariesForRegistration(Role.PATIENT);
    }

    @Transactional(readOnly = true)
    public User login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }
        return user;
    }
}
