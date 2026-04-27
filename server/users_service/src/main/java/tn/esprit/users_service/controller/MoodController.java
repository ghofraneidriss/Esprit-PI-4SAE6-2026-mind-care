package tn.esprit.users_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.users_service.entity.MoodEntry;
import tn.esprit.users_service.repository.MoodEntryRepository;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/users/mood")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4210"})
@RequiredArgsConstructor
public class MoodController {

    private final MoodEntryRepository moodEntryRepository;

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<MoodEntry>> getByPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(moodEntryRepository.findByPatientIdOrderByDateDesc(patientId));
    }

    @GetMapping("/patient/{patientId}/range")
    public ResponseEntity<List<MoodEntry>> getByPatientRange(
            @PathVariable Long patientId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(
                moodEntryRepository.findByPatientIdAndDateBetweenOrderByDateAsc(patientId, start, end));
    }

    @GetMapping("/caregiver/{caregiverId}")
    public ResponseEntity<List<MoodEntry>> getByCaregiver(@PathVariable Long caregiverId) {
        return ResponseEntity.ok(moodEntryRepository.findByCaregiverIdOrderByDateDesc(caregiverId));
    }

    @PostMapping
    public ResponseEntity<MoodEntry> create(@RequestBody MoodEntry entry) {
        if (entry.getDate() == null) entry.setDate(LocalDate.now());
        return new ResponseEntity<>(moodEntryRepository.save(entry), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        moodEntryRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
