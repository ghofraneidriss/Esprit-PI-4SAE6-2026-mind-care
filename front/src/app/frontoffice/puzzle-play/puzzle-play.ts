import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import {
  Puzzle,
  PuzzleLeaderboardEntry,
  PuzzleSession,
} from '../../backoffice/recommendation/recommendation.model';
import { RecommendationService } from '../../backoffice/recommendation/recommendation.service';
import { AuthService, AuthUser } from '../auth/auth.service';

interface PuzzleTile {
  value: number;
  isEmpty: boolean;
}

@Component({
  selector: 'app-puzzle-play',
  standalone: false,
  templateUrl: './puzzle-play.html',
  styleUrls: ['./puzzle-play.css'],
})
export class PuzzlePlayPage implements OnInit, OnDestroy {
  puzzle: Puzzle | null = null;
  leaderboard: PuzzleLeaderboardEntry[] = [];
  latestSessions: PuzzleSession[] = [];
  currentPatient: AuthUser | null = null;

  loading = true;
  submitting = false;
  errorMessage = '';
  successMessage = '';
  showReference = false;

  sessionId: number | null = null;
  startTimestamp = 0;
  elapsedSeconds = 0;
  movesCount = 0;
  hintsUsed = 0;
  timerHandle: ReturnType<typeof setInterval> | null = null;
  submitted = false;

