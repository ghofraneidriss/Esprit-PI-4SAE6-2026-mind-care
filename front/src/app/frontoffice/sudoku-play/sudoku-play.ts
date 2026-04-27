import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { SudokuGame } from '../../backoffice/recommendation/recommendation.model';
import { RecommendationService } from '../../backoffice/recommendation/recommendation.service';
import { User } from '../../core/models/user.model';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-sudoku-play',
  standalone: false,
  templateUrl: './sudoku-play.html',
  styleUrls: ['./sudoku-play.css'],
})
export class SudokuPlayPage implements OnInit, OnDestroy {
  game: SudokuGame | null = null;
  grid: number[][] = [];
  fixedCells: boolean[][] = [];
  errorCells: boolean[][] = [];

  sessionId: number | null = null;
  loading = true;
  submitting = false;
  submitted = false;
  showSuccessOverlay = false;
  finalScore: number | null = null;
  errorMessage = '';

  elapsedSeconds = 0;
  timerHandle: ReturnType<typeof setInterval> | null = null;
  startTimestamp = 0;

  currentPatient: User | null = null;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly recommendationService: RecommendationService,
    private readonly authService: AuthService
  ) {}

  ngOnInit(): void {
    this.currentPatient = this.authService.getCurrentUser();
    const eventId = Number(this.route.snapshot.paramMap.get('eventId'));
    if (!Number.isFinite(eventId) || eventId <= 0) {
      this.errorMessage = 'Jeu Sudoku invalide.';
      this.loading = false;
      return;
    }
    this.loadGame(eventId);
  }

  ngOnDestroy(): void {
    this.stopTimer();
    if (this.game && this.sessionId && !this.submitted) {
      this.doSubmit(false, true);
    }
  }

  get canPlay(): boolean {
    return !!this.currentPatient?.userId && this.authService.isPatient();
  }

  get errorsCount(): number {
    let count = 0;
    for (let r = 0; r < this.errorCells.length; r++) {
      for (let c = 0; c < (this.errorCells[r]?.length ?? 0); c++) {
        if (this.errorCells[r][c]) count++;
      }
    }
    return count;
  }

  get completionPercent(): number {
    if (!this.grid.length) return 0;
    const size = this.grid.length;
    let filled = 0;
    let total = 0;
    for (let r = 0; r < size; r++) {
      for (let c = 0; c < size; c++) {
        if (!this.fixedCells[r]?.[c]) {
          total++;
          if (this.grid[r][c] > 0 && !this.errorCells[r]?.[c]) filled++;
        }
      }
    }
    return total === 0 ? 0 : Math.round((filled / total) * 100);
  }

  get formattedTime(): string {
    const m = Math.floor(this.elapsedSeconds / 60);
    const s = this.elapsedSeconds % 60;
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  }

  loadGame(eventId: number): void {
    this.loading = true;
    this.recommendationService.getSudokuByEvent(eventId).subscribe({
      next: (game) => {
        this.game = game;
        this.loading = false;
        this.initGrid();
        if (!this.canPlay) {
          this.errorMessage = 'Connectez-vous comme patient pour jouer ce Sudoku.';
          return;
        }
        this.startSession();
      },
      error: () => {
        this.errorMessage = 'Impossible de charger le jeu Sudoku.';
        this.loading = false;
      },
    });
  }

  initGrid(): void {
    if (!this.game?.puzzle) return;
    try {
      const parsed: number[][] = JSON.parse(this.game.puzzle);
      const size = parsed.length;
      this.grid = parsed.map(row => [...row]);
      this.fixedCells = parsed.map(row => row.map(v => v !== 0));
      this.errorCells = Array.from({ length: size }, () => Array(size).fill(false));
    } catch {
      this.errorMessage = 'Erreur lors du chargement de la grille.';
    }
  }

  startSession(): void {
    if (!this.game?.id || !this.currentPatient?.userId) return;
    this.showSuccessOverlay = false;
    this.finalScore = null;
    this.errorMessage = '';

    this.recommendationService.startSudokuSession(this.game.id, this.currentPatient.userId).subscribe({
      next: (response) => {
        this.sessionId = response.sessionId;
        this.startTimestamp = Date.now();
        this.elapsedSeconds = 0;
        this.submitted = false;
        this.startTimer();
      },
      error: (err) => {
        this.errorMessage = err?.error?.message || 'Impossible de démarrer la session Sudoku.';
      },
    });
  }

  onCellInput(row: number, col: number, event: Event): void {
    const input = event.target as HTMLInputElement;
    const raw = parseInt(input.value, 10);
    const size = this.grid.length;
    const value = isNaN(raw) || raw < 1 || raw > size ? 0 : raw;
    this.grid[row][col] = value;
    if (value === 0) {
      input.value = '';
    }
    this.updateErrorCells();
  }

  isCellValid(row: number, col: number, num: number): boolean {
    if (num === 0) return true;
    const size = this.grid.length;
    // Check row
    for (let c = 0; c < size; c++) {
      if (c !== col && this.grid[row][c] === num) return false;
    }
    // Check column
    for (let r = 0; r < size; r++) {
      if (r !== row && this.grid[r][col] === num) return false;
    }
    // Check 2x2 box
    const boxSize = Math.sqrt(size);
    const boxRowStart = Math.floor(row / boxSize) * boxSize;
    const boxColStart = Math.floor(col / boxSize) * boxSize;
    for (let r = boxRowStart; r < boxRowStart + boxSize; r++) {
      for (let c = boxColStart; c < boxColStart + boxSize; c++) {
        if ((r !== row || c !== col) && this.grid[r][c] === num) return false;
      }
    }
    return true;
  }

  updateErrorCells(): void {
    const size = this.grid.length;
    for (let r = 0; r < size; r++) {
      for (let c = 0; c < size; c++) {
        if (!this.fixedCells[r]?.[c] && this.grid[r][c] > 0) {
          this.errorCells[r][c] = !this.isCellValid(r, c, this.grid[r][c]);
        } else {
          this.errorCells[r][c] = false;
        }
      }
    }
  }

  validateSolution(): void {
    if (this.submitting || this.submitted) return;
    this.doSubmit(this.completionPercent >= 100, false);
  }

  doSubmit(completed: boolean, abandoned: boolean): void {
    if (!this.game?.id || !this.sessionId || !this.currentPatient?.userId) return;
    this.stopTimer();
    this.submitting = true;

    this.recommendationService
      .submitSudokuSession(this.game.id, this.sessionId, {
        patientId: this.currentPatient.userId,
        durationSeconds: this.elapsedSeconds,
        errorsCount: this.errorsCount,
        hintsUsed: 0,
        completionPercent: completed ? 100 : this.completionPercent,
        completed,
        abandoned,
      })
      .subscribe({
        next: (session) => {
          this.submitted = true;
          this.submitting = false;
          this.finalScore = session.score ?? 0;
          if (!abandoned) {
            this.showSuccessOverlay = true;
          }
        },
        error: (err) => {
          this.submitting = false;
          this.errorMessage = err?.error?.message || 'Erreur lors de la soumission.';
        },
      });
  }

  goToRecommendations(): void {
    this.router.navigate(['/recommendations']);
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
}
