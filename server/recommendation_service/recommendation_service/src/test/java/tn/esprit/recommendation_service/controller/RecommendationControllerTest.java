package tn.esprit.recommendation_service.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tn.esprit.recommendation_service.dto.recommendation.AutoRecommendationGenerateRequest;
import tn.esprit.recommendation_service.dto.recommendation.ClinicalEscalationAlertResponse;
import tn.esprit.recommendation_service.dto.recommendation.RecommendationCreateRequest;
import tn.esprit.recommendation_service.dto.recommendation.RecommendationResponse;
import tn.esprit.recommendation_service.dto.recommendation.RecommendationStatsResponse;
import tn.esprit.recommendation_service.dto.recommendation.RecommendationStatusUpdateRequest;
import tn.esprit.recommendation_service.dto.recommendation.RecommendationUpdateRequest;
import tn.esprit.recommendation_service.enums.AlertStatus;
import tn.esprit.recommendation_service.enums.RecommendationStatus;
import tn.esprit.recommendation_service.enums.RecommendationType;
import tn.esprit.recommendation_service.service.RecommendationService;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationControllerTest {

    @Mock
    private RecommendationService recommendationService;

    @InjectMocks
    private RecommendationController controller;

    @Test
    void shouldDelegateAllEndpoints() {
        RecommendationCreateRequest createRequest = RecommendationCreateRequest.builder()
                .content("Hydratation")
                .type(RecommendationType.DIET)
                .doctorId(1L)
                .patientId(2L)
                .expirationDate(LocalDate.now().plusDays(2))
                .build();
        RecommendationStatusUpdateRequest statusRequest = RecommendationStatusUpdateRequest.builder()
                .status(RecommendationStatus.ACCEPTED)
                .build();
        RecommendationUpdateRequest updateRequest = RecommendationUpdateRequest.builder()
                .content("Marche")
                .type(RecommendationType.EXERCISE)
                .priority(3)
                .expirationDate(LocalDate.now().plusDays(3))
                .build();
        AutoRecommendationGenerateRequest autoRequest = AutoRecommendationGenerateRequest.builder()
                .patientId(2L)
                .doctorId(1L)
                .age(72)
                .weeklyFrequency(2)
                .acceptedCount(3)
                .rejectedCount(1)
                .lowPhysicalActivity(true)
                .level(tn.esprit.recommendation_service.enums.PatientLevel.MEDIUM)
                .build();

        RecommendationResponse response = RecommendationResponse.builder()
                .id(10L)
                .content("Hydratation")
                .status(RecommendationStatus.ACTIVE)
                .type(RecommendationType.DIET)
                .patientId(2L)
                .build();
        RecommendationStatsResponse stats = RecommendationStatsResponse.builder()
                .acceptedCount(3L)
                .rejectedCount(1L)
                .build();
        ClinicalEscalationAlertResponse alert = ClinicalEscalationAlertResponse.builder()
                .id(20L)
                .doctorId(1L)
                .patientId(2L)
                .status(AlertStatus.RESOLVED)
                .build();

        when(recommendationService.createRecommendation(createRequest)).thenReturn(response);
        when(recommendationService.getAllRecommendations()).thenReturn(List.of(response));
        when(recommendationService.searchRecommendations("Hyd")).thenReturn(List.of(response));
        when(recommendationService.getRecommendationById(10L)).thenReturn(response);
        when(recommendationService.getRecommendationsByPatient(2L)).thenReturn(List.of(response));
        when(recommendationService.generateAutomaticRecommendations(autoRequest)).thenReturn(List.of(response));
        when(recommendationService.getActiveRecommendationsByPatient(2L)).thenReturn(List.of(response));
        when(recommendationService.getRecommendationsSortedByPriorityAndCreatedAt(2L)).thenReturn(List.of(response));
        when(recommendationService.countAcceptedVsRejectedByPatient(2L)).thenReturn(stats);
        when(recommendationService.getAlertsByDoctor(1L)).thenReturn(List.of(alert));
        when(recommendationService.resolveAlert(20L)).thenReturn(alert);
        when(recommendationService.updateRecommendationStatus(10L, statusRequest)).thenReturn(response);
        when(recommendationService.updateRecommendationDetails(10L, updateRequest)).thenReturn(response);
        when(recommendationService.acceptRecommendation(10L)).thenReturn(response);
        when(recommendationService.dismissRecommendation(10L)).thenReturn(response);
        when(recommendationService.archiveExpiredRecommendations()).thenReturn(4);

        ResponseEntity<RecommendationResponse> created = controller.create(createRequest);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getBody()).isEqualTo(response);

        assertThat(controller.getAll().getBody()).containsExactly(response);
        assertThat(controller.search("Hyd").getBody()).containsExactly(response);
        assertThat(controller.getById(10L).getBody()).isEqualTo(response);
        assertThat(controller.getByPatient(2L).getBody()).containsExactly(response);
        assertThat(controller.autoGenerate(autoRequest).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.getActiveByPatient(2L).getBody()).containsExactly(response);
        assertThat(controller.getSortedByPatient(2L).getBody()).containsExactly(response);
        assertThat(controller.getStatsByPatient(2L).getBody()).isEqualTo(stats);
        assertThat(controller.getDoctorAlerts(1L).getBody()).containsExactly(alert);
        assertThat(controller.resolveAlert(20L).getBody()).isEqualTo(alert);
        assertThat(controller.updateStatus(10L, statusRequest).getBody()).isEqualTo(response);
        assertThat(controller.updateDetails(10L, updateRequest).getBody()).isEqualTo(response);
        assertThat(controller.patchStatus(10L, statusRequest).getBody()).isEqualTo(response);
        assertThat(controller.approve(10L).getBody()).isEqualTo(response);
        assertThat(controller.accept(10L).getBody()).isEqualTo(response);
        assertThat(controller.dismiss(10L).getBody()).isEqualTo(response);
        assertThat(controller.archiveExpired().getBody()).isEqualTo(4);
        assertThat(controller.delete(10L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        verify(recommendationService).deleteRecommendation(10L);
    }
}
