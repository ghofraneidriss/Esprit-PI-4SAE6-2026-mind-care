import { Component } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { RecommendationService } from '../recommendation/recommendation.service';
import { Puzzle, PuzzleSession } from '../recommendation/recommendation.model';

@Component({
    selector: 'app-puzzle-management',
    templateUrl: './puzzle-management.html',
    styleUrls: ['./puzzle-management.css'],
    standalone: false
})
export class PuzzleManagementPage {
    patientId: number = 4;

    puzzles: Puzzle[] = [];
    selectedPuzzle: Puzzle | null = null;
    sessions: PuzzleSession[] = [];

    isLoadingPuzzles = false;
    isLoadingSessions = false;

    puzzleError = '';
    sessionError = '';

    constructor(private readonly recommendationService: RecommendationService) {}

    loadPuzzles(): void {
        if (!this.patientId) {
            return;
        }
        this.isLoadingPuzzles = true;
        this.puzzleError = '';
        this.puzzles = [];
        this.selectedPuzzle = null;
        this.sessions = [];
        this.sessionError = '';

        this.recommendationService.getPuzzlesByPatient(this.patientId).subscribe({
            next: (data) => {
                this.puzzles = data;
                this.isLoadingPuzzles = false;
            },
            error: (err) => {
                this.puzzleError = this.extractErrorMessage(err, 'Impossible de charger les puzzles. Vérifiez que le backend tourne.');
                this.isLoadingPuzzles = false;
            }
        });
    }

    selectPuzzle(puzzle: Puzzle): void {
        this.selectedPuzzle = puzzle;
        this.sessions = [];
        this.sessionError = '';
        this.isLoadingSessions = true;

        this.recommendationService.getPuzzleSessions(puzzle.id, this.patientId).subscribe({
            next: (data) => {
                this.sessions = data;
                this.isLoadingSessions = false;
            },
            error: (err) => {
                this.sessionError = this.extractErrorMessage(err, 'Impossible de charger les sessions.');
                this.isLoadingSessions = false;
            }
        });
    }

    getDifficultyClass(difficulty?: string): string {
        const map: Record<string, string> = {
            EASY: 'difficulty-badge badge-easy',
            MEDIUM: 'difficulty-badge badge-medium',
            HARD: 'difficulty-badge badge-hard'
        };
        return map[difficulty || ''] || 'difficulty-badge badge-medium';
    }

    getStatusClass(status?: string): string {
        const map: Record<string, string> = {
            ACTIVE: 'status-badge badge-active',
            COMPLETED: 'status-badge badge-completed',
            ARCHIVED: 'status-badge badge-archived'
        };
        return map[status || ''] || 'status-badge badge-active';
    }

    formatDate(value?: string | null): string {
        if (!value) return '-';
        const d = new Date(value);
        return isNaN(d.getTime()) ? value : d.toLocaleString();
    }

    formatDuration(seconds?: number | null): string {
        if (seconds == null) return '-';
        const m = Math.floor(seconds / 60);
        const s = seconds % 60;
        return m > 0 ? `${m}m ${s}s` : `${s}s`;
    }

    private extractErrorMessage(err: unknown, fallback: string): string {
        if (err instanceof HttpErrorResponse) {
            if (typeof err.error === 'string') return err.error;
            if (err.error?.message) return err.error.message;
            if (err.message) return err.message;
        }
        return fallback;
    }
}
