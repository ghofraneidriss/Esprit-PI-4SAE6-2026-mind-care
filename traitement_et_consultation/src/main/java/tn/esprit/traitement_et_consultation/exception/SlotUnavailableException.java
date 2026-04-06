package tn.esprit.traitement_et_consultation.exception;

import tn.esprit.traitement_et_consultation.dto.SlotSuggestionResponse;

public class SlotUnavailableException extends RuntimeException {
    private final SlotSuggestionResponse suggestionResponse;

    public SlotUnavailableException(SlotSuggestionResponse suggestionResponse) {
        super(suggestionResponse.getMessage());
        this.suggestionResponse = suggestionResponse;
    }

    public SlotSuggestionResponse getSuggestionResponse() {
        return suggestionResponse;
    }
}
