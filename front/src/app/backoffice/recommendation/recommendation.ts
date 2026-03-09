import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { Recommendation, RecommendationType, RecommendationStatus, MedicalEvent } from './recommendation.model';
import { RecommendationService } from './recommendation.service';

@Component({
    selector: 'app-recommendation',
    templateUrl: './recommendation.html',
    styleUrls: ['./recommendation.css'],
    standalone: false
})
export class RecommendationPage implements OnInit {
    recommendations: Recommendation[] = [];
    medicalEvents: MedicalEvent[] = []; // Les jeux disponibles pour prescription
    showForm = false;
    isEditing = false;
    isLoading = true;
    searchTerm = '';
    errorMessage = '';

    recommendationTypes = Object.values(RecommendationType);
    recommendationStatuses = Object.values(RecommendationStatus);

    currentRecommendation: Recommendation = {
        content: '',
        type: RecommendationType.OTHER,
        status: RecommendationStatus.PENDING,
        doctorId: 0,
        patientId: 0
    };

    constructor(private recommendationService: RecommendationService, private cdr: ChangeDetectorRef) { }

    ngOnInit(): void {
        this.loadRecommendations();
        this.loadMedicalEvents(); // Load medical events on init
    }

    loadRecommendations(callback?: () => void): void {
        this.isLoading = true;
        this.errorMessage = '';

        const obs = this.searchTerm.trim()
            ? this.recommendationService.searchRecommendations(this.searchTerm)
            : this.recommendationService.getAll();

        obs.subscribe({
            next: (data: any) => {
                // Le backend renvoie parfois un Page (data.content) au lieu d'une liste directe. On gère les deux cas.
                if (data && data.content && Array.isArray(data.content)) {
                    this.recommendations = data.content;
                } else if (Array.isArray(data)) {
                    this.recommendations = data;
                } else if (data && typeof data === 'object') {
                    // Fallback si la structure est inattendue mais contient des données
                    this.recommendations = data.content || [];
                } else {
                    this.recommendations = [];
                }

                this.isLoading = false;
                this.cdr.detectChanges();
                if (callback) callback();
            },
            error: (err) => {
                this.errorMessage = 'Le service de recommandation est injoignable (port 8085).';
                console.error('Fetch error:', err);
                this.isLoading = false;
                this.cdr.detectChanges();
                if (callback) callback();
            }
        });
    }

    onSearch(): void {
        this.loadRecommendations();
    }

    loadMedicalEvents(): void {
        this.recommendationService.getAllEvents().subscribe({
            next: (data) => this.medicalEvents = data,
            error: () => console.error('Erreur de chargement des jeux médicaux.')
        });
    }

    openCreateForm(): void {
        this.loadRecommendations();
        this.loadMedicalEvents();
        this.isEditing = false;
        this.currentRecommendation = this.initNew();
        this.showForm = true;
    }

    private initNew(): Recommendation {
        return {
            content: '',
            type: RecommendationType.LIFESTYLE,
            status: RecommendationStatus.PENDING,
            doctorId: 0,
            patientId: 0,
            medicalEvents: []
        };
    }

    isEventSelected(event: MedicalEvent): boolean {
        if (!this.currentRecommendation.medicalEvents) return false;
        return this.currentRecommendation.medicalEvents.some(e => e.id === event.id);
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

    edit(rec: Recommendation): void {
        this.isEditing = true;
        this.currentRecommendation = { ...rec };
        this.showForm = true;
    }

    save(): void {
        if (!this.currentRecommendation.content.trim()) return;

        const handleSuccess = () => {
            this.loadRecommendations(() => {
                this.showForm = false;
            });
        };

        if (this.isEditing && this.currentRecommendation.id) {
            this.recommendationService.update(this.currentRecommendation.id, this.currentRecommendation).subscribe({
                next: handleSuccess,
                error: (err) => this.errorMessage = 'Erreur de mise à jour.'
            });
        } else {
            this.recommendationService.create(this.currentRecommendation).subscribe({
                next: handleSuccess,
                error: (err) => this.errorMessage = 'Erreur de création.'
            });
        }
    }

    cancel(): void {
        this.showForm = false;
        this.errorMessage = '';
        this.loadRecommendations(); // Refresh just in case while closing
    }

    approve(id: number | undefined): void {
        if (!id) return;
        this.recommendationService.approve(id).subscribe({
            next: () => this.loadRecommendations(),
            error: (err) => { this.errorMessage = 'Erreur lors de l\'approbation.'; console.error(err); }
        });
    }

    reject(rec: Recommendation): void {
        if (!rec.id) return;
        const updated = { ...rec, status: RecommendationStatus.REJECTED };
        this.recommendationService.update(rec.id, updated).subscribe({
            next: () => this.loadRecommendations(),
            error: (err) => { this.errorMessage = 'Erreur lors du rejet.'; console.error(err); }
        });
    }

    delete(id: number | undefined): void {
        if (id && confirm('Voulez-vous vraiment supprimer cette recommandation ?')) {
            this.recommendationService.delete(id).subscribe({
                next: () => this.loadRecommendations(),
                error: (err) => { this.errorMessage = 'Erreur lors de la suppression.'; console.error(err); }
            });
        }
    }
}
