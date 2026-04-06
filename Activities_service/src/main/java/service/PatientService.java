package tn.esprit.users_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.users_service.entity.Patient;
import tn.esprit.users_service.repository.PatientRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;

    public Patient createPatient(Patient patient) {
        return patientRepository.save(patient);
    }

    public List<Patient> getAllPatients() {
        return patientRepository.findAll();
    }

    public Optional<Patient> getPatientById(Long id) {
        return patientRepository.findById(id);
    }

    public Patient updatePatient(Long id, Patient patientDetails) {
        return patientRepository.findById(id).map(patient -> {
            patient.setDateOfBirth(patientDetails.getDateOfBirth());
            patient.setAddress(patientDetails.getAddress());
            patient.setEmergencyContact(patientDetails.getEmergencyContact());
            patient.setMedicalHistory(patientDetails.getMedicalHistory());
            patient.setUser(patientDetails.getUser());
            return patientRepository.save(patient);
        }).orElseThrow(() -> new RuntimeException("Patient not found with id " + id));
    }

    public void deletePatient(Long id) {
        patientRepository.deleteById(id);
    }
}
