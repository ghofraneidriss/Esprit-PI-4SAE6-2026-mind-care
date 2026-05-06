package tn.esprit.recommendation_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SudokuServiceTest {

    @Mock
    private SudokuGameRepository sudokuGameRepository;

    @Mock
    private SudokuSessionRepository sudokuSessionRepository;

    @Mock
    private MedicalEventRepository medicalEventRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private SudokuService service;

    private MedicalEvent event;
    private SudokuGame game;

    @BeforeEach
    void setUp() {
        event = MedicalEvent.builder()
                .id(15L)
                .title("Sudoku")
                .type(MedicalEventType.SUDOKU)
                .difficulty(DifficultyLevel.EASY)
                .status(MedicalEventStatus.ACTIVE)
                .patientId(7L)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(1))
                .build();
        game = SudokuGame.builder()
                .id(3L)
                .medicalEvent(event)
                .patientId(7L)
                .difficulty(DifficultyLevel.EASY)
                .puzzle("[[1,0],[0,1]]")
                .solution("[[1,2],[2,1]]")
                .gridSize(4)
                .timeLimitSeconds(300)
                .active(true)
                .completedSessions(0)
                .build();
    }

    @Test
    void generateSolution_shouldCreateValidGrid() {
        int[][] solution = service.generateSolution(4);

        assertThat(solution.length).isEqualTo(4);
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                int value = solution[row][col];
                assertThat(value).isBetween(1, 4);
                int original = solution[row][col];
                solution[row][col] = 0;
                assertThat(service.isValid(solution, row, col, original, 4)).isTrue();
                solution[row][col] = original;
            }
        }
    }

    @Test
    void generatePuzzle_shouldRemoveCells() {
        int[][] puzzle = service.generatePuzzle(new int[][]{
                {1, 2, 3, 4},
                {3, 4, 1, 2},
                {2, 1, 4, 3},
                {4, 3, 2, 1}
        }, DifficultyLevel.EASY);

        long zeros = java.util.Arrays.stream(puzzle)
                .flatMapToInt(java.util.Arrays::stream)
                .filter(value -> value == 0)
                .count();

        assertThat(zeros).isEqualTo(6);
    }

    @Test
    void createGame_shouldRejectInvalidPatientId() {
        SudokuCreateRequest request = SudokuCreateRequest.builder()
                .patientId(0L)
                .difficulty(DifficultyLevel.EASY)
                .build();

        assertThatThrownBy(() -> service.createGame(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("patientId");
    }

    @Test
    void createGame_shouldPersistEventAndGame() {
        SudokuCreateRequest request = SudokuCreateRequest.builder()
                .patientId(7L)
                .title("Sudoku")
                .description("desc")
                .difficulty(DifficultyLevel.EASY)
                .timeLimitSeconds(420)
                .build();
        when(medicalEventRepository.save(any(MedicalEvent.class))).thenReturn(event);
        when(sudokuGameRepository.save(any(SudokuGame.class))).thenAnswer(invocation -> {
            SudokuGame saved = invocation.getArgument(0);
            saved.setId(3L);
            return saved;
        });

        SudokuResponse response = service.createGame(request);

        assertThat(response.getId()).isEqualTo(3L);
        assertThat(response.getPatientId()).isEqualTo(7L);
        assertThat(response.getTimeLimitSeconds()).isEqualTo(420);
    }

    @Test
    void queryMethods_shouldMapGame() {
        when(sudokuGameRepository.findById(3L)).thenReturn(Optional.of(game));
        when(sudokuGameRepository.findByMedicalEvent_Id(15L)).thenReturn(Optional.of(game));
        when(sudokuGameRepository.findByPatientIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(game));

        assertThat(service.getById(3L).getMedicalEventId()).isEqualTo(15L);
        assertThat(service.getByEvent(15L).getId()).isEqualTo(3L);
        assertThat(service.getByPatient(7L)).hasSize(1);
    }

    @Test
    void getById_shouldThrowWhenMissing() {
        when(sudokuGameRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void startSession_shouldPersistNewSession() {
        SudokuSession session = SudokuSession.builder()
                .id(8L)
                .sudokuGame(game)
                .patientId(7L)
                .startedAt(LocalDateTime.now())
                .build();
        when(sudokuGameRepository.findById(3L)).thenReturn(Optional.of(game));
        when(sudokuSessionRepository.save(any(SudokuSession.class))).thenReturn(session);

        SudokuSessionStartResponse response = service.startSession(3L, 7L);

        assertThat(response.getSessionId()).isEqualTo(8L);
        assertThat(response.getGameId()).isEqualTo(3L);
    }

    @Test
    void submitSession_shouldUpdateCompletedGameAndReturnResponse() {
        SudokuSession session = SudokuSession.builder()
                .id(9L)
                .sudokuGame(game)
                .patientId(7L)
                .startedAt(LocalDateTime.now().minusMinutes(3))
                .build();
        SudokuSessionSubmitRequest request = SudokuSessionSubmitRequest.builder()
                .patientId(7L)
                .durationSeconds(200)
                .errorsCount(1)
                .hintsUsed(0)
                .completionPercent(100)
                .completed(true)
                .abandoned(false)
                .build();

        when(sudokuGameRepository.findById(3L)).thenReturn(Optional.of(game));
        when(sudokuSessionRepository.findById(9L)).thenReturn(Optional.of(session));
        when(sudokuSessionRepository.save(any(SudokuSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SudokuSessionResponse response = service.submitSession(3L, 9L, request);

        assertThat(response.getCompleted()).isTrue();
        assertThat(response.getScore()).isGreaterThan(0);
        assertThat(game.getCompletedSessions()).isEqualTo(1);
        assertThat(game.getBestScore()).isEqualTo(response.getScore());
        verify(sudokuGameRepository).save(game);
    }

    @Test
    void submitSession_shouldRejectMismatchesAndDuplicates() {
        SudokuGame otherGame = SudokuGame.builder().id(77L).medicalEvent(event).patientId(7L).difficulty(DifficultyLevel.EASY).puzzle("[]").solution("[]").build();
        SudokuSession session = SudokuSession.builder()
                .id(9L)
                .sudokuGame(otherGame)
                .patientId(7L)
                .startedAt(LocalDateTime.now().minusMinutes(2))
                .build();
        SudokuSessionSubmitRequest request = SudokuSessionSubmitRequest.builder()
                .patientId(7L)
                .completed(true)
                .completionPercent(100)
                .build();

        when(sudokuGameRepository.findById(3L)).thenReturn(Optional.of(game));
        when(sudokuSessionRepository.findById(9L)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.submitSession(3L, 9L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("requested game");

        session.setSudokuGame(game);
        session.setPatientId(99L);
        assertThatThrownBy(() -> service.submitSession(3L, 9L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("patient does not match");

        session.setPatientId(7L);
        session.setFinishedAt(LocalDateTime.now());
        assertThatThrownBy(() -> service.submitSession(3L, 9L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already been submitted");
    }

    @Test
    void getSessionsByPatient_shouldReturnMappedResponses() {
        SudokuSession session = SudokuSession.builder()
                .id(9L)
                .sudokuGame(game)
                .patientId(7L)
                .startedAt(LocalDateTime.now())
                .score(430)
                .build();
        when(sudokuGameRepository.findById(3L)).thenReturn(Optional.of(game));
        when(sudokuSessionRepository.findBySudokuGame_IdAndPatientIdOrderByStartedAtDesc(3L, 7L)).thenReturn(List.of(session));

        List<SudokuSessionResponse> responses = service.getSessionsByPatient(3L, 7L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getScore()).isEqualTo(430);
    }

    @Test
    void toJson_shouldSerializeGrid() {
        assertThat(service.toJson(new int[][]{{1, 2}, {3, 4}})).isEqualTo("[[1,2],[3,4]]");
    }
}
