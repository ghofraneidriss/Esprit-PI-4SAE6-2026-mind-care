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
        checkCognitiveScoreAlert(followUp, patientId);
        checkAgitationConfusionAlert(followUp, patientId);
        checkSleepQualityAlert(followUp, patientId);
        checkADLDependencyAlert(followUp, patientId);
        checkMoodAlert(followUp, patientId);
        log.info("Auto-alert evaluation completed for patient {} on {}", patientId, followUp.getFollowUpDate());
    }

    private void checkCognitiveScoreAlert(FollowUp followUp, Long patientId) {
        if (followUp.getCognitiveScore() != null && followUp.getCognitiveScore() < 18) {
            AlertLevel level = followUp.getCognitiveScore() < 10 ? AlertLevel.CRITICAL : AlertLevel.HIGH;
            createAlertIfNotExists(patientId, "Low Cognitive Score Detected",
                    "Patient cognitive score is " + followUp.getCognitiveScore()
                            + "/30 on " + followUp.getFollowUpDate() + ". Immediate evaluation recommended.", level);
        }
    }

    private void checkAgitationConfusionAlert(FollowUp followUp, Long patientId) {
        if (Boolean.TRUE.equals(followUp.getAgitationObserved()) && Boolean.TRUE.equals(followUp.getConfusionObserved())) {
            createAlertIfNotExists(patientId, "Agitation & Confusion Combined",
                    "Both agitation and confusion observed on " + followUp.getFollowUpDate()
                            + ". Risk of delirium or acute cognitive decline.", AlertLevel.CRITICAL);
        } else if (Boolean.TRUE.equals(followUp.getAgitationObserved())) {
            createAlertIfNotExists(patientId, "Agitation Observed",
                    "Agitation noted during follow-up on " + followUp.getFollowUpDate() + ".", AlertLevel.MEDIUM);
        }
    }

    private void checkSleepQualityAlert(FollowUp followUp, Long patientId) {
        if (followUp.getSleepQuality() == SleepQuality.POOR) {
            createAlertIfNotExists(patientId, "Poor Sleep Quality",
                    "Patient reported poor sleep (" + followUp.getHoursSlept()
                            + "h) on " + followUp.getFollowUpDate() + ".", AlertLevel.MEDIUM);
        }
    }

    private void checkADLDependencyAlert(FollowUp followUp, Long patientId) {
        if (followUp.getEating() == IndependenceLevel.DEPENDENT
                || followUp.getDressing() == IndependenceLevel.DEPENDENT
                || followUp.getMobility() == IndependenceLevel.DEPENDENT) {
            List<String> deps = new ArrayList<>();
            if (followUp.getEating() == IndependenceLevel.DEPENDENT) deps.add("eating");
            if (followUp.getDressing() == IndependenceLevel.DEPENDENT) deps.add("dressing");
            if (followUp.getMobility() == IndependenceLevel.DEPENDENT) deps.add("mobility");
            createAlertIfNotExists(patientId, "Full Dependency Detected",
                    "Patient is fully dependent in: " + String.join(", ", deps)
                            + " as of " + followUp.getFollowUpDate() + ".", AlertLevel.HIGH);
        }
    }

    private void checkMoodAlert(FollowUp followUp, Long patientId) {
        if (followUp.getMood() == MoodState.DEPRESSED) {
            createAlertIfNotExists(patientId, "Depressed Mood Detected",
                    "Patient showing depressed mood on " + followUp.getFollowUpDate()
                            + ". Consider psychological support.", AlertLevel.MEDIUM);
        }
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
        FollowUp latest = followUps.stream().filter(f -> f.getFollowUpDate() != null)
                .max(Comparator.comparing(FollowUp::getFollowUpDate)).orElse(null);

        if (latest != null) {
            riskScore += calculateCognitiveRisk(latest, riskFactors);
            riskScore += calculateMoodRisk(latest, riskFactors);
            riskScore += calculateFlagsRisk(latest, riskFactors);
            riskScore += calculateSleepRisk(latest, riskFactors);
            riskScore += calculateADLRisk(latest, riskFactors);
        }

        riskScore += calculateAlertRisk(alerts);
        riskScore = Math.min(100, riskScore);
        String riskLevel = determineRiskLevel(riskScore);
        long unresolvedAlerts = alerts.stream().filter(a -> a.getStatus() != AlertStatus.RESOLVED).count();

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

    private int calculateCognitiveRisk(FollowUp latest, List<String> riskFactors) {
        if (latest.getCognitiveScore() == null) return 0;
        if (latest.getCognitiveScore() < 10) {
            riskFactors.add("Severe cognitive impairment (score: " + latest.getCognitiveScore() + ")");
            return 30;
        } else if (latest.getCognitiveScore() < 18) {
            riskFactors.add("Moderate cognitive impairment (score: " + latest.getCognitiveScore() + ")");
            return 20;
        } else if (latest.getCognitiveScore() < 24) {
            riskFactors.add("Mild cognitive impairment (score: " + latest.getCognitiveScore() + ")");
            return 10;
        }
        return 0;
    }

    private int calculateMoodRisk(FollowUp latest, List<String> riskFactors) {
        if (latest.getMood() == MoodState.AGITATED || latest.getMood() == MoodState.CONFUSED) {
            riskFactors.add("Current mood: " + latest.getMood());
            return 15;
        } else if (latest.getMood() == MoodState.DEPRESSED || latest.getMood() == MoodState.ANXIOUS) {
            riskFactors.add("Current mood: " + latest.getMood());
            return 10;
        }
        return 0;
    }

    private int calculateFlagsRisk(FollowUp latest, List<String> riskFactors) {
        int risk = 0;
        if (Boolean.TRUE.equals(latest.getAgitationObserved())) {
            riskFactors.add("Agitation observed");
            risk += 10;
        }
        if (Boolean.TRUE.equals(latest.getConfusionObserved())) {
            riskFactors.add("Confusion observed");
            risk += 10;
        }
        return risk;
    }

    private int calculateSleepRisk(FollowUp latest, List<String> riskFactors) {
        if (latest.getSleepQuality() == SleepQuality.POOR) {
            riskFactors.add("Poor sleep quality");
            return 10;
        }
        return 0;
    }

    private int calculateADLRisk(FollowUp latest, List<String> riskFactors) {
        int depCount = 0;
        if (latest.getEating() == IndependenceLevel.DEPENDENT) depCount++;
        if (latest.getDressing() == IndependenceLevel.DEPENDENT) depCount++;
        if (latest.getMobility() == IndependenceLevel.DEPENDENT) depCount++;
        if (depCount > 0) {
            riskFactors.add("Dependent in " + depCount + " ADL activities");
            return depCount * 8;
        }
        return 0;
    }

    private int calculateAlertRisk(List<Alert> alerts) {
        long criticalAlerts = alerts.stream().filter(a -> a.getLevel() == AlertLevel.CRITICAL && a.getStatus() != AlertStatus.RESOLVED).count();
        long unresolvedAlerts = alerts.stream().filter(a -> a.getStatus() != AlertStatus.RESOLVED).count();
        return (int) (criticalAlerts * 5 + unresolvedAlerts * 2);
    }

    private String determineRiskLevel(int riskScore) {
        if (riskScore >= 70) return "CRITICAL";
        if (riskScore >= 45) return "HIGH";
        if (riskScore >= 20) return "MODERATE";
        return "LOW";
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