package tn.esprit.activities_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "game_result")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameResult {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "patient_id", nullable = false)
    private Long patientId;
    
    @Column(name = "patient_email")
    private String patientEmail;
    
    @Column(name = "patient_name")
    private String patientName;
    
    @Column(name = "activity_type", nullable = false)
    private String activityType;
    
    @Column(name = "activity_id", nullable = false)
    private Long activityId;
    
    @Column(name = "activity_title")
    private String activityTitle;
    
    /** Raw score (correct answers * points) */
    private Integer score = 0;
    
    @Column(name = "max_score")
    private Integer maxScore = 100;
    
    /** Intelligent weighted score (0-100) based on difficulty, time, accuracy */
    @Column(name = "weighted_score")
    private Double weightedScore = 0.0;
    
    /** Difficulty of the activity: EASY, MEDIUM, HARD */
    private String difficulty;
    
    @Column(name = "total_questions")
    private Integer totalQuestions = 0;
    
    @Column(name = "correct_answers")
    private Integer correctAnswers = 0;
    
    @Column(name = "time_spent_seconds")
    private Integer timeSpentSeconds = 0;
    
    /** Average time per question in seconds */
    @Column(name = "avg_response_time")
    private Double avgResponseTime = 0.0;
    
    /** Alzheimer risk level: LOW, MEDIUM, HIGH, CRITICAL */
    @Column(name = "risk_level")
    private String riskLevel;
    
    /** Whether an alert email was sent */
    @Column(name = "alert_sent")
    private Boolean alertSent = false;
    
    @Column(name = "completed_at")
    private java.util.Date completedAt;
    
    @PrePersist
    protected void onCreate() {
        completedAt = new java.util.Date();
    }
}
