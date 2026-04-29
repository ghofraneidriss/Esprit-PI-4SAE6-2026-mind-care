package tn.esprit.ordonnance_et_medicaments.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.ordonnance_et_medicaments.dto.DoctorShoppingAlertDTO;
import tn.esprit.ordonnance_et_medicaments.entities.PrescriptionLine;
import tn.esprit.ordonnance_et_medicaments.repository.PrescriptionLineRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service de détection du "Doctor Shopping".
 *
 * Objectif : Identifier les patients qui tentent d'obtenir la même prescription
 * (même médicament) auprès de plusieurs médecins différents alors qu'une
 * prescription active existe déjà chez un autre praticien.
 *
 * Règle métier :
 *  - La prescription existante est considérée "active" uniquement si sa
 *    date de fin (endDate) est supérieure ou égale à aujourd'hui.
 *  - Les prescriptions expirées ou annulées ne déclenchent pas d'alerte.
 *  - Le médecin courant (currentDoctorId) est exclu de la recherche.
 *
 * Cette détection est déclenchée lors de la saisie d'une nouvelle ligne
 * de prescription, avant la soumission finale.
 */
@Service
@RequiredArgsConstructor
public class DoctorShoppingDetectionService {

    /** Repository des lignes de prescription, contient la JPQL de détection */
    private final PrescriptionLineRepository prescriptionLineRepository;

    /**
     * Vérifie si un patient tente un "doctor shopping" pour un médicament donné.
     *
     * Recherche toutes les lignes de prescription actives pour ce médicament,
     * pour ce patient, mais prescrites par un médecin différent du médecin courant.
     *
     * @param patientId       ID du patient concerné
     * @param medicineId      ID du médicament que le médecin courant tente de prescrire
     * @param currentDoctorId ID du médecin en train de rédiger la nouvelle prescription
     * @return Liste de DTO d'alertes (vide si aucun comportement suspect détecté)
     */
    public List<DoctorShoppingAlertDTO> detectDoctorShopping(
            Long patientId,
            Long medicineId,
            Long currentDoctorId) {

        // Date du jour — seules les prescriptions dont endDate >= today sont "actives"
        LocalDate today = LocalDate.now();

        /*
         * Appel JPQL (Query 4 — PrescriptionLineRepository) :
         * Retourne les lignes de prescription actives pour ce médicament,
         * prescrites par un médecin DIFFÉRENT du médecin courant.
         */
        List<PrescriptionLine> activeLines = prescriptionLineRepository
                .findActivePrescriptionsByOtherDoctors(patientId, medicineId, currentDoctorId, today);

        /*
         * Transformation des entités en DTO d'alerte.
         * Chaque ligne trouve sa prescription parente via pl.getPrescription().
         */
        return activeLines.stream()
                .map(pl -> new DoctorShoppingAlertDTO(
                        pl.getPrescription().getId(),          // ID de la prescription en conflit
                        pl.getPrescription().getDoctorId(),    // ID du médecin prescripteur
                        pl.getMedicine().getCommercialName(),  // Nom commercial du médicament
                        pl.getMedicine().getInn(),             // DCI du médicament
                        pl.getStartDate(),                     // Date de début du traitement actif
                        pl.getEndDate(),                       // Date de fin du traitement actif
                        pl.getDosage(),                        // Posologie dans l'ordonnance existante
                        pl.getPrescription().getStatus()       // Statut : SIGNED, PENDING…
                ))
                .collect(Collectors.toList());
    }
}
