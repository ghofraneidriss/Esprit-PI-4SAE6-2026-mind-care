package tn.esprit.traitement_et_consultation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.traitement_et_consultation.entity.AlzheimerStage;
import tn.esprit.traitement_et_consultation.entity.Consultation;
import tn.esprit.traitement_et_consultation.repository.ConsultationRepository;

import java.util.List;
import java.util.Optional;

/**
 * Service métier pour la gestion des consultations médicales.
 * Règle clé : chaque rendez-vous (appointmentId) ne peut être lié qu'à UNE
 * SEULE consultation.
 * Cette règle est vérifiée explicitement avant toute création ou mise à jour.
 */
@Service
@RequiredArgsConstructor
public class ConsultationService {

    private final ConsultationRepository consultationRepository;

    /**
     * Crée une nouvelle consultation après avoir vérifié que le rendez-vous associé
     * n'est pas déjà utilisé par une autre consultation.
     * Le stade Alzheimer est automatiquement déterminé selon le score MMSE fourni.
     *
     * @param consultation données de la consultation à créer
     * @return la consultation sauvegardée
     * @throws IllegalStateException si le rendez-vous est déjà lié à une autre
     *                               consultation
     */
    public Consultation saveConsultation(Consultation consultation) {
        // Vérification unicité : un rendez-vous → une seule consultation
        Optional<Consultation> existing = consultationRepository.findByAppointmentId(consultation.getAppointmentId());
        if (existing.isPresent()) {
            throw new IllegalStateException(
                    "Appointment #" + consultation.getAppointmentId() +
                            " already has a consultation (ID: " + existing.get().getId() + "). " +
                            "Each appointment can only be linked to one consultation.");
        }

        // Détermination automatique du stade Alzheimer selon le score MMSE
        consultation.setAlzheimerStage(suggestAlzheimerStage(consultation.getMmseScore()));
        return consultationRepository.save(consultation);
    }

    /**
     * Détermine automatiquement le stade Alzheimer selon le score MMSE.
     * Barème standard utilisé en clinique :
     * - 26–30 : Préclinique (normal ou très léger)
     * - 20–25 : Léger (mild)
     * - 10–19 : Modéré
     * - 0–9 : Sévère
     *
     * @param mmseScore score MMSE entre 0 et 30
     * @return stade Alzheimer correspondant
     */
    public AlzheimerStage suggestAlzheimerStage(Integer mmseScore) {
        if (mmseScore == null)
            return null;

        if (mmseScore >= 26) {
            return AlzheimerStage.PRECLINICAL;
        } else if (mmseScore >= 20) {
            return AlzheimerStage.MILD;
        } else if (mmseScore >= 10) {
            return AlzheimerStage.MODERATE;
        } else {
            return AlzheimerStage.SEVERE;
        }
    }

    /**
     * Retourne la liste complète de toutes les consultations enregistrées.
     *
     * @return liste de consultations
     */
    public List<Consultation> getAllConsultations() {
        return consultationRepository.findAll();
    }

    /**
     * Fonction de filtrage avancée
     * Cette fonction permet de filtrer la liste des consultations par stade
     * d'Alzheimer
     * et par mot-clé (searchTerm)
     */
    public List<Consultation> getFilteredConsultations(String stage, String searchTerm) {
        return consultationRepository.findAll().stream()
                .filter(c -> stage == null || "all".equalsIgnoreCase(stage)
                        || (c.getAlzheimerStage() != null && c.getAlzheimerStage().name().equalsIgnoreCase(stage)))
                .filter(c -> {
                    if (searchTerm == null || searchTerm.trim().isEmpty())
                        return true;
                    String term = searchTerm.toLowerCase();
                    boolean matchNotes = c.getClinicalNotes() != null
                            && c.getClinicalNotes().toLowerCase().contains(term);
                    boolean matchTension = c.getBloodPressure() != null
                            && c.getBloodPressure().toLowerCase().contains(term);
                    boolean matchAppt = c.getAppointmentId() != null && c.getAppointmentId().toString().contains(term);
                    boolean matchStageStr = c.getAlzheimerStage() != null
                            && c.getAlzheimerStage().name().toLowerCase().contains(term);
                    return matchNotes || matchTension || matchAppt || matchStageStr;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Recherche une consultation par son identifiant.
     * @param id identifiant de la consultation
     * @return Optional contenant la consultation si elle existe
     */
    public Optional<Consultation> getConsultationById(Long id) {
        return consultationRepository.findById(id);
    }

    /**
     * Met à jour une consultation existante.
     * Si l'appointmentId est modifié, vérifie que le nouveau rendez-vous
     * n'est pas déjà utilisé par une autre consultation.
     *
     * @param id      identifiant de la consultation à modifier
     * @param details nouvelles données de la consultation
     * @return la consultation mise à jour
     * @throws IllegalStateException si le nouveau rendez-vous est déjà lié à une
     *                               autre consultation
     */
    public Consultation updateConsultation(Long id, Consultation details) {
        Consultation consultation = consultationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Consultation not found with id: " + id));

        // Vérification unicité uniquement si l'appointmentId change
        if (!consultation.getAppointmentId().equals(details.getAppointmentId())) {
            Optional<Consultation> existing = consultationRepository.findByAppointmentId(details.getAppointmentId());
            if (existing.isPresent()) {
                throw new IllegalStateException(
                        "Appointment #" + details.getAppointmentId() +
                                " already has a consultation (ID: " + existing.get().getId() + "). " +
                                "Each appointment can only be linked to one consultation.");
            }
        }

        consultation.setAppointmentId(details.getAppointmentId());
        consultation.setClinicalNotes(details.getClinicalNotes());
        consultation.setCurrentWeight(details.getCurrentWeight());
        consultation.setBloodPressure(details.getBloodPressure());
        consultation.setMmseScore(details.getMmseScore());
        // Recalcul automatique du stade Alzheimer selon le nouveau score MMSE
        consultation.setAlzheimerStage(suggestAlzheimerStage(details.getMmseScore()));

        return consultationRepository.save(consultation);
    }

    /**
     * Supprime une consultation par son identifiant.
     *
     * @param id identifiant de la consultation à supprimer
     */
    public void deleteConsultation(Long id) {
        consultationRepository.deleteById(id);
    }
}
