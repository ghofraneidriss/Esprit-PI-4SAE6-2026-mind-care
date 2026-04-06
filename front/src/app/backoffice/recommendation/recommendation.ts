import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
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

@Component({
    selector: 'app-recommendation',
    templateUrl: './recommendation.html',
    styleUrls: ['./recommendation.css'],
    standalone: false
})
export class RecommendationPage implements OnInit {
    recommendations: Recommendation[] = [];
    medicalEvents: MedicalEvent[] = [];
    showForm = false;
    isEditing = false;
    isLoading = true;
    isSaving = false;
    isGeneratingAuto = false;
    isLoadingAlerts = false;
    searchTerm = '';
    errorMessage = '';
    successMessage = '';
    alertDoctorId: number | null = null;
    clinicalAlerts: ClinicalEscalationAlert[] = [];

    recommendationTypes = Object.values(RecommendationType);
    recommendationStatuses = Object.values(RecommendationStatus);
    patientLevels = Object.values(PatientLevel);

    currentRecommendation: Recommendation = {
        content: '',
        type: RecommendationType.OTHER,
        status: RecommendationStatus.ACTIVE,
        doctorId: 0,
        patientId: 0
    };

    autoRecommendationForm: AutoRecommendationGenerateRequest = this.initAutoRecommendationForm();

    constructor(private recommendationService: RecommendationService, private cdr: ChangeDetectorRef) { }

    ngOnInit(): void {
        this.loadRecommendations();
        this.loadMedicalEvents();
        this.loadAlerts();
    }

