import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { RecommendationService } from '../recommendation/recommendation.service';
import { MedicalEvent } from '../recommendation/recommendation.model';

@Component({
    selector: 'app-medical-events',
    templateUrl: './medical-events.html',
    styleUrls: ['./medical-events.css'],
    standalone: false
})
export class MedicalEventsPage implements OnInit {
    events: MedicalEvent[] = [];
    showForm = false;
    isLoading = true;
    searchTerm = '';
    errorMessage = '';
    successMessage = '';

    difficultyOptions = ['EASY', 'MEDIUM', 'HARD'];
    typeOptions = ['GAME', 'MEMORY_TEST', 'COGNITIVE_TEST', 'REACTION_TEST'];

    newEvent: MedicalEvent = this.initNew();

    constructor(private recommendationService: RecommendationService, private cdr: ChangeDetectorRef) { }

    ngOnInit(): void {
        this.loadEvents();
    }

    private initNew(): MedicalEvent {
        return { title: '', description: '', type: 'GAME', difficulty: 'EASY' };
    }

    loadEvents(callback?: () => void): void {
        this.isLoading = true;
        const obs = this.searchTerm.trim()
            ? this.recommendationService.searchEvents(this.searchTerm)
            : this.recommendationService.getAllEvents();

        obs.subscribe({
            next: (data: any) => {
                // Gestion robuste : tableau direct ou structure paginée content
                if (Array.isArray(data)) {
                    this.events = data;
                } else if (data && data.content && Array.isArray(data.content)) {
                    this.events = data.content;
                } else {
                    this.events = data ? (data.content || []) : [];
                }
                this.isLoading = false;
                this.cdr.detectChanges();
                if (callback) callback();
            },
            error: (err) => {
                console.error('Erreur de chargement des jeux médicaux:', err);
                this.errorMessage = 'Impossible de charger les jeux. Vérifiez que le backend tourne sur le port 8085.';
                this.isLoading = false;
                this.cdr.detectChanges();
                if (callback) callback();
            }
        });
    }

    onSearch(): void {
        this.loadEvents();
    }

    openForm(): void {
        this.newEvent = this.initNew();
        this.showForm = true;
        this.successMessage = '';
        this.errorMessage = '';
    }

    save(): void {
        if (!this.newEvent.title.trim()) return;

        this.recommendationService.createEvent(this.newEvent).subscribe({
            next: () => {
                this.successMessage = `Jeu "${this.newEvent.title}" créé avec succès !`;
                this.errorMessage = '';
                this.loadEvents(() => this.showForm = false);
            },
            error: () => {
                this.errorMessage = 'Erreur lors de la création du jeu.';
                this.successMessage = '';
            }
        });
    }

    delete(id: number | undefined): void {
        if (id && confirm('Supprimer ce jeu médical ?')) {
            this.recommendationService.deleteEvent(id).subscribe({
                next: () => { this.successMessage = 'Jeu supprimé.'; this.loadEvents(); },
                error: () => { this.errorMessage = 'Erreur lors de la suppression.'; }
            });
        }
    }

    cancel(): void {
        this.showForm = false;
    }

    getDifficultyClass(difficulty: string): string {
        const map: Record<string, string> = {
            EASY: 'bg-success-subtle text-success',
            MEDIUM: 'bg-warning-subtle text-warning',
            HARD: 'bg-danger-subtle text-danger'
        };
        return map[difficulty] || 'bg-secondary-subtle text-secondary';
    }

    getTypeIcon(type: string): string {
        const map: Record<string, string> = {
            GAME: 'fi-rr-dice',
            MEMORY_TEST: 'fi-rr-brain',
            COGNITIVE_TEST: 'fi-rr-star',
            REACTION_TEST: 'fi-rr-time-fast'
        };
        return map[type] || 'fi-rr-square';
    }
}
