package tn.esprit.activities_service.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "quiz_question")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "question")
    private String text;

    @Column(nullable = false, name = "optiona")
    private String optionA;

    @Column(nullable = false, name = "optionb")
    private String optionB;

    @Column(nullable = false, name = "optionc")
    private String optionC;

    @Column(name = "optiond")
    private String optionD;

    @Column(nullable = false, name = "correct_answer")
    private String correctAnswer;

    @Column(nullable = false, name = "points")
    private int score;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_activity_id")
    @JsonIgnore
    private QuizActivity quiz;
}
