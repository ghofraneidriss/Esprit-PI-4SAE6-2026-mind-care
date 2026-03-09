package tn.esprit.recommendation_service.Services;

import tn.esprit.recommendation_service.Entities.MedicalEvent;
import java.util.List;

public interface MedicalEventService {
    MedicalEvent addEvent(MedicalEvent event);

    List<MedicalEvent> getAllEvents();

    MedicalEvent getEventById(Long id);

    void deleteEvent(Long id);

    List<MedicalEvent> searchEvents(String keyword);
}
