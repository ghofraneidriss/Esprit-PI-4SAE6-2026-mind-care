package tn.esprit.activities_service.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.activities_service.entity.QuizActivity;
import tn.esprit.activities_service.service.QuizActivityService;

import java.util.List;

@RestController
@RequestMapping("/api/simple")
public class SimpleQuizController {

    @Autowired
    private QuizActivityService quizActivityService;

    @GetMapping("/quizzes")
    public ResponseEntity<List<QuizActivity>> getQuizzes() {
        List<QuizActivity> quizzes = quizActivityService.getAllQuizActivities();
        return ResponseEntity.ok(quizzes);
    }
}
