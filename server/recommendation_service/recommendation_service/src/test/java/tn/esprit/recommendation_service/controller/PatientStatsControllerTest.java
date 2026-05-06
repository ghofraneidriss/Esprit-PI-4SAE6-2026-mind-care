package tn.esprit.recommendation_service.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.recommendation_service.dto.stats.PatientStatsResponse;
import tn.esprit.recommendation_service.service.PatientStatsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientStatsControllerTest {

    @Mock
    private PatientStatsService patientStatsService;

    @InjectMocks
    private PatientStatsController controller;

    @Test
    void getByPatient_shouldReturnResponseFromService() {
        PatientStatsResponse response = PatientStatsResponse.builder()
                .patientId(7L)
                .totalSessions(4)
                .completedSessions(3)
                .build();
        when(patientStatsService.getByPatient(7L)).thenReturn(response);

        assertThat(controller.getByPatient(7L).getBody()).isEqualTo(response);
    }
}
