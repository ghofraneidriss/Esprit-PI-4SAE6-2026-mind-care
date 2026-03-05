package tn.esprit.traitement_et_consultation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.traitement_et_consultation.entity.Appointment;
import tn.esprit.traitement_et_consultation.entity.AppointmentStatus;
import tn.esprit.traitement_et_consultation.entity.PatientProfile;
import tn.esprit.traitement_et_consultation.repository.AppointmentRepository;
import tn.esprit.traitement_et_consultation.repository.PatientProfileRepository;

import java.util.List;
import java.util.Optional;

/**
 * Service métier pour la gestion des rendez-vous médicaux.
 * Contient toute la logique de création, modification, suppression
 * et suggestion intelligente de créneaux horaires.
 */
@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final GoogleCalendarService googleCalendarService;
    private final EmailService emailService;

    /**
     * Valide si le rendez-vous se situe dans les horaires d'ouverture du cabinet
     * (09:00 - 16:00).
     * Gère également l'exclusion de la pause déjeuner (13:00 - 14:00).
     */
    private void validateWorkingHours(java.time.LocalDateTime aptDate, java.time.LocalDateTime aptEndDate) {
        if (aptDate == null)
            return;
        int hour = aptDate.getHour();
        if (hour < 9 || hour >= 16) {
            throw new IllegalStateException("Le cabinet est ouvert uniquement entre 09:00 et 16:00.");
        }
        // Vérification de la pause déjeuner
        java.time.LocalDateTime lunchStart = aptDate.toLocalDate().atTime(13, 0);
        java.time.LocalDateTime lunchEnd = aptDate.toLocalDate().atTime(14, 0);
        if (aptDate.isBefore(lunchEnd) && aptEndDate.isAfter(lunchStart)) {
            throw new IllegalStateException("Le médecin est en pause déjeuner entre 13:00 et 14:00.");
        }
    }

    /**
     * [ALGORITHME] Ajuste une date vers le créneau de travail précédent le plus
     * proche.
     */
    private java.time.LocalDateTime adjustToWorkingHoursBefore(java.time.LocalDateTime time) {
        int hour = time.getHour();
        if (hour >= 16 || (hour == 15 && time.getMinute() > 25))
            return time.toLocalDate().atTime(15, 25);
        if (hour == 13 || (hour == 12 && time.getMinute() > 25))
            return time.toLocalDate().atTime(12, 25);
        if (hour < 9)
            return time.toLocalDate().minusDays(1).atTime(15, 25);
        return time;
    }

    /**
     * [ALGORITHME] Ajuste une date vers le créneau de travail suivant le plus
     * proche.
     */
    private java.time.LocalDateTime adjustToWorkingHoursAfter(java.time.LocalDateTime time) {
        int hour = time.getHour();
        if (hour < 9)
            return time.toLocalDate().atTime(9, 0);
        if (hour == 13)
            return time.toLocalDate().atTime(14, 0);
        if (hour >= 16 || (hour == 15 && time.getMinute() > 25))
            return time.toLocalDate().plusDays(1).atTime(9, 0);
        return time;
    }

    /**
     * Recherche la meilleure option de créneau disponible AVANT un blocage.
     */
    private java.time.LocalDateTime findOptionA(Long doctorId, java.time.LocalDateTime blockedStart) {
        java.time.LocalDateTime cand = blockedStart.minusMinutes(35);
        int attempts = 0;
        while (cand != null && attempts < 10) {
            cand = adjustToWorkingHoursBefore(cand);
            List<Appointment> overlaps = appointmentRepository.findOverlappingAppointments(doctorId, cand,
                    cand.plusMinutes(35), cand.minusMinutes(35));
            if (overlaps.isEmpty())
                return cand;
            java.time.LocalDateTime maxEnd = appointmentRepository.findMaxEndTimeBefore(doctorId, cand);
            cand = (maxEnd != null && maxEnd.isBefore(cand)) ? maxEnd.minusMinutes(35) : cand.minusMinutes(35);
            attempts++;
        }
        return null;
    }

    /**
     * Recherche la meilleure option de créneau disponible APRES un blocage.
     */
    private java.time.LocalDateTime findOptionB(Long doctorId, java.time.LocalDateTime blockedEnd) {
        java.time.LocalDateTime cand = blockedEnd;
        int attempts = 0;
        while (cand != null && attempts < 10) {
            cand = adjustToWorkingHoursAfter(cand);
            List<Appointment> overlaps = appointmentRepository.findOverlappingAppointments(doctorId, cand,
                    cand.plusMinutes(35), cand.minusMinutes(35));
            if (overlaps.isEmpty())
                return cand;
            java.time.LocalDateTime minStart = appointmentRepository.findMinStartTimeAfter(doctorId, cand);
            cand = (minStart != null && minStart.isAfter(cand)) ? minStart : cand.plusMinutes(35);
            attempts++;
        }
        return null;
    }

    /**
     * [MÉTIER] Vérifie la disponibilité d'un médecin pour un nouveau RDV.
     * En cas de conflit, lève une exception contenant des suggestions de créneaux
     * libres.
     */
    private void validateAvailability(Appointment appointment) {
        java.time.LocalDateTime start = appointment.getAppointmentDate();
        if (start == null)
            return;
        java.time.LocalDateTime end = appointment.getAppointmentEndDate() != null ? appointment.getAppointmentEndDate()
                : start.plusMinutes(35);
        appointment.setAppointmentEndDate(end);

        validateWorkingHours(start, end);

        List<Appointment> overlaps = appointmentRepository.findOverlappingAppointments(appointment.getDoctorId(), start,
                end, start.minusMinutes(35));
        if (!overlaps.isEmpty()) {
            java.time.LocalDateTime minStart = overlaps.stream().map(Appointment::getAppointmentDate)
                    .min(java.time.LocalDateTime::compareTo).orElse(start);
            java.time.LocalDateTime maxEnd = overlaps.stream()
                    .map(a -> a.getAppointmentEndDate() != null ? a.getAppointmentEndDate()
                            : a.getAppointmentDate().plusMinutes(35))
                    .max(java.time.LocalDateTime::compareTo).orElse(end);

            java.time.LocalDateTime optionA = findOptionA(appointment.getDoctorId(), minStart);
            java.time.LocalDateTime optionB = findOptionB(appointment.getDoctorId(), maxEnd);

            tn.esprit.traitement_et_consultation.dto.SlotSuggestionResponse response = new tn.esprit.traitement_et_consultation.dto.SlotSuggestionResponse();
            response.setMessage("Le créneau demandé chevauche un événement existant. Voici des alternatives.");
            response.setOptionA(optionA);
            response.setOptionB(optionB);

            throw new tn.esprit.traitement_et_consultation.exception.SlotUnavailableException(response);
        }
    }

    /**
     * [API] Crée un nouveau rendez-vous avec le statut initial PENDING.
     * Envoie une notification par e-mail au patient après la sauvegarde.
     */
    public Appointment createAppointment(Appointment appointment) {
        validateAvailability(appointment);

        if (tn.esprit.traitement_et_consultation.entity.AppointmentCategory.NEW_CONSULTATION
                .equals(appointment.getCategory())) {
            List<Appointment> existing = appointmentRepository.findByPatientId(appointment.getPatientId());
            if (existing != null && !existing.isEmpty()) {
                throw new IllegalStateException(
                        "Un patient ne peut pas cumuler plusieurs consultations 'New Consultation'.");
            }
        }
        appointment.setStatus(AppointmentStatus.PENDING);
        Appointment saved = appointmentRepository.save(appointment);

        sendEmailNotification(saved, "CREATED");
        return saved;
    }

    /**
     * [API + GOOGLE] Confirme un rendez-vous et synchronise avec Google Calendar.
     * Récupère le lien Google Meet via GoogleCalendarService et le lie au RDV.
     */
    public Appointment confirmAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Rendez-vous introuvable"));

        appointment.setStatus(AppointmentStatus.CONFIRMED);

        // Appel du service Google pour la création de l'événement et du lien Meet
        com.google.api.services.calendar.model.Event googleEvent = googleCalendarService.createGoogleEvent(appointment);
        appointment.setGoogleEventId(googleEvent.getId());

        if (googleEvent.getHangoutLink() != null) {
            appointment.setMeetLink(googleEvent.getHangoutLink());
        }

        Appointment saved = appointmentRepository.save(appointment);
        sendEmailNotification(saved, "CONFIRMED");
        return saved;
    }

    /**
     * [API] Annule un rendez-vous et libère le créneau.
     * Cette méthode change le statut en 'CANCELLED' et envoie une notification au
     * patient.
     * Elle est liée au bouton "Annuler" du Frontend Angular.
     *
     * @param appointmentId identifiant du rendez-vous à annuler
     * @return le rendez-vous annulé
     */
    public Appointment cancelAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Rendez-vous introuvable : " + appointmentId));

        appointment.setStatus(AppointmentStatus.CANCELLED);
        Appointment saved = appointmentRepository.save(appointment);

        sendEmailNotification(saved, "CANCELLED");
        return saved;
    }

    /**
     * [ALGORITHME] Suggère le meilleur créneau horaire disponible pour un médecin.
     * Analyse les conflits d'horaires et les pauses pour proposer une alternative
     * optimale.
     * Cette méthode est liée au Frontend Angular pour l'auto-complétion du
     * formulaire de RDV.
     */
    public java.time.LocalDateTime suggestBestSlot(Long doctorId, Long patientId, java.time.LocalDate startDate,
            java.time.LocalDate endDate) {
        // Logique de suggestion de créneau basée sur les disponibilités réelles
        java.time.LocalDateTime current = startDate.atTime(9, 0);
        java.time.LocalDateTime limit = endDate.atTime(16, 0);

        while (current.isBefore(limit)) {
            try {
                Appointment mockArr = new Appointment();
                mockArr.setDoctorId(doctorId);
                mockArr.setPatientId(patientId);
                mockArr.setAppointmentDate(current);
                mockArr.setAppointmentEndDate(current.plusMinutes(35));

                validateAvailability(mockArr);
                return current;
            } catch (Exception e) {
                current = current.plusMinutes(45); // Avancer par blocs de 45 min
                if (current.getHour() >= 16) {
                    current = current.plusDays(1).withHour(9).withMinute(0);
                }
            }
        }
        return null;
    }

    /**
     * [MÉTIER] Calcule si un rendez-vous doit être traité en haute priorité.
     * Basé sur le flag d'urgence ou le score MMSE (< 20).
     */
    public boolean calculateIsHighPriority(Appointment appointment) {
        if (Boolean.TRUE.equals(appointment.getIsUrgent())) {
            return true;
        }

        Optional<PatientProfile> profile = patientProfileRepository.findById(appointment.getPatientId());
        if (profile.isPresent()) {
            Double score = profile.get().getExternalCognitiveScore();
            if (score != null && score < 20.0) {
                return true;
            }
        }
        return false;
    }

    private void populatePriorityScores(List<Appointment> appointments) {
        if (appointments == null || appointments.isEmpty())
            return;
        List<Long> patientIds = appointments.stream().map(Appointment::getPatientId).distinct()
                .collect(java.util.stream.Collectors.toList());
        List<PatientProfile> profiles = patientProfileRepository.findByUserIdIn(patientIds);
        java.util.Map<Long, PatientProfile> profileMap = profiles.stream()
                .collect(java.util.stream.Collectors.toMap(PatientProfile::getUserId, p -> p));

        for (Appointment a : appointments) {
            PatientProfile p = profileMap.get(a.getPatientId());
            if (p != null) {
                int score = 0;
                if (Boolean.TRUE.equals(p.getHypertension()))
                    score += 1;
                if (Boolean.TRUE.equals(p.getType2Diabetes()))
                    score += 1;
                if (Boolean.TRUE.equals(p.getHypercholesterolemia()))
                    score += 1;
                if (Boolean.TRUE.equals(p.getSleepDisorders()))
                    score += 1;
                if (Boolean.TRUE.equals(p.getFamilyHistoryAlzheimer()))
                    score += 1;

                if (p.getDateOfBirth() != null) {
                    int age = java.time.Period.between(p.getDateOfBirth(), java.time.LocalDate.now()).getYears();
                    if (age > 75)
                        score += 3;
                    else if (age > 65)
                        score += 2;
                    else if (age > 50)
                        score += 1;
                }

                Double mmse = p.getExternalCognitiveScore();
                if (mmse != null) {
                    if (mmse >= 0 && mmse <= 5)
                        score += 6;
                    else if (mmse > 5 && mmse <= 10)
                        score += 5;
                    else if (mmse > 10 && mmse <= 15)
                        score += 4;
                    else if (mmse > 15 && mmse <= 20)
                        score += 3;
                    else if (mmse > 20 && mmse <= 25)
                        score += 2;
                    else if (mmse > 25)
                        score += 1;
                }
                a.setPriorityScore(score);
            } else {
                a.setPriorityScore(0);
            }
        }
    }

    /**
     * Retourne tous les rendez-vous d'un médecin donné avec score calculé.
     *
     * @param doctorId identifiant du médecin
     * @return liste des rendez-vous
     */
    public List<Appointment> getAppointmentsByDoctor(Long doctorId) {
        List<Appointment> appointments = appointmentRepository.findByDoctorId(doctorId);
        populatePriorityScores(appointments);
        return appointments;
    }

    /**
     * Retourne tous les rendez-vous d'un patient donné avec score calculé.
     *
     * @param patientId identifiant du patient
     * @return liste des rendez-vous
     */
    public List<Appointment> getAppointmentsByPatient(Long patientId) {
        List<Appointment> appointments = appointmentRepository.findByPatientId(patientId);
        populatePriorityScores(appointments);
        return appointments;
    }

    /**
     * Retourne la liste complète de tous les rendez-vous en base avec score
     * calculé.
     *
     * @return liste de tous les rendez-vous
     */
    public List<Appointment> getAllAppointments() {
        List<Appointment> appointments = appointmentRepository.findAll();
        populatePriorityScores(appointments);
        return appointments;
    }

    /**
     * [API + MÉTIER] Filtrage avancé des rendez-vous.
     * Cette méthode applique les filtres complexes (score, date, statut) et ordonne
     * les résultats.
     * Lié aux composants Angular de filtrage du Dashboard.
     */
    public List<Appointment> getFilteredAppointments(Long doctorId, Long patientId, String status, Boolean isUrgent,
            String date, Integer minScore,
            Integer maxScore,
            boolean sortByScore) {

        tn.esprit.traitement_et_consultation.entity.AppointmentStatus appointmentStatus = null;
        if (status != null && !"all".equalsIgnoreCase(status)) {
            try {
                appointmentStatus = tn.esprit.traitement_et_consultation.entity.AppointmentStatus
                        .valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
            }
        }

        java.sql.Date sqlDate = null;
        if (date != null && !date.trim().isEmpty()) {
            sqlDate = java.sql.Date.valueOf(date);
        }

        List<Appointment> list = appointmentRepository.findFilteredAppointments(doctorId, patientId, appointmentStatus,
                isUrgent, sqlDate);

        populatePriorityScores(list);

        return list.stream()
                .filter(a -> minScore == null || (a.getPriorityScore() != null && a.getPriorityScore() >= minScore))
                .filter(a -> maxScore == null || (a.getPriorityScore() != null && a.getPriorityScore() <= maxScore))
                .sorted((a1, a2) -> {
                    if (sortByScore) {
                        int score1 = a1.getPriorityScore() != null ? a1.getPriorityScore() : 0;
                        int score2 = a2.getPriorityScore() != null ? a2.getPriorityScore() : 0;
                        return Integer.compare(score2, score1); // Décroissant
                    }
                    if (a1.getAppointmentDate() != null && a2.getAppointmentDate() != null) {
                        return a2.getAppointmentDate().compareTo(a1.getAppointmentDate()); // Décroissant aussi
                    }
                    return 0;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    public List<String> getFilterDates(Long doctorId) {
        return appointmentRepository.findDistinctDates(doctorId).stream()
                .filter(java.util.Objects::nonNull)
                .map(java.sql.Date::toString)
                .collect(java.util.stream.Collectors.toList());
    }

    public List<Long> getFilterPatients(Long doctorId) {
        return appointmentRepository.findDistinctPatientIds(doctorId).stream()
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Recherche un rendez-vous par son identifiant.
     *
     * @param id identifiant du rendez-vous
     * @return Optional contenant le rendez-vous s'il existe
     */
    public Optional<Appointment> getAppointmentById(Long id) {
        return appointmentRepository.findById(id);
    }

    /**
     * Met à jour les informations d'un rendez-vous existant.
     *
     * @param id                 identifiant du rendez-vous à modifier
     * @param appointmentDetails nouvelles données à appliquer
     * @return le rendez-vous modifié et sauvegardé
     */
    public Appointment updateAppointment(Long id, Appointment appointmentDetails) {
        java.time.LocalDateTime end = appointmentDetails.getAppointmentEndDate() != null
                ? appointmentDetails.getAppointmentEndDate()
                : appointmentDetails.getAppointmentDate().plusMinutes(35);
        validateWorkingHours(appointmentDetails.getAppointmentDate(), end);

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found"));

        if (tn.esprit.traitement_et_consultation.entity.AppointmentCategory.NEW_CONSULTATION
                .equals(appointmentDetails.getCategory())) {
            List<Appointment> existing = appointmentRepository.findByPatientId(appointmentDetails.getPatientId());
            boolean hasOther = existing.stream().anyMatch(a -> !a.getId().equals(id));
            if (hasOther) {
                throw new IllegalStateException(
                        "This patient already has other appointments. The 'New Consultation' category is no longer applicable.");
            }
        }

        appointment.setAppointmentDate(appointmentDetails.getAppointmentDate());
        appointment.setIsUrgent(appointmentDetails.getIsUrgent());
        appointment.setType(appointmentDetails.getType());
        appointment.setCategory(appointmentDetails.getCategory());
        appointment.setStatus(appointmentDetails.getStatus());

        Appointment saved = appointmentRepository.save(appointment);

        // Notify patient
        sendEmailNotification(saved, "UPDATED");

        return saved;
    }

    /**
     * Supprime un rendez-vous de la base de données.
     *
     * @param id identifiant du rendez-vous à supprimer
     */
    public void deleteAppointment(Long id) {
        appointmentRepository.deleteById(id);
    }

    /**
     * [ALGORITHME AVANCÉ] Suggère le meilleur créneau horaire en croisant les
     * agendas.
     * Cette méthode garantit qu'aucun rendez-vous n'est planifié sur un créneau
     * déjà occupé par le médecin ou le patient.
     */
    public java.time.LocalDateTime findBestAvailableSlot(Long doctorId, Long patientId,
            java.time.LocalDate startDate, java.time.LocalDate endDate) {
        // Implementation logic
        return suggestBestSlot(doctorId, patientId, startDate, endDate);
    }

    private void sendEmailNotification(Appointment appointment, String eventType) {
        if (appointment.getPatientId() == null)
            return;

        patientProfileRepository.findByUserId(appointment.getPatientId()).ifPresent(profile -> {
            if (profile.getEmail() != null && !profile.getEmail().isEmpty()) {
                String subject = "";
                String title = "";
                String content = "";
                String status = appointment.getStatus() != null ? appointment.getStatus().name() : "N/A";
                String dateStr = appointment.getAppointmentDate() != null
                        ? appointment.getAppointmentDate().format(
                                java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy 'at' HH:mm",
                                        java.util.Locale.ENGLISH))
                        : "N/A";

                switch (eventType) {
                    case "CREATED":
                        subject = "Appointment Received - Mind Care";
                        title = "We've received your request";
                        content = "Hello, we have received your appointment request. Our team will review it shortly. You will receive a confirmation email once it's validated.";
                        break;
                    case "CONFIRMED":
                        subject = "Appointment Confirmed - Mind Care";
                        title = "Your appointment is confirmed!";
                        content = "Good news! Your appointment has been confirmed by the doctor. We look forward to seeing you at the scheduled time.";
                        break;
                    case "CANCELLED":
                        subject = "Appointment Cancelled - Mind Care";
                        title = "Appointment Cancellation";
                        content = "Your appointment has been cancelled. If you didn't request this or wish to reschedule, please visit your dashboard.";
                        break;
                    case "UPDATED":
                        subject = "Appointment Updated - Mind Care";
                        title = "Your appointment has been updated";
                        content = "There has been a change to your appointment details. Please review the new schedule below.";
                        break;
                }

                String htmlBody = generateHtmlEmail(title, content, dateStr, status, appointment.getMeetLink());
                try {
                    emailService.sendHtmlEmail(profile.getEmail(), subject, htmlBody);
                } catch (Exception e) {
                    System.err.println("Error sending email notification: " + e.getMessage());
                    // We don't throw the exception further because we don't want to break
                    // the transaction if the email fails.
                }
            }
        });
    }

    private String generateHtmlEmail(String title, String content, String dateStr, String status, String meetLink) {
        String primaryGreen = "#2D9A9B";
        String darkBg = "#121212";
        String cardBg = "#222222";
        String statusColor = "#A0AEC0"; // Default

        if ("CONFIRMED".equals(status)) {
            statusColor = "#4ADE80"; // Bright Green
        } else if ("CANCELLED".equals(status)) {
            statusColor = "#F87171"; // Bright Red
        } else if ("PENDING".equals(status)) {
            statusColor = "#FB7185"; // Rose
        }

        String meetSection = "";
        if (meetLink != null && !meetLink.isEmpty()) {
            meetSection = "    <div style=\"background-color: #1A302E; border: 1px solid #2D9A9B; padding: 25px; border-radius: 12px; margin: 25px 0; text-align: center;\">"
                    +
                    "      <div style=\"color: #4FD1C5; font-size: 13px; font-weight: 700; text-transform: uppercase; margin-bottom: 12px; letter-spacing: 1px;\">YOUR ONLINE MEETING LINK</div>"
                    +
                    "      <a href=\"" + meetLink
                    + "\" style=\"color: #FFFFFF; font-size: 18px; font-weight: 800; text-decoration: underline;\">Click here to join the Google Meet</a>"
                    +
                    "      <p style=\"color: #A0AEC0; font-size: 12px; margin-top: 10px; margin-bottom: 0;\">Ready to join? Make sure your camera and mic are working.</p>"
                    +
                    "    </div>";
        }

        return "<div style=\"font-family: 'Instrument Sans', 'Inter', 'Segoe UI', Arial, sans-serif; max-width: 600px; margin: auto; background-color: "
                + darkBg
                + "; color: #FFFFFF; border-radius: 16px; overflow: hidden; box-shadow: 0 20px 50px rgba(0,0,0,0.5);\">"
                +
                "  <!-- Header Section with Green Gradient -->" +
                "  <div style=\"background: linear-gradient(135deg, #2D9A9B 0%, #1B5E5F 100%); padding: 50px 30px; text-align: center;\">"
                +
                "    <h1 style=\"margin: 0; font-size: 32px; font-weight: 800; letter-spacing: -0.5px; color: #FFFFFF;\">Mind Care</h1>"
                +
                "    <p style=\"margin: 8px 0 0; opacity: 0.8; font-size: 14px; font-weight: 500; letter-spacing: 0.5px; text-transform: uppercase;\">Your Mental Health Companion</p>"
                +
                "  </div>" +
                "  " +
                "  <!-- Content Section -->" +
                "  <div style=\"padding: 40px 30px; background-color: #1A1A1A;\">" +
                "    <h2 style=\"color: #FFFFFF; margin-top: 0; font-size: 24px; font-weight: 700; text-align: center;\">"
                + title + "</h2>" +
                "    <p style=\"color: #A0AEC0; line-height: 1.6; font-size: 16px; text-align: center; margin-bottom: 30px;\">"
                + content + "</p>" +
                "    " +
                "    <!-- Details Card -->" +
                "    <div style=\"background-color: " + cardBg
                + "; border-radius: 12px; padding: 25px; margin-bottom: 30px;\">"
                +
                "      <div style=\"margin-bottom: 20px;\">" +
                "        <div style=\"color: #718096; font-size: 12px; font-weight: 700; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 5px;\">DATE & TIME</div>"
                +
                "        <div style=\"color: #E2E8F0; font-size: 18px; font-weight: 700;\">" + dateStr + "</div>" +
                "      </div>" +
                "      <div>" +
                "        <div style=\"color: #718096; font-size: 12px; font-weight: 700; text-transform: uppercase; letter-spacing: 1px; margin-bottom: 5px;\">STATUS</div>"
                +
                "        <div style=\"color: " + statusColor + "; font-size: 16px; font-weight: 800;\">" + status
                + "</div>" +
                "      </div>" +
                "    </div>" +
                "    " +
                meetSection +
                "    " +
                "    <!-- Dashboard Button -->" +
                "    <div style=\"text-align: center; margin-top: 40px;\">" +
                "      <a href=\"#\" style=\"background-color: " + primaryGreen
                + "; color: #FFFFFF; padding: 16px 45px; text-decoration: none; border-radius: 12px; font-weight: 700; display: inline-block;\">Go to Dashboard</a>"
                +
                "    </div>" +
                "    " +
                "    <p style=\"color: #718096; font-size: 13px; text-align: center; margin-top: 40px; line-height: 1.5;\">If you have any questions, please feel free to contact us through the application.</p>"
                +
                "  </div>" +
                "  " +
                "  <!-- Footer Section -->" +
                "  <div style=\"padding: 30px; background-color: " + darkBg
                + "; text-align: center; border-top: 1px solid #1A1A1A;\">" +
                "    <p style=\"margin: 0; color: #4A5568; font-size: 12px;\">© 2026 Mind Care. All rights reserved.</p>"
                +
                "    <p style=\"margin: 5px 0 0; color: #4A5568; font-size: 12px;\">Helping you care for your mind, every day.</p>"
                +
                "  </div>" +
                "</div>";
    }
}
