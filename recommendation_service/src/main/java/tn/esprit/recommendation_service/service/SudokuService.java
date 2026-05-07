package tn.esprit.recommendation_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.recommendation_service.dto.sudoku.SudokuCreateRequest;
import tn.esprit.recommendation_service.dto.sudoku.SudokuResponse;
import tn.esprit.recommendation_service.dto.sudoku.SudokuSessionResponse;
import tn.esprit.recommendation_service.dto.sudoku.SudokuSessionStartResponse;
import tn.esprit.recommendation_service.dto.sudoku.SudokuSessionSubmitRequest;
import tn.esprit.recommendation_service.entity.MedicalEvent;
import tn.esprit.recommendation_service.entity.SudokuGame;
import tn.esprit.recommendation_service.entity.SudokuSession;
import tn.esprit.recommendation_service.enums.DifficultyLevel;
import tn.esprit.recommendation_service.enums.MedicalEventStatus;
import tn.esprit.recommendation_service.enums.MedicalEventType;
import tn.esprit.recommendation_service.exception.BusinessException;
import tn.esprit.recommendation_service.exception.ResourceNotFoundException;
import tn.esprit.recommendation_service.repository.MedicalEventRepository;
import tn.esprit.recommendation_service.repository.SudokuGameRepository;
import tn.esprit.recommendation_service.repository.SudokuSessionRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Transactional
public class SudokuService {

    private final SudokuGameRepository sudokuGameRepository;
    private final SudokuSessionRepository sudokuSessionRepository;
    private final MedicalEventRepository medicalEventRepository;
    private final ObjectMapper objectMapper;

    private final Random random = new Random();

    // ── Grid generation ──────────────────────────────────────────────────────

    public int[][] generateSolution(int size) {
        int[][] grid = new int[size][size];
        fillGrid(grid, size);
        return grid;
    }

