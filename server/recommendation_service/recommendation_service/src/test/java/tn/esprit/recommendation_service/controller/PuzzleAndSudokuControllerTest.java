package tn.esprit.recommendation_service.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import tn.esprit.recommendation_service.dto.puzzle.PuzzleCreateRequest;
import tn.esprit.recommendation_service.dto.puzzle.PuzzleLeaderboardEntry;
import tn.esprit.recommendation_service.dto.puzzle.PuzzleResponse;
import tn.esprit.recommendation_service.dto.puzzle.PuzzleSessionResponse;
import tn.esprit.recommendation_service.dto.puzzle.PuzzleSessionStartResponse;
import tn.esprit.recommendation_service.dto.puzzle.PuzzleSessionSubmitRequest;
import tn.esprit.recommendation_service.dto.puzzle.PuzzleUpdateRequest;
import tn.esprit.recommendation_service.dto.sudoku.SudokuCreateRequest;
import tn.esprit.recommendation_service.dto.sudoku.SudokuResponse;
import tn.esprit.recommendation_service.dto.sudoku.SudokuSessionResponse;
import tn.esprit.recommendation_service.dto.sudoku.SudokuSessionStartResponse;
import tn.esprit.recommendation_service.dto.sudoku.SudokuSessionSubmitRequest;
import tn.esprit.recommendation_service.enums.DifficultyLevel;
import tn.esprit.recommendation_service.enums.PuzzleStatus;
import tn.esprit.recommendation_service.service.SouvenirPuzzleService;
import tn.esprit.recommendation_service.service.SudokuService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PuzzleAndSudokuControllerTest {

    @Mock
    private SouvenirPuzzleService souvenirPuzzleService;

    @Mock
    private SudokuService sudokuService;

    @InjectMocks
    private SouvenirPuzzleController souvenirPuzzleController;

    @InjectMocks
    private SudokuController sudokuController;

    @Test
    void shouldDelegatePuzzleEndpoints() {
        PuzzleCreateRequest createRequest = PuzzleCreateRequest.builder()
                .souvenirEntryId(10L)
                .patientId(2L)
                .title("Puzzle")
                .difficulty(DifficultyLevel.EASY)
                .build();
        PuzzleUpdateRequest updateRequest = PuzzleUpdateRequest.builder()
                .title("Puzzle maj")
                .description("desc")
                .difficulty(DifficultyLevel.MEDIUM)
                .timeLimitSeconds(400)
                .maxHints(2)
                .status(PuzzleStatus.ACTIVE)
                .build();
        PuzzleSessionSubmitRequest submitRequest = PuzzleSessionSubmitRequest.builder()
                .patientId(2L)
                .durationSeconds(100)
                .movesCount(20)
                .completed(true)
                .completionPercent(100)
                .build();

        PuzzleResponse response = PuzzleResponse.builder()
                .id(1L)
                .medicalEventId(100L)
                .patientId(2L)
                .title("Puzzle")
                .difficulty(DifficultyLevel.EASY)
                .status(PuzzleStatus.ACTIVE)
                .build();
        PuzzleSessionStartResponse sessionStart = PuzzleSessionStartResponse.builder()
                .sessionId(4L)
                .puzzleId(1L)
                .patientId(2L)
                .build();
        PuzzleSessionResponse sessionResponse = PuzzleSessionResponse.builder()
                .id(4L)
                .puzzleId(1L)
                .patientId(2L)
                .score(90)
                .build();
        PuzzleLeaderboardEntry leaderboard = PuzzleLeaderboardEntry.builder()
                .rank(1)
                .patientId(2L)
                .bestScore(90)
                .build();

        when(souvenirPuzzleService.createPuzzle(createRequest)).thenReturn(response);
        when(souvenirPuzzleService.getPuzzleById(1L)).thenReturn(response);
        when(souvenirPuzzleService.getPuzzlesByPatient(2L)).thenReturn(List.of(response));
        when(souvenirPuzzleService.getPuzzleByMedicalEvent(100L)).thenReturn(response);
        when(souvenirPuzzleService.updatePuzzle(1L, updateRequest)).thenReturn(response);
        when(souvenirPuzzleService.startSession(1L, 2L)).thenReturn(sessionStart);
        when(souvenirPuzzleService.submitSession(1L, 4L, submitRequest)).thenReturn(sessionResponse);
        when(souvenirPuzzleService.getSessionsByPatient(1L, 2L)).thenReturn(List.of(sessionResponse));
        when(souvenirPuzzleService.getLeaderboard(1L)).thenReturn(List.of(leaderboard));

        assertThat(souvenirPuzzleController.create(createRequest).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(souvenirPuzzleController.getById(1L).getBody()).isEqualTo(response);
        assertThat(souvenirPuzzleController.getByPatient(2L).getBody()).containsExactly(response);
        assertThat(souvenirPuzzleController.getByEvent(100L).getBody()).isEqualTo(response);
        assertThat(souvenirPuzzleController.update(1L, updateRequest).getBody()).isEqualTo(response);
        assertThat(souvenirPuzzleController.delete(1L).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(souvenirPuzzleController.startSession(1L, 2L).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(souvenirPuzzleController.submitSession(1L, 4L, submitRequest).getBody()).isEqualTo(sessionResponse);
        assertThat(souvenirPuzzleController.getSessionsByPatient(1L, 2L).getBody()).containsExactly(sessionResponse);
        assertThat(souvenirPuzzleController.getLeaderboard(1L).getBody()).containsExactly(leaderboard);

        verify(souvenirPuzzleService).deletePuzzle(1L);
    }

    @Test
    void shouldDelegateSudokuEndpoints() {
        SudokuCreateRequest createRequest = SudokuCreateRequest.builder()
                .patientId(7L)
                .title("Sudoku")
                .difficulty(DifficultyLevel.EASY)
                .timeLimitSeconds(300)
                .build();
        SudokuSessionSubmitRequest submitRequest = SudokuSessionSubmitRequest.builder()
                .patientId(7L)
                .durationSeconds(110)
                .errorsCount(1)
                .hintsUsed(0)
                .completionPercent(100)
                .completed(true)
                .build();

        SudokuResponse response = SudokuResponse.builder()
                .id(3L)
                .medicalEventId(50L)
                .patientId(7L)
                .difficulty(DifficultyLevel.EASY)
                .build();
        SudokuSessionStartResponse startResponse = SudokuSessionStartResponse.builder()
                .sessionId(9L)
                .gameId(3L)
                .patientId(7L)
                .build();
        SudokuSessionResponse sessionResponse = SudokuSessionResponse.builder()
                .id(9L)
                .gameId(3L)
                .patientId(7L)
                .score(470)
                .build();

        when(sudokuService.createGame(createRequest)).thenReturn(response);
        when(sudokuService.getById(3L)).thenReturn(response);
        when(sudokuService.getByEvent(50L)).thenReturn(response);
        when(sudokuService.getByPatient(7L)).thenReturn(List.of(response));
        when(sudokuService.startSession(3L, 7L)).thenReturn(startResponse);
        when(sudokuService.submitSession(3L, 9L, submitRequest)).thenReturn(sessionResponse);
        when(sudokuService.getSessionsByPatient(3L, 7L)).thenReturn(List.of(sessionResponse));

        assertThat(sudokuController.createGame(createRequest).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(sudokuController.getById(3L).getBody()).isEqualTo(response);
        assertThat(sudokuController.getByEvent(50L).getBody()).isEqualTo(response);
        assertThat(sudokuController.getByPatient(7L).getBody()).containsExactly(response);
        assertThat(sudokuController.startSession(3L, 7L).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(sudokuController.submitSession(3L, 9L, submitRequest).getBody()).isEqualTo(sessionResponse);
        assertThat(sudokuController.getSessionsByPatient(3L, 7L).getBody()).containsExactly(sessionResponse);
    }
}
