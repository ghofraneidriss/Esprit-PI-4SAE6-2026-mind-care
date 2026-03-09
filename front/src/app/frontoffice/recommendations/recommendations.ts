import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { Recommendation, RecommendationStatus, RecommendationType, MedicalEvent } from '../../backoffice/recommendation/recommendation.model';
import { RecommendationService } from '../../backoffice/recommendation/recommendation.service';

@Component({
    selector: 'app-recommendations',
    templateUrl: './recommendations.html',
    styleUrls: ['./recommendations.css'],
    standalone: false
})
export class RecommendationsPage implements OnInit {
    recommendations: Recommendation[] = [];
    medicalEvents: MedicalEvent[] = []; // Available games
    loading = true;
    showForm = false;
    isEditing = false;
    errorMessage = '';
    successMessage = '';

    recommendationTypes = Object.values(RecommendationType);
    recommendationStatuses = Object.values(RecommendationStatus);

    currentRecommendation: Recommendation = this.initNewRecommendation();

    constructor(private recommendationService: RecommendationService, private cdr: ChangeDetectorRef) { }

    ngOnInit(): void {
        this.loadRecommendations();
        this.loadMedicalEvents();
    }

    private initNewRecommendation(): Recommendation {
        return {
            content: '',
            type: RecommendationType.LIFESTYLE,
            status: RecommendationStatus.PENDING,
            doctorId: 0,
            patientId: 0,
            medicalEvents: []
        };
    }

    loadRecommendations(): void {
        this.loading = true;
        this.recommendationService.getAll().subscribe({
            next: (data: any) => {
                // Robust handling for both Array and Page object
                if (Array.isArray(data)) {
                    this.recommendations = data;
                } else if (data && data.content) {
                    this.recommendations = data.content;
                } else {
                    this.recommendations = [];
                }
                this.loading = false;
                this.cdr.detectChanges();
            },
            error: () => {
                this.errorMessage = 'Erreur lors de la connexion au service de recommandation (Port 8085).';
                this.loading = false;
                this.cdr.detectChanges();
            }
        });
    }

    isEventSelected(event: MedicalEvent): boolean {
        if (!this.currentRecommendation.medicalEvents || !event.id) return false;
        return this.currentRecommendation.medicalEvents.some(m => m.id === event.id);
    }

    loadMedicalEvents(): void {
        this.recommendationService.getAllEvents().subscribe({
            next: (data) => this.medicalEvents = data,
            error: () => console.error('Impossible de charger les jeux médicaux.')
        });
    }

    openCreateForm(): void {
        this.isEditing = false;
        this.currentRecommendation = this.initNewRecommendation();
        this.showForm = true;
    }

    toggleEvent(event: MedicalEvent): void {
        if (!this.currentRecommendation.medicalEvents) {
            this.currentRecommendation.medicalEvents = [];
        }
        const index = this.currentRecommendation.medicalEvents.findIndex(e => e.id === event.id);
        if (index > -1) {
            this.currentRecommendation.medicalEvents.splice(index, 1);
        } else {
            this.currentRecommendation.medicalEvents.push(event);
        }
    }



    edit(recommendation: Recommendation): void {
        this.isEditing = true;
        this.currentRecommendation = { ...recommendation };
        this.showForm = true;
        this.successMessage = '';
    }

    save(): void {
        if (!this.currentRecommendation.content.trim()) return;

        if (this.isEditing && this.currentRecommendation.id) {
            this.recommendationService.update(this.currentRecommendation.id, this.currentRecommendation).subscribe({
                next: () => {
                    this.loadRecommendations();
                    this.showForm = false;
                    this.successMessage = 'Recommandation mise à jour avec succès.';
                },
                error: () => { this.errorMessage = 'Erreur lors de la mise à jour.'; }
            });
        } else {
            this.recommendationService.create(this.currentRecommendation).subscribe({
                next: () => {
                    this.loadRecommendations();
                    this.showForm = false;
                    this.successMessage = 'Recommandation créée avec succès.';
                },
                error: () => { this.errorMessage = 'Erreur lors de la création.'; }
            });
        }
    }

    delete(id: number | undefined): void {
        if (id && confirm('Voulez-vous vraiment supprimer cette recommandation ?')) {
            this.recommendationService.delete(id).subscribe({
                next: () => {
                    this.loadRecommendations();
                    this.successMessage = 'Recommandation supprimée.';
                },
                error: () => { this.errorMessage = 'Erreur lors de la suppression.'; }
            });
        }
    }

    cancel(): void {
        this.showForm = false;
    }

    getTypeBadgeClass(type: string): string {
        const map: Record<string, string> = {
            MEDICINE: 'bg-danger-subtle text-danger',
            EXERCISE: 'bg-success-subtle text-success',
            DIET: 'bg-warning-subtle text-warning',
            LIFESTYLE: 'bg-info-subtle text-info',
            OTHER: 'bg-secondary-subtle text-secondary'
        };
        return map[type] || 'bg-secondary-subtle text-secondary';
    }
}
