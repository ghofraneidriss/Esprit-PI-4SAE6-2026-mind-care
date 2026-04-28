package tn.esprit.lost_item_service.Controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.lost_item_service.dto.CreateLostItemRequest;
import tn.esprit.lost_item_service.dto.DTOMapper;
import tn.esprit.lost_item_service.dto.LostItemDTO;
import tn.esprit.lost_item_service.dto.UpdateLostItemRequest;
import tn.esprit.lost_item_service.Entity.ItemCategory;
import tn.esprit.lost_item_service.Entity.ItemStatus;
import tn.esprit.lost_item_service.Entity.LostItem;
import tn.esprit.lost_item_service.Service.AuthorizationService;
import tn.esprit.lost_item_service.Service.LostItemService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lost-items")
@RequiredArgsConstructor
public class LostItemController {

    private final LostItemService lostItemService;
    private final AuthorizationService authorizationService;

    /**
     * GET /api/lost-items
     * - ADMIN / DOCTOR: returns all items
     * - CAREGIVER: returns only items for their assigned patients
     * - PATIENT: returns only their own items
     */
    @GetMapping
    public ResponseEntity<List<LostItemDTO>> getAllLostItems(
            @RequestHeader(value = "X-User-Id",   required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        String role = userRole != null ? userRole.toUpperCase() : "ADMIN";

        if ("PATIENT".equals(role) && userId != null) {
            return ResponseEntity.ok(DTOMapper.toLostItemDTOList(lostItemService.getItemsByPatientIdFlat(userId)));
        }
        if ("CAREGIVER".equals(role) && userId != null) {
            return ResponseEntity.ok(DTOMapper.toLostItemDTOList(lostItemService.getItemsByCaregiverId(userId)));
        }
        return ResponseEntity.ok(DTOMapper.toLostItemDTOList(lostItemService.getAllLostItems()));
    }

    @PostMapping
    public ResponseEntity<LostItemDTO> createLostItem(@Valid @RequestBody CreateLostItemRequest request) {
        LostItem lostItem = DTOMapper.toLostItem(request);
        LostItem created = lostItemService.createLostItem(lostItem);
        return new ResponseEntity<>(DTOMapper.toLostItemDTO(created), HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LostItemDTO> getLostItemById(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id",   required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        LostItem item = authorizationService.checkItemAccess(id, userId, userRole);
        return ResponseEntity.ok(DTOMapper.toLostItemDTO(item));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<Map<String, Object>> getPatientLostItems(
            @PathVariable Long patientId,
            @RequestParam(required = false) ItemStatus status,
            @RequestParam(required = false) ItemCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<LostItem> resultPage = lostItemService.getPatientLostItems(patientId, status, category, page, size);
        Map<String, Object> response = new HashMap<>();
        response.put("content", DTOMapper.toLostItemDTOList(resultPage.getContent()));
        response.put("totalElements", resultPage.getTotalElements());
        response.put("totalPages", resultPage.getTotalPages());
        response.put("currentPage", resultPage.getNumber());
        response.put("pageSize", resultPage.getSize());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LostItemDTO> updateLostItem(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLostItemRequest request,
            @RequestHeader(value = "X-User-Id",   required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        authorizationService.checkItemAccess(id, userId, userRole);
        LostItem lostItem = DTOMapper.toLostItemForUpdate(request);
        LostItem updated = lostItemService.updateLostItem(id, lostItem);
        return ResponseEntity.ok(DTOMapper.toLostItemDTO(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteLostItem(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id",   required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        authorizationService.checkItemAccess(id, userId, userRole);
        lostItemService.deleteLostItem(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Lost item id=" + id + " has been closed (soft deleted).");
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/mark-found")
    public ResponseEntity<LostItem> markAsFound(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id",   required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        authorizationService.checkItemAccess(id, userId, userRole);
        return ResponseEntity.ok(lostItemService.markAsFound(id));
    }

    @GetMapping("/critical/all")
    public ResponseEntity<Map<String, Object>> getAllCriticalItems() {
        List<LostItem> items = lostItemService.getAllCriticalItems();
        Map<String, Object> response = new HashMap<>();
        response.put("items", items);
        response.put("urgentCount", items.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/patient/{patientId}/critical")
    public ResponseEntity<Map<String, Object>> getCriticalLostItems(@PathVariable Long patientId) {
        List<LostItem> items = lostItemService.getCriticalLostItems(patientId);
        Map<String, Object> response = new HashMap<>();
        response.put("items", items);
        response.put("urgentCount", items.size());
        return ResponseEntity.ok(response);
    }

    // ── Caregiver-scoped Endpoints ────────────────────────────────────────────

    @GetMapping("/caregiver/{caregiverId}")
    public ResponseEntity<List<LostItem>> getItemsByCaregiverId(@PathVariable Long caregiverId) {
        return ResponseEntity.ok(lostItemService.getItemsByCaregiverId(caregiverId));
    }

    @GetMapping("/caregiver/{caregiverId}/critical")
    public ResponseEntity<Map<String, Object>> getCriticalItemsByCaregiverId(@PathVariable Long caregiverId) {
        List<LostItem> items = lostItemService.getCriticalItemsByCaregiverId(caregiverId);
        Map<String, Object> response = new HashMap<>();
        response.put("items", items);
        response.put("urgentCount", items.size());
        return ResponseEntity.ok(response);
    }

    // ── Advanced Endpoints ────────────────────────────────────────────────────

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getGlobalStatistics() {
        return ResponseEntity.ok(lostItemService.getGlobalStatistics());
    }

    @GetMapping("/patient/{patientId}/risk")
    public ResponseEntity<Map<String, Object>> getPatientItemRisk(@PathVariable Long patientId) {
        return ResponseEntity.ok(lostItemService.calculatePatientItemRisk(patientId));
    }

    @GetMapping("/patient/{patientId}/trend")
    public ResponseEntity<Map<String, Object>> getPatientFrequencyTrend(@PathVariable Long patientId) {
        return ResponseEntity.ok(lostItemService.detectFrequentLosing(patientId));
    }
}
