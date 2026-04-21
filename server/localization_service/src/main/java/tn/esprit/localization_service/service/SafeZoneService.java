package tn.esprit.localization_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.localization_service.entity.SafeZone;
import tn.esprit.localization_service.repository.SafeZoneRepository;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SafeZoneService {

    private final SafeZoneRepository safeZoneRepository;

    @Transactional
    public SafeZone createSafeZone(SafeZone safeZone) {
        if (safeZone.isHomeReference()) {
            clearHomeReferenceForPatientExcept(safeZone.getPatientId(), null);
        }
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

    public List<Long> getPatientIdsWithSafeZones() {
        return safeZoneRepository.findDistinctPatientIds();
    }

    @Transactional
    public SafeZone updateSafeZone(Long id, SafeZone safeZoneDetails) {
        return safeZoneRepository.findById(id).map(safeZone -> {
            if (safeZoneDetails.isHomeReference()) {
                clearHomeReferenceForPatientExcept(safeZoneDetails.getPatientId(), id);
            }
            safeZone.setName(safeZoneDetails.getName());
            safeZone.setCenterLatitude(safeZoneDetails.getCenterLatitude());
            safeZone.setCenterLongitude(safeZoneDetails.getCenterLongitude());
            safeZone.setRadius(safeZoneDetails.getRadius());
            safeZone.setPatientId(safeZoneDetails.getPatientId());
            safeZone.setHomeReference(safeZoneDetails.isHomeReference());
            return safeZoneRepository.save(safeZone);
        }).orElseThrow(() -> new RuntimeException("SafeZone not found with id " + id));
    }

    /** At most one zone per patient may be the registered home. */
    private void clearHomeReferenceForPatientExcept(Long patientId, Long excludeZoneId) {
        if (patientId == null) {
            return;
        }
        for (SafeZone z : safeZoneRepository.findByPatientId(patientId)) {
            if (excludeZoneId != null && excludeZoneId.equals(z.getId())) {
                continue;
            }
            if (z.isHomeReference()) {
                z.setHomeReference(false);
                safeZoneRepository.save(z);
            }
        }
    }

    public void deleteSafeZone(Long id) {
        safeZoneRepository.deleteById(id);
    }
}
