package tn.esprit.users_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.users_service.entity.MoodEntry;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MoodEntryRepository extends JpaRepository<MoodEntry, Long> {
    List<MoodEntry> findByPatientIdOrderByDateDesc(Long patientId);
    List<MoodEntry> findByPatientIdAndDateBetweenOrderByDateAsc(Long patientId, LocalDate start, LocalDate end);
    List<MoodEntry> findByCaregiverIdOrderByDateDesc(Long caregiverId);
}
