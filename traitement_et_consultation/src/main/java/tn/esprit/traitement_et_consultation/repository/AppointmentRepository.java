package tn.esprit.traitement_et_consultation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.traitement_et_consultation.entity.Appointment;
import java.util.List;

/**
 * Référentiel JPA pour la gestion des rendez-vous.
 * Fournit des méthodes de recherche personnalisées basées sur les conventions
 * de nommage Spring Data.
 */
@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

        /**
         * Récupère la liste des rendez-vous d'un médecin spécifique.
         * Lié au Frontend Angular pour l'affichage du calendrier du docteur.
         */
        List<Appointment> findByDoctorId(Long doctorId);

        /**
         * Récupère la liste des rendez-vous d'un patient spécifique.
         * Lié au Frontend Angular pour l'historique personnel du patient.
         */
        List<Appointment> findByPatientId(Long patientId);

        /**
         * Recherche des rendez-vous sur une période précise pour un médecin.
         * Utilisé par l'API de suggestion de créneaux (Endpoint: /suggest-slot).
         */
        List<Appointment> findByDoctorIdAndAppointmentDateBetween(Long doctorId,
                        java.time.LocalDateTime startDate,
                        java.time.LocalDateTime endDate);

        /**
         * Recherche des rendez-vous sur une période précise pour un patient.
         */
        List<Appointment> findByPatientIdAndAppointmentDateBetween(Long patientId,
                        java.time.LocalDateTime startDate,
                        java.time.LocalDateTime endDate);

        /**
         * [JPQL] Calcul complexe du score de priorité clinique d'un patient.
         * Cette requête combine les pathologies (Hypertension, Diabète, etc.), l'âge et
         * le score MMSE.
         * Elle permet au dashboard Angular d'ordonner les consultations selon l'urgence
         * médicale.
         */
        @org.springframework.data.jpa.repository.Query("SELECT a, (" +
                        " (CASE WHEN p.hypertension = true THEN 1 ELSE 0 END) + " +
                        " (CASE WHEN p.type2Diabetes = true THEN 1 ELSE 0 END) + " +
                        " (CASE WHEN p.hypercholesterolemia = true THEN 1 ELSE 0 END) + " +
                        " (CASE WHEN p.sleepDisorders = true THEN 1 ELSE 0 END) + " +
                        " (CASE WHEN p.familyHistoryAlzheimer = true THEN 1 ELSE 0 END) + " +
                        " (CASE " +
                        "   WHEN p.dateOfBirth IS NOT NULL AND (YEAR(CURRENT_DATE) - YEAR(p.dateOfBirth)) > 75 THEN 3 "
                        +
                        "   WHEN p.dateOfBirth IS NOT NULL AND (YEAR(CURRENT_DATE) - YEAR(p.dateOfBirth)) > 65 THEN 2 "
                        +
                        "   WHEN p.dateOfBirth IS NOT NULL AND (YEAR(CURRENT_DATE) - YEAR(p.dateOfBirth)) > 50 THEN 1 "
                        +
                        "   ELSE 0 END) + " +
                        " (CASE " +
                        "   WHEN p.externalCognitiveScore >= 0 AND p.externalCognitiveScore <= 5 THEN 6 " +
                        "   WHEN p.externalCognitiveScore > 5 AND p.externalCognitiveScore <= 10 THEN 5 " +
                        "   WHEN p.externalCognitiveScore > 10 AND p.externalCognitiveScore <= 15 THEN 4 " +
                        "   WHEN p.externalCognitiveScore > 15 AND p.externalCognitiveScore <= 20 THEN 3 " +
                        "   WHEN p.externalCognitiveScore > 20 AND p.externalCognitiveScore <= 25 THEN 2 " +
                        "   WHEN p.externalCognitiveScore > 25 THEN 1 " +
                        "   ELSE 0 END) " +
                        ") AS priorityScore " +
                        "FROM Appointment a " +
                        "LEFT JOIN PatientProfile p ON a.patientId = p.userId " +
                        "ORDER BY priorityScore DESC")
        List<Object[]> findAppointmentsWithPriorityScore();

        /**
         * [JPQL] Récupère les dates uniques pour le filtre de recherche.
         * Assure que chaque date n'apparaît qu'une fois dans le menu déroulant du
         * Frontend Angular.
         */
        @org.springframework.data.jpa.repository.Query("SELECT DISTINCT CAST(a.appointmentDate AS date) FROM Appointment a WHERE (:doctorId IS NULL OR a.doctorId = :doctorId)")
        List<java.sql.Date> findDistinctDates(
                        @org.springframework.data.repository.query.Param("doctorId") Long doctorId);

        /**
         * [JPQL] Récupère la liste des patients uniques pour alimenter les filtres du
         * dashboard.
         */
        @org.springframework.data.jpa.repository.Query("SELECT DISTINCT a.patientId FROM Appointment a WHERE (:doctorId IS NULL OR a.doctorId = :doctorId)")
        List<Long> findDistinctPatientIds(@org.springframework.data.repository.query.Param("doctorId") Long doctorId);

        /**
         * [JPQL + API] Coeur du filtrage multi-critères.
         * Applique les filtres (Date, Patient, Urgence, Statut) directement au niveau
         * de la base de données.
         */
        @org.springframework.data.jpa.repository.Query("SELECT a FROM Appointment a WHERE " +
                        "(:doctorId IS NULL OR a.doctorId = :doctorId) AND " +
                        "(:patientId IS NULL OR a.patientId = :patientId) AND " +
                        "(:status IS NULL OR a.status = :status) AND " +
                        "(:isUrgent IS NULL OR a.isUrgent = :isUrgent) AND " +
                        "(CAST(:apptDate AS date) IS NULL OR CAST(a.appointmentDate AS date) = CAST(:apptDate AS date))")
        List<Appointment> findFilteredAppointments(
                        @org.springframework.data.repository.query.Param("doctorId") Long doctorId,
                        @org.springframework.data.repository.query.Param("patientId") Long patientId,
                        @org.springframework.data.repository.query.Param("status") tn.esprit.traitement_et_consultation.entity.AppointmentStatus status,
                        @org.springframework.data.repository.query.Param("isUrgent") Boolean isUrgent,
                        @org.springframework.data.repository.query.Param("apptDate") java.sql.Date apptDate);

        /**
         * [JPQL] Détecte les chevauchements de rendez-vous pour éviter les doubles
         * réservations.
         */
        @org.springframework.data.jpa.repository.Query("SELECT a FROM Appointment a WHERE a.doctorId = :doctorId AND a.status != 'CANCELLED' "
                        +
                        "AND a.appointmentDate < :endTime " +
                        "AND (a.appointmentEndDate IS NOT NULL AND a.appointmentEndDate > :startTime OR a.appointmentEndDate IS NULL AND a.appointmentDate > :startTimeMinus35)")
        List<Appointment> findOverlappingAppointments(
                        @org.springframework.data.repository.query.Param("doctorId") Long doctorId,
                        @org.springframework.data.repository.query.Param("startTime") java.time.LocalDateTime startTime,
                        @org.springframework.data.repository.query.Param("endTime") java.time.LocalDateTime endTime,
                        @org.springframework.data.repository.query.Param("startTimeMinus35") java.time.LocalDateTime startTimeMinus35);

        /**
         * [JPQL] Recherche le créneau de fin disponible le plus proche pour
         * l'algorithme de suggestion.
         */
        @org.springframework.data.jpa.repository.Query("SELECT MAX(a.appointmentEndDate) FROM Appointment a WHERE a.doctorId = :doctorId AND a.status != 'CANCELLED' AND a.appointmentEndDate <= :requestedTime")
        java.time.LocalDateTime findMaxEndTimeBefore(
                        @org.springframework.data.repository.query.Param("doctorId") Long doctorId,
                        @org.springframework.data.repository.query.Param("requestedTime") java.time.LocalDateTime requestedTime);

        /**
         * [JPQL] Recherche le prochain créneau de début libre pour l'algorithme de
         * suggestion.
         */
        @org.springframework.data.jpa.repository.Query("SELECT MIN(a.appointmentDate) FROM Appointment a WHERE a.doctorId = :doctorId AND a.status != 'CANCELLED' AND a.appointmentDate >= :requestedTime")
        java.time.LocalDateTime findMinStartTimeAfter(
                        @org.springframework.data.repository.query.Param("doctorId") Long doctorId,
                        @org.springframework.data.repository.query.Param("requestedTime") java.time.LocalDateTime requestedTime);
}
