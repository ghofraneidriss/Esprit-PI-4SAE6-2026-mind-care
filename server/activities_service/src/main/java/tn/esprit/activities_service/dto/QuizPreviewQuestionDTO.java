package tn.esprit.activities_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Lightweight question payload for read-only preview (no scores or correct answers). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuizPreviewQuestionDTO {
    private String text;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
}
