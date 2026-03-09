package tn.esprit.recommendation_service.Services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.recommendation_service.Entities.MedicalEvent;
import tn.esprit.recommendation_service.Repository.MedicalEventRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicalEventServiceImpl implements MedicalEventService {

    private final MedicalEventRepository repository;

    @Override
    public MedicalEvent addEvent(MedicalEvent event) {
        return repository.save(event);
    }

    @Override
    public List<MedicalEvent> getAllEvents() {
        return repository.findAllByOrderByIdDesc();
    }

    @Override
    public MedicalEvent getEventById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Event not found"));
    }

    @Override
    public void deleteEvent(Long id) {
        repository.deleteById(id);
    }

    @Override
    public List<MedicalEvent> searchEvents(String keyword) {
        return repository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrderByTitleAsc(
                keyword, keyword);
    }
}
