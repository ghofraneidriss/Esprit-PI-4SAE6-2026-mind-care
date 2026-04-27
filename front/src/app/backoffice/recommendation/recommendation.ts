import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import {
    AutoRecommendationGenerateRequest,
    ClinicalEscalationAlert,
    MedicalEvent,
    MedicalEventStatus,
    PatientLevel,
    Recommendation,
    RecommendationStatus,
    RecommendationType
} from './recommendation.model';
import { RecommendationService } from './recommendation.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
    selector: 'app-recommendation',
    templateUrl: './recommendation.html',
    styleUrls: ['./recommendation.css'],
    standalone: false
})
export class RecommendationPage implements OnInit {
    recommendations: Recommendation[] = [];
    filteredRecommendations: Recommendation[] = [];
    selectedRecommendation: Recommendation | null = null;
    medicalEvents: MedicalEvent[] = [];

    showForm = false;
    isEditing = false;
    isLoading = true;
    isSaving = false;
    isGeneratingAuto = false;
    isLoadingAlerts = false;

    searchTerm = '';
    statusFilter: RecommendationStatus | '' = '';
    typeFilter: RecommendationType | '' = '';
    errorMessage = '';
    successMessage = '';
    alertDoctorId: number | null = null;
    clinicalAlerts: ClinicalEscalationAlert[] = [];

    readonly recommendationTypes = Object.values(RecommendationType);
    readonly recommendationStatuses = Object.values(RecommendationStatus);
    readonly patientLevels = Object.values(PatientLevel);
    readonly activeStatus = RecommendationStatus.ACTIVE;
    readonly acceptedStatus = RecommendationStatus.ACCEPTED;

    currentRecommendation: Recommendation = this.initNew();
    autoRecommendationForm: AutoRecommendationGenerateRequest = this.initAutoRecommendationForm();

    constructor(
        private readonly recommendationService: RecommendationService,
        private readonly cdr: ChangeDetectorRef,
        public readonly authService: AuthService
    ) { }

    ngOnInit(): void {
        this.loadRecommendations();
        this.loadMedicalEvents();
    }

    get openAlertsCount(): number {
        return this.clinicalAlerts.filter((alert) => alert.status === 'OPEN').length;
    }

    get autoGenerationMedicalEvents(): MedicalEvent[] {
        const patientId = this.autoRecommendationForm.patientId;
        return this.medicalEvents.filter((event) => {
            const samePatient = patientId > 0 ? event.patientId === patientId : true;
            const activeStatus = !event.status || event.status === MedicalEventStatus.ACTIVE;
            return samePatient && activeStatus;
        });
    }

    get compatibleMedicalEvents(): MedicalEvent[] {
        const patientId = this.currentRecommendation.patientId;
        const recommendationType = this.currentRecommendation.type;

        return this.medicalEvents.filter((event) => {
            const samePatient = patientId > 0 ? event.patientId === patientId : true;
            const sameType = this.matchesRecommendationType(event.type, recommendationType);
            const activeStatus = !event.status || event.status === MedicalEventStatus.ACTIVE;
            return samePatient && sameType && activeStatus;
        });
    }

    countByStatus(status: RecommendationStatus): number {
        return this.recommendations.filter((recommendation) => recommendation.status === status).length;
    }

    loadRecommendations(preferredSelectionId?: number | null): void {
        this.isLoading = true;
        this.recommendationService.getAll().subscribe({
            next: (data) => {
                this.recommendations = this.normalizeRecommendations(data);
                this.applyFilters(preferredSelectionId);
                this.syncAlertDoctorIdFromRecommendations();
                this.isLoading = false;
                this.syncView();
            },
            error: (err) => {
                this.errorMessage = this.extractErrorMessage(
                    err,
                    'Le microservice recommendation est injoignable via la gateway.'
                );
                this.recommendations = [];
                this.filteredRecommendations = [];
                this.selectedRecommendation = null;
                this.isLoading = false;
                this.syncView();
            }
        });
    }

    applyFilters(preferredSelectionId?: number | null): void {
        const query = this.searchTerm.trim().toLowerCase();
        const currentSelectionId = preferredSelectionId ?? this.selectedRecommendation?.id ?? null;

        this.filteredRecommendations = this.recommendations.filter((recommendation) => {
            const matchesQuery =
                !query
                || recommendation.content.toLowerCase().includes(query)
                || String(recommendation.id ?? '').includes(query)
                || String(recommendation.patientId ?? '').includes(query)
                || String(recommendation.doctorId ?? '').includes(query)
                || (recommendation.generatedMedicalEventTitle ?? '').toLowerCase().includes(query);
            const matchesStatus = !this.statusFilter || recommendation.status === this.statusFilter;
            const matchesType = !this.typeFilter || recommendation.type === this.typeFilter;
            return matchesQuery && matchesStatus && matchesType;
        });

        this.selectedRecommendation =
            this.filteredRecommendations.find((recommendation) => recommendation.id === currentSelectionId)
            ?? this.filteredRecommendations[0]
            ?? null;
    }

