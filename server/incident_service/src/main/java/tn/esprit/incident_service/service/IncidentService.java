package tn.esprit.incident_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import tn.esprit.incident_service.dto.IncidentStatsDTO;
import tn.esprit.incident_service.dto.PatientStatsDTO;
import tn.esprit.incident_service.entity.Incident;
import tn.esprit.incident_service.entity.IncidentComment;
import tn.esprit.incident_service.entity.IncidentType;
import tn.esprit.incident_service.enums.IncidentStatus;
import tn.esprit.incident_service.enums.SeverityLevel;
import tn.esprit.incident_service.repository.IncidentCommentRepository;
import tn.esprit.incident_service.repository.IncidentRepository;
import tn.esprit.incident_service.repository.IncidentTypeRepository;

import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final IncidentTypeRepository incidentTypeRepository;
    private final IncidentCommentRepository incidentCommentRepository;
    private final EmailService emailService;
    private final RestTemplate restTemplate;

    // --- INCIDENTS ---

    // Get Active Incidents (Default List) - Soft Deleted are hidden
    public List<Incident> getAllActiveIncidents() {
        return incidentRepository.findAllActive();
    }

    // Get Incidents for specific Patient (Active Only) - Front Office
    public List<Incident> getActiveIncidentsByPatient(Long patientId) {
        return incidentRepository.findByPatientIdActive(patientId);
    }

    // Get Incident History for specific Patient (Front Office History)
    public List<Incident> getPatientIncidentsHistory(Long patientId) {
        return incidentRepository.findByPatientIdAll(patientId);
    }

    // Get Incident History (Include Deleted) - Admin History
    public List<Incident> getAllHistory() {
        return incidentRepository.findAllIncludingHistory();
    }

    // Get Incidents for specific Caregiver - Back Office
    public List<Incident> getActiveIncidentsByCaregiver(Long caregiverId) {
        return incidentRepository.findByCaregiverIdActive(caregiverId);
    }

    // Get Incidents reported by Caregivers - Admin Back Office
    public List<Incident> getCaregiverReportedIncidents() {
        return incidentRepository.findBySourceActive("CAREGIVER");
    }

    // Create Incident with auto-scoring
    public Incident createIncident(Incident incident) {
        if (incident.getType() != null && incident.getType().getId() != null) {
            incidentTypeRepository.findById(incident.getType().getId()).ifPresent(type -> {
                int score = type.getPoints() != null ? type.getPoints() : 10;
                // Recurrence bonus: +5 per incident in last 30 days for this patient
                if (incident.getPatientId() != null) {
                    long recentCount = incidentRepository.countRecentByPatient(
                            incident.getPatientId(), LocalDateTime.now().minusDays(30));
                    score += (int) recentCount * 5;
                }
                incident.setComputedScore(score);
                incident.setSeverityLevel(scoreToSeverity(score));
                log.info("Auto-scored incident: typePoints={}, score={}, severity={}",
                        type.getPoints(), score, incident.getSeverityLevel());
            });
        }
        log.info("Creating new incident for patient {}", incident.getPatientId());
        Incident saved = incidentRepository.save(incident);

        // --- Email notification: send to caregiver when a doctor reports an incident ---
        if ("DOCTOR".equalsIgnoreCase(incident.getSource()) && incident.getPatientId() != null) {
            sendCaregiverNotification(saved);
        }

        return saved;
    }

    /**
     * Appelle le users-service via Eureka pour récupérer le patient,
     * puis son caregiver, et envoie un e-mail de notification.
     */
    @SuppressWarnings("unchecked")
    private void sendCaregiverNotification(Incident incident) {
        try {
            // 1. Récupérer les infos du patient via users-service
            String patientUrl = "http://users-service/api/users/" + incident.getPatientId();
            Map<String, Object> patient = restTemplate.getForObject(patientUrl, Map.class);
            if (patient == null) {
                log.warn("Patient {} introuvable pour la notification e-mail", incident.getPatientId());
                return;
            }

            String patientName = patient.get("firstName") + " " + patient.get("lastName");
            Object cgIdObj = patient.get("caregiverId");

            if (cgIdObj == null) {
                log.info("Patient {} n'a pas de caregiver assigné — pas d'e-mail envoyé", incident.getPatientId());
                return;
            }

            Long caregiverId = Long.valueOf(cgIdObj.toString());

            // 2. Récupérer les infos du caregiver
            String caregiverUrl = "http://users-service/api/users/" + caregiverId;
            Map<String, Object> caregiver = restTemplate.getForObject(caregiverUrl, Map.class);
            if (caregiver == null || caregiver.get("email") == null) {
                log.warn("Caregiver {} introuvable ou sans e-mail", caregiverId);
                return;
            }

            String caregiverEmail = caregiver.get("email").toString();
            String caregiverName = caregiver.get("firstName") + " " + caregiver.get("lastName");

            // 3. Envoyer l'e-mail
            emailService.sendIncidentNotification(caregiverEmail, caregiverName, patientName, incident);

        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de la notification par e-mail : {}", e.getMessage());
        }
    }

    private SeverityLevel scoreToSeverity(int score) {
        if (score >= 30) return SeverityLevel.CRITICAL;
        if (score >= 20) return SeverityLevel.HIGH;
        if (score >= 10) return SeverityLevel.MEDIUM;
        return SeverityLevel.LOW;
    }

    // Update Incident
    public Incident updateIncident(Long id, Incident updatedIncident) {
        return incidentRepository.findById(id).map(existing -> {
            existing.setType(updatedIncident.getType());
            existing.setDescription(updatedIncident.getDescription());
            existing.setSeverityLevel(updatedIncident.getSeverityLevel());
            existing.setStatus(updatedIncident.getStatus());
            existing.setIncidentDate(updatedIncident.getIncidentDate());
            // On ne change pas le patientId/caregiverId sauf si explicite
            if (updatedIncident.getPatientId() != null) existing.setPatientId(updatedIncident.getPatientId());
            if (updatedIncident.getCaregiverId() != null) existing.setCaregiverId(updatedIncident.getCaregiverId());
            return incidentRepository.save(existing);
        }).orElseThrow(() -> new RuntimeException("Incident not found with id " + id));
    }

    // Update Incident Status Only
    public Incident updateIncidentStatus(Long id, String status) {
        return incidentRepository.findById(id).map(existing -> {
           // existing.setStatus(status);
            existing.setStatus(IncidentStatus.valueOf(status.toUpperCase()));
            log.info("Updated incident {} status to {}", id, status);
            return incidentRepository.save(existing);
        }).orElseThrow(() -> new RuntimeException("Incident not found with id " + id));
    }

    // Soft Delete Incident
    // La suppression physique est gérée par @SQLDelete dans l'entité
    public void deleteIncident(Long id) {
        log.info("Soft deleting incident {}", id);
        incidentRepository.deleteById(id);
    }

    // --- INCIDENT TYPES ---

    public List<IncidentType> getAllIncidentTypes() {
        return incidentTypeRepository.findAll();
    }

    public IncidentType createIncidentType(IncidentType type) {
        return incidentTypeRepository.save(type);
    }

    public IncidentType updateIncidentType(Long id, IncidentType typeDetails) {
        return incidentTypeRepository.findById(id).map(existing -> {
            existing.setName(typeDetails.getName());
            existing.setDescription(typeDetails.getDescription());
            if (typeDetails.getDefaultSeverity() != null) existing.setDefaultSeverity(typeDetails.getDefaultSeverity());
            if (typeDetails.getPoints() != null && typeDetails.getPoints() > 0) existing.setPoints(typeDetails.getPoints());
            return incidentTypeRepository.save(existing);
        }).orElseThrow(() -> new RuntimeException("Incident Type not found with id " + id));
    }

    public void deleteIncidentType(Long id) {
        incidentTypeRepository.deleteById(id);
    }

    // --- STATS ---
    public IncidentStatsDTO getStats() {
        Map<String, Long> bySeverity = new LinkedHashMap<>();
        for (SeverityLevel level : SeverityLevel.values()) {
            bySeverity.put(level.name(), incidentRepository.countBySeverityActive(level));
        }

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (IncidentStatus status : IncidentStatus.values()) {
            byStatus.put(status.name(), incidentRepository.countByStatusActive(status));
        }

        // Last 6 months
        Map<String, Long> byMonth = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 5; i >= 0; i--) {
            LocalDateTime start = now.minusMonths(i).withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime end = start.plusMonths(1);
            String label = start.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            byMonth.put(label, incidentRepository.countByMonthRange(start, end));
        }

        return IncidentStatsDTO.builder()
                .totalActive(incidentRepository.countAllActive())
                .totalHistory(incidentRepository.countAllTotal())
                .bySeverity(bySeverity)
                .byStatus(byStatus)
                .byMonth(byMonth)
                .build();
    }

    // --- PATIENT STATS (severity score + frequency) ---

    /**
     * Retourne les statistiques de tous les patients ayant au moins un incident.
     */
    @SuppressWarnings("unchecked")
    public List<PatientStatsDTO> getPatientStats() {
        List<Incident> all = incidentRepository.findAllIncludingHistory();

        // Group by patientId
        Map<Long, List<Incident>> byPatient = new LinkedHashMap<>();
        for (Incident i : all) {
            if (i.getPatientId() != null) {
                byPatient.computeIfAbsent(i.getPatientId(), k -> new ArrayList<>()).add(i);
            }
        }

        List<PatientStatsDTO> result = new ArrayList<>();
        for (Map.Entry<Long, List<Incident>> entry : byPatient.entrySet()) {
            Long patientId = entry.getKey();
            List<Incident> incidents = entry.getValue();

            // Fetch patient name from users-service
            String patientName = "Patient #" + patientId;
            try {
                Map<String, Object> user = restTemplate.getForObject(
                        "http://users-service/api/users/" + patientId, Map.class);
                if (user != null) {
                    patientName = user.get("firstName") + " " + user.get("lastName");
                }
            } catch (Exception e) {
                log.warn("Unable to fetch patient name for ID {}", patientId);
            }

            long total = incidents.size();
            long active = incidents.stream().filter(i -> !i.isDeleted() && i.getStatus() != IncidentStatus.RESOLVED).count();
            long resolved = incidents.stream().filter(i -> i.getStatus() == IncidentStatus.RESOLVED).count();

            // Severity breakdown
            Map<String, Long> sev = new LinkedHashMap<>();
            for (SeverityLevel level : SeverityLevel.values()) {
                sev.put(level.name(), incidents.stream()
                        .filter(i -> i.getSeverityLevel() == level).count());
            }

            // Compute average days between incidents
            double avgDays = computeAvgDaysBetween(incidents);

            // Compute severity score (0-100)
            int severityScore = computeSeverityScore(incidents, avgDays);
            String riskLevel = severityScoreToRisk(severityScore);

            result.add(PatientStatsDTO.builder()
                    .patientId(patientId)
                    .patientName(patientName)
                    .totalIncidents(total)
                    .activeIncidents(active)
                    .resolvedIncidents(resolved)
                    .severityScore(severityScore)
                    .riskLevel(riskLevel)
                    .avgDaysBetween(Math.round(avgDays * 10.0) / 10.0)
                    .bySeverity(sev)
                    .build());
        }

        // Sort by severity score descending (highest risk first)
        result.sort((a, b) -> Integer.compare(b.getSeverityScore(), a.getSeverityScore()));
        return result;
    }

    /**
     * Statistiques pour un seul patient.
     */
    @SuppressWarnings("unchecked")
    public PatientStatsDTO getPatientStatsById(Long patientId) {
        List<Incident> incidents = incidentRepository.findByPatientIdAll(patientId);

        String patientName = "Patient #" + patientId;
        try {
            Map<String, Object> user = restTemplate.getForObject(
                    "http://users-service/api/users/" + patientId, Map.class);
            if (user != null) {
                patientName = user.get("firstName") + " " + user.get("lastName");
            }
        } catch (Exception e) {
            log.warn("Unable to fetch patient name for ID {}", patientId);
        }

        long total = incidents.size();
        long active = incidents.stream().filter(i -> !i.isDeleted() && i.getStatus() != IncidentStatus.RESOLVED).count();
        long resolved = incidents.stream().filter(i -> i.getStatus() == IncidentStatus.RESOLVED).count();

        Map<String, Long> sev = new LinkedHashMap<>();
        for (SeverityLevel level : SeverityLevel.values()) {
            sev.put(level.name(), incidents.stream().filter(i -> i.getSeverityLevel() == level).count());
        }

        double avgDays = computeAvgDaysBetween(incidents);
        int severityScore = computeSeverityScore(incidents, avgDays);
        String riskLevel = severityScoreToRisk(severityScore);

        return PatientStatsDTO.builder()
                .patientId(patientId)
                .patientName(patientName)
                .totalIncidents(total)
                .activeIncidents(active)
                .resolvedIncidents(resolved)
                .severityScore(severityScore)
                .riskLevel(riskLevel)
                .avgDaysBetween(Math.round(avgDays * 10.0) / 10.0)
                .bySeverity(sev)
                .build();
    }

    private double computeAvgDaysBetween(List<Incident> incidents) {
        List<LocalDateTime> dates = incidents.stream()
                .filter(i -> i.getIncidentDate() != null)
                .map(Incident::getIncidentDate)
                .sorted()
                .toList();
        if (dates.size() < 2) return 0.0;

        double totalDays = 0;
        for (int i = 1; i < dates.size(); i++) {
            totalDays += java.time.Duration.between(dates.get(i - 1), dates.get(i)).toDays();
        }
        return totalDays / (dates.size() - 1);
    }

    /**
     * Score de gravité (0-100) basé sur :
     * - Nombre total d'incidents (max 30 pts)
     * - Sévérité des incidents (max 40 pts)
     * - Fréquence : durée moyenne entre incidents (max 30 pts) — plus court = plus grave
     */
    private int computeSeverityScore(List<Incident> incidents, double avgDaysBetween) {
        if (incidents.isEmpty()) return 0;

        // 1. Volume score (0-30)
        int volumeScore = Math.min(30, incidents.size() * 5);

        // 2. Severity-weighted score (0-40)
        int severityPoints = 0;
        for (Incident i : incidents) {
            if (i.getSeverityLevel() == null) continue;
            switch (i.getSeverityLevel()) {
                case CRITICAL -> severityPoints += 4;
                case HIGH     -> severityPoints += 3;
                case MEDIUM   -> severityPoints += 2;
                case LOW      -> severityPoints += 1;
            }
        }
        int maxSevPts = incidents.size() * 4; // max if all were CRITICAL
        int severityScore = maxSevPts > 0 ? (int) ((severityPoints / (double) maxSevPts) * 40) : 0;

        // 3. Frequency score (0-30) — shorter interval = higher score
        int frequencyScore = 0;
        if (incidents.size() >= 2 && avgDaysBetween > 0) {
            if (avgDaysBetween <= 1)       frequencyScore = 30;
            else if (avgDaysBetween <= 3)  frequencyScore = 25;
            else if (avgDaysBetween <= 7)  frequencyScore = 20;
            else if (avgDaysBetween <= 14) frequencyScore = 15;
            else if (avgDaysBetween <= 30) frequencyScore = 10;
            else                           frequencyScore = 5;
        }

        return Math.min(100, volumeScore + severityScore + frequencyScore);
    }

    private String severityScoreToRisk(int score) {
        if (score >= 75) return "CRITICAL";
        if (score >= 50) return "HIGH";
        if (score >= 25) return "MODERATE";
        return "LOW";
    }

    // --- COMMENTS ---

    public List<IncidentComment> getCommentsByIncident(Long incidentId) {
        return incidentCommentRepository.findByIncidentIdOrderByCreatedAtAsc(incidentId);
    }

    public IncidentComment addComment(Long incidentId, IncidentComment comment) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new RuntimeException("Incident not found: " + incidentId));
        comment.setIncident(incident);
        log.info("Adding comment to incident {} by {}", incidentId, comment.getAuthorName());
        return incidentCommentRepository.save(comment);
    }

    public void deleteComment(Long commentId) {
        log.info("Deleting comment {}", commentId);
        incidentCommentRepository.deleteById(commentId);
    }

    // --- PDF REPORT ---
    public List<Incident> getIncidentsForReport(Long patientId) {
        return patientId != null
                ? incidentRepository.findByPatientIdAll(patientId)
                : incidentRepository.findAllIncludingHistory();
    }
}
