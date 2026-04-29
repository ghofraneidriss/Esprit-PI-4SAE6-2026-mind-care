package tn.esprit.traitement_et_consultation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.traitement_et_consultation.entity.Appointment;
import tn.esprit.traitement_et_consultation.service.AppointmentService;
import tn.esprit.traitement_et_consultation.service.EmailService;
import tn.esprit.traitement_et_consultation.dto.AlertRequest;

import java.util.List;

/**
 * Contrôleur REST exposant les endpoints HTTP pour la gestion des rendez-vous
 * médicaux.
 * Préfixe de base : /api/appointments
 */
@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@CrossOrigin("*")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final EmailService emailService;

    @ExceptionHandler(tn.esprit.traitement_et_consultation.exception.SlotUnavailableException.class)
    public ResponseEntity<tn.esprit.traitement_et_consultation.dto.SlotSuggestionResponse> handleSlotUnavailable(
            tn.esprit.traitement_et_consultation.exception.SlotUnavailableException ex) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CONFLICT).body(ex.getSuggestionResponse());
    }

    /**
     * Crée un nouveau rendez-vous.
     * Le statut initial est automatiquement défini à PENDING (en attente).
     *
     * POST /api/appointments
     *
     * @param appointment données du rendez-vous envoyées dans le corps de la
     *                    requête
     * @return le rendez-vous créé avec son identifiant généré
     */
    @PostMapping
    public ResponseEntity<Appointment> createAppointment(@RequestBody Appointment appointment) {
        return ResponseEntity.ok(appointmentService.createAppointment(appointment));
    }

    /**
     * Confirme un rendez-vous en attente et déclenche la création de l'événement
     * Google Calendar.
     * Le statut passe de PENDING à CONFIRMED.
     *
     * POST /api/appointments/{id}/confirm
     *
     * @param id identifiant du rendez-vous à confirmer
     * @return le rendez-vous avec son statut mis à jour et son googleEventId
     */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<Appointment> confirmAppointment(@PathVariable("id") Long id) {
        return ResponseEntity.ok(appointmentService.confirmAppointment(id));
    }

    /**
     * Annule un rendez-vous en changeant son statut à 'CANCELLED'.
     * Cette action peut être effectuée par le patient ou le médecin.
     * Une fois annulé, le créneau redeviendra disponible pour de nouvelles
     * réservations.
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Appointment> cancelAppointment(@PathVariable("id") Long id) {
        return ResponseEntity.ok(appointmentService.cancelAppointment(id));
    }

    /**
     * Récupère tous les rendez-vous d'un médecin donné.
     *
     * GET /api/appointments/doctor/{doctorId}
     *
     * @param doctorId identifiant du médecin
     * @return liste des rendez-vous du médecin
     */
    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<List<Appointment>> getDoctorAppointments(@PathVariable("doctorId") Long doctorId) {
        return ResponseEntity.ok(appointmentService.getAppointmentsByDoctor(doctorId));
    }

    /**
     * Récupère tous les rendez-vous d'un patient donné.
     *
     * GET /api/appointments/patient/{patientId}
     *
     * @param patientId identifiant du patient
     * @return liste des rendez-vous du patient
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<Appointment>> getPatientAppointments(@PathVariable("patientId") Long patientId) {
        return ResponseEntity.ok(appointmentService.getAppointmentsByPatient(patientId));
    }

    /**
     * Récupère la liste complète de tous les rendez-vous enregistrés.
     *
     * GET /api/appointments
     *
     * @return liste de tous les rendez-vous
     */
    @GetMapping
    public ResponseEntity<List<Appointment>> getAll() {
        return ResponseEntity.ok(appointmentService.getAllAppointments());
    }

    /**
     * COMMENTAIRE POUR LE REPERAGE (Demande utilisateur) :
     * Point d'entrée pour récupérer les rendez-vous filtrés depuis le backend.
     * L'application (frontend) envoie ces paramètres pour que Spring boot fasse le
     * travail de tri et filtrage.
     * 
     * GET /api/appointments/filter
     */
    @GetMapping("/filter")
    public ResponseEntity<List<Appointment>> getFilteredAppointments(
            @RequestParam(name = "doctorId", required = false) Long doctorId,
            @RequestParam(name = "patientId", required = false) Long patientId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "isUrgent", required = false) Boolean isUrgent,
            @RequestParam(name = "date", required = false) String date,
            @RequestParam(name = "minScore", required = false) Integer minScore,
            @RequestParam(name = "maxScore", required = false) Integer maxScore,
            @RequestParam(name = "sortByScore", defaultValue = "false") boolean sortByScore) {
        return ResponseEntity
                .ok(appointmentService.getFilteredAppointments(doctorId, patientId, status, isUrgent, date, minScore,
                        maxScore,
                        sortByScore));
    }

    /**
     * NOUVEAU: Récupère les options de dates uniques pour le filtrage
     */
    @GetMapping("/filter-options/dates")
    public ResponseEntity<List<String>> getFilterDates(
            @RequestParam(name = "doctorId", required = false) Long doctorId) {
        return ResponseEntity.ok(appointmentService.getFilterDates(doctorId));
    }

    /**
     * NOUVEAU: Récupère les identifiants de patients uniques pour le filtrage
     */
    @GetMapping("/filter-options/patients")
    public ResponseEntity<List<Long>> getFilterPatients(
            @RequestParam(name = "doctorId", required = false) Long doctorId) {
        return ResponseEntity.ok(appointmentService.getFilterPatients(doctorId));
    }

    /**
     * Récupère un rendez-vous par son identifiant.
     *
     * GET /api/appointments/{id}
     *
     * @param id identifiant du rendez-vous
     * @return le rendez-vous trouvé, ou 404 s'il n'existe pas
     */
    @GetMapping("/{id}")
    public ResponseEntity<Appointment> getById(@PathVariable(name = "id") Long id) {
        return appointmentService.getAppointmentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Met à jour un rendez-vous existant.
     *
     * PUT /api/appointments/{id}
     *
     * @param id          identifiant du rendez-vous à modifier
     * @param appointment nouvelles données du rendez-vous
     * @return le rendez-vous mis à jour
     */
    @PutMapping("/{id}")
    public ResponseEntity<Appointment> update(@PathVariable(name = "id") Long id,
            @RequestBody Appointment appointment) {
        return ResponseEntity.ok(appointmentService.updateAppointment(id, appointment));
    }

    /**
     * Supprime un rendez-vous par son identifiant.
     *
     * DELETE /api/appointments/{id}
     *
     * @param id identifiant du rendez-vous à supprimer
     * @return réponse 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable(name = "id") Long id) {
        appointmentService.deleteAppointment(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Envoie un e-mail d'alerte à une adresse donnée.
     * Utilisé pour notifier un patient ou un médecin en cas d'urgence ou de
     * changement.
     *
     * POST /api/appointments/alert
     *
     * @param request objet contenant l'adresse e-mail, le sujet et le message
     * @return message de confirmation ou d'erreur
     */
    @PostMapping("/alert")
    public ResponseEntity<String> sendAlertEmail(@RequestBody AlertRequest request) {
        try {
            emailService.sendEmail(request.getEmail(), request.getSubject(), request.getMessage());
            return ResponseEntity.ok("Alert email sent successfully to " + request.getEmail());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error sending email: " + e.getMessage());
        }
    }

    /**
     * Analyse la charge de travail d'un médecin et les occupations d'un patient
     * pour retourner automatiquement le meilleur créneau disponible sur une période
     * donnée.
     *
     * Les règles appliquées côté service :
     * - Créneaux disponibles : 09h00 → 16h00
     * - Pause déjeuner exclue : 13h00 → 14h00
     * - Durée de consultation : 35 min (plage 30-40 min)
     * - Pause entre deux patients : 10 min minimum
     * - Disponibilité du patient vérifiée également (simulation Google Calendar)
     *
     * GET /api/appointments/doctor/{doctorId}/suggest-slot
     *
     * @param doctorId  identifiant du médecin ciblé
     * @param patientId identifiant du patient (pour croiser sa disponibilité)
     * @param startDate date de début de la plage de recherche (format ISO :
     *                  yyyy-MM-dd)
     * @param endDate   date de fin de la plage de recherche (format ISO :
     *                  yyyy-MM-dd)
     * @return la date et l'heure du meilleur créneau disponible, ou 404 si aucun
     *         trouvé
     */
    @GetMapping("/doctor/{doctorId}/suggest-slot")
    public ResponseEntity<java.time.LocalDateTime> suggestSlot(
            @PathVariable(name = "doctorId") Long doctorId,
            @RequestParam(name = "patientId") Long patientId,
            @RequestParam(name = "startDate") @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate startDate,
            @RequestParam(name = "endDate") @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate endDate) {

        java.time.LocalDateTime suggested = appointmentService.suggestBestSlot(
                doctorId, patientId, startDate, endDate);

        if (suggested != null) {
            return ResponseEntity.ok(suggested);
        }
        // Aucun créneau disponible sur la période → retour 404
        return ResponseEntity.notFound().build();
    }
}