    loadRecommendations(callback?: () => void): void {
        this.isLoading = true;

        const obs = this.searchTerm.trim()
            ? this.recommendationService.searchRecommendations(this.searchTerm)
            : this.recommendationService.getAll();

        obs.subscribe({
            next: (data: any) => {
                if (data && data.content && Array.isArray(data.content)) {
                    this.recommendations = data.content;
                } else if (Array.isArray(data)) {
                    this.recommendations = data;
                } else if (data && typeof data === 'object') {
                    this.recommendations = data.content || [];
                } else {
                    this.recommendations = [];
                }

                this.syncAlertDoctorIdFromRecommendations();

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
            next: (data) => (this.medicalEvents = data),
            error: () => console.error('Erreur de chargement des jeux medicaux.')
        });
    }

    get autoGenerationMedicalEvents(): MedicalEvent[] {
        const patientId = this.autoRecommendationForm.patientId;
        return this.medicalEvents.filter((event) => {
            const samePatient = patientId > 0 ? event.patientId === patientId : true;
            const activeStatus = !event.status || event.status === MedicalEventStatus.ACTIVE;
            return samePatient && activeStatus;
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
            },
            error: (err) => {
                this.errorMessage = this.extractErrorMessage(err, 'Erreur de chargement des alertes cliniques.');
                this.isLoadingAlerts = false;
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

    isEventSelected(event: MedicalEvent): boolean {
        return this.currentRecommendation.generatedMedicalEventId === event.id;
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
        if (!selectedId) return;

        const selectedEvent = this.medicalEvents.find((event) => event.id === selectedId);
        if (selectedEvent && !this.isEventCompatible(selectedEvent)) {
            this.currentRecommendation.generatedMedicalEventId = null;
            this.currentRecommendation.generatedMedicalEventTitle = null;
        }
    }

    edit(rec: Recommendation): void {
        this.isEditing = true;
        this.errorMessage = '';
        this.successMessage = '';
        this.currentRecommendation = {
            ...rec,
            expirationDate: this.normalizeDateForInput(rec.expirationDate),
            priority: rec.priority ?? 0
        };
        this.showForm = true;
    }

    save(): void {
        if (!this.currentRecommendation.content.trim()) return;
        if (this.currentRecommendation.expirationDate && this.isPastDate(this.currentRecommendation.expirationDate)) {
            this.errorMessage = "La date d'expiration ne peut pas etre dans le passe.";
            return;
        }

        this.isSaving = true;
        this.errorMessage = '';

        const handleSuccess = (saved: Recommendation) => {
            this.searchTerm = '';
            this.successMessage = this.isEditing
                ? 'Recommandation mise a jour avec succes.'
                : 'Recommandation ajoutee avec succes.';

            this.upsertRecommendation(saved);
            this.showForm = false;
            this.isSaving = false;
            this.loadRecommendations();
        };

        if (this.isEditing && this.currentRecommendation.id) {
            this.recommendationService.update(this.currentRecommendation.id, this.currentRecommendation).subscribe({
                next: handleSuccess,
                error: (err) => {
                    console.error(err);
                    this.errorMessage = this.extractErrorMessage(err, 'Erreur de mise a jour.');
                    this.isSaving = false;
                }
            });
        } else {
            this.recommendationService.create(this.currentRecommendation).subscribe({
                next: handleSuccess,
                error: (err) => {
                    console.error(err);
                    this.errorMessage = this.extractErrorMessage(err, 'Erreur de creation.');
                    this.isSaving = false;
                }
            });
        }
    }

    cancel(): void {
        this.showForm = false;
        this.isSaving = false;
        this.errorMessage = '';
        this.loadRecommendations();
    }

    approve(id: number | undefined): void {
        if (!id) return;
        this.recommendationService.approve(id).subscribe({
            next: () => this.loadRecommendations(),
            error: (err) => {
                this.errorMessage = "Erreur lors de l'approbation.";
                console.error(err);
            }
        });
    }

    reject(rec: Recommendation): void {
        if (!rec.id) return;
        this.recommendationService.dismiss(rec.id).subscribe({
            next: () => this.loadRecommendations(),
            error: (err) => {
                this.errorMessage = 'Erreur lors du rejet.';
                console.error(err);
            }
        });
    }

    delete(id: number | undefined): void {
        if (id && confirm('Voulez-vous vraiment supprimer cette recommandation ?')) {
            this.recommendationService.delete(id).subscribe({
                next: () => {
                    this.successMessage = 'Recommandation supprimee avec succes.';
                    this.loadRecommendations();
                },
                error: (err) => {
                    this.errorMessage = 'Erreur lors de la suppression.';
                    console.error(err);
                }
            });
        }
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
                this.searchTerm = '';
                this.loadRecommendations();
                this.alertDoctorId = this.autoRecommendationForm.doctorId;
                this.loadAlerts();
                this.isGeneratingAuto = false;
            },
            error: (err) => {
                this.errorMessage = this.extractErrorMessage(err, 'Erreur lors de la generation automatique.');
                this.isGeneratingAuto = false;
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
        if (!selectedId) return;

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
            },
            error: (err) => {
                this.errorMessage = this.extractErrorMessage(err, "Erreur lors de la resolution de l'alerte.");
            }
        });
    }

    private normalizeDateForInput(value?: string | null): string | null {
        if (!value) return null;
        if (value.includes('T')) return value.split('T')[0];
        const frMatch = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(value);
        if (frMatch) {
            return `${frMatch[3]}-${frMatch[2]}-${frMatch[1]}`;
        }
        return value;
    }

    private isPastDate(value: string): boolean {
        const d = new Date(value);
        if (Number.isNaN(d.getTime())) return false;
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        d.setHours(0, 0, 0, 0);
        return d < today;
    }

    private extractErrorMessage(err: unknown, fallback: string): string {
        if (err instanceof HttpErrorResponse) {
            if (typeof err.error === 'string') return err.error;
            if (err.error?.message) return err.error.message;
            if (err.message) return err.message;
        }
        return fallback;
    }

    private upsertRecommendation(saved: Recommendation): void {
        const index = this.recommendations.findIndex((r) => r.id === saved.id);
        if (index >= 0) {
            this.recommendations[index] = saved;
            this.recommendations = [...this.recommendations];
        } else {
            this.recommendations = [saved, ...this.recommendations];
        }

        this.syncAlertDoctorIdFromRecommendations();
    }

    private syncAlertDoctorIdFromRecommendations(): void {
        if (this.recommendations.length === 0) {
            this.clinicalAlerts = [];
            return;
        }

        const availableDoctorIds = this.recommendations
            .map((recommendation) => recommendation.doctorId)
            .filter((doctorId): doctorId is number => !!doctorId && doctorId > 0);

        if (availableDoctorIds.length === 0) {
            return;
        }

        if (!this.alertDoctorId || !availableDoctorIds.includes(this.alertDoctorId)) {
            this.alertDoctorId = availableDoctorIds[0];
        }

        this.loadAlerts();
    }

    getEventDisplayName(rec: Recommendation): string | null {
        if (rec.generatedMedicalEventTitle) {
            return rec.generatedMedicalEventTitle;
        }

        if (rec.generatedMedicalEventId != null) {
            const match = this.medicalEvents.find((e) => e.id === rec.generatedMedicalEventId);
            return match?.title ?? null;
        }

        return null;
    }

    private matchesRecommendationType(eventType: string | undefined, recommendationType: string | undefined): boolean {
        if (!recommendationType) {
            return true;
        }
        return eventType === recommendationType;
    }

    formatDateTime(value?: string): string {
        if (!value) return '-';
        const date = new Date(value);
        return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
    }
}
