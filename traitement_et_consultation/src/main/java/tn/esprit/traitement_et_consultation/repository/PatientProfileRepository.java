package tn.esprit.traitement_et_consultation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.traitement_et_consultation.entity.PatientProfile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PatientProfileRepository extends JpaRepository<PatientProfile, Long> {
        Optional<PatientProfile> findByUserId(Long userId);

        Optional<PatientProfile> findByEmail(String email);

        List<PatientProfile> findByUserIdIn(List<Long> userIds);

        @Query("SELECT DISTINCT p FROM PatientProfile p, Appointment a1, Consultation c1, Appointment a2, Consultation c2 "
                        +
                        "WHERE a1.patientId = p.userId AND c1.appointmentId = a1.id " +
                        "AND a2.patientId = p.userId AND c2.appointmentId = a2.id " +
                        "AND (:treatment IS NULL OR :treatment = '' OR p.medications LIKE CONCAT('%', :treatment, '%')) "
                        +
                        "AND a1.appointmentDate > a2.appointmentDate " +
                        "AND (c2.mmseScore - c1.mmseScore) >= :degradationThreshold")
        List<PatientProfile> findPatientsWithRapidDegradation(@Param("treatment") String treatment,
                        @Param("degradationThreshold") Integer degradationThreshold);

        @Query("SELECT DISTINCT p FROM PatientProfile p, Appointment a, Consultation c " +
                        "WHERE a.patientId = p.userId AND c.appointmentId = a.id " +
                        "AND c.alzheimerStage = tn.esprit.traitement_et_consultation.entity.AlzheimerStage.SEVERE " +
                        "AND a.appointmentDate < :threeMonthsAgo " +
                        "AND a.appointmentDate = (SELECT MAX(a2.appointmentDate) FROM Consultation c2, Appointment a2 WHERE c2.appointmentId = a2.id AND a2.patientId = p.userId) "
                        +
                        "AND NOT EXISTS (SELECT a3 FROM Appointment a3 WHERE a3.patientId = p.userId AND a3.appointmentDate > CURRENT_TIMESTAMP)")
        List<PatientProfile> findSeverePatientsWithoutFollowUp(@Param("threeMonthsAgo") LocalDateTime threeMonthsAgo);
}
