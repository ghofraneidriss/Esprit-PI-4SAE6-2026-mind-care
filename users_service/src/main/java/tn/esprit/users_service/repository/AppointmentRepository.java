package tn.esprit.users_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.users_service.entity.Appointment;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findAllByOrderByStartDateAsc();
    List<Appointment> findByPatientIdOrderByStartDateAsc(Long patientId);
    List<Appointment> findByCaregiverIdOrderByStartDateAsc(Long caregiverId);
    List<Appointment> findByStartDateBetweenOrderByStartDateAsc(LocalDateTime start, LocalDateTime end);
}
