package tn.esprit.recommendation_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.recommendation_service.dto.recommendation.RecommendationCreateRequest;
import tn.esprit.recommendation_service.dto.recommendation.RecommendationResponse;
import tn.esprit.recommendation_service.dto.recommendation.RecommendationStatusUpdateRequest;
import tn.esprit.recommendation_service.entity.Recommendation;
import tn.esprit.recommendation_service.enums.RecommendationStatus;
import tn.esprit.recommendation_service.enums.RecommendationType;
import tn.esprit.recommendation_service.exception.ResourceNotFoundException;
import tn.esprit.recommendation_service.repository.ClinicalEscalationAlertRepository;
import tn.esprit.recommendation_service.repository.MedicalEventRepository;
import tn.esprit.recommendation_service.repository.RecommendationRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private RecommendationRepository recommendationRepository;

    @Mock
    private MedicalEventRepository medicalEventRepository;

    @Mock
    private ClinicalEscalationAlertRepository clinicalEscalationAlertRepository;

    @InjectMocks
    private RecommendationService recommendationService;

    private Recommendation recommendation;

    @BeforeEach
    void setUp() {
        recommendation = Recommendation.builder()
                .id(1L)
                .content("Faire une promenade quotidienne")
                .type(RecommendationType.EXERCISE)
                .status(RecommendationStatus.ACTIVE)
                .doctorId(5L)
                .patientId(10L)
                .priority(3)
                .dismissed(false)
                .rejectionCount(0)
                .build();
    }

    @Test
    void createRecommendation_shouldReturnResponse_whenValidRequest() {
        RecommendationCreateRequest request = RecommendationCreateRequest.builder()
                .content("Faire une promenade quotidienne")
                .type(RecommendationType.EXERCISE)
                .doctorId(5L)
                .patientId(10L)
                .priority(3)
                .build();

        when(recommendationRepository.save(any(Recommendation.class))).thenReturn(recommendation);

        RecommendationResponse response = recommendationService.createRecommendation(request);

        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEqualTo("Faire une promenade quotidienne");
        assertThat(response.getPatientId()).isEqualTo(10L);
        verify(recommendationRepository, times(1)).save(any(Recommendation.class));
    }

    @Test
    void createRecommendation_shouldThrow_whenExpirationDateInPast() {
        RecommendationCreateRequest request = RecommendationCreateRequest.builder()
                .content("Contenu")
                .type(RecommendationType.EXERCISE)
                .doctorId(5L)
                .patientId(10L)
                .expirationDate(LocalDate.now().minusDays(1))
                .build();

        assertThatThrownBy(() -> recommendationService.createRecommendation(request))
                .isInstanceOf(Exception.class);
    }

    @Test
    void getRecommendationById_shouldReturnResponse_whenExists() {
        when(recommendationRepository.findById(1L)).thenReturn(Optional.of(recommendation));

        RecommendationResponse response = recommendationService.getRecommendationById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    void getRecommendationById_shouldThrow_whenNotFound() {
        when(recommendationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recommendationService.getRecommendationById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getRecommendationsByPatient_shouldReturnList() {
        when(recommendationRepository.findByPatientId(10L))
                .thenReturn(List.of(recommendation));

        List<RecommendationResponse> responses = recommendationService.getRecommendationsByPatient(10L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getPatientId()).isEqualTo(10L);
    }

    @Test
    void acceptRecommendation_shouldSetStatusAccepted() {
        when(recommendationRepository.findById(1L)).thenReturn(Optional.of(recommendation));
        when(recommendationRepository.save(any(Recommendation.class))).thenReturn(recommendation);

        RecommendationResponse response = recommendationService.acceptRecommendation(1L);

        assertThat(response).isNotNull();
        verify(recommendationRepository, times(1)).save(recommendation);
    }

    @Test
    void dismissRecommendation_shouldSetDismissedTrue() {
        when(recommendationRepository.findById(1L)).thenReturn(Optional.of(recommendation));
        when(recommendationRepository.save(any(Recommendation.class))).thenReturn(recommendation);

        RecommendationResponse response = recommendationService.dismissRecommendation(1L);

        assertThat(response).isNotNull();
        verify(recommendationRepository, times(1)).save(recommendation);
    }

    @Test
    void deleteRecommendation_shouldCallDelete_whenExists() {
        when(recommendationRepository.findById(1L)).thenReturn(Optional.of(recommendation));

        recommendationService.deleteRecommendation(1L);

        verify(recommendationRepository, times(1)).delete(recommendation);
    }

    @Test
    void archiveExpiredRecommendations_shouldReturnCount() {
        when(recommendationRepository.archiveExpiredRecommendations(any(LocalDate.class))).thenReturn(3);

        int count = recommendationService.archiveExpiredRecommendations();

        assertThat(count).isEqualTo(3);
    }
}
