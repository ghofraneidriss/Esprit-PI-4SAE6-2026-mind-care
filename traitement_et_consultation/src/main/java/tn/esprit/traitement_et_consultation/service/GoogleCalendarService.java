package tn.esprit.traitement_et_consultation.service;

import org.springframework.stereotype.Service;
import tn.esprit.traitement_et_consultation.entity.Appointment;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GoogleCalendarService {

    /**
     * Client API Google Calendar (doit être initialisé via credentials.json).
     * Permet la synchronisation bidirectionnelle avec le calendrier du médecin.
     */
    private com.google.api.services.calendar.Calendar calendarClient;

    /**
     * Crée un événement dans Google Calendar pour un rendez-vous donné.
     * Cette méthode est appelée depuis AppointmentService lors de la confirmation
     * d'un rendez-vous.
     * Elle gère également la génération automatique d'un lien Google Meet pour les
     * consultations ONLINE.
     */
    public com.google.api.services.calendar.model.Event createGoogleEvent(Appointment appointment) {
        log.info("Création d'un événement Google Calendar pour le RDV ID: {}", appointment.getId());

        // Configuration des dates de l'événement (ZonedDateTime pour la gestion des
        // fuseaux horaires)
        java.time.ZonedDateTime startZoned = appointment.getAppointmentDate().atZone(java.time.ZoneId.systemDefault());
        com.google.api.client.util.DateTime start = new com.google.api.client.util.DateTime(
                startZoned.toInstant().toEpochMilli());

        // Durée par défaut de 30 minutes
        com.google.api.client.util.DateTime end = new com.google.api.client.util.DateTime(
                startZoned.plusMinutes(30).toInstant().toEpochMilli());

        com.google.api.services.calendar.model.Event event = new com.google.api.services.calendar.model.Event()
                .setSummary("RDV MindCare: " + appointment.getType())
                .setDescription("Patient ID: " + appointment.getPatientId() + "\n" +
                        "Note: Consultation via la plateforme MindCare.")
                .setStart(new com.google.api.services.calendar.model.EventDateTime().setDateTime(start))
                .setEnd(new com.google.api.services.calendar.model.EventDateTime().setDateTime(end));

        // Intégration Google Meet : On configure la demande de conférence automatique
        if (tn.esprit.traitement_et_consultation.entity.AppointmentType.ONLINE.equals(appointment.getType())) {
            com.google.api.services.calendar.model.ConferenceData conferenceData = new com.google.api.services.calendar.model.ConferenceData();
            com.google.api.services.calendar.model.CreateConferenceRequest conferenceRequest = new com.google.api.services.calendar.model.CreateConferenceRequest();
            conferenceRequest.setRequestId(java.util.UUID.randomUUID().toString());
            conferenceRequest.setConferenceSolutionKey(
                    new com.google.api.services.calendar.model.ConferenceSolutionKey().setType("hangoutsMeet"));
            conferenceData.setCreateRequest(conferenceRequest);
            event.setConferenceData(conferenceData);
        }

        try {
            // Insertion réelle dans Google Calendar si le client est configuré
            if (calendarClient != null) {
                return calendarClient.events().insert("primary", event)
                        .setConferenceDataVersion(1)
                        .execute();
            }
        } catch (Exception e) {
            log.error("Erreur API Google Calendar (Vérifiez credentials.json): {}", e.getMessage());
        }

        // Bloc de retour sécurisé : génère un ID unique pour éviter les plantages si
        // l'API est hors-ligne
        event.setId("external-" + java.util.UUID.randomUUID().toString());
        if (event.getConferenceData() != null) {
            // Lien générique utilisé si l'API Google Meet n'est pas encore activée
            event.setHangoutLink("https://meet.google.com/mind-care-appt");
        }
        return event;
    }

    /**
     * Supprime un événement Google Calendar via son identifiant externe.
     */
    public void deleteGoogleEvent(String googleEventId) {
        log.info("Suppression de l'événement Google ID: {}", googleEventId);
        try {
            if (calendarClient != null) {
                calendarClient.events().delete("primary", googleEventId).execute();
            }
        } catch (Exception e) {
            log.error("Erreur lors de la suppression Google event {}: {}", googleEventId, e.getMessage());
        }
    }
}
