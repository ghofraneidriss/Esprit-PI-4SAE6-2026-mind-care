package tn.esprit.traitement_et_consultation.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tn.esprit.traitement_et_consultation.entity.*;
import tn.esprit.traitement_et_consultation.repository.*;

import java.time.LocalDateTime;

/**
 * Ce composant initialise la base de données avec des données de test réalistes
 * pour valider le fonctionnement du tableau de bord "Status Tracking".
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataPopulator implements org.springframework.boot.CommandLineRunner {

        private final PatientProfileRepository patientProfileRepository;
        private final AppointmentRepository appointmentRepository;
        private final ConsultationRepository consultationRepository;

        @Override
        public void run(String... args) {
                log.info("VÉRIFICATION DES DONNÉES DE TEST...");

                // On nettoie spécifiquement nos IDs de test pour pouvoir ré-exécuter si besoin
                java.util.List<Long> testUserIds = java.util.Arrays.asList(101L, 102L, 103L, 104L);

                for (Long uid : testUserIds) {
                        patientProfileRepository.findByUserId(uid).ifPresent(p -> {
                                log.info("Nettoyage des anciennes données pour le userId: {}", uid);
                                // On supprime les consultations et RDV liés
                                java.util.List<Appointment> appts = appointmentRepository.findByPatientId(uid);
                                for (Appointment a : appts) {
                                        consultationRepository.findByAppointmentId(a.getId())
                                                        .ifPresent(consultationRepository::delete);
                                        appointmentRepository.delete(a);
                                }
                                patientProfileRepository.delete(p);
                        });
                }

                log.info("INSERTION DES DONNÉES DE TEST POUR STATUS TRACKING...");

                // --- PATIENT 1 : JEAN DUPONT (DÉGRADATION RAPIDE) ---
                PatientProfile p1 = PatientProfile.builder()
                                .userId(101L)
                                .patientName("Jean Dupont")
                                .email("jean.dupont@test.com")
                                .dateOfBirth(java.time.LocalDate.of(1950, 5, 15))
                                .medications("Donepezil")
                                .externalCognitiveScore(18.0)
                                .hypertension(true)
                                .build();
                patientProfileRepository.save(p1);

                // --- PATIENT 2 : MARIE CURIE (SEVERE SANS SUIVI) ---
                PatientProfile p2 = PatientProfile.builder()
                                .userId(102L)
                                .patientName("Marie Curie")
                                .email("marie.curie@test.com")
                                .dateOfBirth(java.time.LocalDate.of(1945, 11, 7))
                                .medications("Memantine")
                                .externalCognitiveScore(8.0)
                                .familyHistoryAlzheimer(true)
                                .build();
                patientProfileRepository.save(p2);

                // --- PATIENT 3 : ALBERT EINSTEIN (STABLE) ---
                PatientProfile p3 = PatientProfile.builder()
                                .userId(103L)
                                .patientName("Albert Einstein")
                                .email("albert.einstein@test.com")
                                .dateOfBirth(java.time.LocalDate.of(1955, 3, 14))
                                .externalCognitiveScore(28.0)
                                .build();
                patientProfileRepository.save(p3);

                // --- PATIENT 4 : ISAAC NEWTON (SEVERE AVEC RDV PRÉVU) ---
                PatientProfile p4 = PatientProfile.builder()
                                .userId(104L)
                                .patientName("Isaac Newton")
                                .email("isaac.newton@test.com")
                                .dateOfBirth(java.time.LocalDate.of(1948, 1, 4))
                                .externalCognitiveScore(12.0)
                                .build();
                patientProfileRepository.save(p4);

                // --- CRÉATION DE L'HISTORIQUE (RDV + CONSULTATIONS) ---

                // Jean Dupont : 2 RDV, chute de score MMSE (25 -> 18)
                Appointment a1_old = createConfirmedAppt(101L, LocalDateTime.now().minusMonths(4));
                consultationRepository.save(Consultation.builder()
                                .appointmentId(a1_old.getId())
                                .mmseScore(25)
                                .alzheimerStage(AlzheimerStage.MILD)
                                .clinicalNotes("État stable initial.")
                                .build());

                Appointment a1_new = createConfirmedAppt(101L, LocalDateTime.now().minusWeeks(1));
                consultationRepository.save(Consultation.builder()
                                .appointmentId(a1_new.getId())
                                .mmseScore(18)
                                .alzheimerStage(AlzheimerStage.MODERATE)
                                .clinicalNotes("Baisse cognitive notable (Alerte Dégradation).")
                                .build());

                // Marie Curie : 1 RDV ancien, stade SEVERE, pas de futur RDV
                Appointment a2 = createConfirmedAppt(102L, LocalDateTime.now().minusMonths(5));
                consultationRepository.save(Consultation.builder()
                                .appointmentId(a2.getId())
                                .mmseScore(8)
                                .alzheimerStage(AlzheimerStage.SEVERE)
                                .clinicalNotes("Suivi critique nécessaire (Alerte Suivi Manquant).")
                                .build());

                // Albert Einstein : Suivi normal
                createConfirmedAppt(103L, LocalDateTime.now().minusMonths(1));

                // Isaac Newton : Stade sévère mais possède un RDV futur (ne doit pas être
                // alerté)
                Appointment a4_old = createConfirmedAppt(104L, LocalDateTime.now().minusMonths(6));
                consultationRepository.save(Consultation.builder()
                                .appointmentId(a4_old.getId())
                                .mmseScore(12)
                                .alzheimerStage(AlzheimerStage.SEVERE)
                                .build());

                // Futur RDV pour Newton
                appointmentRepository.save(Appointment.builder()
                                .patientId(104L)
                                .doctorId(1L)
                                .appointmentDate(LocalDateTime.now().plusDays(10))
                                .appointmentEndDate(LocalDateTime.now().plusDays(10).plusMinutes(35))
                                .status(AppointmentStatus.PENDING)
                                .type(AppointmentType.IN_PERSON)
                                .build());

                log.info("DONNÉES DE TEST INJECTÉES AVEC SUCCÈS.");
        }

        private Appointment createConfirmedAppt(Long patientId, LocalDateTime date) {
                Appointment appt = Appointment.builder()
                                .patientId(patientId)
                                .doctorId(1L)
                                .appointmentDate(date)
                                .appointmentEndDate(date.plusMinutes(35))
                                .status(AppointmentStatus.CONFIRMED)
                                .type(AppointmentType.IN_PERSON)
                                .build();
                return appointmentRepository.save(appt);
        }
}
