package tn.esprit.recommendation_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.recommendation_service.dto.recommendation.AutoRecommendationGenerateRequest;
import tn.esprit.recommendation_service.dto.recommendation.ClinicalEscalationAlertResponse;
import tn.esprit.recommendation_service.dto.recommendation.RecommendationCreateRequest;
import tn.esprit.recommendation_service.dto.recommendation.RecommendationResponse;
import tn.esprit.recommendation_service.dto.recommendation.RecommendationStatsResponse;
import tn.esprit.recommendation_service.dto.recommendation.RecommendationStatusUpdateRequest;
import tn.esprit.recommendation_service.dto.recommendation.RecommendationUpdateRequest;
import tn.esprit.recommendation_service.entity.ClinicalEscalationAlert;
import tn.esprit.recommendation_service.entity.MedicalEvent;
import tn.esprit.recommendation_service.entity.Recommendation;
import tn.esprit.recommendation_service.enums.AlertStatus;
import tn.esprit.recommendation_service.enums.DifficultyLevel;
import tn.esprit.recommendation_service.enums.MedicalEventStatus;
import tn.esprit.recommendation_service.enums.MedicalEventType;
import tn.esprit.recommendation_service.enums.PatientLevel;
import tn.esprit.recommendation_service.enums.RecommendationStatus;
import tn.esprit.recommendation_service.enums.RecommendationType;
import tn.esprit.recommendation_service.exception.BusinessException;
import tn.esprit.recommendation_service.exception.ResourceNotFoundException;
import tn.esprit.recommendation_service.repository.ClinicalEscalationAlertRepository;
import tn.esprit.recommendation_service.repository.MedicalEventRepository;
import tn.esprit.recommendation_service.repository.RecommendationRepository;
import tn.esprit.recommendation_service.repository.projection.RecommendationStatusCountProjection;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final MedicalEventRepository medicalEventRepository;
    private final ClinicalEscalationAlertRepository clinicalEscalationAlertRepository;

    @Value("${app.recommendation.escalation-threshold:3}")
    private int escalationThreshold;

    public RecommendationResponse createRecommendation(RecommendationCreateRequest request) {
        validateExpirationDate(request.getExpirationDate());

        Recommendation recommendation = Recommendation.builder()
                .content(request.getContent())
                .type(request.getType())
                .status(RecommendationStatus.ACTIVE)
                .dismissed(Boolean.FALSE)
                .priority(defaultPriority(request.getPriority()))
                .doctorId(request.getDoctorId())
                .patientId(request.getPatientId())
                .rejectionCount(0)
                .expirationDate(request.getExpirationDate())
                .build();

        recommendation.setGeneratedMedicalEvent(resolveAttachedEvent(
                request.getGeneratedMedicalEventId(),
                request.getPatientId(),
                request.getType()
        ));

        return toResponse(recommendationRepository.save(recommendation));
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponse> getAllRecommendations() {
        return recommendationRepository.findAll().stream()
                .sorted(Comparator.comparing(Recommendation::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RecommendationResponse getRecommendationById(Long recommendationId) {
        return toResponse(getOrThrow(recommendationId));
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponse> getRecommendationsByPatient(Long patientId) {
        return recommendationRepository.findByPatientId(patientId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponse> getActiveRecommendationsByPatient(Long patientId) {
        return recommendationRepository.findActiveByPatientId(patientId, LocalDate.now()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponse> getRecommendationsSortedByPriorityAndCreatedAt(Long patientId) {
        return recommendationRepository.findByPatientIdOrderByPriorityDescCreatedAtDesc(patientId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RecommendationStatsResponse countAcceptedVsRejectedByPatient(Long patientId) {
        RecommendationStatusCountProjection projection = recommendationRepository.countAcceptedAndRejectedByPatientId(patientId);
        long accepted = projection == null || projection.getAcceptedCount() == null ? 0L : projection.getAcceptedCount();
        long rejected = projection == null || projection.getRejectedCount() == null ? 0L : projection.getRejectedCount();

        return RecommendationStatsResponse.builder()
                .patientId(patientId)
                .acceptedCount(accepted)
                .rejectedCount(rejected)
                .build();
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponse> searchRecommendations(String query) {
        return recommendationRepository.findByContentContainingIgnoreCase(query).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClinicalEscalationAlertResponse> getAlertsByDoctor(Long doctorId) {
        return clinicalEscalationAlertRepository.findByDoctorIdOrderByCreatedAtDesc(doctorId).stream()
                .map(this::toAlertResponse)
                .toList();
    }

    public RecommendationResponse updateRecommendationStatus(Long recommendationId, RecommendationStatusUpdateRequest request) {
        RecommendationStatus status = request.getStatus();

        if (status == RecommendationStatus.ACCEPTED) {
            return acceptRecommendation(recommendationId);
        }

        if (status == RecommendationStatus.REJECTED) {
            return dismissRecommendation(recommendationId);
        }

        Recommendation recommendation = getOrThrow(recommendationId);
        recommendation.setStatus(status);
        recommendation.setDismissed(status == RecommendationStatus.EXPIRED);

        return toResponse(recommendationRepository.save(recommendation));
    }

    public RecommendationResponse updateRecommendationDetails(Long recommendationId, RecommendationUpdateRequest request) {
        Recommendation recommendation = getOrThrow(recommendationId);

        if (recommendation.getStatus() == RecommendationStatus.EXPIRED) {
            throw new BusinessException("Expired recommendation cannot be modified.");
        }

        validateExpirationDate(request.getExpirationDate());

        recommendation.setContent(request.getContent());
        recommendation.setType(request.getType());
        recommendation.setDoctorId(request.getDoctorId());
        recommendation.setPatientId(request.getPatientId());
        recommendation.setPriority(defaultPriority(request.getPriority()));
        recommendation.setExpirationDate(request.getExpirationDate());
        recommendation.setGeneratedMedicalEvent(resolveAttachedEvent(
                request.getGeneratedMedicalEventId(),
                request.getPatientId(),
                request.getType()
        ));

        return toResponse(recommendationRepository.save(recommendation));
    }

    public RecommendationResponse acceptRecommendation(Long recommendationId) {
        Recommendation recommendation = getOrThrow(recommendationId);

        if (recommendation.getExpirationDate() != null && recommendation.getExpirationDate().isBefore(LocalDate.now())) {
            recommendation.setStatus(RecommendationStatus.EXPIRED);
            recommendationRepository.save(recommendation);
            throw new BusinessException("Recommendation already expired and cannot be accepted.");
        }

        if (recommendation.getGeneratedMedicalEvent() == null) {
            MedicalEvent medicalEvent = medicalEventRepository.save(buildMedicalEventFromRecommendation(recommendation));
            recommendation.setGeneratedMedicalEvent(medicalEvent);
        }

        recommendation.setStatus(RecommendationStatus.ACCEPTED);
        recommendation.setDismissed(Boolean.FALSE);
        recommendation.setAcceptedAt(LocalDateTime.now());

        return toResponse(recommendationRepository.save(recommendation));
    }

    public RecommendationResponse dismissRecommendation(Long recommendationId) {
        Recommendation recommendation = getOrThrow(recommendationId);
        recommendation.setDismissed(Boolean.TRUE);
        recommendation.setStatus(RecommendationStatus.REJECTED);
        recommendation.setRejectionCount(safeInteger(recommendation.getRejectionCount()) + 1);

        Recommendation savedRecommendation = recommendationRepository.save(recommendation);
        createOrRefreshEscalationAlert(savedRecommendation);
        return toResponse(savedRecommendation);
    }

    public void deleteRecommendation(Long recommendationId) {
        Recommendation recommendation = getOrThrow(recommendationId);
        recommendationRepository.delete(recommendation);
    }

    public int archiveExpiredRecommendations() {
        return recommendationRepository.archiveExpiredRecommendations(LocalDate.now());
    }

    @Scheduled(cron = "${app.recommendation.archive-cron:0 0 * * * *}")
    public void archiveExpiredRecommendationsScheduler() {
        archiveExpiredRecommendations();
    }

    public List<RecommendationResponse> generateAutomaticRecommendations(AutoRecommendationGenerateRequest request) {
        List<RuleCandidate> candidates = determineRuleCandidates(request);
        List<RecommendationResponse> generatedRecommendations = new ArrayList<>();
        MedicalEvent preferredMedicalEvent = resolvePreferredAutoEvent(
                request.getPreferredMedicalEventId(),
                request.getPatientId()
        );

        for (RuleCandidate candidate : candidates) {
            if (hasRecentRecommendationOfSameType(request.getPatientId(), candidate.type())) {
                continue;
            }

            Recommendation recommendation = Recommendation.builder()
                    .content(candidate.content())
                    .type(candidate.type())
                    .status(RecommendationStatus.ACTIVE)
                    .dismissed(Boolean.FALSE)
                    .priority(candidate.priority())
                    .doctorId(request.getDoctorId())
                    .patientId(request.getPatientId())
                    .rejectionCount(0)
                    .expirationDate(LocalDate.now().plusDays(candidate.expirationDays()))
                    .build();

            if (preferredMedicalEvent != null
                    && preferredMedicalEvent.getType() == mapToMedicalEventType(candidate.type(), request)) {
                recommendation.setGeneratedMedicalEvent(preferredMedicalEvent);
            } else {
                MedicalEvent generatedMedicalEvent = medicalEventRepository.save(
                        buildMedicalEventFromCandidate(candidate, request.getPatientId(), request)
                );
                recommendation.setGeneratedMedicalEvent(generatedMedicalEvent);
            }

            generatedRecommendations.add(toResponse(recommendationRepository.save(recommendation)));
        }

        if (generatedRecommendations.isEmpty()) {
            throw new BusinessException("No new recommendation generated because equivalent recent recommendations already exist.");
        }

        return generatedRecommendations;
    }

    public ClinicalEscalationAlertResponse resolveAlert(Long alertId) {
        ClinicalEscalationAlert alert = clinicalEscalationAlertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Clinical escalation alert not found: " + alertId));
        alert.setStatus(AlertStatus.RESOLVED);
        return toAlertResponse(clinicalEscalationAlertRepository.save(alert));
    }

    private List<RuleCandidate> determineRuleCandidates(AutoRecommendationGenerateRequest request) {
        Map<RecommendationType, RuleCandidate> candidates = new LinkedHashMap<>();
        List<RecommendationType> recentTypes = request.getRecentRecommendationTypes() == null
                ? List.of()
                : request.getRecentRecommendationTypes();

        int weeklyFrequency = safeInteger(request.getWeeklyFrequency());
        int acceptedCount = safeInteger(request.getAcceptedCount());
        int rejectedCount = safeInteger(request.getRejectedCount());

        if (request.getAge() >= 70 || Boolean.TRUE.equals(request.getCognitiveDropObserved())) {
            registerCandidate(candidates, new RuleCandidate(
                    RecommendationType.MEMORY,
                    priorityFor(request.getLevel(), 7),
                    "Mettre en place une seance de stimulation de la memoire avec photos familiales et rappels de reperes temporels.",
                    10
            ));
        }

        if (Boolean.TRUE.equals(request.getMedicationAdherenceIssue())) {
            registerCandidate(candidates, new RuleCandidate(
                    RecommendationType.MEDICATION,
                    9,
                    "Programmer une routine quotidienne de prise de medicaments avec verification simple et checklist visuelle.",
                    7
            ));
        }

        if (Boolean.TRUE.equals(request.getLowPhysicalActivity()) || weeklyFrequency < 2) {
            registerCandidate(candidates, new RuleCandidate(
                    RecommendationType.EXERCISE,
                    priorityFor(request.getLevel(), 6),
                    "Ajouter une activite physique douce et repetitive adaptee au patient au moins trois fois par semaine.",
                    14
            ));
        }

        if (request.getLevel() == PatientLevel.HIGH || recentTypes.contains(RecommendationType.MEMORY)) {
            registerCandidate(candidates, new RuleCandidate(
                    RecommendationType.ATTENTION,
                    priorityFor(request.getLevel(), 8),
                    "Proposer des exercices courts de concentration avec consignes simples et progression pas a pas.",
                    10
            ));
        }

        if (acceptedCount >= 2 && !recentTypes.contains(RecommendationType.FLUENCY)) {
            registerCandidate(candidates, new RuleCandidate(
                    RecommendationType.FLUENCY,
                    priorityFor(request.getLevel(), 5),
                    "Introduire un exercice de fluence verbale base sur des categories familières pour renforcer l'expression.",
                    12
            ));
        }

        if (rejectedCount > acceptedCount || weeklyFrequency == 0) {
            registerCandidate(candidates, new RuleCandidate(
                    RecommendationType.LIFESTYLE,
                    6,
                    "Revoir l'organisation de la journee avec des routines fixes pour diminuer la charge cognitive du patient.",
                    15
            ));
        }

        if (candidates.isEmpty()) {
            registerCandidate(candidates, new RuleCandidate(
                    RecommendationType.DIET,
                    4,
                    "Maintenir une hygiene de vie reguliere avec hydratation, repas structures et temps de repos stables.",
                    15
            ));
        }

        return new ArrayList<>(candidates.values());
    }

    private void registerCandidate(Map<RecommendationType, RuleCandidate> candidates, RuleCandidate candidate) {
        RuleCandidate existing = candidates.get(candidate.type());
        if (existing == null || candidate.priority() > existing.priority()) {
            candidates.put(candidate.type(), candidate);
        }
    }

    private boolean hasRecentRecommendationOfSameType(Long patientId, RecommendationType type) {
        return recommendationRepository.findTopByPatientIdAndTypeOrderByCreatedAtDesc(patientId, type)
                .filter(recommendation -> recommendation.getCreatedAt() != null)
                .filter(recommendation -> recommendation.getCreatedAt().isAfter(LocalDateTime.now().minusDays(7)))
                .isPresent();
    }

    private void createOrRefreshEscalationAlert(Recommendation recommendation) {
        long totalRejectionCount = recommendationRepository.sumRejectionCountByPatientIdAndType(
                recommendation.getPatientId(),
                recommendation.getType()
        );

        if (totalRejectionCount < escalationThreshold) {
            return;
        }

        ClinicalEscalationAlert alert = clinicalEscalationAlertRepository
                .findTopByDoctorIdAndPatientIdAndRecommendationTypeAndStatusOrderByCreatedAtDesc(
                        recommendation.getDoctorId(),
                        recommendation.getPatientId(),
                        recommendation.getType(),
                        AlertStatus.OPEN
                )
                .orElseGet(() -> ClinicalEscalationAlert.builder()
                        .recommendationId(recommendation.getId())
                        .doctorId(recommendation.getDoctorId())
                        .patientId(recommendation.getPatientId())
                        .recommendationType(recommendation.getType())
                        .status(AlertStatus.OPEN)
                        .build());

        alert.setRecommendationId(recommendation.getId());
        alert.setRejectionCount((int) totalRejectionCount);
        alert.setMessage(buildEscalationMessage(recommendation, totalRejectionCount));
        clinicalEscalationAlertRepository.save(alert);
    }

    private String buildEscalationMessage(Recommendation recommendation, long totalRejectionCount) {
        return "Le patient " + recommendation.getPatientId()
                + " a rejete " + totalRejectionCount
                + " fois les recommandations de type "
                + recommendation.getType()
                + ". Une reevaluation clinique est conseillee.";
    }

    private Recommendation getOrThrow(Long recommendationId) {
        return recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new ResourceNotFoundException("Recommendation not found: " + recommendationId));
    }

    private void validateExpirationDate(LocalDate expirationDate) {
        if (expirationDate != null && expirationDate.isBefore(LocalDate.now())) {
            throw new BusinessException("Expiration date cannot be in the past.");
        }
    }

    private int defaultPriority(Integer priority) {
        return priority == null ? 0 : priority;
    }

    private int safeInteger(Integer value) {
        return value == null ? 0 : value;
    }

    private int priorityFor(PatientLevel level, int basePriority) {
        if (level == null) {
            return basePriority;
        }
        return switch (level) {
            case LOW -> Math.max(1, basePriority - 1);
            case MEDIUM -> basePriority;
            case HIGH -> Math.min(10, basePriority + 1);
        };
    }

    private MedicalEvent buildMedicalEventFromRecommendation(Recommendation recommendation) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = recommendation.getExpirationDate() == null
                ? now.plusDays(7)
                : recommendation.getExpirationDate().atTime(23, 59, 59);

        if (!endDate.isAfter(now)) {
            endDate = now.plusDays(1);
        }

        return MedicalEvent.builder()
                .title("Recommendation " + recommendation.getType())
                .description(recommendation.getContent())
                .type(mapToMedicalEventType(recommendation))
                .difficulty(mapDifficulty(recommendation.getPriority()))
                .status(MedicalEventStatus.ACTIVE)
                .patientId(recommendation.getPatientId())
                .startDate(now)
                .endDate(endDate)
                .autoGenerated(Boolean.TRUE)
                .build();
    }

    private MedicalEvent buildMedicalEventFromCandidate(
            RuleCandidate candidate,
            Long patientId,
            AutoRecommendationGenerateRequest request
    ) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = now.plusDays(candidate.expirationDays());
        MedicalEventType eventType = mapToMedicalEventType(candidate.type(), request);

        return MedicalEvent.builder()
                .title(buildAutoGeneratedEventTitle(candidate.type(), eventType))
                .description(candidate.content())
                .type(eventType)
                .difficulty(mapDifficulty(candidate.priority()))
                .status(MedicalEventStatus.ACTIVE)
                .patientId(patientId)
                .startDate(now)
                .endDate(endDate)
                .autoGenerated(Boolean.TRUE)
                .build();
    }

    private MedicalEvent resolveAttachedEvent(Long eventId, Long patientId, RecommendationType recommendationType) {
        if (eventId == null) {
            return null;
        }

        MedicalEvent event = medicalEventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalEvent not found: " + eventId));

        if (!event.getPatientId().equals(patientId)) {
            throw new BusinessException("MedicalEvent must belong to the same patient as the recommendation.");
        }

        if (event.getStatus() != MedicalEventStatus.ACTIVE) {
            throw new BusinessException("Only active MedicalEvents can be attached to a recommendation.");
        }

        if (!isCompatible(recommendationType, event.getType())) {
            throw new BusinessException("MedicalEvent type (" + event.getType() + ") is not compatible with recommendation type (" + recommendationType + ").");
        }

        return event;
    }

    private boolean isCompatible(RecommendationType recType, MedicalEventType eventType) {
        if (recType.name().equals(eventType.name())) {
            return true;
        }

        // PUZZLE events are compatible with cognitive recommendation types
        if (eventType == MedicalEventType.PUZZLE) {
            return recType == RecommendationType.VISUOSPATIAL
                    || recType == RecommendationType.MEMORY
                    || recType == RecommendationType.ATTENTION
                    || recType == RecommendationType.PUZZLE;
        }

        return false;
    }

    private MedicalEvent resolvePreferredAutoEvent(Long eventId, Long patientId) {
        if (eventId == null) {
            return null;
        }

        MedicalEvent event = medicalEventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalEvent not found: " + eventId));

        if (!event.getPatientId().equals(patientId)) {
            throw new BusinessException("Preferred MedicalEvent must belong to the same patient.");
        }

        if (event.getStatus() != MedicalEventStatus.ACTIVE) {
            throw new BusinessException("Preferred MedicalEvent must be ACTIVE.");
        }

        return event;
    }

    private MedicalEventType mapToMedicalEventType(Recommendation recommendation) {
        return mapToMedicalEventType(recommendation.getType());
    }

    private MedicalEventType mapToMedicalEventType(RecommendationType recommendationType) {
        try {
            return MedicalEventType.valueOf(recommendationType.name());
        } catch (IllegalArgumentException ex) {
            return MedicalEventType.OTHER;
        }
    }

    private MedicalEventType mapToMedicalEventType(
            RecommendationType recommendationType,
            AutoRecommendationGenerateRequest request
    ) {
        return switch (recommendationType) {
            case MEMORY -> request.getLevel() == PatientLevel.HIGH
                    ? MedicalEventType.ATTENTION
                    : MedicalEventType.MEMORY;
            case EXERCISE -> MedicalEventType.EXERCISE;
            case MEDICATION -> request.getWeeklyFrequency() == null || request.getWeeklyFrequency() == 0
                    ? MedicalEventType.LIFESTYLE
                    : MedicalEventType.ATTENTION;
            case DIET -> MedicalEventType.LIFESTYLE;
            case LIFESTYLE -> MedicalEventType.ATTENTION;
            default -> mapToMedicalEventType(recommendationType);
        };
    }

    private String buildAutoGeneratedEventTitle(RecommendationType recommendationType, MedicalEventType eventType) {
        return "Auto Event " + recommendationType + " -> " + eventType;
    }

    private DifficultyLevel mapDifficulty(Integer priority) {
        int safePriority = priority == null ? 0 : priority;
        if (safePriority >= 8) {
            return DifficultyLevel.HARD;
        }
        if (safePriority >= 4) {
            return DifficultyLevel.MEDIUM;
        }
        return DifficultyLevel.EASY;
    }

    private RecommendationResponse toResponse(Recommendation recommendation) {
        MedicalEvent event = recommendation.getGeneratedMedicalEvent();

        return RecommendationResponse.builder()
                .id(recommendation.getId())
                .content(recommendation.getContent())
                .type(recommendation.getType())
                .status(recommendation.getStatus())
                .dismissed(recommendation.getDismissed())
                .priority(recommendation.getPriority())
                .doctorId(recommendation.getDoctorId())
                .patientId(recommendation.getPatientId())
                .rejectionCount(recommendation.getRejectionCount())
                .expirationDate(recommendation.getExpirationDate())
                .acceptedAt(recommendation.getAcceptedAt())
                .generatedMedicalEventId(event == null ? null : event.getId())
                .generatedMedicalEventTitle(event == null ? null : event.getTitle())
                .generatedMedicalEventType(event == null ? null : event.getType())
                .createdAt(recommendation.getCreatedAt())
                .updatedAt(recommendation.getUpdatedAt())
                .build();
    }

    private ClinicalEscalationAlertResponse toAlertResponse(ClinicalEscalationAlert alert) {
        return ClinicalEscalationAlertResponse.builder()
                .id(alert.getId())
                .recommendationId(alert.getRecommendationId())
                .doctorId(alert.getDoctorId())
                .patientId(alert.getPatientId())
                .recommendationType(alert.getRecommendationType())
                .rejectionCount(alert.getRejectionCount())
                .message(alert.getMessage())
                .status(alert.getStatus())
                .createdAt(alert.getCreatedAt())
                .updatedAt(alert.getUpdatedAt())
                .build();
    }

    private record RuleCandidate(
            RecommendationType type,
            int priority,
            String content,
            int expirationDays
    ) {
    }
}
