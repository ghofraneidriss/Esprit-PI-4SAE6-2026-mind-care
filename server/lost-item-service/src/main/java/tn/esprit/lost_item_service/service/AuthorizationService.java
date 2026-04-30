package tn.esprit.lost_item_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.lost_item_service.entity.LostItem;
import tn.esprit.lost_item_service.entity.LostItemAlert;
import tn.esprit.lost_item_service.entity.SearchReport;
import tn.esprit.lost_item_service.exception.AccessDeniedException;
import tn.esprit.lost_item_service.repository.LostItemAlertRepository;
import tn.esprit.lost_item_service.repository.LostItemRepository;
import tn.esprit.lost_item_service.repository.SearchReportRepository;

@Service
@RequiredArgsConstructor
public class AuthorizationService {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String DOCTOR_ROLE = "DOCTOR";
    private static final String CAREGIVER_ROLE = "CAREGIVER";
    private static final String PATIENT_ROLE = "PATIENT";

    private final LostItemRepository lostItemRepository;
    private final SearchReportRepository searchReportRepository;
    private final LostItemAlertRepository lostItemAlertRepository;

    // ── Role helpers ──────────────────────────────────────────────────────────

    private boolean isFullAccess(String role) {
        return ADMIN_ROLE.equals(role) || DOCTOR_ROLE.equals(role);
    }

    private String normalise(String role) {
        return role != null ? role.toUpperCase() : ADMIN_ROLE;
    }

    // ── Lost Item access ──────────────────────────────────────────────────────

    /**
     * Verifies that the caller may read / modify the given lost item.
     * ADMIN / DOCTOR → always allowed.
     * CAREGIVER      → must be the item's assigned caregiver.
     * PATIENT        → must be the item's patient.
     */
    public LostItem checkItemAccess(Long itemId, Long userId, String rawRole) {
        String role = normalise(rawRole);
        LostItem item = lostItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Lost item not found: " + itemId));

        if (isFullAccess(role)) return item;

        if (CAREGIVER_ROLE.equals(role) && (userId == null || !userId.equals(item.getCaregiverId()))) {
            throw new AccessDeniedException("You are not assigned to this item.");
        } else if (PATIENT_ROLE.equals(role) && (userId == null || !userId.equals(item.getPatientId()))) {
            throw new AccessDeniedException("You do not own this item.");
        }
        return item;
    }

    /**
     * Verifies access to a lost item scoped only by its ID (no item pre-loading).
     * Use when the item entity is not needed but access must still be checked.
     */
    public void checkItemIdAccess(Long itemId, Long userId, String rawRole) {
        checkItemAccess(itemId, userId, rawRole);
    }

    // ── Search Report access ──────────────────────────────────────────────────

    /**
     * Verifies that the caller may read / modify the given search report.
     * Delegates to the parent lost item's access check.
     */
    public SearchReport checkReportAccess(Long reportId, Long userId, String rawRole) {
        String role = normalise(rawRole);
        if (isFullAccess(role)) {
            return searchReportRepository.findById(reportId)
                    .orElseThrow(() -> new RuntimeException("Search report not found: " + reportId));
        }

        SearchReport report = searchReportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Search report not found: " + reportId));

        // Validate via parent item
        checkItemAccess(report.getLostItemId(), userId, role);
        return report;
    }

    /**
     * Verifies that the caller may list / count reports for a given lost item.
     */
    public void checkReportListAccess(Long lostItemId, Long userId, String rawRole) {
        checkItemIdAccess(lostItemId, userId, rawRole);
    }

    // ── Alert access ──────────────────────────────────────────────────────────

    /**
     * Verifies that the caller may read / modify the given alert.
     * CAREGIVER: must be the alert's caregiverId.
     * PATIENT: must be the alert's patientId.
     */
    public LostItemAlert checkAlertAccess(Long alertId, Long userId, String rawRole) {
        String role = normalise(rawRole);
        LostItemAlert alert = lostItemAlertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found: " + alertId));

        if (isFullAccess(role)) return alert;

        if (CAREGIVER_ROLE.equals(role) && (userId == null || !userId.equals(alert.getCaregiverId()))) {
            throw new AccessDeniedException("You are not assigned to this alert.");
        } else if (PATIENT_ROLE.equals(role) && (userId == null || !userId.equals(alert.getPatientId()))) {
            throw new AccessDeniedException("You do not own this alert.");
        }
        return alert;
    }

    /**
     * Verifies that the caller may list alerts for a given patient.
     * CAREGIVER / ADMIN / DOCTOR → allowed.
     * PATIENT → only their own patient ID.
     */
    public void checkPatientAlertAccess(Long patientId, Long userId, String rawRole) {
        String role = normalise(rawRole);
        if (isFullAccess(role) || CAREGIVER_ROLE.equals(role)) return;
        if (PATIENT_ROLE.equals(role) && (userId == null || !userId.equals(patientId))) {
            throw new AccessDeniedException("You can only view your own alerts.");
        }
    }
}
