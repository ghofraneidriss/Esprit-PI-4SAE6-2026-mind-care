package tn.esprit.activities_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Stores the maximum number of quizzes a patient is allowed to complete.
 * Set by ADMIN or DOCTOR. One entry per patient.
 */
@Entity
@Table(name = "quiz_limit")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The patient this limit applies to */
    @Column(name = "patient_id", nullable = false, unique = true)
    private Long patientId;

    /** Maximum number of quizzes the patient can complete (total game results) */
    @Column(name = "max_quizzes", nullable = false)
    private Integer maxQuizzes = 10;

    /** Who set this limit (user ID of admin/doctor) */
    @Column(name = "set_by")
    private Long setBy;

    /** Name of the person who set the limit */
    @Column(name = "set_by_name")
    private String setByName;

    @Column(name = "updated_at")
    private java.util.Date updatedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        updatedAt = new java.util.Date();
    }
}
