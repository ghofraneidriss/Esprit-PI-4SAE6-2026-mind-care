package tn.esprit.movement_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.movement_service.entity.SafeZone;
import tn.esprit.movement_service.repository.SafeZoneRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SafeZoneService {

    private final SafeZoneRepository safeZoneRepository;

    public SafeZone createSafeZone(SafeZone safeZone) {
        return safeZoneRepository.save(safeZone);
    }

    public List<SafeZone> getAllSafeZones() {
        return safeZoneRepository.findAll();
    }

    public Optional<SafeZone> getSafeZoneById(Long id) {
        return safeZoneRepository.findById(id);
    }

    public List<SafeZone> getSafeZonesByPatientId(Long patientId) {
        return safeZoneRepository.findByPatientId(patientId);
    }

    public SafeZone updateSafeZone(Long id, SafeZone safeZoneDetails) {
        return safeZoneRepository.findById(id).map(safeZone -> {
            safeZone.setName(safeZoneDetails.getName());
            safeZone.setCenterLatitude(safeZoneDetails.getCenterLatitude());
            safeZone.setCenterLongitude(safeZoneDetails.getCenterLongitude());
            safeZone.setRadius(safeZoneDetails.getRadius());
            safeZone.setPatientId(safeZoneDetails.getPatientId());
            return safeZoneRepository.save(safeZone);
        }).orElseThrow(() -> new RuntimeException("SafeZone not found with id " + id));
    }

    public void deleteSafeZone(Long id) {
        safeZoneRepository.deleteById(id);
    }
}
