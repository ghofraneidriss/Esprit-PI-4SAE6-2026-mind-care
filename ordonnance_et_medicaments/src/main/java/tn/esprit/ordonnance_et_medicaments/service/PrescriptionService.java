package tn.esprit.ordonnance_et_medicaments.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.ordonnance_et_medicaments.entities.Prescription;
import tn.esprit.ordonnance_et_medicaments.entities.PrescriptionLine;
import tn.esprit.ordonnance_et_medicaments.entities.Medicine;
import tn.esprit.ordonnance_et_medicaments.repository.PrescriptionRepository;
import tn.esprit.ordonnance_et_medicaments.repository.MedicineRepository;

import java.util.List;
import java.util.Optional;

/**
 * Service pour la gestion des prescriptions (Prescription).
 */
@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final MedicineRepository medicineRepository; // Importation du repository des médicaments

    /** Service d'envoi d'email — déclenché lors de la signature d'une prescription */
    private final PrescriptionMailService mailService;

    /**
     * Pour le MEDECIN : Créer une prescription avec ses lignes associées.
     * Gère l'ajout à la volée de nouveaux médicaments s'ils ne sont pas trouvés.
     */
    @Transactional
    public Prescription createPrescription(Prescription prescription) {
        if (prescription.getPrescriptionLines() != null) {
            for (PrescriptionLine line : prescription.getPrescriptionLines()) {
                line.setPrescription(prescription);

                // Vérifier si le médicament est nouveau ou doit être créé à la volée
                if (line.getMedicine() != null && line.getMedicine().getId() == null) {
                    Medicine med = line.getMedicine();
                    
                    // Utilisation spécifique de JPQL pour voir si ce médicament existe déjà malgré l'absence d'ID
                    Optional<Medicine> match = medicineRepository.findExactMatchJPQL(
                            med.getCommercialName(), med.getInn());

                    if (match.isPresent()) {
                        line.setMedicine(match.get()); // Rattachement au médicament existant trouvé via JPQL
                    } else {
                        // Création du nouveau médicament dans le référentiel à la volée
                        medicineRepository.save(med);
                    }
                }
            }
        }
        return prescriptionRepository.save(prescription);
    }

    /**
     * Pour le MEDECIN : Sauvegarder la prescription actuelle en tant que brouillon (DRAFT).
     * Cela permet de sauvegarder le travail en cours tout en ajoutant de nouveaux médicaments.
     */
    @Transactional
    public Prescription saveAsDraft(Prescription prescription) {
        // Définit le statut comme brouillon pour permettre une modification ultérieure
        prescription.setStatus("DRAFT");
        return createPrescription(prescription);
    }

    /**
     * Pour le MEDECIN : Modifier une prescription existante.
     */
    @Transactional
    public Prescription updatePrescription(Long id, Prescription details) {
        Prescription p = prescriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prescription not found with id: " + id));

        // Mise à jour du statut
        p.setStatus(details.getStatus());

        // Mise à jour de la signature du médecin
        p.setDoctorSignature(details.getDoctorSignature());

        // On remplace les anciennes lignes par les nouvelles
        if (details.getPrescriptionLines() != null) {
            p.getPrescriptionLines().clear();
            details.getPrescriptionLines().forEach(line -> {
                line.setPrescription(p);
                p.getPrescriptionLines().add(line);
            });
        }

        Prescription saved = prescriptionRepository.save(p);

        /*
         * Envoi de l'email au patient uniquement quand la prescription passe en statut SIGNED.
         * L'email contient la liste de tous les médicaments prescrits avec posologie et dates.
         * L'envoi est non-bloquant : une erreur d'email ne fait pas échouer la mise à jour.
         */
        if ("SIGNED".equalsIgnoreCase(details.getStatus())) {
            mailService.sendSignedPrescriptionEmail(saved);
        }

        return saved;
    }

    /**
     * Pour le MEDECIN : Supprimer une prescription.
     */
    public void deletePrescription(Long id) {
        prescriptionRepository.deleteById(id);
    }

    /**
     * Pour le MEDECIN & PATIENT : Consulter l'historique d'un patient.
     */
    public List<Prescription> getHistoryByPatientId(Long patientId) {
        return prescriptionRepository.findByPatientIdOrderByCreatedAtDesc(patientId);
    }

    /**
     * Recherche d'une prescription par son ID.
     */
    public Prescription getPrescriptionById(Long id) {
        return prescriptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prescription not found with id: " + id));
    }
}
