import { Component } from '@angular/core';
import { RecommendationService } from '../recommendation/recommendation.service';

@Component({
  selector: 'app-sudoku-management',
  templateUrl: './sudoku-management.html',
  styleUrls: ['./sudoku-management.css'],
  standalone: false,
})
export class SudokuManagementPage {
  patientId: number = 0;
  timeLimitSeconds: number = 300;
  difficulty = 'EASY';

  isLoading = false;
  successMessage = '';
  errorMessage = '';

  constructor(private readonly recommendationService: RecommendationService) {}

  createSudoku(): void {
    this.successMessage = '';
    this.errorMessage = '';

    if (!this.patientId || this.patientId <= 0) {
      this.errorMessage = 'Veuillez saisir un identifiant patient valide (nombre positif).';
      return;
    }

    this.isLoading = true;

    this.recommendationService
      .createSudoku({
        patientId: this.patientId,
        difficulty: this.difficulty,
        timeLimitSeconds: this.timeLimitSeconds,
        title: 'Sudoku thérapeutique',
      })
      .subscribe({
        next: (game) => {
          this.isLoading = false;
          this.successMessage = `Sudoku créé avec succès (event #${game.medicalEventId})`;
          this.patientId = 0;
          this.timeLimitSeconds = 300;
        },
        error: (err) => {
          this.isLoading = false;
          this.errorMessage =
            err?.error?.message || err?.message || 'Erreur lors de la création du Sudoku. Vérifiez que le service est démarré.';
        },
      });
  }
}
