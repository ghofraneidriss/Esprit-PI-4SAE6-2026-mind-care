package tn.esprit.users_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.users_service.entity.Appointment;
import tn.esprit.users_service.repository.AppointmentRepository;

import java.util.List;

@RestController
@RequestMapping("/api/users/appointments")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4210"})
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentRepository appointmentRepository;

    @GetMapping
    public ResponseEntity<List<Appointment>> getAll() {
        return ResponseEntity.ok(appointmentRepository.findAllByOrderByStartDateAsc());
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<Appointment>> getByPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(appointmentRepository.findByPatientIdOrderByStartDateAsc(patientId));
    }

    @GetMapping("/caregiver/{caregiverId}")
    public ResponseEntity<List<Appointment>> getByCaregiver(@PathVariable Long caregiverId) {
        return ResponseEntity.ok(appointmentRepository.findByCaregiverIdOrderByStartDateAsc(caregiverId));
    }

    @PostMapping
    public ResponseEntity<Appointment> create(@RequestBody Appointment appointment) {
        if (appointment.getColor() == null) appointment.setColor("primary");
        return new ResponseEntity<>(appointmentRepository.save(appointment), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Appointment> update(@PathVariable Long id, @RequestBody Appointment updated) {
        return appointmentRepository.findById(id).map(existing -> {
            existing.setTitle(updated.getTitle());
            existing.setDescription(updated.getDescription());
            existing.setStartDate(updated.getStartDate());
            existing.setEndDate(updated.getEndDate());
            existing.setType(updated.getType());
            existing.setColor(updated.getColor());
            if (updated.getPatientId() != null) existing.setPatientId(updated.getPatientId());
            if (updated.getCaregiverId() != null) existing.setCaregiverId(updated.getCaregiverId());
            return ResponseEntity.ok(appointmentRepository.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        appointmentRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
