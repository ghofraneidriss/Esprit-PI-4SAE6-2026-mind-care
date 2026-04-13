import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import {
  ReclamationCourseRatingStats,
  ReclamationRatingSummary,
  ReclamationService
} from '../../services/reclamation.service';
import { Reclamation, ReclamationStatus } from '../../models/reclamation.model';
import jsPDF from 'jspdf';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-reclamation-management',
  templateUrl: './reclamation-management.component.html',
  styleUrl: './reclamation-management.component.css',
  standalone: false
})
export class ReclamationManagementComponent implements OnInit {
  reclamations: Reclamation[] = [];
  loading = false;
  errorMessage = '';
  successMessage = '';
  editingId: number | null = null;
  speaking = false;
  ttsSupported = typeof window !== 'undefined' && 'speechSynthesis' in window;
  ratingSummary: ReclamationRatingSummary = {
    averageRating: 0,
    reclamationCount: 0,
    ratedReclamationCount: 0,
    rankedCourseCount: 0
  };
  courseRanking: ReclamationCourseRatingStats[] = [];

  readonly statuses: ReclamationStatus[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'REJECTED'];

  formModel: Reclamation = {
    userName: '',
    userEmail: '',
    subject: '',
    imageUrl: '',
    rating: 3,
    description: '',
    status: 'OPEN'
  };

  constructor(
    private readonly reclamationService: ReclamationService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadReclamations();
  }

