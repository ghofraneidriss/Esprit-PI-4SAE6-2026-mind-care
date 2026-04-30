package tn.esprit.lost_item_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.lost_item_service.entity.AlertLevel;
import tn.esprit.lost_item_service.entity.AlertStatus;
import tn.esprit.lost_item_service.entity.LostItemAlert;
import tn.esprit.lost_item_service.repository.LostItemAlertRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LostItemAlertService {

    private static final String ALERT_NOT_FOUND = "LostItemAlert not found with id: ";
    private static final String ALREADY_CRITICAL = "Alert is already at CRITICAL level — cannot escalate further.";

    private final LostItemAlertRepository lostItemAlertRepository;

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public LostItemAlert createAlert(LostItemAlert alert) {
        log.info("Creating lost-item alert: {} [{}] for lostItem={}", alert.getTitle(), alert.getLevel(), alert.getLostItemId());
        return lostItemAlertRepository.save(alert);
    }

    public List<LostItemAlert> getAllAlerts() {
        return lostItemAlertRepository.findAll();
    }

    public LostItemAlert getAlertById(Long id) {
        return lostItemAlertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(ALERT_NOT_FOUND + id));
    }

    public List<LostItemAlert> getAlertsByLostItemId(Long lostItemId) {
        return lostItemAlertRepository.findByLostItemId(lostItemId);
    }

    public List<LostItemAlert> getAlertsByPatientId(Long patientId) {
        return lostItemAlertRepository.findByPatientId(patientId);
    }

    public List<LostItemAlert> getAlertsByLevel(AlertLevel level) {
        return lostItemAlertRepository.findByLevel(level);
    }

    public List<LostItemAlert> getAlertsByStatus(AlertStatus status) {
        return lostItemAlertRepository.findByStatus(status);
    }

    public List<LostItemAlert> getCriticalNewAlerts() {
        return lostItemAlertRepository.findByLevelAndStatus(AlertLevel.CRITICAL, AlertStatus.NEW);
    }

    public List<LostItemAlert> getAlertsByCaregiverId(Long caregiverId) {
        return lostItemAlertRepository.findByCaregiverId(caregiverId);
    }

    public LostItemAlert updateAlert(Long id, LostItemAlert updated) {
        LostItemAlert existing = getAlertById(id);
        existing.setTitle(updated.getTitle());
        existing.setDescription(updated.getDescription());
        existing.setLevel(updated.getLevel());
        existing.setStatus(updated.getStatus());
        return lostItemAlertRepository.save(existing);
    }

    public void deleteAlert(Long id) {
        lostItemAlertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(ALERT_NOT_FOUND + id));
        lostItemAlertRepository.deleteById(id);
    }

    // ── Advanced Actions ──────────────────────────────────────────────────────

    public LostItemAlert markAsViewed(Long id) {
        LostItemAlert alert = getAlertById(id);
        if (alert.getStatus() == AlertStatus.NEW) {
            alert.setStatus(AlertStatus.VIEWED);
            alert.setViewedAt(LocalDateTime.now());
            log.info("LostItemAlert id={} marked as VIEWED", id);
        }
        return lostItemAlertRepository.save(alert);
    }

    public LostItemAlert resolveAlert(Long id) {
        LostItemAlert alert = getAlertById(id);
        alert.setStatus(AlertStatus.RESOLVED);
        log.info("LostItemAlert id={} marked as RESOLVED", id);
        return lostItemAlertRepository.save(alert);
    }

    /**
     * Escalates alert level: LOW → MEDIUM → HIGH → CRITICAL.
     * Resets status to NEW after escalation so it gets attention again.
     */
    public LostItemAlert escalateAlert(Long id) {
        LostItemAlert alert = getAlertById(id);
        AlertLevel current = alert.getLevel();
        AlertLevel next;
        switch (current) {
            case LOW    -> next = AlertLevel.MEDIUM;
            case MEDIUM -> next = AlertLevel.HIGH;
            case HIGH   -> next = AlertLevel.CRITICAL;
            default     -> throw new IllegalStateException(ALREADY_CRITICAL);
        }
        alert.setLevel(next);
        alert.setStatus(AlertStatus.NEW);
        log.info("LostItemAlert id={} escalated from {} to {}", id, current, next);
        return lostItemAlertRepository.save(alert);
    }

    /**
     * Bulk resolve all non-RESOLVED alerts for a specific lost item.
     * Called when item is marked FOUND or CLOSED.
     */
    public int resolveAllByLostItem(Long lostItemId) {
        List<LostItemAlert> active = lostItemAlertRepository.findByLostItemIdAndStatus(lostItemId, AlertStatus.NEW);
        active.addAll(lostItemAlertRepository.findByLostItemIdAndStatus(lostItemId, AlertStatus.VIEWED));
        active.forEach(a -> a.setStatus(AlertStatus.RESOLVED));
        lostItemAlertRepository.saveAll(active);
        log.info("Bulk resolved {} alerts for lostItemId={}", active.size(), lostItemId);
        return active.size();
    }

    /**
     * Bulk resolve all non-RESOLVED alerts for a patient.
     */
    public int resolveAllByPatient(Long patientId) {
        List<LostItemAlert> active = lostItemAlertRepository.findByPatientIdAndStatus(patientId, AlertStatus.NEW);
        active.addAll(lostItemAlertRepository.findByPatientIdAndStatus(patientId, AlertStatus.VIEWED));
        active.forEach(a -> a.setStatus(AlertStatus.RESOLVED));
        lostItemAlertRepository.saveAll(active);
        log.info("Bulk resolved {} alerts for patientId={}", active.size(), patientId);
        return active.size();
    }

    // ── Statistics ────────────────────────────────────────────────────────────

    public Map<String, Object> getStatistics() {
        List<LostItemAlert> all = lostItemAlertRepository.findAll();
        long total = all.size();

        Map<String, Long> byStatus = all.stream()
                .collect(Collectors.groupingBy(a -> a.getStatus().name(), Collectors.counting()));

        Map<String, Long> byLevel = all.stream()
                .collect(Collectors.groupingBy(a -> a.getLevel().name(), Collectors.counting()));

        Map<String, Long> alertsPerPatient = all.stream()
                .collect(Collectors.groupingBy(a -> String.valueOf(a.getPatientId()), Collectors.counting()));

        long criticalUnresolved = all.stream()
                .filter(a -> a.getLevel() == AlertLevel.CRITICAL && a.getStatus() != AlertStatus.RESOLVED)
                .count();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalAlerts", total);
        stats.put("newCount", byStatus.getOrDefault("NEW", 0L));
        stats.put("viewedCount", byStatus.getOrDefault("VIEWED", 0L));
        stats.put("resolvedCount", byStatus.getOrDefault("RESOLVED", 0L));
        stats.put("levelDistribution", byLevel);
        stats.put("alertsPerPatient", alertsPerPatient);
        stats.put("criticalUnresolvedCount", criticalUnresolved);
        return stats;
    }
}
