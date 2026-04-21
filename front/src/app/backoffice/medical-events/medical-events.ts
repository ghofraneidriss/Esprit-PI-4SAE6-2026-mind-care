import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../core/services/auth.service';
import { RecommendationService } from '../recommendation/recommendation.service';
import {
    DifficultyLevel,
    MedicalEvent,
    MedicalEventStatus,
    MedicalEventType
} from '../recommendation/recommendation.model';

@Component({
    selector: 'app-medical-events',
    templateUrl: './medical-events.html',
    styleUrls: ['./medical-events.css'],
    standalone: false
})
export class MedicalEventsPage implements OnInit {
    events: MedicalEvent[] = [];
    filteredEvents: MedicalEvent[] = [];
    selectedEvent: MedicalEvent | null = null;

    showForm = false;
    isEditing = false;
    isLoading = true;
    isSaving = false;

    searchTerm = '';
    typeFilter: MedicalEventType | '' = '';
    difficultyFilter: DifficultyLevel | '' = '';
    errorMessage = '';
    successMessage = '';

    readonly difficultyOptions = Object.values(DifficultyLevel);
    readonly typeOptions = Object.values(MedicalEventType);
    readonly statusOptions = Object.values(MedicalEventStatus);

    currentEvent: MedicalEvent = this.initNew();

    constructor(
        private readonly recommendationService: RecommendationService,
        private readonly cdr: ChangeDetectorRef,
        public readonly authService: AuthService
    ) { }

    ngOnInit(): void {
        this.loadEvents();
    }

    get activeCount(): number {
        return this.events.filter((event) => event.status === MedicalEventStatus.ACTIVE || !event.status).length;
    }

    get completedCount(): number {
        return this.events.filter((event) => event.status === MedicalEventStatus.COMPLETED).length;
    }

    get hardCount(): number {
        return this.events.filter((event) => event.difficulty === DifficultyLevel.HARD).length;
    }

    loadEvents(preferredSelectionId?: number | null): void {
        this.isLoading = true;

        this.recommendationService.getAllEvents().subscribe({
            next: (data) => {
                this.events = this.normalizeEvents(data);
                this.applyFilters(preferredSelectionId);
                this.isLoading = false;
                this.syncView();
            },
            error: (err) => {
                this.errorMessage = this.extractErrorMessage(
                    err,
                    'Le microservice medical events est injoignable via la gateway.'
                );
                this.events = [];
                this.filteredEvents = [];
                this.selectedEvent = null;
                this.isLoading = false;
                this.syncView();
            }
        });
    }

    applyFilters(preferredSelectionId?: number | null): void {
        const query = this.searchTerm.trim().toLowerCase();
        const currentSelectionId = preferredSelectionId ?? this.selectedEvent?.id ?? null;

        this.filteredEvents = this.events.filter((event) => {
            const matchesQuery =
                !query
                || String(event.id ?? '').includes(query)
                || event.title.toLowerCase().includes(query)
                || (event.description ?? '').toLowerCase().includes(query)
                || (event.type ?? '').toLowerCase().includes(query)
                || String(event.patientId ?? '').includes(query);
            const matchesType = !this.typeFilter || event.type === this.typeFilter;
            const matchesDifficulty = !this.difficultyFilter || event.difficulty === this.difficultyFilter;
            return matchesQuery && matchesType && matchesDifficulty;
        });

        this.selectedEvent =
            this.filteredEvents.find((event) => event.id === currentSelectionId)
            ?? this.filteredEvents[0]
            ?? null;
    }

    selectEvent(event: MedicalEvent): void {
        this.selectedEvent = event;
    }

    openCreateForm(): void {
        this.isEditing = false;
        this.currentEvent = this.initNew();
        this.showForm = true;
        this.successMessage = '';
        this.errorMessage = '';
    }

    editEvent(event: MedicalEvent): void {
        this.isEditing = true;
        this.currentEvent = {
            ...event,
            startDate: this.normalizeDateForInput(event.startDate),
            endDate: this.normalizeDateForInput(event.endDate)
        };
        this.showForm = true;
        this.successMessage = '';
        this.errorMessage = '';
    }

    save(): void {
        if (!this.currentEvent.title.trim()) {
            return;
        }

        if (this.hasInvalidDateRange(this.currentEvent.startDate, this.currentEvent.endDate)) {
            this.errorMessage = 'La date de fin doit etre posterieure ou egale a la date de debut.';
            return;
        }

        this.isSaving = true;
        this.errorMessage = '';

        const payload: MedicalEvent = {
            ...this.currentEvent,
            title: this.currentEvent.title.trim(),
            description: (this.currentEvent.description ?? '').trim()
        };

        if (this.isEditing && this.currentEvent.id) {
            this.recommendationService.updateEvent(this.currentEvent.id, payload).subscribe({
                next: (updated) => {
                    this.successMessage = `Jeu "${updated.title}" mis a jour avec succes.`;
                    this.showForm = false;
                    this.isSaving = false;
                    this.syncView();
                    this.loadEvents(updated.id ?? null);
                },
                error: (err) => {
                    this.errorMessage = this.extractErrorMessage(err, 'Erreur lors de la mise a jour du jeu.');
                    this.isSaving = false;
                    this.syncView();
                }
            });
            return;
        }

        this.recommendationService.createEvent(payload).subscribe({
            next: (created) => {
                this.successMessage = `Jeu "${created.title}" cree avec succes.`;
                this.showForm = false;
                this.isSaving = false;
                this.syncView();
                this.loadEvents(created.id ?? null);
            },
            error: (err) => {
                this.errorMessage = this.extractErrorMessage(err, 'Erreur lors de la creation du jeu.');
                this.isSaving = false;
                this.syncView();
            }
        });
    }

