import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import {
    Recommendation,
    RecommendationStatus,
    RecommendationType
} from '../../backoffice/recommendation/recommendation.model';
import { RecommendationService } from '../../backoffice/recommendation/recommendation.service';
import { User } from '../../core/models/user.model';
import { AuthService } from '../../core/services/auth.service';

@Component({
    selector: 'app-recommendations',
    templateUrl: './recommendations.html',
    styleUrls: ['./recommendations.css'],
    standalone: false
})
export class RecommendationsPage implements OnInit {
    recommendations: Recommendation[] = [];
    loading = true;
    actionLoadingId: number | null = null;
    errorMessage = '';
    successMessage = '';
    currentPatient: User | null = null;

    readonly recommendationStatuses = Object.values(RecommendationStatus);
    readonly recommendationTypes = Object.values(RecommendationType);

    constructor(
        private readonly recommendationService: RecommendationService,
        private readonly authService: AuthService,
        private readonly cdr: ChangeDetectorRef
    ) { }

    ngOnInit(): void {
        this.currentPatient = this.authService.getCurrentUser();
        this.loadRecommendations();
    }

    get isPatientLoggedIn(): boolean {
        return this.currentPatient !== null && this.authService.isPatient();
    }

    loadRecommendations(): void {
        if (!this.isPatientLoggedIn || !this.currentPatient?.userId) {
            this.loading = false;
            this.recommendations = [];
            this.errorMessage = this.currentPatient
                ? 'Cette page patient affiche uniquement les recommandations du patient connecte.'
                : 'Connectez-vous comme patient pour voir vos recommandations.';
            return;
        }

        this.loading = true;
        this.errorMessage = '';

        this.recommendationService.getSortedByPatient(this.currentPatient.userId).subscribe({
            next: (data) => {
                console.log('[Recommendations] data received:', data);
                this.recommendations = Array.isArray(data) ? data : [];
                this.loading = false;
                this.cdr.detectChanges();
            },
            error: (err) => {
                console.error('[Recommendations] error:', err);
                const msg = err?.error?.message || err?.message || '';
                this.errorMessage = msg
                    ? `Erreur: ${msg}`
                    : 'Impossible de charger les recommandations. Vérifiez que le service est démarré (port 8085).';
                this.recommendations = [];
                this.loading = false;
                this.cdr.detectChanges();
            }
        });
    }

    acceptRecommendation(recommendation: Recommendation): void {
        if (!recommendation.id || this.actionLoadingId !== null) return;

        this.actionLoadingId = recommendation.id;
        this.errorMessage = '';
        this.successMessage = '';

        this.recommendationService.accept(recommendation.id).subscribe({
            next: (updated) => {
                this.updateRecommendationInList(updated);
                this.successMessage = 'Recommandation acceptee avec succes.';
                this.actionLoadingId = null;
            },
            error: () => {
                this.errorMessage = "Erreur lors de l'acceptation de la recommandation.";
                this.actionLoadingId = null;
            }
        });
    }

    rejectRecommendation(recommendation: Recommendation): void {
        if (!recommendation.id || this.actionLoadingId !== null) return;

        this.actionLoadingId = recommendation.id;
        this.errorMessage = '';
        this.successMessage = '';

        this.recommendationService.dismiss(recommendation.id).subscribe({
            next: (updated) => {
                this.updateRecommendationInList(updated);
                this.successMessage = 'Recommandation rejetee avec succes.';
                this.actionLoadingId = null;
            },
            error: () => {
                this.errorMessage = 'Erreur lors du rejet de la recommandation.';
                this.actionLoadingId = null;
            }
        });
    }

    isActionLoading(recommendationId?: number): boolean {
        return recommendationId != null && this.actionLoadingId === recommendationId;
    }

    trackById(_: number, rec: Recommendation): number {
        return rec.id ?? _;
    }

    getTypeBadgeClass(type: string): string {
        const map: Record<string, string> = {
            MEDICATION: 'bg-danger-subtle text-danger',
            EXERCISE: 'bg-success-subtle text-success',
            DIET: 'bg-warning-subtle text-warning',
            LIFESTYLE: 'bg-info-subtle text-info',
            MEMORY: 'bg-primary-subtle text-primary',
            ATTENTION: 'bg-info-subtle text-info',
            FLUENCY: 'bg-secondary-subtle text-secondary',
            VISUOSPATIAL: 'bg-dark-subtle text-dark',
            PUZZLE: 'bg-success-subtle text-success',
            SUDOKU: 'bg-warning-subtle text-warning',
            OTHER: 'bg-secondary-subtle text-secondary'
        };
        return map[type] || 'bg-secondary-subtle text-secondary';
    }

    getStatusBadgeClass(status?: string): string {
        const map: Record<string, string> = {
            ACTIVE: 'bg-warning-subtle text-warning',
            ACCEPTED: 'bg-success-subtle text-success',
            REJECTED: 'bg-danger-subtle text-danger',
            EXPIRED: 'bg-secondary-subtle text-secondary'
        };
        return map[status || ''] || 'bg-secondary-subtle text-secondary';
    }

    private updateRecommendationInList(updated: Recommendation): void {
        this.recommendations = this.recommendations.map((recommendation) =>
            recommendation.id === updated.id ? { ...recommendation, ...updated } : recommendation
        );
    }
}
