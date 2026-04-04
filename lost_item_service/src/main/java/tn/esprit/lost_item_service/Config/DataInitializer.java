package tn.esprit.lost_item_service.Config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import tn.esprit.lost_item_service.Entity.*;
import tn.esprit.lost_item_service.Repository.ItemAlertRepository;
import tn.esprit.lost_item_service.Repository.LostItemRepository;
import tn.esprit.lost_item_service.Repository.SearchReportRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Seeds lost_item_db with demo data.
 * User IDs match the users_service DataInitializer insertion order:
 *   1 = admin, 2 = doctor, 3 = caregiver1 (Sana), 4 = caregiver2 (Yassine)
 *   5 = patient1 (Mohamed), 6 = patient2 (Fatma), 7 = patient3 (Hedi)
 *
 * Runs only when lost_item table is empty.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final LostItemRepository lostItemRepository;
    private final SearchReportRepository searchReportRepository;
    private final ItemAlertRepository itemAlertRepository;

    // User IDs (matching users_service seed order)
    private static final Long CAREGIVER_1 = 3L;
    private static final Long CAREGIVER_2 = 4L;
    private static final Long PATIENT_1   = 5L;  // Mohamed Gharbi
    private static final Long PATIENT_2   = 6L;  // Fatma Riahi
    private static final Long PATIENT_3   = 7L;  // Hedi Zouari

    @Override
    public void run(ApplicationArguments args) {
        if (lostItemRepository.count() > 0) {
            log.info("[DataInitializer] lost_item table already populated — skipping seed.");
            return;
        }

        seedLostItems();
        log.info("[DataInitializer] Seeded lost items, search reports, and alerts.");
    }

    private void seedLostItems() {

        // ── Patient 1 (Mohamed): 4 items, mixed statuses ─────────────────────

        LostItem wallet = lostItemRepository.save(LostItem.builder()
            .title("Brown leather wallet")
            .description("Contains national ID card and bank cards. Very important.")
            .category(ItemCategory.ACCESSORY)
            .patientId(PATIENT_1).caregiverId(CAREGIVER_1)
            .lastSeenLocation("Living room sofa")
            .lastSeenDate(LocalDate.now().minusDays(5))
            .status(ItemStatus.SEARCHING).priority(ItemPriority.CRITICAL)
            .build());

        LostItem keys = lostItemRepository.save(LostItem.builder()
            .title("House keys (blue keychain)")
            .description("Set of 3 keys on a blue plastic keychain.")
            .category(ItemCategory.ACCESSORY)
            .patientId(PATIENT_1).caregiverId(CAREGIVER_1)
            .lastSeenLocation("Kitchen counter")
            .lastSeenDate(LocalDate.now().minusDays(2))
            .status(ItemStatus.LOST).priority(ItemPriority.HIGH)
            .build());

        LostItem medication = lostItemRepository.save(LostItem.builder()
            .title("Donepezil medication box")
            .description("Blue Aricept box — daily medication. Must be found urgently.")
            .category(ItemCategory.MEDICATION)
            .patientId(PATIENT_1).caregiverId(CAREGIVER_1)
            .lastSeenLocation("Bedroom drawer")
            .lastSeenDate(LocalDate.now().minusDays(1))
            .status(ItemStatus.LOST).priority(ItemPriority.CRITICAL)
            .build());

        LostItem jacket = lostItemRepository.save(LostItem.builder()
            .title("Grey winter jacket")
            .description("Large grey jacket with hood.")
            .category(ItemCategory.CLOTHING)
            .patientId(PATIENT_1).caregiverId(CAREGIVER_1)
            .lastSeenLocation("Entrance hallway")
            .lastSeenDate(LocalDate.now().minusDays(10))
            .status(ItemStatus.FOUND).priority(ItemPriority.LOW)
            .build());

        // ── Patient 2 (Fatma): 3 items ────────────────────────────────────────

        LostItem glasses = lostItemRepository.save(LostItem.builder()
            .title("Reading glasses (gold frame)")
            .description("Gold-framed reading glasses, left in case.")
            .category(ItemCategory.ACCESSORY)
            .patientId(PATIENT_2).caregiverId(CAREGIVER_1)
            .lastSeenLocation("Dining table")
            .lastSeenDate(LocalDate.now().minusDays(3))
            .status(ItemStatus.LOST).priority(ItemPriority.MEDIUM)
            .build());

        LostItem phone = lostItemRepository.save(LostItem.builder()
            .title("Samsung phone (black)")
            .description("Black Samsung Galaxy A series, cracked screen protector.")
            .category(ItemCategory.ELECTRONIC)
            .patientId(PATIENT_2).caregiverId(CAREGIVER_1)
            .lastSeenLocation("Bathroom")
            .lastSeenDate(LocalDate.now().minusDays(1))
            .status(ItemStatus.SEARCHING).priority(ItemPriority.HIGH)
            .build());

        LostItem idCard = lostItemRepository.save(LostItem.builder()
            .title("National ID card")
            .description("Tunisian national ID card, green background.")
            .category(ItemCategory.DOCUMENT)
            .patientId(PATIENT_2).caregiverId(CAREGIVER_2)
            .lastSeenLocation("Handbag — living room")
            .lastSeenDate(LocalDate.now().minusDays(7))
            .status(ItemStatus.LOST).priority(ItemPriority.CRITICAL)
            .build());

        // ── Patient 3 (Hedi): 2 items ─────────────────────────────────────────

        LostItem watch = lostItemRepository.save(LostItem.builder()
            .title("Silver wristwatch")
            .description("Old silver Casio watch — sentimental value.")
            .category(ItemCategory.ACCESSORY)
            .patientId(PATIENT_3).caregiverId(CAREGIVER_2)
            .lastSeenLocation("Bedroom nightstand")
            .lastSeenDate(LocalDate.now().minusDays(4))
            .status(ItemStatus.LOST).priority(ItemPriority.MEDIUM)
            .build());

        LostItem book = lostItemRepository.save(LostItem.builder()
            .title("Memory exercise booklet")
            .description("Blue A5 booklet prescribed by therapist — filled with exercises.")
            .category(ItemCategory.OTHER)
            .patientId(PATIENT_3).caregiverId(CAREGIVER_2)
            .lastSeenLocation("Doctor's waiting room")
            .lastSeenDate(LocalDate.now().minusDays(6))
            .status(ItemStatus.CLOSED).priority(ItemPriority.LOW)
            .build());

        // ── Search Reports ─────────────────────────────────────────────────────

        // Wallet — 3 search attempts
        saveReport(wallet.getId(), CAREGIVER_1, LocalDate.now().minusDays(4),
            "Living room sofa", SearchResult.NOT_FOUND, ReportStatus.OPEN,
            "Checked under all cushions, not found.");
        saveReport(wallet.getId(), CAREGIVER_1, LocalDate.now().minusDays(3),
            "Living room + corridor", SearchResult.NOT_FOUND, ReportStatus.OPEN,
            "Checked corridor and shelf near entrance.");
        saveReport(wallet.getId(), CAREGIVER_1, LocalDate.now().minusDays(1),
            "Bedroom and bathroom", SearchResult.PARTIALLY_FOUND, ReportStatus.OPEN,
            "Found the ID card separately on bathroom shelf. Wallet still missing.");

        // Keys — 2 search attempts
        saveReport(keys.getId(), CAREGIVER_1, LocalDate.now().minusDays(1),
            "Kitchen", SearchResult.NOT_FOUND, ReportStatus.OPEN,
            "Searched all kitchen drawers and counter surfaces.");
        saveReport(keys.getId(), CAREGIVER_1, LocalDate.now(),
            "Living room and entrance", SearchResult.NOT_FOUND, ReportStatus.OPEN,
            "Checked shoe rack and entrance cabinet.");

        // Medication — urgent, 1 report
        saveReport(medication.getId(), CAREGIVER_1, LocalDate.now(),
            "Bedroom drawer and cabinet", SearchResult.NOT_FOUND, ReportStatus.ESCALATED,
            "URGENT — daily dose missed. Searched entire bedroom. Notifying doctor.");

        // Jacket — found, 2 reports
        saveReport(jacket.getId(), CAREGIVER_1, LocalDate.now().minusDays(9),
            "Wardrobe", SearchResult.NOT_FOUND, ReportStatus.CLOSED,
            "Not in wardrobe.");
        saveReport(jacket.getId(), CAREGIVER_1, LocalDate.now().minusDays(8),
            "Guest room closet", SearchResult.FOUND, ReportStatus.CLOSED,
            "Found in guest room closet, hanging behind other coats.");

        // Glasses — 1 report
        saveReport(glasses.getId(), CAREGIVER_1, LocalDate.now().minusDays(2),
            "Dining table + kitchen", SearchResult.NOT_FOUND, ReportStatus.OPEN,
            "Checked entire dining area and kitchen surfaces.");

        // Phone — 2 reports
        saveReport(phone.getId(), CAREGIVER_1, LocalDate.now().minusDays(1),
            "Bathroom and hallway", SearchResult.NOT_FOUND, ReportStatus.OPEN,
            "Tried calling the phone — battery dead or silenced.");
        saveReport(phone.getId(), CAREGIVER_1, LocalDate.now(),
            "Bedroom and living room", SearchResult.PARTIALLY_FOUND, ReportStatus.OPEN,
            "Found the phone charger on the bedside table. Phone still missing.");

        // ID card — 2 reports
        saveReport(idCard.getId(), CAREGIVER_2, LocalDate.now().minusDays(6),
            "Handbag contents", SearchResult.NOT_FOUND, ReportStatus.OPEN,
            "Emptied entire handbag, not found.");
        saveReport(idCard.getId(), CAREGIVER_2, LocalDate.now().minusDays(4),
            "All jacket pockets", SearchResult.NOT_FOUND, ReportStatus.OPEN,
            "Checked all coats and jackets in wardrobe.");

        // Watch — 1 report
        saveReport(watch.getId(), CAREGIVER_2, LocalDate.now().minusDays(3),
            "Bedroom nightstand + bathroom shelf", SearchResult.NOT_FOUND, ReportStatus.OPEN,
            "Searched nightstand, bathroom shelf, and side tables.");

        // ── Manual Alerts ──────────────────────────────────────────────────────
        // (Auto-alerts are generated by service on create, but those items were
        //  saved directly via repo above, so add key alerts manually)

        itemAlertRepository.save(ItemAlert.builder()
            .lostItemId(medication.getId()).patientId(PATIENT_1).caregiverId(CAREGIVER_1)
            .title("Critical: Medication item lost")
            .description("Patient 5 has lost a MEDICATION item: 'Donepezil medication box'. Immediate action required.")
            .level(AlertLevel.CRITICAL).build());

        itemAlertRepository.save(ItemAlert.builder()
            .lostItemId(wallet.getId()).patientId(PATIENT_1).caregiverId(CAREGIVER_1)
            .title("High priority item reported lost")
            .description("A CRITICAL priority item 'Brown leather wallet' has been reported lost by patient 5.")
            .level(AlertLevel.HIGH).build());

        itemAlertRepository.save(ItemAlert.builder()
            .lostItemId(idCard.getId()).patientId(PATIENT_2).caregiverId(CAREGIVER_2)
            .title("Critical: Document item lost")
            .description("A CRITICAL priority DOCUMENT 'National ID card' is missing for patient 6.")
            .level(AlertLevel.CRITICAL).build());

        itemAlertRepository.save(ItemAlert.builder()
            .lostItemId(phone.getId()).patientId(PATIENT_2).caregiverId(CAREGIVER_1)
            .title("High priority item reported lost")
            .description("Item 'Samsung phone (black)' with HIGH priority reported as lost by patient 6.")
            .level(AlertLevel.HIGH).build());

        itemAlertRepository.save(ItemAlert.builder()
            .lostItemId(keys.getId()).patientId(PATIENT_1).caregiverId(CAREGIVER_1)
            .title("Multiple items lost — patient requires attention")
            .description("Patient 5 currently has 3 active LOST items. Caregiver intervention recommended.")
            .level(AlertLevel.MEDIUM).build());
    }

    private void saveReport(Long itemId, Long reportedBy, LocalDate date,
                            String location, SearchResult result,
                            ReportStatus status, String notes) {
        searchReportRepository.save(SearchReport.builder()
            .lostItemId(itemId)
            .reportedBy(reportedBy)
            .searchDate(date)
            .locationSearched(location)
            .searchResult(result)
            .status(status)
            .notes(notes)
            .build());
    }
}
