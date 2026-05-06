package tn.esprit.traitement_et_consultation.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.traitement_et_consultation.entity.AlzheimerStage;
import tn.esprit.traitement_et_consultation.entity.Consultation;
import tn.esprit.traitement_et_consultation.repository.ConsultationRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultationServiceTest {

    @Mock
    private ConsultationRepository consultationRepository;

    @InjectMocks
    private ConsultationService consultationService;

    @Test
    void suggestAlzheimerStageMapsMmseScores() {
        assertEquals(AlzheimerStage.PRECLINICAL, consultationService.suggestAlzheimerStage(28));
        assertEquals(AlzheimerStage.MILD, consultationService.suggestAlzheimerStage(22));
        assertEquals(AlzheimerStage.MODERATE, consultationService.suggestAlzheimerStage(15));
        assertEquals(AlzheimerStage.SEVERE, consultationService.suggestAlzheimerStage(8));
        assertNull(consultationService.suggestAlzheimerStage(null));
    }

    @Test
    void saveConsultationAssignsDerivedStage() {
        Consultation consultation = Consultation.builder()
                .appointmentId(10L)
                .mmseScore(22)
                .build();

        when(consultationRepository.findByAppointmentId(10L)).thenReturn(Optional.empty());
        when(consultationRepository.save(consultation)).thenReturn(consultation);

        Consultation saved = consultationService.saveConsultation(consultation);

        assertEquals(AlzheimerStage.MILD, saved.getAlzheimerStage());
        verify(consultationRepository).save(consultation);
    }

    @Test
    void saveConsultationRejectsDuplicateAppointment() {
        Consultation consultation = Consultation.builder()
                .appointmentId(10L)
                .mmseScore(22)
                .build();
        Consultation existing = Consultation.builder()
                .id(99L)
                .appointmentId(10L)
                .build();

        when(consultationRepository.findByAppointmentId(10L)).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class, () -> consultationService.saveConsultation(consultation));
        verify(consultationRepository, never()).save(consultation);
    }

    @Test
    void getFilteredConsultationsFiltersByStageAndSearchTerm() {
        Consultation mild = Consultation.builder()
                .appointmentId(10L)
                .clinicalNotes("memory loss follow-up")
                .bloodPressure("12/8")
                .alzheimerStage(AlzheimerStage.MILD)
                .build();
        Consultation severe = Consultation.builder()
                .appointmentId(22L)
                .clinicalNotes("advanced monitoring")
                .bloodPressure("13/9")
                .alzheimerStage(AlzheimerStage.SEVERE)
                .build();

        when(consultationRepository.findAll()).thenReturn(List.of(mild, severe));

        List<Consultation> filtered = consultationService.getFilteredConsultations("mild", "memory");

        assertEquals(1, filtered.size());
        assertSame(mild, filtered.get(0));
    }

    @Test
    void updateConsultationRecomputesStageWhenAppointmentStaysSame() {
        Consultation existing = Consultation.builder()
                .id(3L)
                .appointmentId(12L)
                .clinicalNotes("before")
                .mmseScore(25)
                .alzheimerStage(AlzheimerStage.MILD)
                .build();
        Consultation details = Consultation.builder()
                .appointmentId(12L)
                .clinicalNotes("updated")
                .currentWeight(70.0)
                .bloodPressure("11/7")
                .mmseScore(9)
                .build();

        when(consultationRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(consultationRepository.save(existing)).thenReturn(existing);

        Consultation updated = consultationService.updateConsultation(3L, details);

        assertEquals("updated", updated.getClinicalNotes());
        assertEquals(70.0, updated.getCurrentWeight());
        assertEquals("11/7", updated.getBloodPressure());
        assertEquals(9, updated.getMmseScore());
        assertEquals(AlzheimerStage.SEVERE, updated.getAlzheimerStage());
    }

    @Test
    void updateConsultationRejectsDuplicateNewAppointment() {
        Consultation existing = Consultation.builder()
                .id(3L)
                .appointmentId(12L)
                .build();
        Consultation details = Consultation.builder()
                .appointmentId(90L)
                .build();
        Consultation duplicate = Consultation.builder()
                .id(4L)
                .appointmentId(90L)
                .build();

        when(consultationRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(consultationRepository.findByAppointmentId(90L)).thenReturn(Optional.of(duplicate));

        assertThrows(IllegalStateException.class, () -> consultationService.updateConsultation(3L, details));
        verify(consultationRepository, never()).save(existing);
    }

    @Test
    void updateConsultationThrowsWhenMissing() {
        when(consultationRepository.findById(500L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> consultationService.updateConsultation(500L, Consultation.builder().appointmentId(9L).build()));
    }

    @Test
    void basicRepositoryDelegationMethodsWork() {
        Consultation consultation = Consultation.builder().id(7L).appointmentId(44L).build();
        when(consultationRepository.findAll()).thenReturn(List.of(consultation));
        when(consultationRepository.findById(7L)).thenReturn(Optional.of(consultation));

        assertEquals(1, consultationService.getAllConsultations().size());
        assertEquals(Optional.of(consultation), consultationService.getConsultationById(7L));

        consultationService.deleteConsultation(7L);
        verify(consultationRepository).deleteById(7L);
    }
}
