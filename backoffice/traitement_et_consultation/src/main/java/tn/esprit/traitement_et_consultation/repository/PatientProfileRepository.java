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

        @Query("SELECT DISTINCT p FROM PatientProfile p, Consultation c2, Appointment a2 WHERE a2.patientId = p.userId AND c2.appointmentId = a2.id " +
                        "AND a2.appointmentDate = (SELECT MAX(a3.appointmentDate) FROM Consultation c3, Appointment a3 WHERE c3.appointmentId = a3.id AND a3.patientId = p.userId) " +
                        "AND NOT EXISTS (SELECT a4 FROM Appointment a4 WHERE a4.patientId = p.userId AND a4.appointmentDate > CURRENT_TIMESTAMP)")
        List<PatientProfile> findSeverePatientsWithoutFollowUp(@Param("threeMonthsAgo") LocalDateTime threeMonthsAgo);

        /**
         * JPQL — Vérifie si un patient a une allergie correspondant au nom commercial
         * ou à la famille thérapeutique d'un médicament (comparaison insensible à la casse).
         *
         * @param userId           ID du patient
         * @param medicineName     Nom commercial du médicament (ex: "Penicillin")
         * @param therapeuticFamily Famille thérapeutique (ex: "Antibiotics")
         * @return Liste des allergies du patient correspondantes
         */
        @Query("SELECT a FROM PatientProfile p JOIN p.allergies a " +
               "WHERE p.userId = :userId " +
               "AND (LOWER(a) LIKE LOWER(CONCAT('%', :medicineName, '%')) " +
               "OR LOWER(a) LIKE LOWER(CONCAT('%', :therapeuticFamily, '%')) " +
               "OR LOWER(:medicineName) LIKE LOWER(CONCAT('%', a, '%')) " +
               "OR LOWER(:therapeuticFamily) LIKE LOWER(CONCAT('%', a, '%')))")
        List<String> findMatchingAllergies(@Param("userId") Long userId,
                                           @Param("medicineName") String medicineName,
                                           @Param("therapeuticFamily") String therapeuticFamily);
}
