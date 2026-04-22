package tn.esprit.movement_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.movement_service.dto.LocationReportRequest;
import tn.esprit.movement_service.entity.LocationPing;
import tn.esprit.movement_service.service.MovementMonitoringService;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {

    private final MovementMonitoringService movementMonitoringService;

    @PostMapping("/report")
    public ResponseEntity<LocationPing> reportLocation(@RequestBody LocationReportRequest request) {
        return ResponseEntity.ok(movementMonitoringService.reportLocation(request));
    }

    @GetMapping("/patient/{patientId}/latest")
    public ResponseEntity<LocationPing> getLatest(@PathVariable Long patientId) {
        return movementMonitoringService.getLatestLocation(patientId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/patient/{patientId}/history")
    public ResponseEntity<List<LocationPing>> getHistory(
            @PathVariable Long patientId,
            @RequestParam(defaultValue = "180") int minutes
    ) {
        return ResponseEntity.ok(movementMonitoringService.getPatientHistory(patientId, minutes));
    }
}
