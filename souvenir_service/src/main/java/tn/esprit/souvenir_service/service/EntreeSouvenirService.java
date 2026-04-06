package tn.esprit.souvenir_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

@Service
@RequiredArgsConstructor
@Transactional
public class EntreeSouvenirService {

    private final EntreeSouvenirRepository entreeSouvenirRepository;

    public EntreeSouvenirResponse createEntree(EntreeSouvenirCreateRequest request) {
        validateMediaPayload(request.getMediaType(), request.getMediaUrl());

        EntreeSouvenir entree = EntreeSouvenir.builder()
                .patientId(request.getPatientId())
                .doctorId(request.getDoctorId())
                .caregiverId(request.getCaregiverId())
                .infosCaregiver(request.getInfosCaregiver())
                .texte(request.getTexte())
                .mediaType(request.getMediaType())
                .mediaUrl(request.getMediaUrl())
                .mediaTitle(request.getMediaTitle())
                .expectedSpeakerName(request.getExpectedSpeakerName())
                .expectedSpeakerRelation(request.getExpectedSpeakerRelation())
                .themeCulturel(request.getThemeCulturel())
                .traitee(Boolean.FALSE)
                .voiceRecognized(Boolean.FALSE)
                .build();

        return toResponse(entreeSouvenirRepository.save(entree));
    }

    @Transactional(readOnly = true)
    public EntreeSouvenirResponse getEntreeById(Long entreeId) {
        return toResponse(getOrThrow(entreeId));
    }

    @Transactional(readOnly = true)
    public List<EntreeSouvenirResponse> getEntreesByPatient(Long patientId, ThemeCulturel theme, MediaType mediaType) {
        List<EntreeSouvenir> entries;

        if (theme != null && mediaType != null) {
            entries = entreeSouvenirRepository.findByPatientIdAndThemeCulturelAndMediaTypeOrderByCreatedAtAsc(patientId, theme, mediaType);
        } else if (theme != null) {
            entries = entreeSouvenirRepository.findByPatientIdAndThemeCulturelOrderByCreatedAtAsc(patientId, theme);
        } else if (mediaType != null) {
            entries = entreeSouvenirRepository.findByPatientIdAndMediaTypeOrderByCreatedAtAsc(patientId, mediaType);
        } else {
            entries = entreeSouvenirRepository.findByPatientIdOrderByCreatedAtAsc(patientId);
        }

        return entries.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<EntreeSouvenirResponse> getEntreesByTheme(ThemeCulturel themeCulturel) {
        return entreeSouvenirRepository.findByThemeCulturelOrderByCreatedAtDesc(themeCulturel)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public EntreeSouvenirResponse updateEntree(Long entreeId, EntreeSouvenirUpdateRequest request) {
        validateMediaPayload(request.getMediaType(), request.getMediaUrl());

        EntreeSouvenir entree = getOrThrow(entreeId);
        entree.setDoctorId(request.getDoctorId());
        entree.setCaregiverId(request.getCaregiverId());
        entree.setInfosCaregiver(request.getInfosCaregiver());
        entree.setTexte(request.getTexte());
        entree.setMediaType(request.getMediaType());
        entree.setMediaUrl(request.getMediaUrl());
        entree.setMediaTitle(request.getMediaTitle());
        entree.setExpectedSpeakerName(request.getExpectedSpeakerName());
        entree.setExpectedSpeakerRelation(request.getExpectedSpeakerRelation());
        entree.setThemeCulturel(request.getThemeCulturel());
        return toResponse(entreeSouvenirRepository.save(entree));
    }

    public EntreeSouvenirResponse updateVoiceRecognition(Long entreeId, VoiceRecognitionUpdateRequest request) {
        EntreeSouvenir entree = getOrThrow(entreeId);
        if (entree.getMediaType() != MediaType.AUDIO) {
            throw new BusinessException("Voice recognition is available only for AUDIO souvenirs.");
        }

        entree.setPatientGuessSpeakerName(request.getPatientGuessSpeakerName());
        entree.setVoiceRecognized(request.getVoiceRecognized() != null && request.getVoiceRecognized());

        return toResponse(entreeSouvenirRepository.save(entree));
    }

    public void deleteEntree(Long entreeId) {
        EntreeSouvenir entree = getOrThrow(entreeId);
        entreeSouvenirRepository.delete(entree);
    }

    public EntreeSouvenirResponse markAsTraitee(Long entreeId) {
        EntreeSouvenir entree = getOrThrow(entreeId);
        entree.setTraitee(Boolean.TRUE);
        return toResponse(entreeSouvenirRepository.save(entree));
    }

    @Transactional(readOnly = true)
    public EntreeCountResponse countEntriesByPatient(Long patientId) {
        return EntreeCountResponse.builder()
                .patientId(patientId)
                .totalEntrees(entreeSouvenirRepository.countByPatientId(patientId))
                .build();
    }

    private EntreeSouvenir getOrThrow(Long entreeId) {
        return entreeSouvenirRepository.findById(entreeId)
                .orElseThrow(() -> new ResourceNotFoundException("EntreeSouvenir not found: " + entreeId));
    }

    private void validateMediaPayload(MediaType mediaType, String mediaUrl) {
        if (mediaType == null) {
            throw new BusinessException("mediaType is required.");
        }
    }

    private EntreeSouvenirResponse toResponse(EntreeSouvenir entree) {
        return EntreeSouvenirResponse.builder()
                .id(entree.getId())
                .patientId(entree.getPatientId())
                .doctorId(entree.getDoctorId())
                .caregiverId(entree.getCaregiverId())
                .infosCaregiver(entree.getInfosCaregiver())
                .texte(entree.getTexte())
                .mediaType(entree.getMediaType())
                .mediaUrl(entree.getMediaUrl())
                .mediaTitle(entree.getMediaTitle())
                .expectedSpeakerName(entree.getExpectedSpeakerName())
                .expectedSpeakerRelation(entree.getExpectedSpeakerRelation())
                .patientGuessSpeakerName(entree.getPatientGuessSpeakerName())
                .voiceRecognized(entree.getVoiceRecognized())
                .themeCulturel(entree.getThemeCulturel())
                .traitee(entree.getTraitee())
                .createdAt(entree.getCreatedAt())
                .updatedAt(entree.getUpdatedAt())
                .build();
    }
}
