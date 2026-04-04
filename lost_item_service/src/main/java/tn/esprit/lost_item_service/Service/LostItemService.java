package tn.esprit.lost_item_service.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.lost_item_service.Entity.*;
import tn.esprit.lost_item_service.Repository.ItemAlertRepository;
import tn.esprit.lost_item_service.Repository.LostItemRepository;
import tn.esprit.lost_item_service.Repository.SearchReportRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LostItemService {

    private final LostItemRepository lostItemRepository;
    private final SearchReportRepository searchReportRepository;
    private final ItemAlertRepository itemAlertRepository;

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public List<LostItem> getAllLostItems() {
        return lostItemRepository.findAll();
    }

    @Transactional
    public LostItem createLostItem(LostItem lostItem) {
        log.info("Creating lost item: {}", lostItem.getTitle());
        LostItem saved = lostItemRepository.save(lostItem);
        autoGenerateAlerts(saved);
        return saved;
    }

    public LostItem getLostItemById(Long id) {
        return lostItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lost item not found with id: " + id));
    }

    public Page<LostItem> getPatientLostItems(Long patientId, ItemStatus status, ItemCategory category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (status != null && category != null) {
            return lostItemRepository.findByPatientIdAndStatusAndCategoryOrderByCreatedAtDesc(patientId, status, category, pageable);
        } else if (status != null) {
            return lostItemRepository.findByPatientIdAndStatusOrderByCreatedAtDesc(patientId, status, pageable);
        } else if (category != null) {
            return lostItemRepository.findByPatientIdAndCategoryOrderByCreatedAtDesc(patientId, category, pageable);
        } else {
            return lostItemRepository.findByPatientIdOrderByCreatedAtDesc(patientId, pageable);
        }
    }

    @Transactional
    public LostItem updateLostItem(Long id, LostItem updated) {
        LostItem existing = getLostItemById(id);
        existing.setTitle(updated.getTitle());
        existing.setDescription(updated.getDescription());
        existing.setCategory(updated.getCategory());
        existing.setPatientId(updated.getPatientId());
        existing.setCaregiverId(updated.getCaregiverId());
        existing.setLastSeenLocation(updated.getLastSeenLocation());
        existing.setLastSeenDate(updated.getLastSeenDate());
        existing.setStatus(updated.getStatus());
        existing.setPriority(updated.getPriority());
        existing.setImageUrl(updated.getImageUrl());
        LostItem saved = lostItemRepository.save(existing);
        autoGenerateAlerts(saved);
        return saved;
    }

    @Transactional
    public void deleteLostItem(Long id) {
        LostItem item = getLostItemById(id);
        item.setStatus(ItemStatus.CLOSED);
        lostItemRepository.save(item);
        List<SearchReport> openReports = searchReportRepository.findByLostItemIdAndStatus(id, ReportStatus.OPEN);
        openReports.forEach(r -> r.setStatus(ReportStatus.CLOSED));
        searchReportRepository.saveAll(openReports);
        // Auto-resolve all item alerts on closure
        resolveItemAlerts(id);
        log.info("Soft deleted lost item id={}, closed {} open reports", id, openReports.size());
    }

    @Transactional
    public LostItem markAsFound(Long id) {
        LostItem item = getLostItemById(id);
        item.setStatus(ItemStatus.FOUND);
        List<SearchReport> openReports = searchReportRepository.findByLostItemIdAndStatus(id, ReportStatus.OPEN);
        openReports.forEach(r -> r.setStatus(ReportStatus.CLOSED));
        searchReportRepository.saveAll(openReports);
        LostItem saved = lostItemRepository.save(item);
        // Auto-resolve all item alerts when found
        resolveItemAlerts(id);
        log.info("Marked lost item id={} as FOUND, closed {} open reports", id, openReports.size());
        return saved;
    }

    public List<LostItem> getCriticalLostItems(Long patientId) {
        return lostItemRepository.findCriticalByPatientId(patientId, ItemPriority.CRITICAL, ItemStatus.SEARCHING);
    }

    public List<LostItem> getAllCriticalItems() {
        return lostItemRepository.findAll().stream()
                .filter(i -> i.getPriority() == ItemPriority.CRITICAL || i.getStatus() == ItemStatus.SEARCHING)
                .toList();
    }

    public List<LostItem> getItemsByCaregiverId(Long caregiverId) {
        return lostItemRepository.findByCaregiverIdOrderByCreatedAtDesc(caregiverId);
    }

    public List<LostItem> getCriticalItemsByCaregiverId(Long caregiverId) {
        List<LostItem> all = lostItemRepository.findByCaregiverIdOrderByCreatedAtDesc(caregiverId);
        return all.stream()
                .filter(i -> i.getPriority() == ItemPriority.CRITICAL || i.getStatus() == ItemStatus.SEARCHING)
                .toList();
    }

    // ── Auto-Alert Generation ─────────────────────────────────────────────────

    /**
     * Automatically generates ItemAlerts based on the item's state.
     * Mirrors FollowUpService.autoGenerateAlerts() logic adapted for lost items.
     *
     * Rules:
     *  1. MEDICATION category + LOST status        → CRITICAL alert
     *  2. MEDICATION category + SEARCHING status   → HIGH alert
     *  3. CRITICAL priority (non-MEDICATION) + LOST→ HIGH alert
     *  4. HIGH priority + LOST status              → MEDIUM alert
     *  5. Patient has > 3 active LOST items        → MEDIUM alert (pattern detection)
     */
    private void autoGenerateAlerts(LostItem item) {
        if (item.getStatus() == ItemStatus.FOUND || item.getStatus() == ItemStatus.CLOSED) {
            return;
        }

        List<AlertSpec> specs = new ArrayList<>();

        // Rule 1: Medication lost is always critical
        if (item.getCategory() == ItemCategory.MEDICATION && item.getStatus() == ItemStatus.LOST) {
            specs.add(new AlertSpec(
                    "Critical: Medication item lost",
                    String.format("Patient %d has lost a MEDICATION item: '%s'. Immediate action required.", item.getPatientId(), item.getTitle()),
                    AlertLevel.CRITICAL
            ));
        }

        // Rule 2: Medication being searched
        else if (item.getCategory() == ItemCategory.MEDICATION && item.getStatus() == ItemStatus.SEARCHING) {
            specs.add(new AlertSpec(
                    "Medication item still missing",
                    String.format("MEDICATION item '%s' is still being searched. Last seen: %s.", item.getTitle(), item.getLastSeenLocation()),
                    AlertLevel.HIGH
            ));
        }

        // Rule 3: Critical priority (non-medication)
        else if (item.getPriority() == ItemPriority.CRITICAL && item.getStatus() == ItemStatus.LOST) {
            specs.add(new AlertSpec(
                    "High priority item reported lost",
                    String.format("A CRITICAL priority item '%s' has been reported lost by patient %d.", item.getTitle(), item.getPatientId()),
                    AlertLevel.HIGH
            ));
        }

        // Rule 4: High priority lost
        else if (item.getPriority() == ItemPriority.HIGH && item.getStatus() == ItemStatus.LOST) {
            specs.add(new AlertSpec(
                    "High priority item lost",
                    String.format("Item '%s' with HIGH priority reported as lost.", item.getTitle()),
                    AlertLevel.MEDIUM
            ));
        }

        // Rule 5: Multiple lost items for same patient
        long activeLostCount = lostItemRepository.countByPatientIdAndStatus(item.getPatientId(), ItemStatus.LOST);
        if (activeLostCount > 3) {
            specs.add(new AlertSpec(
                    "Multiple items lost — patient requires attention",
                    String.format("Patient %d currently has %d active LOST items. Caregiver intervention recommended.", item.getPatientId(), activeLostCount),
                    AlertLevel.MEDIUM
            ));
        }

        // Persist each spec only if a non-RESOLVED alert with that title doesn't already exist
        for (AlertSpec spec : specs) {
            boolean alreadyExists = itemAlertRepository
                    .existsByLostItemIdAndTitleAndStatusNot(item.getId(), spec.title(), AlertStatus.RESOLVED);
            if (!alreadyExists) {
                ItemAlert alert = ItemAlert.builder()
                        .lostItemId(item.getId())
                        .patientId(item.getPatientId())
                        .caregiverId(item.getCaregiverId())
                        .title(spec.title())
                        .description(spec.description())
                        .level(spec.level())
                        .build();
                itemAlertRepository.save(alert);
                log.info("Auto-generated {} alert for lostItem id={}: {}", spec.level(), item.getId(), spec.title());
            }
        }
    }

    private record AlertSpec(String title, String description, AlertLevel level) {}

    private void resolveItemAlerts(Long lostItemId) {
        List<ItemAlert> active = itemAlertRepository.findByLostItemIdAndStatus(lostItemId, AlertStatus.NEW);
        active.addAll(itemAlertRepository.findByLostItemIdAndStatus(lostItemId, AlertStatus.VIEWED));
        active.forEach(a -> a.setStatus(AlertStatus.RESOLVED));
        itemAlertRepository.saveAll(active);
    }

    // ── Patient Risk Scoring ──────────────────────────────────────────────────

    /**
     * Calculates a 0–100 risk score for a patient based on lost item history.
     * Mirrors FollowUpService.calculatePatientRisk() adapted for items.
     *
     * Score factors:
     *  - Each active LOST item:         8 pts  (max 40)
     *  - CRITICAL priority item:       15 pts  (max 30)
     *  - MEDICATION item lost:         20 pts  (flat bonus)
     *  - Unresolved CRITICAL alerts:   10 pts  (max 20)
     *  - Unresolved HIGH alerts:        5 pts  (max 15)
     *  - Frequent losing trend:        15 pts  (flat bonus)
     */
    public Map<String, Object> calculatePatientItemRisk(Long patientId) {
        List<LostItem> allItems = lostItemRepository.findByPatientId(patientId);
        List<LostItem> activeItems = allItems.stream()
                .filter(i -> i.getStatus() == ItemStatus.LOST || i.getStatus() == ItemStatus.SEARCHING)
                .toList();
        List<LostItem> foundItems = allItems.stream()
                .filter(i -> i.getStatus() == ItemStatus.FOUND)
                .toList();

        List<ItemAlert> unresolvedAlerts = itemAlertRepository.findByPatientIdAndStatus(patientId, AlertStatus.NEW);
        unresolvedAlerts.addAll(itemAlertRepository.findByPatientIdAndStatus(patientId, AlertStatus.VIEWED));

        int score = 0;
        List<String> riskFactors = new ArrayList<>();

        // Factor 1: Active lost items
        int activeCount = activeItems.size();
        int activeScore = Math.min(activeCount * 8, 40);
        if (activeCount > 0) {
            score += activeScore;
            riskFactors.add(activeCount + " active lost/searching item(s) (+"+activeScore+" pts)");
        }

        // Factor 2: Critical priority items
        long criticalItems = activeItems.stream().filter(i -> i.getPriority() == ItemPriority.CRITICAL).count();
        int criticalItemScore = (int) Math.min(criticalItems * 15, 30);
        if (criticalItems > 0) {
            score += criticalItemScore;
            riskFactors.add(criticalItems + " CRITICAL priority item(s) (+"+criticalItemScore+" pts)");
        }

        // Factor 3: Medication lost
        boolean hasMedicationLost = activeItems.stream().anyMatch(i -> i.getCategory() == ItemCategory.MEDICATION);
        if (hasMedicationLost) {
            score += 20;
            riskFactors.add("Medication item currently lost (+20 pts)");
        }

        // Factor 4: Unresolved CRITICAL alerts
        long criticalAlerts = unresolvedAlerts.stream().filter(a -> a.getLevel() == AlertLevel.CRITICAL).count();
        int critAlertScore = (int) Math.min(criticalAlerts * 10, 20);
        if (criticalAlerts > 0) {
            score += critAlertScore;
            riskFactors.add(criticalAlerts + " unresolved CRITICAL alert(s) (+"+critAlertScore+" pts)");
        }

        // Factor 5: Unresolved HIGH alerts
        long highAlerts = unresolvedAlerts.stream().filter(a -> a.getLevel() == AlertLevel.HIGH).count();
        int highAlertScore = (int) Math.min(highAlerts * 5, 15);
        if (highAlerts > 0) {
            score += highAlertScore;
            riskFactors.add(highAlerts + " unresolved HIGH alert(s) (+"+highAlertScore+" pts)");
        }

        // Factor 6: Frequent losing trend
        Map<String, Object> trend = detectFrequentLosing(patientId);
        if (Boolean.TRUE.equals(trend.get("isFrequentLoser"))) {
            score += 15;
            riskFactors.add("Frequent item-losing pattern detected (+15 pts)");
        }

        score = Math.min(score, 100);
        String riskLevel;
        if (score <= 25) riskLevel = "LOW";
        else if (score <= 50) riskLevel = "MODERATE";
        else if (score <= 75) riskLevel = "HIGH";
        else riskLevel = "CRITICAL";

        double recoveryRate = allItems.isEmpty() ? 0.0
                : Math.round((foundItems.size() * 100.0 / allItems.size()) * 10.0) / 10.0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("patientId", patientId);
        result.put("riskScore", score);
        result.put("riskLevel", riskLevel);
        result.put("riskFactors", riskFactors);
        result.put("totalItems", allItems.size());
        result.put("activeItems", activeCount);
        result.put("foundItems", foundItems.size());
        result.put("unresolvedAlerts", unresolvedAlerts.size());
        result.put("criticalAlerts", criticalAlerts);
        result.put("recoveryRate", recoveryRate);
        result.put("hasMedicationLost", hasMedicationLost);
        return result;
    }

    // ── Frequency Trend Detection ─────────────────────────────────────────────

    /**
     * Detects if a patient is losing items at an increasing frequency.
     * Mirrors FollowUpService.detectCognitiveDecline() concept applied to item loss patterns.
     *
     * Compares item counts across the last 3 months.
     * Creates a CRITICAL alert if an increasing trend with ≥ 3 items per month is detected.
     */
    public Map<String, Object> detectFrequentLosing(Long patientId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneMonthAgo   = now.minus(30, ChronoUnit.DAYS);
        LocalDateTime twoMonthsAgo  = now.minus(60, ChronoUnit.DAYS);
        LocalDateTime threeMonthsAgo = now.minus(90, ChronoUnit.DAYS);

        int recentCount    = lostItemRepository.findByPatientIdAndCreatedAtBetween(patientId, oneMonthAgo, now).size();
        int previousCount  = lostItemRepository.findByPatientIdAndCreatedAtBetween(patientId, twoMonthsAgo, oneMonthAgo).size();
        int oldestCount    = lostItemRepository.findByPatientIdAndCreatedAtBetween(patientId, threeMonthsAgo, twoMonthsAgo).size();

        boolean increasing = recentCount > previousCount && previousCount > oldestCount;
        boolean frequentLoser = increasing && recentCount >= 3;

        String trend;
        if (recentCount > previousCount) trend = "INCREASING";
        else if (recentCount < previousCount) trend = "DECREASING";
        else trend = "STABLE";

        if (frequentLoser) {
            String alertTitle = "Frequent item-losing pattern detected for patient " + patientId;
            boolean alreadyExists = itemAlertRepository
                    .findByPatientIdAndStatus(patientId, AlertStatus.NEW).stream()
                    .anyMatch(a -> a.getTitle().equals(alertTitle));
            if (!alreadyExists) {
                ItemAlert trendAlert = ItemAlert.builder()
                        .lostItemId(0L)
                        .patientId(patientId)
                        .title(alertTitle)
                        .description(String.format(
                                "Patient %d reported %d items lost in the last month, %d the month before, and %d two months ago — consistent upward trend.",
                                patientId, recentCount, previousCount, oldestCount))
                        .level(AlertLevel.CRITICAL)
                        .build();
                itemAlertRepository.save(trendAlert);
                log.warn("CRITICAL trend alert created for patientId={}", patientId);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("patientId", patientId);
        result.put("isFrequentLoser", frequentLoser);
        result.put("trend", trend);
        result.put("recentMonthCount", recentCount);
        result.put("previousMonthCount", previousCount);
        result.put("twoMonthsAgoCount", oldestCount);
        return result;
    }

    // ── Global Statistics ─────────────────────────────────────────────────────

    /**
     * Returns a global statistics snapshot across all lost items and alerts.
     */
    public Map<String, Object> getGlobalStatistics() {
        List<LostItem> all = lostItemRepository.findAll();

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (Object[] row : lostItemRepository.countGroupedByStatus()) {
            byStatus.put(row[0].toString(), (Long) row[1]);
        }

        Map<String, Long> byCategory = new LinkedHashMap<>();
        for (Object[] row : lostItemRepository.countGroupedByCategory()) {
            byCategory.put(row[0].toString(), (Long) row[1]);
        }

        Map<String, Long> byPriority = new LinkedHashMap<>();
        for (Object[] row : lostItemRepository.countGroupedByPriority()) {
            byPriority.put(row[0].toString(), (Long) row[1]);
        }

        Map<String, Long> byPatient = new LinkedHashMap<>();
        for (Object[] row : lostItemRepository.countGroupedByPatient()) {
            byPatient.put(row[0].toString(), (Long) row[1]);
        }

        long totalItems = all.size();
        long foundCount = byStatus.getOrDefault("FOUND", 0L);
        long nonClosed  = totalItems - byStatus.getOrDefault("CLOSED", 0L);
        double recoveryRate = nonClosed == 0 ? 0.0
                : Math.round((foundCount * 100.0 / nonClosed) * 10.0) / 10.0;

        // Average days to find (from createdAt to updatedAt for FOUND items)
        OptionalDouble avgDaysToFind = all.stream()
                .filter(i -> i.getStatus() == ItemStatus.FOUND && i.getCreatedAt() != null && i.getUpdatedAt() != null)
                .mapToLong(i -> ChronoUnit.DAYS.between(i.getCreatedAt(), i.getUpdatedAt()))
                .average();

        long criticalUnresolved = itemAlertRepository.countByLevelAndStatus(AlertLevel.CRITICAL, AlertStatus.NEW)
                + itemAlertRepository.countByLevelAndStatus(AlertLevel.CRITICAL, AlertStatus.VIEWED);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalItems", totalItems);
        stats.put("lostCount",      byStatus.getOrDefault("LOST", 0L));
        stats.put("searchingCount", byStatus.getOrDefault("SEARCHING", 0L));
        stats.put("foundCount",     foundCount);
        stats.put("closedCount",    byStatus.getOrDefault("CLOSED", 0L));
        stats.put("categoryDistribution", byCategory);
        stats.put("priorityDistribution", byPriority);
        stats.put("itemsPerPatient", byPatient);
        stats.put("recoveryRate", recoveryRate);
        stats.put("averageDaysToFind", avgDaysToFind.isPresent() ? Math.round(avgDaysToFind.getAsDouble() * 10.0) / 10.0 : 0.0);
        stats.put("criticalUnresolvedAlerts", criticalUnresolved);
        return stats;
    }
}
