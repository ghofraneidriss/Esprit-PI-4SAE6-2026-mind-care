package tn.esprit.souvenir_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.souvenir_service.dto.entree.EntreeSouvenirCreateRequest;
import tn.esprit.souvenir_service.dto.entree.EntreeSouvenirResponse;
import tn.esprit.souvenir_service.dto.entree.EntreeSouvenirUpdateRequest;
import tn.esprit.souvenir_service.dto.entree.VoiceRecognitionUpdateRequest;
import tn.esprit.souvenir_service.entity.EntreeSouvenir;
import tn.esprit.souvenir_service.enums.MediaType;
import tn.esprit.souvenir_service.enums.ThemeCulturel;
import tn.esprit.souvenir_service.exception.BusinessException;
import tn.esprit.souvenir_service.exception.ResourceNotFoundException;
import tn.esprit.souvenir_service.repository.EntreeSouvenirRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntreeSouvenirServiceTest {

    @Mock
    private EntreeSouvenirRepository entreeSouvenirRepository;

    @InjectMocks
    private EntreeSouvenirService entreeSouvenirService;

    private EntreeSouvenir entree;

    @BeforeEach
    void setUp() {
        entree = EntreeSouvenir.builder()
                .id(1L)
                .patientId(10L)
                .doctorId(5L)
                .texte("Souvenir de famille")
                .mediaType(MediaType.IMAGE)
                .mediaUrl("http://example.com/image.jpg")
                .themeCulturel(ThemeCulturel.FAMILLE)
                .traitee(false)
                .voiceRecognized(false)
                .importance(5)
                .build();
    }

    @Test
    void createEntree_shouldReturnResponse_whenValidRequest() {
        EntreeSouvenirCreateRequest request = new EntreeSouvenirCreateRequest();
        request.setPatientId(10L);
        request.setDoctorId(5L);
        request.setTexte("Souvenir de famille");
        request.setMediaType(MediaType.IMAGE);
        request.setMediaUrl("http://example.com/image.jpg");
        request.setThemeCulturel(ThemeCulturel.FAMILLE);

        when(entreeSouvenirRepository.save(any(EntreeSouvenir.class))).thenReturn(entree);

        EntreeSouvenirResponse response = entreeSouvenirService.createEntree(request);

        assertThat(response).isNotNull();
        assertThat(response.getPatientId()).isEqualTo(10L);
        verify(entreeSouvenirRepository, times(1)).save(any(EntreeSouvenir.class));
    }

    @Test
    void createEntree_shouldThrow_whenMediaTypeNullAndUrlMissing() {
        EntreeSouvenirCreateRequest request = new EntreeSouvenirCreateRequest();
        request.setPatientId(10L);
        request.setDoctorId(5L);
        request.setTexte("Souvenir");
        request.setMediaType(null);

        assertThatThrownBy(() -> entreeSouvenirService.createEntree(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("mediaType is required");
    }

    @Test
    void createEntree_shouldThrow_whenNeitherDoctorNorCaregiverProvided() {
        EntreeSouvenirCreateRequest request = new EntreeSouvenirCreateRequest();
        request.setPatientId(10L);
        request.setTexte("Souvenir");
        request.setMediaType(MediaType.TEXT);
        request.setThemeCulturel(ThemeCulturel.FAMILLE);

        assertThatThrownBy(() -> entreeSouvenirService.createEntree(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("doctorId or caregiverId");
    }

    @Test
    void getEntreeById_shouldReturnResponse_whenExists() {
        when(entreeSouvenirRepository.findById(1L)).thenReturn(Optional.of(entree));

        EntreeSouvenirResponse response = entreeSouvenirService.getEntreeById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    void getEntreeById_shouldThrow_whenNotFound() {
        when(entreeSouvenirRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> entreeSouvenirService.getEntreeById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteEntree_shouldCallDelete_whenExists() {
        when(entreeSouvenirRepository.findById(1L)).thenReturn(Optional.of(entree));

        entreeSouvenirService.deleteEntree(1L);

        verify(entreeSouvenirRepository, times(1)).delete(entree);
    }

    @Test
    void markAsTraitee_shouldSetTraiteeTrue() {
        when(entreeSouvenirRepository.findById(1L)).thenReturn(Optional.of(entree));
        when(entreeSouvenirRepository.save(any(EntreeSouvenir.class))).thenReturn(entree);

        EntreeSouvenirResponse response = entreeSouvenirService.markAsTraitee(1L);

        assertThat(response).isNotNull();
        verify(entreeSouvenirRepository, times(1)).save(entree);
    }

    @Test
    void updateVoiceRecognition_shouldThrow_whenNotAudioType() {
        when(entreeSouvenirRepository.findById(1L)).thenReturn(Optional.of(entree));

        VoiceRecognitionUpdateRequest request = new VoiceRecognitionUpdateRequest();
        request.setVoiceRecognized(true);
        request.setPatientGuessSpeakerName("Jean");

        assertThatThrownBy(() -> entreeSouvenirService.updateVoiceRecognition(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("AUDIO");
    }

    @Test
    void getEntreesByPatient_shouldReturnList_whenNoFilters() {
        when(entreeSouvenirRepository.findByPatientIdOrderByCreatedAtAsc(10L))
                .thenReturn(List.of(entree));

        List<EntreeSouvenirResponse> responses = entreeSouvenirService.getEntreesByPatient(10L, null, null);

        assertThat(responses).hasSize(1);
    }

    @Test
    void countEntriesByPatient_shouldReturnCount() {
        when(entreeSouvenirRepository.countByPatientId(10L)).thenReturn(3L);

        var response = entreeSouvenirService.countEntriesByPatient(10L);

        assertThat(response.getTotalEntrees()).isEqualTo(3L);
        assertThat(response.getPatientId()).isEqualTo(10L);
    }
}
