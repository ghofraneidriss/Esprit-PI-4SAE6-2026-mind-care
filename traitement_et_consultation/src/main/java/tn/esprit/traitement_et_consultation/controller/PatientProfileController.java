package tn.esprit.traitement_et_consultation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.traitement_et_consultation.entity.PatientProfile;
import tn.esprit.traitement_et_consultation.service.PatientProfileService;

import java.util.List;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
@CrossOrigin("*")
public class PatientProfileController {

    private final PatientProfileService patientProfileService;

    @PostMapping
    public ResponseEntity<PatientProfile> createProfile(@RequestBody PatientProfile profile) {
        try {
            return ResponseEntity.ok(patientProfileService.saveProfile(profile));
        } catch (IllegalStateException e) {
            // 409 Conflict: a profile already exists for this patient
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<PatientProfile> getProfileByUserId(@PathVariable(name = "userId") Long userId) {
        return patientProfileService.getProfileByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/email/{email:.+}")
    public ResponseEntity<PatientProfile> getProfileByEmail(@PathVariable(name = "email") String email) {
        return patientProfileService.getProfileByEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<PatientProfile>> getAllProfiles() {
        return ResponseEntity.ok(patientProfileService.getAllProfiles());
    }

    @PutMapping("/{id}")
    public ResponseEntity<PatientProfile> updateProfile(@PathVariable(name = "id") Long id,
            @RequestBody PatientProfile profile) {
        try {
            return ResponseEntity.ok(patientProfileService.updateProfile(id, profile));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProfile(@PathVariable(name = "id") Long id) {
        try {
            patientProfileService.deleteProfile(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * [JPQL] Vérifie si un patient a une allergie connue à un médicament donné
     * ou à sa famille thérapeutique.
     *
     * GET /api/profiles/{userId}/check-allergy?medicineName=Penicillin&therapeuticFamily=Antibiotics
     *
     * @return Liste des allergies correspondantes (vide = aucune allergie détectée)
     */
    @GetMapping("/{userId}/check-allergy")
    public ResponseEntity<List<String>> checkAllergy(
            @PathVariable(name = "userId") Long userId,
            @RequestParam(name = "medicineName", defaultValue = "") String medicineName,
            @RequestParam(name = "therapeuticFamily", defaultValue = "") String therapeuticFamily) {
        List<String> matches = patientProfileService.checkAllergyForMedicine(userId, medicineName, therapeuticFamily);
        return ResponseEntity.ok(matches);
    }

    @GetMapping("/degrading")
    public ResponseEntity<List<PatientProfile>> getPatientsWithRapidDegradation(
            @RequestParam("treatment") String treatment,
            @RequestParam("threshold") Integer threshold) {
        System.out.println("API Request: /degrading?treatment=" + treatment + "&threshold=" + threshold);
        List<PatientProfile> results = patientProfileService.getPatientsWithRapidDegradation(treatment, threshold);
        System.out.println("Results found: " + results.size());
        return ResponseEntity.ok(results);
    }

    @GetMapping("/severe-no-followup")
    public ResponseEntity<List<PatientProfile>> getSeverePatientsWithoutFollowUp(
            @RequestParam(value = "months", defaultValue = "3") int monthsAgo) {
        System.out.println("API Request: /severe-no-followup?months=" + monthsAgo);
        List<PatientProfile> results = patientProfileService.getSeverePatientsWithoutFollowUp(monthsAgo);
        System.out.println("Results found: " + results.size());
        return ResponseEntity.ok(results);
    }
}