  tiles: PuzzleTile[] = [];
  emptyIndex = 0;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly recommendationService: RecommendationService,
    private readonly authService: AuthService
  ) {}

  ngOnInit(): void {
    this.currentPatient = this.authService.getLoggedUser();
    const eventId = Number(this.route.snapshot.paramMap.get('eventId'));
    if (!Number.isFinite(eventId) || eventId <= 0) {
      this.errorMessage = 'Puzzle invalide.';
      this.loading = false;
      return;
    }
    this.loadPuzzle(eventId);
  }

  ngOnDestroy(): void {
    this.stopTimer();
    if (this.puzzle && this.sessionId && !this.submitted) {
      this.submitSession(false, true);
    }
  }

  get gridTemplateColumns(): string {
    const gridSize = this.puzzle?.gridSize ?? 3;
    return `repeat(${gridSize}, minmax(0, 1fr))`;
  }

  get completionPercent(): number {
    if (!this.tiles.length) return 0;
    const correctTiles = this.tiles.filter((tile, index) => tile.isEmpty || tile.value === index).length;
    return Math.round((correctTiles / this.tiles.length) * 100);
  }

  get canPlay(): boolean {
    return !!this.currentPatient?.userId && this.authService.isPatient();
  }

  get sourceImage(): string {
    return this.puzzle?.souvenir?.mediaUrl || '';
  }

  get timeLimitExceeded(): boolean {
    return !!this.puzzle?.timeLimitSeconds && this.elapsedSeconds > this.puzzle.timeLimitSeconds;
  }

  loadPuzzle(eventId: number): void {
    this.loading = true;
    this.recommendationService.getPuzzleByEvent(eventId).subscribe({
      next: (puzzle) => {
        this.puzzle = puzzle;
        this.loading = false;
        this.loadLeaderboard();
        this.loadLatestSessions();
        if (!this.canPlay || !this.currentPatient?.userId) {
          this.errorMessage = 'Connectez-vous comme patient pour jouer ce puzzle.';
          return;
        }
        this.startSession();
      },
      error: () => {
        this.errorMessage = 'Impossible de charger le puzzle souvenir.';
        this.loading = false;
      },
    });
  }

  startSession(): void {
    if (!this.puzzle?.id || !this.currentPatient?.userId) return;

    this.recommendationService.startPuzzleSession(this.puzzle.id, this.currentPatient.userId).subscribe({
      next: (response) => {
        this.sessionId = response.sessionId;
        this.startTimestamp = Date.now();
        this.elapsedSeconds = 0;
        this.movesCount = 0;
        this.hintsUsed = 0;
        this.submitted = false;
        this.generateTiles();
        this.startTimer();
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Impossible de demarrer la session du puzzle.';
      },
    });
  }

  generateTiles(): void {
    const gridSize = this.puzzle?.gridSize ?? 3;
    const tileCount = gridSize * gridSize;
    const ordered = Array.from({ length: tileCount }, (_, index) => index);
    let shuffled = [...ordered];

    do {
      shuffled = [...ordered].sort(() => Math.random() - 0.5);
    } while (this.isSolved(shuffled));

    this.tiles = shuffled.map((value) => ({
      value,
      isEmpty: value === tileCount - 1,
    }));
    this.emptyIndex = this.tiles.findIndex((tile) => tile.isEmpty);
  }

  moveTile(index: number): void {
    if (this.submitting || !this.isAdjacent(index, this.emptyIndex)) return;

    [this.tiles[index], this.tiles[this.emptyIndex]] = [this.tiles[this.emptyIndex], this.tiles[index]];
    this.emptyIndex = index;
    this.movesCount++;

    if (this.isSolved(this.tiles.map((tile) => tile.value))) {
      this.submitSession(true, false);
    }
  }

  useHint(): void {
    if (!this.puzzle || this.hintsUsed >= this.puzzle.maxHints) return;
    this.hintsUsed++;
    this.showReference = true;
    setTimeout(() => (this.showReference = false), 1800);
  }

  restartPuzzle(): void {
    this.stopTimer();
    if (this.sessionId && this.puzzle && !this.submitted) {
      this.submitSession(false, true);
    }
    this.startSession();
  }

  trackTile(_: number, tile: PuzzleTile): number {
    return tile.value;
  }

  tileStyle(tile: PuzzleTile): Record<string, string> {
    if (!this.puzzle || tile.isEmpty) {
      return {};
    }

    const gridSize = this.puzzle.gridSize;
    const row = Math.floor(tile.value / gridSize);
    const col = tile.value % gridSize;
    const denominator = Math.max(gridSize - 1, 1);

    return {
      backgroundImage: `url(${this.sourceImage})`,
      backgroundSize: `${gridSize * 100}% ${gridSize * 100}%`,
      backgroundPosition: `${(col / denominator) * 100}% ${(row / denominator) * 100}%`,
    };
  }

  private loadLeaderboard(): void {
    if (!this.puzzle?.id) return;
    this.recommendationService.getPuzzleLeaderboard(this.puzzle.id).subscribe({
      next: (entries) => (this.leaderboard = entries),
      error: () => (this.leaderboard = []),
    });
  }

  private loadLatestSessions(): void {
    if (!this.puzzle?.id || !this.currentPatient?.userId) return;
    this.recommendationService.getPuzzleSessions(this.puzzle.id, this.currentPatient.userId).subscribe({
      next: (sessions) => (this.latestSessions = sessions.slice(0, 5)),
      error: () => (this.latestSessions = []),
    });
  }

  private startTimer(): void {
    this.stopTimer();
    this.timerHandle = setInterval(() => {
      this.elapsedSeconds = Math.floor((Date.now() - this.startTimestamp) / 1000);
    }, 1000);
  }

  private stopTimer(): void {
    if (this.timerHandle) {
      clearInterval(this.timerHandle);
      this.timerHandle = null;
    }
  }

  private submitSession(completed: boolean, abandoned: boolean): void {
    if (!this.puzzle?.id || !this.sessionId || !this.currentPatient?.userId) return;

    this.stopTimer();
    this.submitting = true;

    this.recommendationService
      .submitPuzzleSession(this.puzzle.id, this.sessionId, {
        patientId: this.currentPatient.userId,
        durationSeconds: this.elapsedSeconds,
        movesCount: this.movesCount,
        hintsUsed: this.hintsUsed,
        completionPercent: completed ? 100 : this.completionPercent,
        completed,
        abandoned,
      })
      .subscribe({
        next: (session) => {
          this.submitted = true;
          this.submitting = false;
          this.successMessage = session.completed
            ? `Puzzle termine ! Score final: ${session.score ?? 0}`
            : 'Session enregistree.';
          this.loadLeaderboard();
          this.loadLatestSessions();
        },
        error: (err) => {
          this.submitting = false;
          this.errorMessage = err?.error?.message || 'Erreur lors de la soumission du puzzle.';
        },
      });
  }

  private isAdjacent(indexA: number, indexB: number): boolean {
    const gridSize = this.puzzle?.gridSize ?? 3;
    const rowA = Math.floor(indexA / gridSize);
    const colA = indexA % gridSize;
    const rowB = Math.floor(indexB / gridSize);
    const colB = indexB % gridSize;
    return Math.abs(rowA - rowB) + Math.abs(colA - colB) === 1;
  }

  private isSolved(values: number[]): boolean {
    return values.every((value, index) => value === index);
  }
}
