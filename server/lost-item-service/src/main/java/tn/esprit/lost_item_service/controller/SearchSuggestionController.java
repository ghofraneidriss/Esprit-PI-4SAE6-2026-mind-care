package tn.esprit.lost_item_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.lost_item_service.service.SearchSuggestionService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lost-items")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class SearchSuggestionController {

    private final SearchSuggestionService suggestionService;

    /**
     * GET /api/lost-items/search-suggestions?patientId=5&category=CLOTHING
     *
     * Returns up to 3 location suggestions ranked by historical success rate
     * for the given patient (and optional category).
     */
    @GetMapping("/search-suggestions")
    public ResponseEntity<List<Map<String, Object>>> getSearchSuggestions(
            @RequestParam Long patientId,
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(suggestionService.getSuggestions(patientId, category));
    }
}
