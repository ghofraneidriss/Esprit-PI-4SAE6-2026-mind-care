import { Component, OnInit } from '@angular/core';
import { Recommendation, RecommendationType, RecommendationStatus } from './recommendation.model';
import { RecommendationService } from './recommendation.service';

@Component({
    selector: 'app-recommendation',
    templateUrl: './recommendation.html',
    styleUrls: ['./recommendation.css'],
    standalone: false
})
export class RecommendationPage implements OnInit {
    recommendations: Recommendation[] = [];
    loading = false;
    showForm = false;
    isEditing = false;
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

    constructor(private recommendationService: RecommendationService) { }

    ngOnInit(): void {
        this.loadRecommendations();
    }

    loadRecommendations(): void {
        this.loading = true;
        this.errorMessage = '';
        this.recommendationService.getAll().subscribe({
            next: (data) => {
                this.recommendations = data;
                this.loading = false;
            },
            error: (err) => {
                this.errorMessage = 'Erreur lors du chargement des recommandations. Vérifiez que le serveur est démarré sur le port 8085.';
                console.error(err);
                this.loading = false;
            }
        });
    }

    openCreateForm(): void {
        this.isEditing = false;
        this.currentRecommendation = {
            content: '',
            type: RecommendationType.OTHER,
            status: RecommendationStatus.PENDING,
            doctorId: 0,
            patientId: 0
        };
        this.showForm = true;
    }

    edit(rec: Recommendation): void {
        this.isEditing = true;
        this.currentRecommendation = { ...rec };
        this.showForm = true;
    }

    save(): void {
        if (!this.currentRecommendation.content.trim()) return;

        if (this.isEditing && this.currentRecommendation.id) {
            this.recommendationService.update(this.currentRecommendation.id, this.currentRecommendation).subscribe({
                next: () => { this.loadRecommendations(); this.showForm = false; },
                error: (err) => { this.errorMessage = 'Erreur lors de la mise à jour.'; console.error(err); }
            });
        } else {
            this.recommendationService.create(this.currentRecommendation).subscribe({
                next: () => { this.loadRecommendations(); this.showForm = false; },
                error: (err) => { this.errorMessage = 'Erreur lors de la création.'; console.error(err); }
            });
        }
    }

    cancel(): void {
        this.showForm = false;
        this.errorMessage = '';
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
