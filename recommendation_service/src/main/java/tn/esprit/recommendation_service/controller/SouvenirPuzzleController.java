package tn.esprit.recommendation_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.recommendation_service.dto.puzzle.PuzzleCreateRequest;
import tn.esprit.recommendation_service.dto.puzzle.PuzzleLeaderboardEntry;
import tn.esprit.recommendation_service.dto.puzzle.PuzzleResponse;
import tn.esprit.recommendation_service.dto.puzzle.PuzzleSessionResponse;
import tn.esprit.recommendation_service.dto.puzzle.PuzzleSessionStartResponse;
import tn.esprit.recommendation_service.dto.puzzle.PuzzleSessionSubmitRequest;
import tn.esprit.recommendation_service.dto.puzzle.PuzzleUpdateRequest;
import tn.esprit.recommendation_service.service.SouvenirPuzzleService;

import java.util.List;

@RestController
@RequestMapping("/api/puzzles")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SouvenirPuzzleController {

    private final SouvenirPuzzleService souvenirPuzzleService;

    @PostMapping
    public ResponseEntity<PuzzleResponse> create(@Valid @RequestBody PuzzleCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(souvenirPuzzleService.createPuzzle(request));
    }

    @GetMapping("/{puzzleId}")
    public ResponseEntity<PuzzleResponse> getById(@PathVariable Long puzzleId) {
        return ResponseEntity.ok(souvenirPuzzleService.getPuzzleById(puzzleId));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<PuzzleResponse>> getByPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(souvenirPuzzleService.getPuzzlesByPatient(patientId));
    }

    @GetMapping("/event/{medicalEventId}")
    public ResponseEntity<PuzzleResponse> getByEvent(@PathVariable Long medicalEventId) {
        return ResponseEntity.ok(souvenirPuzzleService.getPuzzleByMedicalEvent(medicalEventId));
    }

    @PutMapping("/{puzzleId}")
    public ResponseEntity<PuzzleResponse> update(@PathVariable Long puzzleId, @Valid @RequestBody PuzzleUpdateRequest request) {
        return ResponseEntity.ok(souvenirPuzzleService.updatePuzzle(puzzleId, request));
    }

    @DeleteMapping("/{puzzleId}")
    public ResponseEntity<Void> delete(@PathVariable Long puzzleId) {
        souvenirPuzzleService.deletePuzzle(puzzleId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{puzzleId}/sessions/start")
    public ResponseEntity<PuzzleSessionStartResponse> startSession(@PathVariable Long puzzleId, @RequestParam Long patientId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(souvenirPuzzleService.startSession(puzzleId, patientId));
    }

    @PostMapping("/{puzzleId}/sessions/{sessionId}/submit")
    public ResponseEntity<PuzzleSessionResponse> submitSession(
            @PathVariable Long puzzleId,
            @PathVariable Long sessionId,
            @Valid @RequestBody PuzzleSessionSubmitRequest request
    ) {
        return ResponseEntity.ok(souvenirPuzzleService.submitSession(puzzleId, sessionId, request));
    }

    @GetMapping("/{puzzleId}/sessions/patient/{patientId}")
    public ResponseEntity<List<PuzzleSessionResponse>> getSessionsByPatient(
            @PathVariable Long puzzleId,
            @PathVariable Long patientId
    ) {
        return ResponseEntity.ok(souvenirPuzzleService.getSessionsByPatient(puzzleId, patientId));
    }

    @GetMapping("/{puzzleId}/leaderboard")
    public ResponseEntity<List<PuzzleLeaderboardEntry>> getLeaderboard(@PathVariable Long puzzleId) {
        return ResponseEntity.ok(souvenirPuzzleService.getLeaderboard(puzzleId));
    }
}
