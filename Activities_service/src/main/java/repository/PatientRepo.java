package tn.esprit.users_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.users_service.entity.Patient;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
}
