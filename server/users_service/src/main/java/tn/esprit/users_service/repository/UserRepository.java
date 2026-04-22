package tn.esprit.users_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.users_service.dto.PatientSummaryDTO;
import tn.esprit.users_service.entity.Role;
import tn.esprit.users_service.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByCaregiverId(Long caregiverId);

    List<User> findByVolunteerId(Long volunteerId);

    List<User> findByRole(Role role);

    @Query("SELECT NEW tn.esprit.users_service.dto.PatientSummaryDTO(u.userId, u.firstName, u.lastName, u.email) "
            + "FROM User u WHERE u.role = :role ORDER BY u.lastName ASC, u.firstName ASC")
    List<PatientSummaryDTO> findPatientSummariesForRegistration(@Param("role") Role role);

    long countByRole(Role role);
}
