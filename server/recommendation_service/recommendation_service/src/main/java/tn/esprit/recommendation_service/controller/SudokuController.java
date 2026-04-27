package tn.esprit.recommendation_service.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.recommendation_service.dto.sudoku.SudokuCreateRequest;
import tn.esprit.recommendation_service.dto.sudoku.SudokuResponse;
import tn.esprit.recommendation_service.dto.sudoku.SudokuSessionResponse;
import tn.esprit.recommendation_service.dto.sudoku.SudokuSessionStartResponse;
import tn.esprit.recommendation_service.dto.sudoku.SudokuSessionSubmitRequest;
import tn.esprit.recommendation_service.service.SudokuService;

import java.util.List;

@RestController
@RequestMapping("/api/sudoku")
@RequiredArgsConstructor
public class SudokuController {

    private final SudokuService sudokuService;

    @PostMapping
    public ResponseEntity<SudokuResponse> createGame(@Valid @RequestBody SudokuCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sudokuService.createGame(request));
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<SudokuResponse> getById(@PathVariable Long gameId) {
        return ResponseEntity.ok(sudokuService.getById(gameId));
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<SudokuResponse> getByEvent(@PathVariable Long eventId) {
        return ResponseEntity.ok(sudokuService.getByEvent(eventId));
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<SudokuResponse>> getByPatient(@PathVariable Long patientId) {
        return ResponseEntity.ok(sudokuService.getByPatient(patientId));
    }

    @PostMapping("/{gameId}/sessions/start")
    public ResponseEntity<SudokuSessionStartResponse> startSession(
            @PathVariable Long gameId,
            @RequestParam Long patientId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sudokuService.startSession(gameId, patientId));
    }

    @PostMapping("/{gameId}/sessions/{sessionId}/submit")
    public ResponseEntity<SudokuSessionResponse> submitSession(
            @PathVariable Long gameId,
            @PathVariable Long sessionId,
            @Valid @RequestBody SudokuSessionSubmitRequest request) {
        return ResponseEntity.ok(sudokuService.submitSession(gameId, sessionId, request));
    }

    @GetMapping("/{gameId}/sessions")
    public ResponseEntity<List<SudokuSessionResponse>> getSessionsByPatient(
            @PathVariable Long gameId,
            @RequestParam Long patientId) {
        return ResponseEntity.ok(sudokuService.getSessionsByPatient(gameId, patientId));
    }
}
