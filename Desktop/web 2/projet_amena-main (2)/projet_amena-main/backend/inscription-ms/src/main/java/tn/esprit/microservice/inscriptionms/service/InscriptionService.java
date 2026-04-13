package tn.esprit.microservice.inscriptionms.service;

import java.util.List;
import org.springframework.stereotype.Service;
import tn.esprit.microservice.inscriptionms.client.CoursClient;
import tn.esprit.microservice.inscriptionms.dto.CoursSummary;
import tn.esprit.microservice.inscriptionms.dto.InscriptionDetails;
import tn.esprit.microservice.inscriptionms.entity.Inscription;
import tn.esprit.microservice.inscriptionms.exception.ResourceNotFoundException;
import tn.esprit.microservice.inscriptionms.repository.InscriptionRepository;

@Service
public class InscriptionService {

    private final InscriptionRepository inscriptionRepository;
    private final CoursClient coursClient;

    public InscriptionService(InscriptionRepository inscriptionRepository, CoursClient coursClient) {
        this.inscriptionRepository = inscriptionRepository;
        this.coursClient = coursClient;
    }

    public List<Inscription> getAllInscriptions() {
        return inscriptionRepository.findAll();
    }

    public Inscription getInscriptionById(Long id) {
        return inscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inscription with id " + id + " not found"));
    }

    public Inscription createInscription(Inscription inscription) {
        return inscriptionRepository.save(inscription);
    }

    public Inscription updateInscription(Long id, Inscription request) {
        Inscription existing = getInscriptionById(id);
        existing.setStudentName(request.getStudentName());
        existing.setStudentEmail(request.getStudentEmail());
        existing.setCoursId(request.getCoursId());
        return inscriptionRepository.save(existing);
    }

    public void deleteInscription(Long id) {
        Inscription inscription = getInscriptionById(id);
        inscriptionRepository.delete(inscription);
    }

    public InscriptionDetails getInscriptionDetails(Long id) {
        Inscription inscription = getInscriptionById(id);
        CoursSummary coursSummary = coursClient.getCoursSummary(inscription.getCoursId());
        return new InscriptionDetails(inscription, coursSummary);
    }
}
