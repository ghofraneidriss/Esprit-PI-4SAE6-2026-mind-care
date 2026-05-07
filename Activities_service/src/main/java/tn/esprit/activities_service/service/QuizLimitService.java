package tn.esprit.activities_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.activities_service.entity.QuizLimit;
import tn.esprit.activities_service.repository.GameResultRepository;
import tn.esprit.activities_service.repository.QuizLimitRepository;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class QuizLimitService {

    private static final Logger log = LoggerFactory.getLogger(QuizLimitService.class);

    /** Default limit if no specific limit is set for the patient */
    private static final int DEFAULT_MAX_QUIZZES = 999999;

    @Autowired
    private QuizLimitRepository quizLimitRepository;

    @Autowired
    private GameResultRepository gameResultRepository;

    /**
     * Get the quiz limit for a patient.
     */
    public Optional<QuizLimit> getLimitForPatient(Long patientId) {
        return quizLimitRepository.findByPatientId(patientId);
    }

    /**
     * Get all quiz limits.
     */
    public List<QuizLimit> getAllLimits() {
        return quizLimitRepository.findAll();
    }

    /**
     * Set (create or update) the quiz limit for a patient.
     */
    public QuizLimit setLimit(Long patientId, Integer maxQuizzes, Long setBy, String setByName) {
        QuizLimit limit = quizLimitRepository.findByPatientId(patientId)
                .orElse(new QuizLimit());
        limit.setPatientId(patientId);
        limit.setMaxQuizzes(maxQuizzes);
        limit.setSetBy(setBy);
        limit.setSetByName(setByName);
        QuizLimit saved = quizLimitRepository.save(limit);
        log.info("Quiz limit set: patientId={}, max={}, by={}", patientId, maxQuizzes, setByName);
        return saved;
    }

    /**
     * Remove the quiz limit for a patient (reverts to unlimited).
     */
    public void removeLimit(Long patientId) {
        quizLimitRepository.deleteByPatientId(patientId);
        log.info("Quiz limit removed for patientId={}", patientId);
    }

    /**
     * Check if a patient can still take quizzes.
     * Returns remaining count (0 = blocked, -1 = unlimited/no limit set).
     */
    public int getRemainingQuizzes(Long patientId) {
        Optional<QuizLimit> limitOpt = quizLimitRepository.findByPatientId(patientId);
        if (limitOpt.isEmpty()) {
            return -1; // No limit set = unlimited
        }
        int max = limitOpt.get().getMaxQuizzes();
        long completed = gameResultRepository.findByPatientId(patientId).size();
        return Math.max(0, max - (int) completed);
    }

    /**
     * Check if patient can play (has remaining quizzes or no limit).
     */
    public boolean canPlay(Long patientId) {
        int remaining = getRemainingQuizzes(patientId);
        return remaining != 0; // -1 = unlimited (can play), >0 = has quota
    }

    /**
     * Get status info for frontend display.
     */
    public QuizLimitStatus getStatus(Long patientId) {
        Optional<QuizLimit> limitOpt = quizLimitRepository.findByPatientId(patientId);
        long completed = gameResultRepository.findByPatientId(patientId).size();

        QuizLimitStatus status = new QuizLimitStatus();
        status.patientId = patientId;
        status.completedQuizzes = (int) completed;

        if (limitOpt.isPresent()) {
            QuizLimit limit = limitOpt.get();
            status.hasLimit = true;
            status.maxQuizzes = limit.getMaxQuizzes();
            status.remaining = Math.max(0, limit.getMaxQuizzes() - (int) completed);
            status.canPlay = status.remaining > 0;
        } else {
            status.hasLimit = false;
            status.maxQuizzes = -1;
            status.remaining = -1;
            status.canPlay = true;
        }
        return status;
    }

    /**
     * DTO for quiz limit status.
     */
    public static class QuizLimitStatus {
        public Long patientId;
        public boolean hasLimit;
        public int maxQuizzes;
        public int completedQuizzes;
        public int remaining;
        public boolean canPlay;
    }
}
