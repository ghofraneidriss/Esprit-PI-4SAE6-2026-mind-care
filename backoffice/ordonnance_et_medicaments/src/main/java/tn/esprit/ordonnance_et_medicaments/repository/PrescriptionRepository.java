package tn.esprit.ordonnance_et_medicaments.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.esprit.ordonnance_et_medicaments.entities.Prescription;

import java.util.List;

/**
 * Repository pour l'entité Prescription.
 */
@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    
    // Historique complet pour un patient donné
    List<Prescription> findByPatientIdOrderByCreatedAtDesc(Long patientId);

    // Accès aux propres prescriptions d'un patient
    List<Prescription> findByPatientId(Long patientId);

    // Recherche de prescriptions liées à une consultation spécifique
    List<Prescription> findByConsultationId(Long consultationId);
}
