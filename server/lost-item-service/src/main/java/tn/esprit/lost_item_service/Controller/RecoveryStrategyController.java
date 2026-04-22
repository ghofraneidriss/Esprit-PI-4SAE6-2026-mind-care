package tn.esprit.lost_item_service.Controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.lost_item_service.Service.AuthorizationService;
import tn.esprit.lost_item_service.Service.RecoveryStrategyService;

import java.util.Map;

@RestController
@RequestMapping("/api/lost-items")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class RecoveryStrategyController {

    private final RecoveryStrategyService recoveryStrategyService;
    private final AuthorizationService authorizationService;

    /**
     * GET /api/lost-items/{id}/recovery-strategy
     *
     * Returns a full smart recovery plan for the given lost item.
     * Access is scoped: ADMIN/DOCTOR see all, CAREGIVER must be assigned,
     * PATIENT must own the item.
     */
    @GetMapping("/{id}/recovery-strategy")
    public ResponseEntity<Map<String, Object>> getRecoveryStrategy(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id",   required = false) Long userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        authorizationService.checkItemAccess(id, userId, userRole);
        return ResponseEntity.ok(recoveryStrategyService.getRecoveryStrategy(id));
    }
}