    private boolean fillGrid(int[][] grid, int size) {
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                if (grid[row][col] == 0) {
                    List<Integer> numbers = new ArrayList<>();
                    for (int n = 1; n <= size; n++) numbers.add(n);
                    Collections.shuffle(numbers, random);
                    for (int num : numbers) {
                        if (isValid(grid, row, col, num, size)) {
                            grid[row][col] = num;
                            if (fillGrid(grid, size)) return true;
                            grid[row][col] = 0;
                        }
                    }
                    return false;
                }
            }
        }
        return true;
    }

    public int[][] generatePuzzle(int[][] solution, DifficultyLevel difficulty) {
        int size = solution.length;
        int[][] puzzle = new int[size][size];
        for (int r = 0; r < size; r++) {
            puzzle[r] = solution[r].clone();
        }

        // For EASY 4x4: remove 6 cells
        int cellsToRemove = 6;
        List<int[]> positions = new ArrayList<>();
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                positions.add(new int[]{r, c});
            }
        }
        Collections.shuffle(positions, random);
        for (int i = 0; i < cellsToRemove && i < positions.size(); i++) {
            puzzle[positions.get(i)[0]][positions.get(i)[1]] = 0;
        }
        return puzzle;
    }

    public boolean isValid(int[][] grid, int row, int col, int num, int size) {
        // Check row
        for (int c = 0; c < size; c++) {
            if (grid[row][c] == num) return false;
        }
        // Check column
        for (int r = 0; r < size; r++) {
            if (grid[r][col] == num) return false;
        }
        // Check 2x2 box (for 4x4 grid)
        int boxSize = (int) Math.sqrt(size);
        int boxRowStart = (row / boxSize) * boxSize;
        int boxColStart = (col / boxSize) * boxSize;
        for (int r = boxRowStart; r < boxRowStart + boxSize; r++) {
            for (int c = boxColStart; c < boxColStart + boxSize; c++) {
                if (grid[r][c] == num) return false;
            }
        }
        return true;
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    public SudokuResponse createGame(SudokuCreateRequest request) {
        if (request.getPatientId() == null || request.getPatientId() <= 0) {
            throw new BusinessException("patientId must be a positive non-null value.");
        }

        int size = 4; // EASY = 4x4
        int[][] solution = generateSolution(size);
        int[][] puzzle = generatePuzzle(solution, request.getDifficulty());

        LocalDateTime now = LocalDateTime.now();
        MedicalEvent event = medicalEventRepository.save(MedicalEvent.builder()
                .title(request.getTitle() != null && !request.getTitle().isBlank()
                        ? request.getTitle().trim()
                        : "Sudoku thérapeutique")
                .description(request.getDescription() != null ? request.getDescription() : "Exercice Sudoku 4×4")
                .type(MedicalEventType.SUDOKU)
                .difficulty(request.getDifficulty())
                .status(MedicalEventStatus.ACTIVE)
                .patientId(request.getPatientId())
                .startDate(now)
                .endDate(now.plusDays(30))
                .autoGenerated(Boolean.FALSE)
                .build());

        SudokuGame game = sudokuGameRepository.save(SudokuGame.builder()
                .medicalEvent(event)
                .patientId(request.getPatientId())
                .difficulty(request.getDifficulty())
                .puzzle(toJson(puzzle))
                .solution(toJson(solution))
                .gridSize(size)
                .timeLimitSeconds(request.getTimeLimitSeconds() != null ? request.getTimeLimitSeconds() : 300)
                .active(Boolean.TRUE)
                .completedSessions(0)
                .build());

        return toResponse(game);
    }

    @Transactional(readOnly = true)
    public SudokuResponse getById(Long gameId) {
        return toResponse(getGameOrThrow(gameId));
    }

    @Transactional(readOnly = true)
    public SudokuResponse getByEvent(Long eventId) {
        SudokuGame game = sudokuGameRepository.findByMedicalEvent_Id(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("SudokuGame not found for event: " + eventId));
        return toResponse(game);
    }

    @Transactional(readOnly = true)
    public List<SudokuResponse> getByPatient(Long patientId) {
        return sudokuGameRepository.findByPatientIdOrderByCreatedAtDesc(patientId).stream()
                .map(this::toResponse)
                .toList();
    }

    // ── Sessions ─────────────────────────────────────────────────────────────

    public SudokuSessionStartResponse startSession(Long gameId, Long patientId) {
        SudokuGame game = getGameOrThrow(gameId);

        SudokuSession session = sudokuSessionRepository.save(SudokuSession.builder()
                .sudokuGame(game)
                .patientId(patientId)
                .startedAt(LocalDateTime.now())
                .completed(Boolean.FALSE)
                .abandoned(Boolean.FALSE)
                .errorsCount(0)
                .hintsUsed(0)
                .completionPercent(0)
                .build());

        return SudokuSessionStartResponse.builder()
                .sessionId(session.getId())
                .gameId(gameId)
                .patientId(patientId)
                .startedAt(session.getStartedAt())
                .build();
    }

    public SudokuSessionResponse submitSession(Long gameId, Long sessionId, SudokuSessionSubmitRequest request) {
        SudokuGame game = getGameOrThrow(gameId);
        SudokuSession session = sudokuSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("SudokuSession not found: " + sessionId));

        if (!session.getSudokuGame().getId().equals(game.getId())) {
            throw new BusinessException("Session does not belong to the requested game.");
        }
        if (!session.getPatientId().equals(request.getPatientId())) {
            throw new BusinessException("Session patient does not match.");
        }
        if (session.getFinishedAt() != null) {
            throw new BusinessException("Session has already been submitted.");
        }

        int duration = defaultIfNull(request.getDurationSeconds(), 0);
        int errors = defaultIfNull(request.getErrorsCount(), 0);
        int hints = defaultIfNull(request.getHintsUsed(), 0);
        int completionPercent = defaultIfNull(request.getCompletionPercent(), 0);
        boolean completed = Boolean.TRUE.equals(request.getCompleted()) && completionPercent >= 100;
        boolean abandoned = Boolean.TRUE.equals(request.getAbandoned());

        session.setFinishedAt(LocalDateTime.now());
        session.setDurationSeconds(duration);
        session.setErrorsCount(errors);
        session.setHintsUsed(hints);
        session.setCompletionPercent(Math.min(completionPercent, 100));
        session.setCompleted(completed);
        session.setAbandoned(abandoned);

        int score = abandoned ? 0 : calculateScore(game, duration, errors, hints, completed);
        session.setScore(score);

        SudokuSession saved = sudokuSessionRepository.save(session);

        // Update bestScore if completed
        if (completed && completionPercent >= 100) {
            game.setCompletedSessions((game.getCompletedSessions() == null ? 0 : game.getCompletedSessions()) + 1);
            if (game.getBestScore() == null || score > game.getBestScore()) {
                game.setBestScore(score);
            }
            sudokuGameRepository.save(game);
        }

        return toSessionResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<SudokuSessionResponse> getSessionsByPatient(Long gameId, Long patientId) {
        getGameOrThrow(gameId);
        return sudokuSessionRepository
                .findBySudokuGame_IdAndPatientIdOrderByStartedAtDesc(gameId, patientId)
                .stream()
                .map(this::toSessionResponse)
                .toList();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private SudokuGame getGameOrThrow(Long gameId) {
        return sudokuGameRepository.findById(gameId)
                .orElseThrow(() -> new ResourceNotFoundException("SudokuGame not found: " + gameId));
    }

    private int calculateScore(SudokuGame game, int durationSeconds, int errorsCount, int hintsUsed, boolean completed) {
        if (!completed) return 0;
        int timeLimit = game.getTimeLimitSeconds() != null ? game.getTimeLimitSeconds() : 300;
        int timePenalty = Math.max(0, (durationSeconds - timeLimit)) / 5;
        int score = 500 - errorsCount * 10 - hintsUsed * 20 - timePenalty;
        return Math.max(0, score);
    }

    private SudokuResponse toResponse(SudokuGame game) {
        return SudokuResponse.builder()
                .id(game.getId())
                .medicalEventId(game.getMedicalEvent().getId())
                .patientId(game.getPatientId())
                .difficulty(game.getDifficulty())
                .gridSize(game.getGridSize())
                .puzzle(game.getPuzzle())
                .timeLimitSeconds(game.getTimeLimitSeconds())
                .active(game.getActive())
                .bestScore(game.getBestScore())
                .completedSessions(game.getCompletedSessions())
                .createdAt(game.getCreatedAt())
                .build();
    }

    private SudokuSessionResponse toSessionResponse(SudokuSession session) {
        return SudokuSessionResponse.builder()
                .id(session.getId())
                .gameId(session.getSudokuGame().getId())
                .patientId(session.getPatientId())
                .startedAt(session.getStartedAt())
                .finishedAt(session.getFinishedAt())
                .durationSeconds(session.getDurationSeconds())
                .errorsCount(session.getErrorsCount())
                .hintsUsed(session.getHintsUsed())
                .completionPercent(session.getCompletionPercent())
                .score(session.getScore())
                .completed(session.getCompleted())
                .abandoned(session.getAbandoned())
                .createdAt(session.getCreatedAt())
                .build();
    }

    public String toJson(int[][] grid) {
        try {
            return objectMapper.writeValueAsString(grid);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize grid to JSON", e);
        }
    }

    private int defaultIfNull(Integer value, int fallback) {
        return value == null ? fallback : value;
    }
}
