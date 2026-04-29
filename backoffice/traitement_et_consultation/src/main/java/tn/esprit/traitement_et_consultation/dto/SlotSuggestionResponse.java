package tn.esprit.traitement_et_consultation.dto;

import java.time.LocalDateTime;

public class SlotSuggestionResponse {
    private String message;
    private LocalDateTime optionA;
    private LocalDateTime optionB;

    public SlotSuggestionResponse() {
    }

    public SlotSuggestionResponse(String message, LocalDateTime optionA, LocalDateTime optionB) {
        this.message = message;
        this.optionA = optionA;
        this.optionB = optionB;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getOptionA() {
        return optionA;
    }

    public void setOptionA(LocalDateTime optionA) {
        this.optionA = optionA;
    }

    public LocalDateTime getOptionB() {
        return optionB;
    }

    public void setOptionB(LocalDateTime optionB) {
        this.optionB = optionB;
    }
}
