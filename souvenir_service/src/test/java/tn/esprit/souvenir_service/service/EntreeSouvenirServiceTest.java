package tn.esprit.souvenir_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.esprit.souvenir_service.dto.entree.EntreeCountResponse;
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
@DisplayName("EntreeSouvenirService — Tests Unitaires")
class EntreeSouvenirServiceTest {

    @Mock
    private EntreeSouvenirRepository repository;

    @InjectMocks
    private EntreeSouvenirService service;

    private EntreeSouvenir sampleEntree;

    @BeforeEach
    void setUp() {
        sampleEntree = EntreeSouvenir.builder()
                .id(1L)
                .patientId(4L)
                .doctorId(3L)
                .texte("Photo du mariage de 1985")
                .mediaType(MediaType.IMAGE)
                .mediaUrl("http://localhost:8086/files/mariage.jpg")
                .mediaTitle("Mariage 1985")
                .themeCulturel(ThemeCulturel.TRADITIONS)
                .traitee(false)
                .voiceRecognized(false)
                .importance(5)
                .build();
    }

    // ── createEntree ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createEntree")
    class CreateEntree {

        @Test
        @DisplayName("Crée un souvenir IMAGE valide avec succès")
        void createImageSouvenir_success() {
            EntreeSouvenirCreateRequest request = buildCreateRequest(MediaType.IMAGE, "http://img.url");
            when(repository.save(any())).thenReturn(sampleEntree);

            EntreeSouvenirResponse response = service.createEntree(request);

            assertThat(response).isNotNull();
            assertThat(response.getPatientId()).isEqualTo(4L);
            assertThat(response.getMediaType()).isEqualTo(MediaType.IMAGE);
            verify(repository, times(1)).save(any(EntreeSouvenir.class));
        }

        @Test
        @DisplayName("Crée un souvenir AUDIO valide avec succès")
        void createAudioSouvenir_success() {
            EntreeSouvenirCreateRequest request = buildCreateRequest(MediaType.AUDIO, "http://audio.url");
            EntreeSouvenir audioEntree = EntreeSouvenir.builder()
                    .id(2L).patientId(4L).doctorId(3L)
                    .texte("Voix de grand-mère").mediaType(MediaType.AUDIO)
                    .mediaUrl("http://audio.url").themeCulturel(ThemeCulturel.MUSIQUE)
                    .traitee(false).voiceRecognized(false).importance(5).build();
            when(repository.save(any())).thenReturn(audioEntree);

            EntreeSouvenirResponse response = service.createEntree(request);

            assertThat(response.getMediaType()).isEqualTo(MediaType.AUDIO);
        }