    selectRecommendation(recommendation: Recommendation): void {
        this.selectedRecommendation = recommendation;
        this.useDoctorAlerts(recommendation.doctorId);
    }

    loadMedicalEvents(): void {
        this.recommendationService.getAllEvents().subscribe({
            next: (data) => {
                this.medicalEvents = data;
                this.syncView();
            },
            error: (err) => {
                this.medicalEvents = [];
                this.errorMessage = this.extractErrorMessage(
                    err,
                    'Erreur de chargement des medical events lies aux recommandations.'
                );
                this.syncView();
            }
        });
    }

    loadAlerts(): void {
        if (!this.alertDoctorId || this.alertDoctorId <= 0) {
            this.clinicalAlerts = [];
            return;
        }

        this.isLoadingAlerts = true;
        this.recommendationService.getDoctorAlerts(this.alertDoctorId).subscribe({
            next: (data) => {
                this.clinicalAlerts = data;
                this.isLoadingAlerts = false;
                this.syncView();
            },
            error: (err) => {
                this.errorMessage = this.extractErrorMessage(err, 'Erreur de chargement des alertes cliniques.');
                this.isLoadingAlerts = false;
                this.syncView();
            }
        });
    }

    openCreateForm(): void {
        this.isEditing = false;
        this.errorMessage = '';
        this.successMessage = '';
        this.currentRecommendation = this.initNew();
        this.showForm = true;
    }

    edit(recommendation: Recommendation): void {
        this.isEditing = true;
        this.errorMessage = '';
        this.successMessage = '';
        this.currentRecommendation = {
            ...recommendation,
            expirationDate: this.normalizeDateForInput(recommendation.expirationDate),
            priority: recommendation.priority ?? 0
        };
        this.showForm = true;
    }

    save(): void {
        if (!this.currentRecommendation.content.trim()) {
            return;
        }

        if (this.currentRecommendation.expirationDate && this.isPastDate(this.currentRecommendation.expirationDate)) {
            this.errorMessage = "La date d'expiration ne peut pas etre dans le passe.";
            return;
        }

        this.isSaving = true;
        this.errorMessage = '';

        const handleSuccess = (saved: Recommendation) => {
            this.successMessage = this.isEditing
                ? 'Recommandation mise a jour avec succes.'
                : 'Recommandation ajoutee avec succes.';
            this.showForm = false;
            this.isSaving = false;
            this.syncView();
            this.loadRecommendations(saved.id ?? null);
        };

        if (this.isEditing && this.currentRecommendation.id) {
            this.recommendationService.update(this.currentRecommendation.id, this.currentRecommendation).subscribe({
                next: handleSuccess,
                error: (err) => {
                    this.errorMessage = this.extractErrorMessage(err, 'Erreur de mise a jour.');
                    this.isSaving = false;
                    this.syncView();
                }
            });
            return;
        }

        this.recommendationService.create(this.currentRecommendation).subscribe({
            next: handleSuccess,
            error: (err) => {
                this.errorMessage = this.extractErrorMessage(err, 'Erreur de creation.');
                this.isSaving = false;
                this.syncView();
            }
        });
    }

    cancel(): void {
        this.showForm = false;
        this.isSaving = false;
        this.errorMessage = '';
    }

    approve(id: number | undefined): void {
        if (!id) {
            return;
        }

        this.recommendationService.approve(id).subscribe({
            next: () => {
                this.successMessage = 'Recommandation approuvee avec succes.';
                this.loadRecommendations(id);
                this.syncView();
            },
            error: (err) => {
                this.errorMessage = this.extractErrorMessage(err, "Erreur lors de l'approbation.");
                this.syncView();
            }
        });
    }

    reject(recommendation: Recommendation): void {
        if (!recommendation.id) {
            return;
        }

        this.recommendationService.dismiss(recommendation.id).subscribe({
            next: () => {
                this.successMessage = 'Recommandation rejetee avec succes.';
                this.loadRecommendations(recommendation.id);
                this.syncView();
            },
            error: (err) => {
                this.errorMessage = this.extractErrorMessage(err, 'Erreur lors du rejet.');
                this.syncView();
            }
        });
    }

