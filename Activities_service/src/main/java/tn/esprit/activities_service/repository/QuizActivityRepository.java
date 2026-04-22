package tn.esprit.activities_service.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.activities_service.entity.QuizActivity;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuizActivityRepository extends JpaRepository<QuizActivity, Long> {

    /** Single query with questions — avoids N+1 when serializing JSON. */
    @Query("SELECT DISTINCT q FROM QuizActivity q LEFT JOIN FETCH q.questions WHERE q.id = :id")
    Optional<QuizActivity> findByIdWithQuestions(@Param("id") Long id);

    /** List all quizzes with questions in one round-trip (admin catalog). */
    @Query("SELECT DISTINCT q FROM QuizActivity q LEFT JOIN FETCH q.questions")
    List<QuizActivity> findAllWithQuestions();
    
    List<QuizActivity> findByTheme(String theme);

    /** Recommandations : limiter le nombre de lignes en base (évite de charger tout le catalogue). */
    List<QuizActivity> findByThemeIgnoreCaseOrderByIdAsc(String theme, Pageable pageable);
    
    List<QuizActivity> findByDifficulty(String difficulty);
    
    List<QuizActivity> findByThemeAndDifficulty(String theme, String difficulty);
    
    @Query("SELECT q FROM QuizActivity q WHERE q.title LIKE %:title%")
    List<QuizActivity> findByTitleContaining(String title);
    
    @Query("SELECT q FROM QuizActivity q ORDER BY q.createdAt DESC")
    List<QuizActivity> findAllOrderByCreatedAtDesc();
}
