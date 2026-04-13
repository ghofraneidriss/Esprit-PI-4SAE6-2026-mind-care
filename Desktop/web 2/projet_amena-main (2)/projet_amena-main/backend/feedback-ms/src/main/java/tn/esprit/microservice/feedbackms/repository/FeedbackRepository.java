package tn.esprit.microservice.feedbackms.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.microservice.feedbackms.entity.Feedback;
import tn.esprit.microservice.feedbackms.entity.FeedbackModerationStatus;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

	List<Feedback> findByModerationStatusOrderByCreatedAtDesc(FeedbackModerationStatus moderationStatus);
}
