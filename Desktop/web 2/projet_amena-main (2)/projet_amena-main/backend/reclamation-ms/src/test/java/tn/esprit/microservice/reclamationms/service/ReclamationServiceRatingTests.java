package tn.esprit.microservice.reclamationms.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tn.esprit.microservice.reclamationms.entity.ReclamationClassificationLevel;
import tn.esprit.microservice.reclamationms.entity.Reclamation;
import tn.esprit.microservice.reclamationms.repository.ReclamationRepository;

class ReclamationServiceRatingTests {

    private ReclamationRepository reclamationRepository;
    private ReclamationService reclamationService;

    @BeforeEach
    void setUp() {
        reclamationRepository = Mockito.mock(ReclamationRepository.class);
        reclamationService = new ReclamationService(
                reclamationRepository,
                new ObjectMapper(),
                "retard intervention,urgence,danger,accident,panne totale,service indisponible,impossible d utiliser,bloque",
                "bug,erreur,deconnexion,paiement,lent,instable,retard,interruption",
                "gps ne marche pas,gps,suggestion,affichage",
                70,
                35
        );
        when(reclamationRepository.save(any(Reclamation.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldClassifyGpsIssueAsFaible() {
        Reclamation reclamation = new Reclamation();
        reclamation.setUserName("Karim");
        reclamation.setUserEmail("karim@mail.com");
        reclamation.setSubject("GPS ne marche pas");
        reclamation.setDescription("Le GPS ne marche pas dans l application.");
        reclamation.setRating(3);

        Reclamation saved = reclamationService.create(reclamation);

        assertEquals(ReclamationClassificationLevel.FAIBLE, saved.getClassificationLevel());
        assertNotNull(saved.getClassificationScore());
        assertTrue(saved.getClassificationScore() < 35);
        assertTrue(saved.getClassificationReason().contains("Auto classification"));
    }

    @Test
    void shouldClassifyRetardInterventionAsFort() {
        Reclamation reclamation = new Reclamation();
        reclamation.setUserName("Leila");
        reclamation.setUserEmail("leila@mail.com");
        reclamation.setSubject("retard intervention");
        reclamation.setDescription("Retard intervention depuis 2 jours, urgent.");
        reclamation.setRating(2);

        Reclamation saved = reclamationService.create(reclamation);

        assertEquals(ReclamationClassificationLevel.FORT, saved.getClassificationLevel());
        assertNotNull(saved.getClassificationScore());
        assertTrue(saved.getClassificationScore() >= 70);
        assertTrue(saved.getClassificationKeywords().contains("retard intervention"));
    }

    @Test
    void analyzeClassificationShouldReturnExpectedLevel() {
        ReclamationService.ClassificationPreview preview = reclamationService.analyzeClassification(
                "retard intervention",
                "Retard intervention depuis plusieurs jours."
        );

        assertEquals(ReclamationClassificationLevel.FORT, preview.level());
        assertTrue(preview.score() >= 70);
    }

    @Test
    void shouldNormalizeRatingWhenCreatingReclamation() {
        Reclamation reclamation = new Reclamation();
        reclamation.setUserName("Karim");
        reclamation.setUserEmail("karim@mail.com");
        reclamation.setSubject("Spring Boot");
        reclamation.setDescription("Probleme video");
        reclamation.setRating(9);

        Reclamation saved = reclamationService.create(reclamation);

        assertEquals(5, saved.getRating());
        verify(reclamationRepository).save(any(Reclamation.class));
    }

    @Test
    void shouldComputeReclamationRatingSummaryAndRanking() {
        when(reclamationRepository.findAll()).thenReturn(List.of(
                buildReclamation("Spring Boot", 5),
                buildReclamation("Spring Boot", 3),
                buildReclamation("Angular", 4)
        ));

        ReclamationService.ReclamationRatingSummary summary = reclamationService.getRatingSummary();
        assertEquals(4.0, summary.averageRating(), 0.001);
        assertEquals(3, summary.reclamationCount());
        assertEquals(3, summary.ratedReclamationCount());
        assertEquals(2, summary.rankedCourseCount());

        List<ReclamationService.ReclamationCourseRatingStats> ranking = reclamationService.getCourseRanking(2);
        assertEquals(2, ranking.size());
        assertEquals("Spring Boot", ranking.get(0).courseTitle());
        assertEquals(4.0, ranking.get(0).averageRating(), 0.001);
        assertEquals(2, ranking.get(0).reclamationCount());
    }

    @Test
    void shouldReturnStatsRowsByCourse() {
        when(reclamationRepository.findAll()).thenReturn(List.of(
                buildReclamation("NodeJS", 2),
                buildReclamation("NodeJS", 4),
                buildReclamation("Java", 5)
        ));

        List<ReclamationService.ReclamationCourseRatingStats> stats = reclamationService.getCourseRatingStats();

        assertEquals(2, stats.size());
        assertNotNull(stats.get(0).courseTitle());
    }

    private Reclamation buildReclamation(String subject, int rating) {
        Reclamation reclamation = new Reclamation();
        reclamation.setUserName("User");
        reclamation.setUserEmail("user@mail.com");
        reclamation.setSubject(subject);
        reclamation.setDescription("Description");
        reclamation.setRating(rating);
        return reclamation;
    }
}
