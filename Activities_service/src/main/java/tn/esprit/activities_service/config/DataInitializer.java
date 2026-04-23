package tn.esprit.activities_service.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import tn.esprit.activities_service.entity.QuizActivity;
import tn.esprit.activities_service.repository.QuizActivityRepository;

import java.util.Date;

import tn.esprit.activities_service.entity.Question;
import java.util.Arrays;

@Component
public class DataInitializer implements CommandLineRunner {

    private final QuizActivityRepository quizActivityRepository;

    public DataInitializer(QuizActivityRepository quizActivityRepository) {
        this.quizActivityRepository = quizActivityRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (quizActivityRepository.count() == 0) {
            // Créer des quiz de test
            QuizActivity quiz1 = new QuizActivity();
            quiz1.setTitle("Quiz Mémoire");
            quiz1.setDescription("Testez votre mémoire avec ce quiz");
            quiz1.setType("QUIZ");
            quiz1.setTheme("MEMORY");
            quiz1.setLevel("EASY");
            quiz1.setDifficulty("EASY");
            quiz1.setCreatedAt(new Date());
            quiz1.setUpdatedAt(new Date());

            Question q1 = new Question();
            q1.setText("Quelle est la capitale de la France ?");
            q1.setOptionA("Paris");
            q1.setOptionB("Londres");
            q1.setOptionC("Berlin");
            q1.setCorrectAnswer("Paris");
            q1.setScore(10);
            q1.setQuiz(quiz1);

            Question q2 = new Question();
            q2.setText("Quelle est la couleur du ciel par beau temps ?");
            q2.setOptionA("Bleu");
            q2.setOptionB("Vert");
            q2.setOptionC("Rouge");
            q2.setCorrectAnswer("Bleu");
            q2.setScore(10);
            q2.setQuiz(quiz1);

            quiz1.getQuestions().add(q1);
            quiz1.getQuestions().add(q2);

            QuizActivity quiz2 = new QuizActivity();
            quiz2.setTitle("Quiz Logique");
            quiz2.setDescription("Exercez votre logique");
            quiz2.setType("QUIZ");
            quiz2.setTheme("LOGIC");
            quiz2.setLevel("MEDIUM");
            quiz2.setDifficulty("MEDIUM");
            quiz2.setCreatedAt(new Date());
            quiz2.setUpdatedAt(new Date());

            Question q3 = new Question();
            q3.setText("Si A=1 et B=2, combien vaut A+B ?");
            q3.setOptionA("2");
            q3.setOptionB("3");
            q3.setOptionC("4");
            q3.setCorrectAnswer("3");
            q3.setScore(10);
            q3.setQuiz(quiz2);

            quiz2.getQuestions().add(q3);

            quizActivityRepository.save(quiz1);
            quizActivityRepository.save(quiz2);

            System.out.println("Données de test initialisées avec succès !");
        }
    }
}
