package tn.esprit.microservice.coursms.service;

import java.util.List;
import org.springframework.stereotype.Service;
import tn.esprit.microservice.coursms.dto.CoursSummary;
import tn.esprit.microservice.coursms.entity.Cours;
import tn.esprit.microservice.coursms.exception.ResourceNotFoundException;
import tn.esprit.microservice.coursms.repository.CoursRepository;

@Service
public class CoursService {

    private final CoursRepository coursRepository;

    public CoursService(CoursRepository coursRepository) {
        this.coursRepository = coursRepository;
    }

    public List<Cours> getAllCours() {
        return coursRepository.findAll();
    }

    public Cours getCoursById(Long id) {
        return coursRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cours with id " + id + " not found"));
    }

    public Cours createCours(Cours cours) {
        return coursRepository.save(cours);
    }

    public Cours updateCours(Long id, Cours request) {
        Cours existing = getCoursById(id);
        existing.setTitle(request.getTitle());
        existing.setDescription(request.getDescription());
        existing.setCover(request.getCover());
        existing.setVideo(request.getVideo());
        existing.setCategory(request.getCategory());
        existing.setLanguage(request.getLanguage());
        existing.setLevel(request.getLevel());
        existing.setPrice(request.getPrice());
        existing.setDuration(request.getDuration());
        existing.setInstructor(request.getInstructor());
        existing.setStatus(request.getStatus());
        return coursRepository.save(existing);
    }

    public void deleteCours(Long id) {
        Cours cours = getCoursById(id);
        coursRepository.delete(cours);
    }

    public CoursSummary getCoursSummary(Long id) {
        Cours cours = getCoursById(id);
        String status = cours.getStatus() == null ? null : cours.getStatus().name();
        return new CoursSummary(cours.getId(), cours.getTitle(), cours.getCategory(), cours.getInstructor(), status);
    }
}
