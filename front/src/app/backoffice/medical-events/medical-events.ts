import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { RecommendationService } from '../recommendation/recommendation.service';
import { DifficultyLevel, MedicalEvent, MedicalEventType } from '../recommendation/recommendation.model';

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
    isSaving = false;
    searchTerm = '';
    errorMessage = '';
    successMessage = '';

    difficultyOptions = Object.values(DifficultyLevel);
    typeOptions = Object.values(MedicalEventType);

    newEvent: MedicalEvent = this.initNew();

    constructor(private recommendationService: RecommendationService, private cdr: ChangeDetectorRef) { }

    ngOnInit(): void {
        this.loadEvents();
    }

    private initNew(): MedicalEvent {
        return { title: '', description: '', type: MedicalEventType.MEMORY, difficulty: DifficultyLevel.EASY };
    }

    loadEvents(callback?: () => void): void {
        this.isLoading = true;
        const obs = this.searchTerm.trim()
            ? this.recommendationService.searchEvents(this.searchTerm)
            : this.recommendationService.getAllEvents();

        obs.subscribe({
            next: (data: any) => {
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
                console.error('Erreur de chargement des jeux medicaux:', err);
                this.errorMessage = this.extractErrorMessage(err, 'Impossible de charger les jeux. Verifiez que le backend tourne sur le port 8085.');
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

        this.isSaving = true;
        this.errorMessage = '';

        this.recommendationService.createEvent(this.newEvent).subscribe({
            next: (created) => {
                this.successMessage = `Jeu "${this.newEvent.title}" cree avec succes !`;
                this.errorMessage = '';
                this.events = [created, ...this.events];
                this.searchTerm = '';
                this.loadEvents(() => {
                    this.showForm = false;
                    this.isSaving = false;
                });
            },
            error: (err) => {
                this.errorMessage = this.extractErrorMessage(err, 'Erreur lors de la creation du jeu.');
                this.successMessage = '';
                this.isSaving = false;
            }
        });
    }

    delete(id: number | undefined): void {
        if (id && confirm('Supprimer ce jeu medical ?')) {
            this.recommendationService.deleteEvent(id).subscribe({
                next: () => {
                    this.successMessage = 'Jeu supprime.';
                    this.loadEvents();
                },
                error: (err) => {
                    this.errorMessage = this.extractErrorMessage(err, 'Erreur lors de la suppression.');
                }
            });
        }
    }

    cancel(): void {
        this.showForm = false;
        this.isSaving = false;
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
            MEMORY: 'fi-rr-brain',
            FLUENCY: 'fi-rr-comment-alt',
            VISUOSPATIAL: 'fi-rr-eye',
            ATTENTION: 'fi-rr-bulb',
            MEDICATION: 'fi-rr-pills',
            EXERCISE: 'fi-rr-dumbbell',
            DIET: 'fi-rr-apple-whole',
            LIFESTYLE: 'fi-rr-heart',
            OTHER: 'fi-rr-dice'
        };
        return map[type] || 'fi-rr-square';
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