    delete(id: number | undefined): void {
        if (!id || !confirm('Supprimer ce jeu medical ?')) {
            return;
        }

        this.recommendationService.deleteEvent(id).subscribe({
            next: () => {
                this.successMessage = 'Jeu supprime.';
                this.syncView();
                this.loadEvents();
            },
            error: (err) => {
                this.errorMessage = this.extractErrorMessage(err, 'Erreur lors de la suppression.');
                this.syncView();
            }
        });
    }

    cancel(): void {
        this.showForm = false;
        this.isSaving = false;
    }

    getDifficultyBadgeClass(difficulty?: string): string {
        const map: Record<string, string> = {
            EASY: 'difficulty-badge badge-easy',
            MEDIUM: 'difficulty-badge badge-medium',
            HARD: 'difficulty-badge badge-hard'
        };
        return map[difficulty || ''] || 'difficulty-badge badge-medium';
    }

    getStatusBadgeClass(status?: string): string {
        const effectiveStatus = status || MedicalEventStatus.ACTIVE;
        const map: Record<string, string> = {
            ACTIVE: 'status-badge badge-active',
            COMPLETED: 'status-badge badge-completed',
            CANCELLED: 'status-badge badge-cancelled'
        };
        return map[effectiveStatus] || 'status-badge badge-active';
    }

    getStatusLabel(status?: string): string {
        return status || MedicalEventStatus.ACTIVE;
    }

    getTypeBadgeClass(type?: string): string {
        const map: Record<string, string> = {
            MEMORY: 'type-chip chip-primary',
            FLUENCY: 'type-chip chip-secondary',
            VISUOSPATIAL: 'type-chip chip-dark',
            ATTENTION: 'type-chip chip-info',
            PUZZLE: 'type-chip chip-teal',
            SUDOKU: 'type-chip chip-teal',
            MEDICATION: 'type-chip chip-danger',
            EXERCISE: 'type-chip chip-success',
            DIET: 'type-chip chip-warning',
            LIFESTYLE: 'type-chip chip-success',
            OTHER: 'type-chip chip-secondary'
        };
        return map[type || ''] || 'type-chip chip-secondary';
    }

    trackEvent(index: number, event: MedicalEvent): number {
        return event.id ?? index;
    }

    formatDateTime(value?: string | null): string {
        if (!value) {
            return '-';
        }

        const date = new Date(value);
        return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
    }

    private initNew(): MedicalEvent {
        return {
            title: '',
            description: '',
            type: MedicalEventType.MEMORY,
            difficulty: DifficultyLevel.EASY,
            status: MedicalEventStatus.ACTIVE,
            patientId: 1,
            familyId: null,
            startDate: '',
            endDate: ''
        };
    }

    private normalizeEvents(data: unknown): MedicalEvent[] {
        if (Array.isArray(data)) {
            return data;
        }

        if (data && typeof data === 'object' && 'content' in data) {
            const content = (data as { content?: MedicalEvent[] }).content;
            return Array.isArray(content) ? content : [];
        }

        return [];
    }

    private normalizeDateForInput(value?: string | null): string {
        if (!value) {
            return '';
        }

        if (value.includes('T')) {
            return value.split('T')[0];
        }

        return value;
    }

    private hasInvalidDateRange(startDate?: string | null, endDate?: string | null): boolean {
        const start = this.parseDateValue(startDate, false);
        const end = this.parseDateValue(endDate, true);

        if (!start || !end) {
            return false;
        }

        return end < start;
    }

    private parseDateValue(value?: string | null, endOfDay = false): Date | null {
        if (!value) {
            return null;
        }

        const trimmedValue = value.trim();
        if (!trimmedValue) {
            return null;
        }

        const dateOnlyPattern = /^\d{4}-\d{2}-\d{2}$/;
        const normalizedValue = dateOnlyPattern.test(trimmedValue)
            ? `${trimmedValue}T${endOfDay ? '23:59:59' : '00:00:00'}`
            : trimmedValue;

        const parsedDate = new Date(normalizedValue);
        return Number.isNaN(parsedDate.getTime()) ? null : parsedDate;
    }

    private extractErrorMessage(err: unknown, fallback: string): string {
        if (err instanceof HttpErrorResponse) {
            if (typeof err.error === 'string') {
                return err.error;
            }
            if (err.error?.message) {
                return err.error.message;
            }
            if (err.message) {
                return err.message;
            }
        }
        return fallback;
    }

    private syncView(): void {
        this.cdr.detectChanges();
    }
}