  loadReclamations(): void {
    this.loading = true;
    this.errorMessage = '';
    this.reclamationService.getAll().subscribe({
      next: (data) => {
        this.reclamations = data;
        this.loadRatingInsights();
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'Impossible de charger les reclamations.';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  saveReclamation(): void {
    this.errorMessage = '';
    this.successMessage = '';

    const payload: Reclamation = {
      userName: this.formModel.userName.trim(),
      userEmail: this.formModel.userEmail.trim(),
      subject: this.formModel.subject.trim(),
      imageUrl: this.formModel.imageUrl?.trim(),
      rating: this.formModel.rating,
      description: this.formModel.description.trim(),
      status: this.formModel.status
    };

    if (!payload.userName || !payload.userEmail || !payload.subject || !payload.description) {
      this.errorMessage = 'Veuillez remplir tous les champs de la reclamation.';
      return;
    }

    if (payload.rating < 1 || payload.rating > 5) {
      this.errorMessage = 'La note doit etre entre 1 et 5.';
      return;
    }

    if (this.editingId === null) {
      this.reclamationService.create(payload).subscribe({
        next: () => {
          this.successMessage = 'Reclamation ajoutee avec succes.';
          this.resetForm();
          this.loadReclamations();
        },
        error: () => {
          this.errorMessage = "Erreur lors de l'ajout de la reclamation.";
        }
      });
      return;
    }

    this.reclamationService.update(this.editingId, payload).subscribe({
      next: () => {
        this.successMessage = 'Reclamation modifiee avec succes.';
        this.resetForm();
        this.loadReclamations();
      },
      error: () => {
        this.errorMessage = 'Erreur lors de la modification de la reclamation.';
      }
    });
  }

  startEdit(item: Reclamation): void {
    if (item.id === undefined) {
      return;
    }

    this.editingId = item.id;
    this.formModel = {
      userName: item.userName,
      userEmail: item.userEmail,
      subject: item.subject,
      imageUrl: item.imageUrl,
      rating: item.rating ?? 3,
      description: item.description,
      status: item.status
    };
  }

  deleteReclamation(id: number | undefined): void {
    if (id === undefined) {
      return;
    }

    this.errorMessage = '';
    this.successMessage = '';

    this.reclamationService.delete(id).subscribe({
      next: () => {
        this.successMessage = 'Reclamation supprimee avec succes.';
        this.loadReclamations();
      },
      error: () => {
        this.errorMessage = 'Erreur lors de la suppression de la reclamation.';
      }
    });
  }

  resetForm(): void {
    this.editingId = null;
    this.formModel = {
      userName: '',
      userEmail: '',
      subject: '',
      imageUrl: '',
      rating: 3,
      description: '',
      status: 'OPEN'
    };
  }

  speakReclamation(item: Reclamation): void {
    if (!this.ttsSupported) {
      this.errorMessage = 'Text-to-speech non supporte sur ce navigateur.';
      return;
    }

    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(
      `Reclamation de ${item.userName}. Sujet: ${item.subject}. Description: ${item.description}. Statut: ${item.status}.`
    );
    utterance.lang = 'fr-FR';
    utterance.onstart = () => {
      this.speaking = true;
      this.cdr.detectChanges();
    };
    utterance.onend = () => {
      this.speaking = false;
      this.cdr.detectChanges();
    };
    utterance.onerror = () => {
      this.errorMessage = 'Erreur lors de la lecture vocale.';
      this.speaking = false;
      this.cdr.detectChanges();
    };
    window.speechSynthesis.speak(utterance);
  }

  stopSpeech(): void {
    if (!this.ttsSupported) {
      return;
    }

    window.speechSynthesis.cancel();
    this.speaking = false;
  }

  shareOnWhatsapp(item: Reclamation): void {
    const text = encodeURIComponent(this.buildShareText(item));
    window.open(`https://wa.me/?text=${text}`, '_blank', 'noopener');
  }

  shareOnTelegram(item: Reclamation): void {
    const text = encodeURIComponent(this.buildShareText(item));
    const url = encodeURIComponent(window.location.href);
    window.open(`https://t.me/share/url?url=${url}&text=${text}`, '_blank', 'noopener');
  }

  exportPdf(item: Reclamation): void {
    const doc = new jsPDF();
    doc.setFontSize(16);
    doc.text('Reclamation', 14, 18);

    doc.setFontSize(12);
    const lines = [
      `ID: ${item.id ?? '-'}`,
      `Nom: ${item.userName}`,
      `Email: ${item.userEmail}`,
      `Sujet: ${item.subject}`,
      `Note: ${item.rating ?? '-'}/5`,
      `Statut: ${item.status}`,
      `Image URL: ${item.imageUrl ?? '-'}`,
      `Description: ${item.description}`
    ];

    let y = 30;
    for (const line of lines) {
      const wrapped = doc.splitTextToSize(line, 180);
      doc.text(wrapped, 14, y);
      y += wrapped.length * 7;
    }

    doc.save(`reclamation-${item.id ?? 'item'}.pdf`);
  }

  getRatingBarWidth(averageRating: number): string {
    const percent = Math.max(8, Math.min(100, averageRating * 20));
    return `${percent}%`;
  }

  getRatingBand(averageRating: number): 'elite' | 'good' | 'mid' | 'low' {
    if (averageRating >= 4.5) {
      return 'elite';
    }
    if (averageRating >= 3.5) {
      return 'good';
    }
    if (averageRating >= 2.5) {
      return 'mid';
    }
    return 'low';
  }

  getClassificationLabel(item: Reclamation): string {
    return item.classificationLevel || 'MOYEN';
  }

  getClassificationClass(item: Reclamation): 'faible' | 'moyen' | 'fort' {
    if (item.classificationLevel === 'FAIBLE') {
      return 'faible';
    }
    if (item.classificationLevel === 'FORT') {
      return 'fort';
    }
    return 'moyen';
  }

  getClassificationScoreLabel(item: Reclamation): string {
    if (item.classificationScore === null || item.classificationScore === undefined) {
      return '0/100';
    }
    return `${item.classificationScore}/100`;
  }

  private buildShareText(item: Reclamation): string {
    return `Reclamation: ${item.subject}\nAuteur: ${item.userName}\nNote: ${item.rating}/5\nStatut: ${item.status}\nDescription: ${item.description}`;
  }

  private loadRatingInsights(): void {
    forkJoin({
      summary: this.reclamationService.getRatingSummary(),
      ranking: this.reclamationService.getRatingsRanking(5)
    }).subscribe({
      next: ({ summary, ranking }) => {
        this.ratingSummary = summary;
        this.courseRanking = ranking;
        this.cdr.detectChanges();
      },
      error: () => {
        this.ratingSummary = {
          averageRating: 0,
          reclamationCount: this.reclamations.length,
          ratedReclamationCount: 0,
          rankedCourseCount: 0
        };
        this.courseRanking = [];
        this.cdr.detectChanges();
      }
    });
  }
}
