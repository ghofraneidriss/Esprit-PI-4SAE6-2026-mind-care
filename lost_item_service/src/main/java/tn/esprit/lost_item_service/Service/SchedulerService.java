package tn.esprit.lost_item_service.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.lost_item_service.Entity.*;
import tn.esprit.lost_item_service.Repository.ItemAlertRepository;
import tn.esprit.lost_item_service.Repository.LostItemRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler that automatically escalates the priority of stale LOST items.
 *
 * Runs every day at 08:00.
 *
 * Rules:
 *  - LOST item with MEDIUM priority for > 7 days  → escalate to HIGH   + MEDIUM alert
 *  - LOST item with HIGH priority for > 14 days   → escalate to CRITICAL + HIGH alert
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulerService {

    private final LostItemRepository lostItemRepository;
    private final ItemAlertRepository itemAlertRepository;

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void escalateStaleLostItems() {
        log.info("=== [Scheduler] Running stale item escalation ===");

        LocalDateTime sevenDaysAgo    = LocalDateTime.now().minusDays(7);
        LocalDateTime fourteenDaysAgo = LocalDateTime.now().minusDays(14);

        // MEDIUM → HIGH after 7 days
        List<LostItem> mediumStale = lostItemRepository
                .findStaleItemsForEscalation(ItemStatus.LOST, ItemPriority.MEDIUM, sevenDaysAgo);
        for (LostItem item : mediumStale) {
            item.setPriority(ItemPriority.HIGH);
            lostItemRepository.save(item);
            createEscalationAlert(item, AlertLevel.MEDIUM,
                    "Item priority auto-escalated to HIGH after 7 days",
                    "Lost item '" + item.getTitle() + "' (patient " + item.getPatientId() + ") has been LOST for over 7 days with no resolution. Priority escalated from MEDIUM to HIGH.");
            log.info("[Scheduler] Escalated lostItem id={} from MEDIUM to HIGH ({}+ days lost)", item.getId(), 7);
        }

        // HIGH → CRITICAL after 14 days
        List<LostItem> highStale = lostItemRepository
                .findStaleItemsForEscalation(ItemStatus.LOST, ItemPriority.HIGH, fourteenDaysAgo);
        for (LostItem item : highStale) {
            item.setPriority(ItemPriority.CRITICAL);
            lostItemRepository.save(item);
            createEscalationAlert(item, AlertLevel.HIGH,
                    "Item priority auto-escalated to CRITICAL after 14 days",
                    "Lost item '" + item.getTitle() + "' (patient " + item.getPatientId() + ") has been LOST for over 14 days. Priority escalated from HIGH to CRITICAL. Immediate intervention required.");
            log.warn("[Scheduler] Escalated lostItem id={} from HIGH to CRITICAL ({}+ days lost)", item.getId(), 14);
        }

        log.info("=== [Scheduler] Escalated {} items (MEDIUM→HIGH: {}, HIGH→CRITICAL: {}) ===",
                mediumStale.size() + highStale.size(), mediumStale.size(), highStale.size());
    }

    private void createEscalationAlert(LostItem item, AlertLevel level, String title, String description) {
        boolean alreadyExists = itemAlertRepository
                .existsByLostItemIdAndTitleAndStatusNot(item.getId(), title, AlertStatus.RESOLVED);
        if (!alreadyExists) {
            ItemAlert alert = ItemAlert.builder()
                    .lostItemId(item.getId())
                    .patientId(item.getPatientId())
                    .title(title)
                    .description(description)
                    .level(level)
                    .build();
            itemAlertRepository.save(alert);
        }
    }
}
