package tn.esprit.recommendation_service.Services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.recommendation_service.Clients.UserServiceClient;
import tn.esprit.recommendation_service.DTOs.UserDTO;
import tn.esprit.recommendation_service.Entities.Recommendation;
import tn.esprit.recommendation_service.Entities.RecommendationStatus;
import tn.esprit.recommendation_service.Entities.RecommendationType;
import tn.esprit.recommendation_service.Repository.RecommendationRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final RecommendationRepository repository;
    private final UserServiceClient userServiceClient;

    @Override
    public Recommendation create(Recommendation recommendation) {
        enrichAndValidateUsers(recommendation);
        recommendation.setStatus(RecommendationStatus.PENDING);
        return repository.save(recommendation);
    }

    @Override
    public List<Recommendation> getAll() {
        return repository.findAll();
    }

    @Override
    public Recommendation getById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Recommendation not found"));
    }

    @Override
    public List<Recommendation> getByType(String type) {
        return repository.findByType(RecommendationType.valueOf(type.toUpperCase()));
    }

    @Override
    public List<Recommendation> getByStatus(String status) {
        return repository.findByStatus(RecommendationStatus.valueOf(status.toUpperCase()));
    }

    @Override
    public Recommendation update(Long id, Recommendation recommendation) {
        Recommendation existing = getById(id);

        // Update basic fields
        existing.setContent(recommendation.getContent());
        existing.setType(recommendation.getType());
        existing.setStatus(recommendation.getStatus());

        // Update specialized fields
        existing.setPatientId(recommendation.getPatientId());
        existing.setDoctorId(recommendation.getDoctorId());
        existing.setMedicalEvents(recommendation.getMedicalEvents());

        enrichAndValidateUsers(existing);

        return repository.save(existing);
    }

    @Override
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Override
    public Recommendation approve(Long id) {
        Recommendation recommendation = getById(id);
        recommendation.setStatus(RecommendationStatus.APPROVED);
        return repository.save(recommendation);
    }

    /**
     * Authenticates users via Feign and fetches full names (similar to
     * medical_report_service pattern)
     */
    private void enrichAndValidateUsers(Recommendation recommendation) {
        if (recommendation.getPatientId() == null || recommendation.getDoctorId() == null) {
            throw new IllegalArgumentException("Patient ID and Doctor ID are required");
        }

        // Fetch user details from users_service
        UserDTO patient = userServiceClient.getUserById(recommendation.getPatientId());
        UserDTO doctor = userServiceClient.getUserById(recommendation.getDoctorId());

        if (patient == null)
            throw new RuntimeException("Patient not found in users-service");
        if (doctor == null)
            throw new RuntimeException("Doctor not found in users-service");

        // Validate roles (stripping ROLE_ prefix if present)
        String pRole = patient.getRole().toUpperCase().replace("ROLE_", "");
        String dRole = doctor.getRole().toUpperCase().replace("ROLE_", "");

        if (!"PATIENT".equals(pRole))
            throw new IllegalArgumentException("User " + recommendation.getPatientId() + " is not a PATIENT");
        if (!"DOCTOR".equals(dRole))
            throw new IllegalArgumentException("User " + recommendation.getDoctorId() + " is not a DOCTOR");

        // Set names for display/denormalization
        recommendation.setPatientName(patient.getFirstName() + " " + patient.getLastName());
        recommendation.setDoctorName("Dr. " + doctor.getFirstName() + " " + doctor.getLastName());
    }
}
