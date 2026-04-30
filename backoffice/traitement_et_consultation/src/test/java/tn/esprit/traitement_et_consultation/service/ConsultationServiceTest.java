package tn.esprit.traitement_et_consultation.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.traitement_et_consultation.entity.AlzheimerStage;
import tn.esprit.traitement_et_consultation.entity.Consultation;
import tn.esprit.traitement_et_consultation.repository.ConsultationRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
