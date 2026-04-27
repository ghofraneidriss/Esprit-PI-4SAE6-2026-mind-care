package tn.esprit.activities_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** Minimal quiz payload for fast read-only preview. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizPreviewDTO {
    private String title;
    private String description;
    private String difficulty;
    private List<QuizPreviewQuestionDTO> questions = new ArrayList<>();
}
