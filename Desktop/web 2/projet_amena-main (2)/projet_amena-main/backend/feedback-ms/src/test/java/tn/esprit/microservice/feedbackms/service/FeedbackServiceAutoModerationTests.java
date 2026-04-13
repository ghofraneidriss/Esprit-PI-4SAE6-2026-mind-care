package tn.esprit.microservice.feedbackms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import tn.esprit.microservice.feedbackms.client.ReclamationClient;
import tn.esprit.microservice.feedbackms.entity.Feedback;
import tn.esprit.microservice.feedbackms.entity.FeedbackModerationStatus;
import tn.esprit.microservice.feedbackms.repository.FeedbackRepository;
import java.util.List;

class FeedbackServiceAutoModerationTests {

    private FeedbackRepository feedbackRepository;
    private ReclamationClient reclamationClient;
    private FeedbackService feedbackService;

    @BeforeEach
    void setUp() {
        feedbackRepository = Mockito.mock(FeedbackRepository.class);
    reclamationClient = Mockito.mock(ReclamationClient.class);
        feedbackService = new FeedbackService(
                feedbackRepository,
                new ObjectMapper(),
                "insulte,haine,raciste,violence,arnaque,spam,escroc",
        70,
        reclamationClient,
        true,
        2
        );

    when(reclamationClient.createReclamation(any()))
        .thenReturn(new ReclamationClient.ReclamationResponse(101L));
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldAutoApproveSafeFeedback() {
        Feedback feedback = buildFeedback(
                "Sara",
                "sara@mail.com",
                "Tres bon cours",
                "Le contenu est clair et utile pour debuter."
        );

        Feedback saved = feedbackService.create(feedback);

        assertEquals(FeedbackModerationStatus.APPROVED, saved.getModerationStatus());
        assertFalse(Boolean.TRUE.equals(saved.getModerationFlagged()));
        assertNotNull(saved.getModerationScore());
        assertEquals("AUTO_MODEL", saved.getModeratedBy());
        assertNotNull(saved.getModeratedAt());
        assertFalse(Boolean.TRUE.equals(saved.getAutoReclamationCreated()));
        assertEquals("SKIPPED_NOT_NEGATIVE", saved.getAutoReclamationStatus());
        verify(feedbackRepository).save(any(Feedback.class));
        verifyNoInteractions(reclamationClient);
    }

    @Test
    void shouldAutoBlockRiskyFeedback() {
        Feedback feedback = buildFeedback(
                "UserTest",
                "usertest@mail.com",
                "Offre limitee",
                "Ceci est une arnaque, clique ici maintenant!!!"
        );

        Feedback saved = feedbackService.create(feedback);

        assertEquals(FeedbackModerationStatus.REJECTED, saved.getModerationStatus());
        assertTrue(Boolean.TRUE.equals(saved.getModerationFlagged()));
        assertNotNull(saved.getModerationScore());
        assertTrue(saved.getModerationScore() >= 70 || saved.getBlockedWords().contains("arnaque"));
        assertTrue(saved.getBlockedWords().contains("arnaque"));
        assertFalse(Boolean.TRUE.equals(saved.getAutoReclamationCreated()));
        assertEquals("SKIPPED_NOT_NEGATIVE", saved.getAutoReclamationStatus());
    }

        @Test
        void shouldCreateAutoReclamationForNegativeFeedback() {
        Feedback feedback = buildFeedback(
            "Nadia",
            "nadia@mail.com",
            "Spring Cloud",
            "Le contenu est difficile a suivre.",
            1
        );

        Feedback saved = feedbackService.create(feedback);

        assertTrue(Boolean.TRUE.equals(saved.getAutoReclamationCreated()));
        assertEquals(101L, saved.getLinkedReclamationId());
        assertEquals("CREATED", saved.getAutoReclamationStatus());

        ArgumentCaptor<ReclamationClient.ReclamationCreateRequest> captor =
            ArgumentCaptor.forClass(ReclamationClient.ReclamationCreateRequest.class);
        verify(reclamationClient).createReclamation(captor.capture());

        assertEquals("Spring Cloud", captor.getValue().subject());
        assertEquals("OPEN", captor.getValue().status());
        }

        @Test
        void shouldStoreFailureWhenAutoReclamationCallFails() {
        when(reclamationClient.createReclamation(any()))
            .thenThrow(new RuntimeException("service indisponible"));

        Feedback feedback = buildFeedback(
            "Yasmine",
            "yasmine@mail.com",
            "Docker",
            "Je ne comprends pas ce module.",
            2
        );

        Feedback saved = feedbackService.create(feedback);

        assertFalse(Boolean.TRUE.equals(saved.getAutoReclamationCreated()));
        assertNull(saved.getLinkedReclamationId());
        assertTrue(saved.getAutoReclamationStatus().startsWith("FAILED:"));
        }

    @Test
    void analyzeDraftShouldReturnScoreAndBlockRecommendationForRiskyContent() {
        FeedbackService.ModerationPreview preview = feedbackService.analyzeDraft(
                "Offre limitee",
                "Ceci est une arnaque, clique ici maintenant!!!"
        );

        assertTrue(preview.score() > 0);
        assertTrue(preview.flagged());
        assertEquals("BLOCK", preview.recommendation());
        assertTrue(preview.blockedWords().contains("arnaque"));
    }

    @Test
    void shouldComputeRatingSummaryAndRankingByCourse() {
        when(feedbackRepository.findAll()).thenReturn(List.of(
                buildFeedback("Sara", "sara@mail.com", "Spring Boot", "Tres bon", 5),
                buildFeedback("Ali", "ali@mail.com", "Spring Boot", "Bien", 4),
                buildFeedback("Maya", "maya@mail.com", "Angular", "Correct", 3)
        ));

        FeedbackService.RatingSummary summary = feedbackService.getRatingSummary();
        assertEquals(4.0, summary.averageRating(), 0.001);
        assertEquals(3, summary.feedbackCount());
        assertEquals(3, summary.ratedFeedbackCount());
        assertEquals(2, summary.rankedCourseCount());

        List<FeedbackService.CourseRatingStats> ranking = feedbackService.getCourseRanking(2);
        assertEquals(2, ranking.size());
        assertEquals("Spring Boot", ranking.get(0).courseTitle());
        assertEquals(4.5, ranking.get(0).averageRating(), 0.001);
        assertEquals(2, ranking.get(0).feedbackCount());
    }

    private Feedback buildFeedback(String userName, String userEmail, String title, String comment) {
        return buildFeedback(userName, userEmail, title, comment, 5);
    }

    private Feedback buildFeedback(String userName, String userEmail, String title, String comment, int rating) {
        Feedback feedback = new Feedback();
        feedback.setUserName(userName);
        feedback.setUserEmail(userEmail);
        feedback.setTitle(title);
        feedback.setImageUrl("");
        feedback.setRating(rating);
        feedback.setComment(comment);
        return feedback;
    }
}