    delete(id: number | undefined): void {
        if (!id || !confirm('Voulez-vous vraiment supprimer cette recommandation ?')) {
            return;
        }

        this.recommendationService.delete(id).subscribe({
            next: () => {
                this.successMessage = 'Recommandation supprimee avec succes.';
                this.loadRecommendations();
                this.syncView();
            },
            error: (err) => {
                this.errorMessage = this.extractErrorMessage(err, 'Erreur lors de la suppression.');
                this.syncView();
            }
        });
    }

    generateAutomaticRecommendations(): void {
        if (this.autoRecommendationForm.doctorId <= 0 || this.autoRecommendationForm.patientId <= 0) {
            this.errorMessage = 'Renseigne un ID docteur et un ID patient valides pour lancer les regles automatiques.';
            return;
        }

        this.isGeneratingAuto = true;
        this.errorMessage = '';
        this.successMessage = '';

        const payload: AutoRecommendationGenerateRequest = {
            ...this.autoRecommendationForm,
            recentRecommendationTypes: [...this.autoRecommendationForm.recentRecommendationTypes]
        };

        this.recommendationService.generateAutomaticRecommendations(payload).subscribe({
            next: (created) => {
                this.successMessage = `${created.length} recommandation(s) automatique(s) generee(s) avec succes.`;
                this.alertDoctorId = this.autoRecommendationForm.doctorId;
                this.isGeneratingAuto = false;
                this.loadRecommendations();
                this.loadAlerts();
                this.syncView();
            },
            error: (err) => {
                this.errorMessage = this.extractErrorMessage(err, 'Erreur lors de la generation automatique.');
                this.isGeneratingAuto = false;
                this.syncView();
            }
        });
    }

    toggleRecentType(type: RecommendationType): void {
        const exists = this.autoRecommendationForm.recentRecommendationTypes.includes(type);
        this.autoRecommendationForm.recentRecommendationTypes = exists
            ? this.autoRecommendationForm.recentRecommendationTypes.filter((item) => item !== type)
            : [...this.autoRecommendationForm.recentRecommendationTypes, type];
    }

    hasRecentType(type: RecommendationType): boolean {
        return this.autoRecommendationForm.recentRecommendationTypes.includes(type);
    }

    resetAutoRecommendationForm(): void {
        this.autoRecommendationForm = this.initAutoRecommendationForm();
    }

    onAutoGenerationPatientChanged(): void {
        const selectedId = this.autoRecommendationForm.preferredMedicalEventId;
        if (!selectedId) {
            return;
        }

        const selectedEvent = this.medicalEvents.find((event) => event.id === selectedId);
        const stillCompatible = selectedEvent
            && (!selectedEvent.status || selectedEvent.status === MedicalEventStatus.ACTIVE)
            && (this.autoRecommendationForm.patientId > 0 ? selectedEvent.patientId === this.autoRecommendationForm.patientId : true);

        if (!stillCompatible) {
            this.autoRecommendationForm.preferredMedicalEventId = null;
        }
    }

    useDoctorAlerts(doctorId?: number): void {
        if (!doctorId) {
            return;
        }

        this.alertDoctorId = doctorId;
        this.loadAlerts();
    }

    resolveAlert(alert: ClinicalEscalationAlert): void {
        this.recommendationService.resolveAlert(alert.id).subscribe({
            next: () => {
                this.successMessage = 'Alerte clinique resolue avec succes.';
                this.loadAlerts();
                this.syncView();
            },
            error: (err) => {
                this.errorMessage = this.extractErrorMessage(err, "Erreur lors de la resolution de l'alerte.");
                this.syncView();
            }
        });
    }

    isEventSelected(event: MedicalEvent): boolean {
        return this.currentRecommendation.generatedMedicalEventId === event.id;
    }

    toggleEvent(event: MedicalEvent): void {
        if (!this.isEventCompatible(event)) {
            this.errorMessage = 'Choisis un medical event du meme patient et du meme type que la recommandation.';
            return;
        }

        this.currentRecommendation.generatedMedicalEventId = event.id ?? null;
        this.currentRecommendation.generatedMedicalEventTitle = event.title ?? null;
    }

    isEventCompatible(event: MedicalEvent): boolean {
        const patientId = this.currentRecommendation.patientId;
        const recommendationType = this.currentRecommendation.type;
        const samePatient = patientId > 0 ? event.patientId === patientId : true;
        const sameType = this.matchesRecommendationType(event.type, recommendationType);
        const activeStatus = !event.status || event.status === MedicalEventStatus.ACTIVE;
        return samePatient && sameType && activeStatus;
    }

    onRecommendationContextChanged(): void {
        const selectedId = this.currentRecommendation.generatedMedicalEventId;
        if (!selectedId) {
            return;
        }

        const selectedEvent = this.medicalEvents.find((event) => event.id === selectedId);
        if (selectedEvent && !this.isEventCompatible(selectedEvent)) {
            this.currentRecommendation.generatedMedicalEventId = null;
            this.currentRecommendation.generatedMedicalEventTitle = null;
        }
    }

