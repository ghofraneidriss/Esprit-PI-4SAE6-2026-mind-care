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
        // Seed demo data only when the database is empty
        if (quizActivityRepository.count() == 0) {
            QuizActivity quiz1 = new QuizActivity();
            quiz1.setTitle("Memory quiz");
            quiz1.setDescription("Practice your memory with this quiz");
            quiz1.setType("QUIZ");
            quiz1.setTheme("MEMORY");
            quiz1.setLevel("EASY");
            quiz1.setDifficulty("EASY");
            quiz1.setCreatedAt(new Date());
            quiz1.setUpdatedAt(new Date());

            Question q1 = new Question(null, "What is the capital of France?", "Paris", "London", "Berlin",
                    null,
                    "Paris", 10, quiz1);
            Question q2 = new Question(null, "What color is the sky on a clear day?", "Blue", "Green", "Red",
                    null,
                    "Blue", 10, quiz1);
            quiz1.setQuestions(Arrays.asList(q1, q2));

            QuizActivity quiz2 = new QuizActivity();
            quiz2.setTitle("Logic quiz");
            quiz2.setDescription("Practice your reasoning skills");
            quiz2.setType("QUIZ");
            quiz2.setTheme("LOGIC");
            quiz2.setLevel("MEDIUM");
            quiz2.setDifficulty("MEDIUM");
            quiz2.setCreatedAt(new Date());
            quiz2.setUpdatedAt(new Date());

            Question q3 = new Question(null, "If A=1 and B=2, what is A+B?", "2", "3", "4", null, "3", 10, quiz2);
            quiz2.setQuestions(Arrays.asList(q3));

            QuizActivity quiz3 = new QuizActivity();
            quiz3.setTitle("Math quiz");
            quiz3.setDescription("Simple math problems");
            quiz3.setType("QUIZ");
            quiz3.setTheme("MATH");
            quiz3.setLevel("HARD");
            quiz3.setDifficulty("HARD");
            quiz3.setCreatedAt(new Date());
            quiz3.setUpdatedAt(new Date());

            quizActivityRepository.save(quiz1);
            quizActivityRepository.save(quiz2);
            quizActivityRepository.save(quiz3);

            System.out.println("Demo quiz data initialized.");
        }
    }
}
