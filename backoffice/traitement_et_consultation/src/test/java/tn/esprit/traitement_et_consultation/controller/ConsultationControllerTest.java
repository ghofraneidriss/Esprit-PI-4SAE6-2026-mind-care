package tn.esprit.traitement_et_consultation.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import tn.esprit.traitement_et_consultation.entity.Consultation;
import tn.esprit.traitement_et_consultation.service.ConsultationService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultationControllerTest {

    @Mock
    private ConsultationService consultationService;

    @InjectMocks
    private ConsultationController consultationController;

    @Test
    void createConsultationReturnsOkOrBadRequest() {
        Consultation consultation = Consultation.builder().appointmentId(1L).build();
        when(consultationService.saveConsultation(consultation)).thenReturn(consultation);

        assertEquals(HttpStatus.OK, consultationController.createConsultation(consultation).getStatusCode());

        when(consultationService.saveConsultation(consultation)).thenThrow(new IllegalStateException("duplicate"));
        assertEquals(HttpStatus.BAD_REQUEST, consultationController.createConsultation(consultation).getStatusCode());
    }

    @Test
    void readEndpointsReturnDataOrNotFound() {
        Consultation consultation = Consultation.builder().id(8L).build();
        when(consultationService.getAllConsultations()).thenReturn(List.of(consultation));
        when(consultationService.getFilteredConsultations("mild", "term")).thenReturn(List.of(consultation));
        when(consultationService.getConsultationById(8L)).thenReturn(Optional.of(consultation));
        when(consultationService.getConsultationById(9L)).thenReturn(Optional.empty());

        assertEquals(1, consultationController.getAll().getBody().size());
        assertEquals(1, consultationController.getFilteredConsultations("mild", "term").getBody().size());
        assertEquals(HttpStatus.OK, consultationController.getById(8L).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, consultationController.getById(9L).getStatusCode());
    }

    @Test
    void updateAndDeleteEndpointsWork() {
        Consultation consultation = Consultation.builder().id(8L).build();
        when(consultationService.updateConsultation(8L, consultation)).thenReturn(consultation);

        assertEquals(HttpStatus.OK, consultationController.update(8L, consultation).getStatusCode());

        when(consultationService.updateConsultation(8L, consultation)).thenThrow(new IllegalStateException("duplicate"));
        assertEquals(HttpStatus.BAD_REQUEST, consultationController.update(8L, consultation).getStatusCode());

        assertEquals(HttpStatus.NO_CONTENT, consultationController.delete(8L).getStatusCode());
        verify(consultationService).deleteConsultation(8L);
    }
}
