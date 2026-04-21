package tn.esprit.ordonnance_et_medicaments.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.ordonnance_et_medicaments.entities.PrescriptionLine;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository for prescription lines (PrescriptionLine).
 * Contains JPQL queries used for drug safety checks.
 */
@Repository
public interface PrescriptionLineRepository extends JpaRepository<PrescriptionLine, Long> {

    // ─────────────────────────────────────────────────────────────────────────
    // QUERY 1 — Exact same medicine (by medicine ID)
    // Detects overdose risk when the exact same medicine is already active.
    // Overlap condition: startA <= endB AND endA >= startB
    // ─────────────────────────────────────────────────────────────────────────
    @Query("SELECT pl FROM PrescriptionLine pl " +
           "JOIN pl.prescription p " +
           "WHERE p.patientId = :patientId " +
           "AND pl.medicine.id = :medicineId " +
           "AND p.id <> :excludePrescriptionId " +
           "AND p.status IN ('SIGNED', 'PENDING', 'COMPLETED', 'ACTIVE') " +
           "AND pl.startDate <= :endDate " +
           "AND pl.endDate >= :startDate " +
           "ORDER BY pl.startDate ASC")
    List<PrescriptionLine> findOverlappingMedicinePrescriptions(
            @Param("patientId") Long patientId,
            @Param("medicineId") Long medicineId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludePrescriptionId") Long excludePrescriptionId
    );

    // ─────────────────────────────────────────────────────────────────────────
    // QUERY 2 — Same INN / active ingredient (different brand, same molecule)
    // Detects cases where the patient is already taking the same active
    // ingredient under a different commercial name — high overdose risk.
    // The new medicine itself is excluded from the results via its own ID.
    // ─────────────────────────────────────────────────────────────────────────
    @Query("SELECT pl FROM PrescriptionLine pl " +
           "JOIN pl.prescription p " +
           "WHERE p.patientId = :patientId " +
           "AND LOWER(pl.medicine.inn) = LOWER(:inn) " +
           "AND pl.medicine.id <> :excludeMedicineId " +
           "AND p.id <> :excludePrescriptionId " +
           "AND p.status IN ('SIGNED', 'PENDING', 'COMPLETED', 'ACTIVE') " +
           "AND pl.startDate <= :endDate " +
           "AND pl.endDate >= :startDate " +
           "ORDER BY pl.startDate ASC")
    List<PrescriptionLine> findOverlappingByInn(
            @Param("patientId") Long patientId,
            @Param("inn") String inn,
            @Param("excludeMedicineId") Long excludeMedicineId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludePrescriptionId") Long excludePrescriptionId
    );

    // ─────────────────────────────────────────────────────────────────────────
    // QUERY 3 — Same therapeutic family
    // Detects overdose risk when the patient is already prescribed a drug
    // from the same pharmacological class (e.g. two SSRIs, two beta-blockers).
    // The new medicine itself is excluded from the results via its own ID.
    // Only runs if therapeuticFamily is not null/blank.
    // ─────────────────────────────────────────────────────────────────────────
    @Query("SELECT pl FROM PrescriptionLine pl " +
           "JOIN pl.prescription p " +
           "WHERE p.patientId = :patientId " +
           "AND LOWER(pl.medicine.therapeuticFamily) = LOWER(:therapeuticFamily) " +
           "AND pl.medicine.id <> :excludeMedicineId " +
           "AND p.id <> :excludePrescriptionId " +
           "AND p.status IN ('SIGNED', 'PENDING', 'COMPLETED', 'ACTIVE') " +
           "AND pl.startDate <= :endDate " +
           "AND pl.endDate >= :startDate " +
           "ORDER BY pl.startDate ASC")
    List<PrescriptionLine> findOverlappingByTherapeuticFamily(
            @Param("patientId") Long patientId,
            @Param("therapeuticFamily") String therapeuticFamily,
            @Param("excludeMedicineId") Long excludeMedicineId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("excludePrescriptionId") Long excludePrescriptionId
    );

    // ─────────────────────────────────────────────────────────────────────────
    // QUERY 4 — Détection du "Doctor Shopping"
    // Identifie les patients qui tentent d'obtenir la même prescription
    // (même médicament) auprès de plusieurs médecins différents sur une
    // période active (la date de fin n'est pas encore passée).
    //
    // Conditions pour déclencher l'alerte :
    //   • même patient (patientId)
    //   • même médicament (medicineId)
    //   • médecin différent (doctorId différent du médecin courant)
    //   • prescription toujours active : endDate >= aujourd'hui (LocalDate.now())
    //   • statut actif : SIGNED ou PENDING (les CANCELLED/COMPLETED terminés sont exclus)
    // ─────────────────────────────────────────────────────────────────────────
    @Query("SELECT pl FROM PrescriptionLine pl " +
           "JOIN pl.prescription p " +
           "WHERE p.patientId = :patientId " +
           "AND pl.medicine.id = :medicineId " +
           "AND p.doctorId <> :currentDoctorId " +
           "AND pl.endDate >= :today " +
           "AND p.status IN ('SIGNED', 'PENDING') " +
           "ORDER BY pl.endDate DESC")
    List<PrescriptionLine> findActivePrescriptionsByOtherDoctors(
            @Param("patientId")       Long patientId,
            @Param("medicineId")      Long medicineId,
            @Param("currentDoctorId") Long currentDoctorId,
            @Param("today")           LocalDate today
    );
}
