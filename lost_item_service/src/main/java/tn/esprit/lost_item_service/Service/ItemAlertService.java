package tn.esprit.lost_item_service.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tn.esprit.lost_item_service.Entity.AlertLevel;
import tn.esprit.lost_item_service.Entity.AlertStatus;
import tn.esprit.lost_item_service.Entity.ItemAlert;
import tn.esprit.lost_item_service.Repository.ItemAlertRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemAlertService {

    private final ItemAlertRepository itemAlertRepository;

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public ItemAlert createAlert(ItemAlert alert) {
        log.info("Creating item alert: {} [{}] for lostItem={}", alert.getTitle(), alert.getLevel(), alert.getLostItemId());
        return itemAlertRepository.save(alert);
    }

    public List<ItemAlert> getAllAlerts() {
        return itemAlertRepository.findAll();
    }

    public ItemAlert getAlertById(Long id) {
        return itemAlertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ItemAlert not found with id: " + id));
    }

    public List<ItemAlert> getAlertsByLostItemId(Long lostItemId) {
        return itemAlertRepository.findByLostItemId(lostItemId);
    }

    public List<ItemAlert> getAlertsByPatientId(Long patientId) {
        return itemAlertRepository.findByPatientId(patientId);
    }

    public List<ItemAlert> getAlertsByLevel(AlertLevel level) {
        return itemAlertRepository.findByLevel(level);
    }

    public List<ItemAlert> getAlertsByStatus(AlertStatus status) {
        return itemAlertRepository.findByStatus(status);
    }

    public List<ItemAlert> getCriticalNewAlerts() {
        return itemAlertRepository.findByLevelAndStatus(AlertLevel.CRITICAL, AlertStatus.NEW);
    }

    public List<ItemAlert> getAlertsByCaregiverId(Long caregiverId) {
        return itemAlertRepository.findByCaregiverId(caregiverId);
    }

    public ItemAlert updateAlert(Long id, ItemAlert updated) {
        ItemAlert existing = getAlertById(id);
        existing.setTitle(updated.getTitle());
        existing.setDescription(updated.getDescription());
        existing.setLevel(updated.getLevel());
        existing.setStatus(updated.getStatus());
        return itemAlertRepository.save(existing);
    }

    public void deleteAlert(Long id) {
        itemAlertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ItemAlert not found with id: " + id));
        itemAlertRepository.deleteById(id);
    }

    // ── Advanced Actions ──────────────────────────────────────────────────────

    public ItemAlert markAsViewed(Long id) {
        ItemAlert alert = getAlertById(id);
        if (alert.getStatus() == AlertStatus.NEW) {
            alert.setStatus(AlertStatus.VIEWED);
            alert.setViewedAt(LocalDateTime.now());
            log.info("Alert id={} marked as VIEWED", id);
        }
        return itemAlertRepository.save(alert);
    }

    public ItemAlert resolveAlert(Long id) {
        ItemAlert alert = getAlertById(id);
        alert.setStatus(AlertStatus.RESOLVED);
        log.info("Alert id={} marked as RESOLVED", id);
        return itemAlertRepository.save(alert);
    }

    /**
     * Escalates alert level: LOW → MEDIUM → HIGH → CRITICAL.
     * Resets status to NEW after escalation so it gets attention again.
     */
    public ItemAlert escalateAlert(Long id) {
        ItemAlert alert = getAlertById(id);
        AlertLevel current = alert.getLevel();
        AlertLevel next;
        switch (current) {
            case LOW    -> next = AlertLevel.MEDIUM;
            case MEDIUM -> next = AlertLevel.HIGH;
            case HIGH   -> next = AlertLevel.CRITICAL;
            default     -> throw new RuntimeException("Alert is already at CRITICAL level — cannot escalate further.");
        }
        alert.setLevel(next);
        alert.setStatus(AlertStatus.NEW);
        log.info("Alert id={} escalated from {} to {}", id, current, next);
        return itemAlertRepository.save(alert);
    }

    /**
     * Bulk resolve all non-RESOLVED alerts for a specific lost item.
     * Called when item is marked FOUND or CLOSED.
     */
    public int resolveAllByLostItem(Long lostItemId) {
        List<ItemAlert> active = itemAlertRepository.findByLostItemIdAndStatus(lostItemId, AlertStatus.NEW);
        active.addAll(itemAlertRepository.findByLostItemIdAndStatus(lostItemId, AlertStatus.VIEWED));
        active.forEach(a -> a.setStatus(AlertStatus.RESOLVED));
        itemAlertRepository.saveAll(active);
        log.info("Bulk resolved {} alerts for lostItemId={}", active.size(), lostItemId);
        return active.size();
    }

    /**
     * Bulk resolve all non-RESOLVED alerts for a patient.
     */
    public int resolveAllByPatient(Long patientId) {
        List<ItemAlert> active = itemAlertRepository.findByPatientIdAndStatus(patientId, AlertStatus.NEW);
        active.addAll(itemAlertRepository.findByPatientIdAndStatus(patientId, AlertStatus.VIEWED));
        active.forEach(a -> a.setStatus(AlertStatus.RESOLVED));
        itemAlertRepository.saveAll(active);
        log.info("Bulk resolved {} alerts for patientId={}", active.size(), patientId);
        return active.size();
    }

    // ── Statistics ────────────────────────────────────────────────────────────

    public Map<String, Object> getStatistics() {
        List<ItemAlert> all = itemAlertRepository.findAll();
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
