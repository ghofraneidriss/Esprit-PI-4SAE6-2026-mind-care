package tn.esprit.ordonnance_et_medicaments.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.ordonnance_et_medicaments.dto.OverlapConflictDTO;
import tn.esprit.ordonnance_et_medicaments.entities.PrescriptionLine;
import tn.esprit.ordonnance_et_medicaments.repository.PrescriptionLineRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service de détection des chevauchements de médicaments entre prescriptions.
 *
 * Logique métier :
 * Un chevauchement existe si le même médicament est prescrit au même patient
 * dans deux prescriptions différentes dont les dates se chevauchent.
 *
 * Formule de chevauchement : startA <= endB AND endA >= startB
 */
@Service
@RequiredArgsConstructor
public class PrescriptionOverlapService {

    private final PrescriptionLineRepository prescriptionLineRepository;

    /**
     * Vérifie si un médicament est déjà prescrit au patient dans une autre ordonnance
     * avec des dates qui se chevauchent.
     *
     * @param patientId             ID du patient
     * @param medicineId            ID du médicament à vérifier
     * @param startDate             Début de la nouvelle ligne de prescription
     * @param endDate               Fin de la nouvelle ligne de prescription
     * @param currentPrescriptionId ID de la prescription en cours (à exclure de la recherche)
     * @return Liste des conflits de chevauchement détectés (vide = aucun conflit)
     */
    public List<OverlapConflictDTO> detectOverlaps(Long patientId,
                                                    Long medicineId,
                                                    LocalDate startDate,
                                                    LocalDate endDate,
                                                    Long currentPrescriptionId) {

        // currentPrescriptionId = 0 signifie nouvelle prescription (pas encore créée)
        Long excludeId = (currentPrescriptionId != null && currentPrescriptionId > 0)
                ? currentPrescriptionId
                : Long.MAX_VALUE;

        // Requête JPQL : trouver les lignes en conflit
        List<PrescriptionLine> conflicts = prescriptionLineRepository
                .findOverlappingMedicinePrescriptions(patientId, medicineId, startDate, endDate, excludeId);

        // Transformer en DTOs pour la réponse API
        return conflicts.stream().map(pl -> new OverlapConflictDTO(
                pl.getPrescription().getId(),
                pl.getMedicine().getCommercialName(),
                pl.getMedicine().getInn(),
                pl.getMedicine().getTherapeuticFamily(),
                pl.getStartDate(),
                pl.getEndDate(),
                pl.getDosage(),
                pl.getPrescription().getStatus()
        )).collect(Collectors.toList());
    }
}
