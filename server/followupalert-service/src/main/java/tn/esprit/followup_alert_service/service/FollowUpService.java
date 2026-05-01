package tn.esprit.followup_alert_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.followup_alert_service.entity.*;
import tn.esprit.followup_alert_service.exception.ResourceNotFoundException;
import tn.esprit.followup_alert_service.repository.AlertRepository;
import tn.esprit.followup_alert_service.repository.FollowUpRepository;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FollowUpService {

    private static final String TOTAL_FOLLOWUPS = "totalFollowUps";
    private final FollowUpRepository followUpRepository;
    private final AlertRepository alertRepository;

    //  CRUD

    @Transactional
    public FollowUp createFollowUp(FollowUp followUp) {
        followUpRepository.findByPatientIdAndFollowUpDate(
                followUp.getPatientId(), followUp.getFollowUpDate()
        ).ifPresent(existing -> {
            throw new ResourceNotFoundException("A follow-up already exists for this patient on this date.");
        });

        FollowUp saved = followUpRepository.save(followUp);

        // ADVANCED: Auto-generate alerts from follow-up data
        autoGenerateAlerts(saved);

        // ADVANCED: Check cognitive decline trend
        detectCognitiveDecline(saved.getPatientId());

        return saved;
    }

    public List<FollowUp> getAllFollowUps() {
        return followUpRepository.findAll();
    }

    public FollowUp getFollowUpById(Long id) {
        return followUpRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Follow-up not found with id: " + id));
    }

    public List<FollowUp> getFollowUpsByPatientId(Long patientId) {
        return followUpRepository.findByPatientId(patientId);
    }

    public List<FollowUp> getFollowUpsByCaregiverId(Long caregiverId) {
        return followUpRepository.findByCaregiverId(caregiverId);
    }

    @Transactional
    public FollowUp updateFollowUp(Long id, FollowUp updatedFollowUp) {
        FollowUp existing = getFollowUpById(id);

        existing.setPatientId(updatedFollowUp.getPatientId());
        existing.setCaregiverId(updatedFollowUp.getCaregiverId());
        existing.setFollowUpDate(updatedFollowUp.getFollowUpDate());
        existing.setCognitiveScore(updatedFollowUp.getCognitiveScore());
        existing.setMood(updatedFollowUp.getMood());
        existing.setAgitationObserved(updatedFollowUp.getAgitationObserved());
        existing.setConfusionObserved(updatedFollowUp.getConfusionObserved());
        existing.setEating(updatedFollowUp.getEating());
        existing.setDressing(updatedFollowUp.getDressing());
        existing.setMobility(updatedFollowUp.getMobility());
        existing.setHoursSlept(updatedFollowUp.getHoursSlept());
        existing.setSleepQuality(updatedFollowUp.getSleepQuality());
        existing.setNotes(updatedFollowUp.getNotes());
        existing.setVitalSigns(updatedFollowUp.getVitalSigns());

        FollowUp saved = followUpRepository.save(existing);

        // ADVANCED: Re-evaluate alerts after update
        autoGenerateAlerts(saved);

        return saved;
    }

    public void deleteFollowUp(Long id) {
        if (!followUpRepository.existsById(id)) {
            throw new ResourceNotFoundException("Follow-up not found with id: " + id);
        }
        followUpRepository.deleteById(id);
    }

    // ==================== FONCTIONNALITE AVANCEE 1: Auto-generate alerts ====================

    private void autoGenerateAlerts(FollowUp followUp) {
        Long patientId = followUp.getPatientId();

        // Rule 1: Low cognitive score -> HIGH or CRITICAL alert
        if (followUp.getCognitiveScore() != null && followUp.getCognitiveScore() < 18) {
            createAlertIfNotExists(patientId,
                    "Low Cognitive Score Detected",
                    "Patient cognitive score is " + followUp.getCognitiveScore()
                            + "/30 on " + followUp.getFollowUpDate() + ". Immediate evaluation recommended.",
                    followUp.getCognitiveScore() < 10 ? AlertLevel.CRITICAL : AlertLevel.HIGH);
        }

        // Rule 2: Agitation + Confusion together -> CRITICAL
        if (Boolean.TRUE.equals(followUp.getAgitationObserved())
                && Boolean.TRUE.equals(followUp.getConfusionObserved())) {
            createAlertIfNotExists(patientId,
                    "Agitation & Confusion Combined",
                    "Both agitation and confusion observed on " + followUp.getFollowUpDate()
                            + ". Risk of delirium or acute cognitive decline.",
                    AlertLevel.CRITICAL);
        } else if (Boolean.TRUE.equals(followUp.getAgitationObserved())) {
            createAlertIfNotExists(patientId,
                    "Agitation Observed",
                    "Agitation noted during follow-up on " + followUp.getFollowUpDate() + ".",
                    AlertLevel.MEDIUM);
        }

        // Rule 3: Poor sleep -> MEDIUM
        if (followUp.getSleepQuality() == SleepQuality.POOR) {
            createAlertIfNotExists(patientId,
                    "Poor Sleep Quality",
                    "Patient reported poor sleep (" + followUp.getHoursSlept()
                            + "h) on " + followUp.getFollowUpDate() + ".",
                    AlertLevel.MEDIUM);
        }

        // Rule 4: Full dependency in any ADL -> HIGH
        if (followUp.getEating() == IndependenceLevel.DEPENDENT
                || followUp.getDressing() == IndependenceLevel.DEPENDENT
                || followUp.getMobility() == IndependenceLevel.DEPENDENT) {
            List<String> deps = new ArrayList<>();
            if (followUp.getEating() == IndependenceLevel.DEPENDENT) deps.add("eating");
            if (followUp.getDressing() == IndependenceLevel.DEPENDENT) deps.add("dressing");
            if (followUp.getMobility() == IndependenceLevel.DEPENDENT) deps.add("mobility");

            createAlertIfNotExists(patientId,
                    "Full Dependency Detected",
                    "Patient is fully dependent in: " + String.join(", ", deps)
                            + " as of " + followUp.getFollowUpDate() + ".",
                    AlertLevel.HIGH);
        }

        // Rule 5: Depressed mood -> MEDIUM
        if (followUp.getMood() == MoodState.DEPRESSED) {
            createAlertIfNotExists(patientId,
                    "Depressed Mood Detected",
                    "Patient showing depressed mood on " + followUp.getFollowUpDate()
                            + ". Consider psychological support.",
                    AlertLevel.MEDIUM);
        }

        log.info("Auto-alert evaluation completed for patient {} on {}", patientId, followUp.getFollowUpDate());
    }

    private void createAlertIfNotExists(Long patientId, String title, String description, AlertLevel level) {
        List<Alert> existing = alertRepository.findByPatientId(patientId);
        boolean alreadyExists = existing.stream()
                .anyMatch(a -> a.getTitle().equals(title) && a.getStatus() != AlertStatus.RESOLVED);

        if (!alreadyExists) {
            Alert alert = new Alert();
            alert.setPatientId(patientId);
            alert.setTitle(title);
            alert.setDescription(description);
            alert.setLevel(level);
            alertRepository.save(alert);
            log.info("Auto-generated alert: [{}] {} for patient {}", level, title, patientId);
        }
    }

    // ==================== FONCTIONNALITE AVANCEE  Cognitive Decline Detection ====================

    public boolean detectCognitiveDecline(Long patientId) {
        List<FollowUp> followUps = followUpRepository.findByPatientId(patientId);

        List<FollowUp> sorted = followUps.stream()
                .filter(f -> f.getCognitiveScore() != null && f.getFollowUpDate() != null)
                .sorted(Comparator.comparing(FollowUp::getFollowUpDate))
                .collect(Collectors.toList());

        if (sorted.size() < 2) return false;

        int checkCount = Math.min(3, sorted.size());
        List<FollowUp> recent = sorted.subList(sorted.size() - checkCount, sorted.size());

        boolean declining = true;
        for (int i = 1; i < recent.size(); i++) {
            if (recent.get(i).getCognitiveScore() >= recent.get(i - 1).getCognitiveScore()) {
                declining = false;
                break;
            }
        }

        if (declining) {
            int drop = recent.get(0).getCognitiveScore() - recent.get(recent.size() - 1).getCognitiveScore();
            createAlertIfNotExists(patientId,
                    "Cognitive Decline Trend",
                    "Cognitive score dropped by " + drop + " points over last "
                            + checkCount + " follow-ups. Trend: "
                            + recent.stream().map(f -> String.valueOf(f.getCognitiveScore()))
                            .collect(Collectors.joining(" -> ")),
                    drop >= 10 ? AlertLevel.CRITICAL : AlertLevel.HIGH);
        }

        return declining;
    }

    // ==================== FONCTIONNALITE AVANCEE 3: Patient Risk Scoring ====================

    public Map<String, Object> calculatePatientRisk(Long patientId) {
        List<FollowUp> followUps = followUpRepository.findByPatientId(patientId);
        List<Alert> alerts = alertRepository.findByPatientId(patientId);

        int riskScore = 0;
        List<String> riskFactors = new ArrayList<>();

        FollowUp latest = followUps.stream()
                .filter(f -> f.getFollowUpDate() != null)
                .max(Comparator.comparing(FollowUp::getFollowUpDate))
                .orElse(null);

        if (latest != null) {
            // Cognitive
            if (latest.getCognitiveScore() != null) {
                if (latest.getCognitiveScore() < 10) { riskScore += 30; riskFactors.add("Severe cognitive impairment (score: " + latest.getCognitiveScore() + ")"); }
                else if (latest.getCognitiveScore() < 18) { riskScore += 20; riskFactors.add("Moderate cognitive impairment (score: " + latest.getCognitiveScore() + ")"); }
                else if (latest.getCognitiveScore() < 24) { riskScore += 10; riskFactors.add("Mild cognitive impairment (score: " + latest.getCognitiveScore() + ")"); }
            }

            // Mood
            if (latest.getMood() == MoodState.AGITATED || latest.getMood() == MoodState.CONFUSED) {
                riskScore += 15; riskFactors.add("Current mood: " + latest.getMood());
            } else if (latest.getMood() == MoodState.DEPRESSED || latest.getMood() == MoodState.ANXIOUS) {
                riskScore += 10; riskFactors.add("Current mood: " + latest.getMood());
            }

            // Flags
            if (Boolean.TRUE.equals(latest.getAgitationObserved())) { riskScore += 10; riskFactors.add("Agitation observed"); }
            if (Boolean.TRUE.equals(latest.getConfusionObserved())) { riskScore += 10; riskFactors.add("Confusion observed"); }

            // Sleep
            if (latest.getSleepQuality() == SleepQuality.POOR) { riskScore += 10; riskFactors.add("Poor sleep quality"); }

            // ADL
            int depCount = 0;
            if (latest.getEating() == IndependenceLevel.DEPENDENT) depCount++;
            if (latest.getDressing() == IndependenceLevel.DEPENDENT) depCount++;
            if (latest.getMobility() == IndependenceLevel.DEPENDENT) depCount++;
            if (depCount > 0) { riskScore += depCount * 8; riskFactors.add("Dependent in " + depCount + " ADL activities"); }
        }

        // Unresolved alerts
        long unresolvedAlerts = alerts.stream().filter(a -> a.getStatus() != AlertStatus.RESOLVED).count();
        long criticalAlerts = alerts.stream().filter(a -> a.getLevel() == AlertLevel.CRITICAL && a.getStatus() != AlertStatus.RESOLVED).count();
        riskScore += (int) (criticalAlerts * 5 + unresolvedAlerts * 2);

        riskScore = Math.min(100, riskScore);

        String riskLevel;
        if (riskScore >= 70) riskLevel = "CRITICAL";
        else if (riskScore >= 45) riskLevel = "HIGH";
        else if (riskScore >= 20) riskLevel = "MODERATE";
        else riskLevel = "LOW";

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("patientId", patientId);
        result.put("riskScore", riskScore);
        result.put("riskLevel", riskLevel);
        result.put("riskFactors", riskFactors);
        result.put(TOTAL_FOLLOWUPS, followUps.size());
        result.put("unresolvedAlerts", unresolvedAlerts);
        if (latest != null) {
            result.put("latestCognitiveScore", latest.getCognitiveScore());
            result.put("latestMood", latest.getMood());
            result.put("latestSleepQuality", latest.getSleepQuality());
        }
        return result;
    }

    // ==================== FONCTIONNALITE AVANCEE 4: Statistics ====================

    public Map<String, Object> getStatistics() {
        List<FollowUp> all = followUpRepository.findAll();

        double avgCog = all.stream().filter(f -> f.getCognitiveScore() != null)
                .mapToInt(FollowUp::getCognitiveScore).average().orElse(0);

        double avgSleep = all.stream().filter(f -> f.getHoursSlept() != null)
                .mapToInt(FollowUp::getHoursSlept).average().orElse(0);

        Map<String, Long> moodDist = all.stream().filter(f -> f.getMood() != null)
                .collect(Collectors.groupingBy(f -> f.getMood().name(), Collectors.counting()));

        Map<String, Long> sleepDist = all.stream().filter(f -> f.getSleepQuality() != null)
                .collect(Collectors.groupingBy(f -> f.getSleepQuality().name(), Collectors.counting()));

        long agitationCount = all.stream().filter(f -> Boolean.TRUE.equals(f.getAgitationObserved())).count();
        long confusionCount = all.stream().filter(f -> Boolean.TRUE.equals(f.getConfusionObserved())).count();
        long lowCogCount = all.stream().filter(f -> f.getCognitiveScore() != null && f.getCognitiveScore() < 18).count();
        long poorSleepCount = all.stream().filter(f -> f.getSleepQuality() == SleepQuality.POOR || f.getSleepQuality() == SleepQuality.FAIR).count();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put(TOTAL_FOLLOWUPS, all.size());
        stats.put("averageCognitiveScore", Math.round(avgCog * 10.0) / 10.0);
        stats.put("averageHoursSlept", Math.round(avgSleep * 10.0) / 10.0);
        stats.put("moodDistribution", moodDist);
        stats.put("sleepQualityDistribution", sleepDist);
        stats.put("agitationCount", agitationCount);
        stats.put("confusionCount", confusionCount);
        stats.put("lowCognitiveCount", lowCogCount);
        stats.put("poorSleepCount", poorSleepCount);
        return stats;
    }

    public Map<String, Object> getStatisticsByPatient(Long patientId) {
        List<FollowUp> all = followUpRepository.findByPatientId(patientId);

        double avgCog = all.stream().filter(f -> f.getCognitiveScore() != null)
                .mapToInt(FollowUp::getCognitiveScore).average().orElse(0);

        Map<String, Long> moodDist = all.stream().filter(f -> f.getMood() != null)
                .collect(Collectors.groupingBy(f -> f.getMood().name(), Collectors.counting()));

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("patientId", patientId);
        stats.put(TOTAL_FOLLOWUPS, all.size());
        stats.put("averageCognitiveScore", Math.round(avgCog * 10.0) / 10.0);
        stats.put("moodDistribution", moodDist);
        return stats;
    }
}