    getEventDisplayName(recommendation: Recommendation): string | null {
        if (recommendation.generatedMedicalEventTitle) {
            return recommendation.generatedMedicalEventTitle;
        }

        if (recommendation.generatedMedicalEventId != null) {
            const match = this.medicalEvents.find((event) => event.id === recommendation.generatedMedicalEventId);
            return match?.title ?? null;
        }

        return null;
    }

    getTypeBadgeClass(type?: string): string {
        const map: Record<string, string> = {
            MEDICATION: 'type-chip chip-danger',
            EXERCISE: 'type-chip chip-success',
            DIET: 'type-chip chip-warning',
            LIFESTYLE: 'type-chip chip-info',
            MEMORY: 'type-chip chip-primary',
            ATTENTION: 'type-chip chip-info',
            FLUENCY: 'type-chip chip-secondary',
            VISUOSPATIAL: 'type-chip chip-dark',
            PUZZLE: 'type-chip chip-teal',
            OTHER: 'type-chip chip-secondary'
        };
        return map[type || ''] || 'type-chip chip-secondary';
    }

    getStatusBadgeClass(status?: string): string {
        const map: Record<string, string> = {
            ACTIVE: 'status-badge badge-active',
            ACCEPTED: 'status-badge badge-accepted',
            REJECTED: 'status-badge badge-rejected',
            EXPIRED: 'status-badge badge-expired'
        };
        return map[status || ''] || 'status-badge badge-expired';
    }

    trackRecommendation(index: number, recommendation: Recommendation): number {
        return recommendation.id ?? index;
    }

    formatDateTime(value?: string | null): string {
        if (!value) {
            return '-';
        }

        const date = new Date(value);
        return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
    }

    private initNew(): Recommendation {
        return {
            content: '',
            type: RecommendationType.LIFESTYLE,
            status: RecommendationStatus.ACTIVE,
            doctorId: 0,
            patientId: 0,
            priority: 0
        };
    }

    private initAutoRecommendationForm(): AutoRecommendationGenerateRequest {
        return {
            doctorId: 0,
            patientId: 0,
            preferredMedicalEventId: null,
            age: 65,
            level: PatientLevel.MEDIUM,
            weeklyFrequency: 0,
            acceptedCount: 0,
            rejectedCount: 0,
            medicationAdherenceIssue: false,
            lowPhysicalActivity: false,
            cognitiveDropObserved: false,
            recentRecommendationTypes: []
        };
    }

    private normalizeRecommendations(data: unknown): Recommendation[] {
        if (Array.isArray(data)) {
            return data;
        }

        if (data && typeof data === 'object' && 'content' in data) {
            const content = (data as { content?: Recommendation[] }).content;
            return Array.isArray(content) ? content : [];
        }

        return [];
    }

    private normalizeDateForInput(value?: string | null): string | null {
        if (!value) {
            return null;
        }

        if (value.includes('T')) {
            return value.split('T')[0];
        }

        const frenchDateMatch = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(value);
        if (frenchDateMatch) {
            return `${frenchDateMatch[3]}-${frenchDateMatch[2]}-${frenchDateMatch[1]}`;
        }

        return value;
    }

    private isPastDate(value: string): boolean {
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return false;
        }

        const today = new Date();
        today.setHours(0, 0, 0, 0);
        date.setHours(0, 0, 0, 0);
        return date < today;
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

    private syncAlertDoctorIdFromRecommendations(): void {
        if (this.selectedRecommendation?.doctorId) {
            if (this.alertDoctorId !== this.selectedRecommendation.doctorId) {
                this.alertDoctorId = this.selectedRecommendation.doctorId;
                this.loadAlerts();
            }
            return;
        }

        const availableDoctorIds = this.recommendations
            .map((recommendation) => recommendation.doctorId)
            .filter((doctorId): doctorId is number => !!doctorId && doctorId > 0);

        if (availableDoctorIds.length === 0) {
            this.alertDoctorId = null;
            this.clinicalAlerts = [];
            return;
        }

        if (!this.alertDoctorId || !availableDoctorIds.includes(this.alertDoctorId)) {
            this.alertDoctorId = availableDoctorIds[0];
            this.loadAlerts();
        }
    }

    private matchesRecommendationType(eventType: string | undefined, recommendationType: string | undefined): boolean {
        if (!recommendationType) {
            return true;
        }

        return eventType === recommendationType;
    }

    private syncView(): void {
        this.cdr.detectChanges();
    }
}