        @Test
        @DisplayName("Lève BusinessException si mediaUrl est vide pour IMAGE")
        void createImageWithoutUrl_throwsBusinessException() {
            EntreeSouvenirCreateRequest request = buildCreateRequest(MediaType.IMAGE, "");

            assertThatThrownBy(() -> service.createEntree(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("mediaUrl is required");
        }

        @Test
        @DisplayName("Lève BusinessException si mediaUrl est null pour AUDIO")
        void createAudioWithoutUrl_throwsBusinessException() {
            EntreeSouvenirCreateRequest request = buildCreateRequest(MediaType.AUDIO, null);

            assertThatThrownBy(() -> service.createEntree(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("mediaUrl is required");
        }

        @Test
        @DisplayName("Lève BusinessException si doctorId et caregiverId sont tous les deux null")
        void createWithoutActor_throwsBusinessException() {
            EntreeSouvenirCreateRequest request = EntreeSouvenirCreateRequest.builder()
                    .patientId(4L).doctorId(null).caregiverId(null)
                    .texte("Texte").mediaType(MediaType.IMAGE).mediaUrl("http://url")
                    .themeCulturel(ThemeCulturel.TRADITIONS).build();

            assertThatThrownBy(() -> service.createEntree(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("doctorId or caregiverId");
        }

        @Test
        @DisplayName("Importance par défaut = 5 si non fournie")
        void createWithoutImportance_defaultsToFive() {
            EntreeSouvenirCreateRequest request = buildCreateRequest(MediaType.IMAGE, "http://url");
            request.setImportance(null);
            when(repository.save(any())).thenReturn(sampleEntree);

            service.createEntree(request);

            verify(repository).save(argThat(e -> e.getImportance() == 5));
        }
    }

    // ── getEntreeById ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getEntreeById")
    class GetEntreeById {

        @Test
        @DisplayName("Retourne le souvenir si trouvé")
        void getById_found() {
            when(repository.findById(1L)).thenReturn(Optional.of(sampleEntree));

            EntreeSouvenirResponse response = service.getEntreeById(1L);

            assertThat(response.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Lève ResourceNotFoundException si non trouvé")
        void getById_notFound() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getEntreeById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    // ── getEntreesByPatient ───────────────────────────────────────────────────

    @Nested
    @DisplayName("getEntreesByPatient")
    class GetEntreesByPatient {

        @Test
        @DisplayName("Retourne tous les souvenirs du patient sans filtre")
        void getByPatient_noFilter() {
            when(repository.findByPatientIdOrderByCreatedAtAsc(4L)).thenReturn(List.of(sampleEntree));

            List<EntreeSouvenirResponse> result = service.getEntreesByPatient(4L, null, null);

            assertThat(result).hasSize(1);
            verify(repository).findByPatientIdOrderByCreatedAtAsc(4L);
        }

        @Test
        @DisplayName("Filtre par mediaType IMAGE")
        void getByPatient_filterByMediaType() {
            when(repository.findByPatientIdAndMediaTypeOrderByCreatedAtAsc(4L, MediaType.IMAGE))
                    .thenReturn(List.of(sampleEntree));

            List<EntreeSouvenirResponse> result = service.getEntreesByPatient(4L, null, MediaType.IMAGE);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getMediaType()).isEqualTo(MediaType.IMAGE);
        }

        @Test
        @DisplayName("Filtre par thème TRADITIONS")
        void getByPatient_filterByTheme() {
            when(repository.findByPatientIdAndThemeCulturelOrderByCreatedAtAsc(4L, ThemeCulturel.TRADITIONS))
                    .thenReturn(List.of(sampleEntree));

            List<EntreeSouvenirResponse> result = service.getEntreesByPatient(4L, ThemeCulturel.TRADITIONS, null);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Filtre par thème ET mediaType")
        void getByPatient_filterByThemeAndMediaType() {
            when(repository.findByPatientIdAndThemeCulturelAndMediaTypeOrderByCreatedAtAsc(
                    4L, ThemeCulturel.TRADITIONS, MediaType.IMAGE))
                    .thenReturn(List.of(sampleEntree));

            List<EntreeSouvenirResponse> result = service.getEntreesByPatient(4L, ThemeCulturel.TRADITIONS, MediaType.IMAGE);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Retourne liste vide si aucun souvenir")
        void getByPatient_empty() {
            when(repository.findByPatientIdOrderByCreatedAtAsc(99L)).thenReturn(List.of());

            List<EntreeSouvenirResponse> result = service.getEntreesByPatient(99L, null, null);

            assertThat(result).isEmpty();
        }
    }

    // ── markAsTraitee ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("markAsTraitee")
    class MarkAsTraitee {

        @Test
        @DisplayName("Marque le souvenir comme traité")
        void markAsTraitee_success() {
            when(repository.findById(1L)).thenReturn(Optional.of(sampleEntree));
            when(repository.save(any())).thenReturn(sampleEntree);

            service.markAsTraitee(1L);

            verify(repository).save(argThat(e -> Boolean.TRUE.equals(e.getTraitee())));
        }

        @Test
        @DisplayName("Lève ResourceNotFoundException si souvenir non trouvé")
        void markAsTraitee_notFound() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.markAsTraitee(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── updateVoiceRecognition ────────────────────────────────────────────────

    @Nested
    @DisplayName("updateVoiceRecognition")
    class UpdateVoiceRecognition {

        @Test
        @DisplayName("Lève BusinessException si souvenir n'est pas AUDIO")
        void updateVoiceRecognition_notAudio_throws() {
            when(repository.findById(1L)).thenReturn(Optional.of(sampleEntree)); // IMAGE

            VoiceRecognitionUpdateRequest request = new VoiceRecognitionUpdateRequest();
            request.setPatientGuessSpeakerName("Grand-mère");
            request.setVoiceRecognized(true);

            assertThatThrownBy(() -> service.updateVoiceRecognition(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("AUDIO");
        }

        @Test
        @DisplayName("Met à jour la reconnaissance vocale pour un souvenir AUDIO")
        void updateVoiceRecognition_audio_success() {
            EntreeSouvenir audioEntree = EntreeSouvenir.builder()
                    .id(2L).patientId(4L).doctorId(3L)
                    .texte("Voix").mediaType(MediaType.AUDIO)
                    .mediaUrl("http://audio.url").themeCulturel(ThemeCulturel.MUSIQUE)
                    .traitee(false).voiceRecognized(false).importance(5).build();

            when(repository.findById(2L)).thenReturn(Optional.of(audioEntree));
            when(repository.save(any())).thenReturn(audioEntree);

            VoiceRecognitionUpdateRequest request = new VoiceRecognitionUpdateRequest();
            request.setPatientGuessSpeakerName("Grand-mère");
            request.setVoiceRecognized(true);

            service.updateVoiceRecognition(2L, request);

            verify(repository).save(argThat(e ->
                    "Grand-mère".equals(e.getPatientGuessSpeakerName()) &&
                    Boolean.TRUE.equals(e.getVoiceRecognized())));
        }
    }

    // ── countEntriesByPatient ─────────────────────────────────────────────────

    @Nested
    @DisplayName("countEntriesByPatient")
    class CountEntriesByPatient {

        @Test
        @DisplayName("Retourne le bon compte pour un patient")
        void count_success() {
            when(repository.countByPatientId(4L)).thenReturn(3L);

            EntreeCountResponse response = service.countEntriesByPatient(4L);

            assertThat(response.getPatientId()).isEqualTo(4L);
            assertThat(response.getTotalEntrees()).isEqualTo(3L);
        }

        @Test
        @DisplayName("Retourne 0 si aucun souvenir")
        void count_zero() {
            when(repository.countByPatientId(99L)).thenReturn(0L);

            EntreeCountResponse response = service.countEntriesByPatient(99L);

            assertThat(response.getTotalEntrees()).isZero();
        }
    }

    // ── deleteEntree ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteEntree")
    class DeleteEntree {

        @Test
        @DisplayName("Supprime le souvenir existant")
        void delete_success() {
            when(repository.findById(1L)).thenReturn(Optional.of(sampleEntree));

            service.deleteEntree(1L);

            verify(repository).delete(sampleEntree);
        }

        @Test
        @DisplayName("Lève ResourceNotFoundException si non trouvé")
        void delete_notFound() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteEntree(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private EntreeSouvenirCreateRequest buildCreateRequest(MediaType mediaType, String mediaUrl) {
        return EntreeSouvenirCreateRequest.builder()
                .patientId(4L)
                .doctorId(3L)
                .texte("Photo du mariage de 1985")
                .mediaType(mediaType)
                .mediaUrl(mediaUrl)
                .mediaTitle("Mariage 1985")
                .themeCulturel(ThemeCulturel.TRADITIONS)
                .importance(7)
                .build();
    }
}
