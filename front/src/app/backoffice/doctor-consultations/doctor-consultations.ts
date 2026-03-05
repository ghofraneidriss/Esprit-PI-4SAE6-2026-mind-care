import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Consultation, AlzheimerStage } from './consultation.model';
import { ConsultationService } from './consultation.service';
import { Router } from '@angular/router';
import { AppointmentService, Appointment } from '../../frontoffice/appointment/appointment.service';
import { AuthService } from '../../frontoffice/auth/auth.service';

@Component({
    selector: 'app-doctor-consultations',
    standalone: false,
    templateUrl: './doctor-consultations.html',
    styleUrls: ['./doctor-consultations.css']
})
export class DoctorConsultations implements OnInit {
    consultations: Consultation[] = [];
    filteredConsultations: Consultation[] = [];
    searchTerm: string = '';
    /** Active stage filter for the tabs (all | PRECLINICAL | MILD | MODERATE | SEVERE) */
    stageFilter: string = 'all';

    isModalOpen: boolean = false;
    modalMode: 'add' | 'edit' = 'add';
    currentConsultationId?: number;

    /** Error message shown if the API load fails */
    loadError: string = '';
    /** Error message shown inside the modal if save/update fails */
    saveError: string = '';
    /** Indicates a loading state */
    isLoading: boolean = false;

    consultationForm: FormGroup;
    alzheimerStages = Object.values(AlzheimerStage);
    appointments: Appointment[] = [];

    /**
     * Appointments available for selection in the form.
     * In ADD mode  : only appointments not already linked to a consultation.
     * In EDIT mode : same filtered list + the currently selected appointment (to keep it selectable).
     */
    get availableAppointments(): Appointment[] {
        // Collect all appointmentIds already used by existing consultations
        const usedIds = new Set(
            this.consultations
                .filter(c => this.modalMode === 'edit' && c.id === this.currentConsultationId ? false : true)
                .map(c => c.appointmentId)
        );
        return this.appointments.filter(a => !usedIds.has(a.id!));
    }

    constructor(
        private fb: FormBuilder,
        private consultationService: ConsultationService,
        private router: Router,
        private appointmentService: AppointmentService,
        private authService: AuthService,
        private cdr: ChangeDetectorRef   // Nécessaire pour forcer la mise à jour de la vue
    ) {
        this.consultationForm = this.fb.group({
            appointmentId: ['', [Validators.required]],
            clinicalNotes: ['', [Validators.required]],
            currentWeight: ['', [Validators.required, Validators.min(0)]],
            bloodPressure: ['', [Validators.required]],
            mmseScore: ['', [Validators.required, Validators.min(0), Validators.max(30)]],
            alzheimerStage: [AlzheimerStage.PRECLINICAL, [Validators.required]]
        });
    }

    ngOnInit(): void {
        this.loadConsultations();
        this.loadAppointments();
        if (this.router.url.endsWith('/new')) {
            this.openAddModal();
        }
    }

    /**
     * Charge les rendez-vous selon le rôle de l'utilisateur connecté.
     * - ADMIN : tous les rendez-vous
     * - DOCTOR : uniquement ses propres rendez-vous
     */
    loadAppointments(): void {
        const user = this.authService.getLoggedUser();
        const role = this.authService.getLoggedRole();
        if (!user) return;

        if (role === 'ADMIN') {
            this.appointmentService.getAppointments().subscribe({
                next: (data) => {
                    this.appointments = data;
                    this.cdr.detectChanges();
                },
                error: (err) => console.error('[Consultations] Error loading appointments:', err)
            });
        } else {
            this.appointmentService.getAppointmentsByDoctor(user.userId).subscribe({
                next: (data) => {
                    this.appointments = data;
                    this.cdr.detectChanges();
                },
                error: (err) => console.error('[Consultations] Error loading doctor appointments:', err)
            });
        }
    }

    /**
     * Charge toutes les consultations depuis le back-end.
     * Met à jour la liste filtrée et force la détection de changement Angular.
     */
    loadConsultations(): void {
        this.isLoading = true;
        this.loadError = '';

        // All Consultations for form matching
        this.consultationService.getAll().subscribe({
            next: (data) => {
                this.consultations = data;
                this.applyFilter();
            },
            error: (err) => {
                this.loadError = `Failed to load consultations (${err.status || 'network error'}). Please make sure the server is running.`;
                this.isLoading = false;
                this.cdr.detectChanges();
            }
        });
    }

    /**
     * COMMENTAIRE POUR LE REPERAGE (Demande utilisateur) :
     * Applique le filtre en appelant le backend Spring Boot.
     * Le front ne gère plus la recherche/filtrage lui-même, il affiche juste le résultat reçu du Backend.
     */
    applyFilter(): void {
        this.isLoading = true;
        this.consultationService.getFiltered(this.stageFilter, this.searchTerm).subscribe({
            next: (filteredData) => {
                this.filteredConsultations = filteredData;
                this.isLoading = false;
                this.cdr.detectChanges();
            },
            error: (err) => {
                console.error('[Consultations] Error filtering through backend :', err);
                this.isLoading = false;
                this.cdr.detectChanges();
            }
        });
    }

    /**
     * Change le filtre actif par stade Alzheimer et relance l'affichage.
     * @param stage stade sélectionné dans les onglets
     */
    setStageFilter(stage: string): void {
        this.stageFilter = stage;
        this.applyFilter();
    }

    /**
     * Compte le nombre de consultations pour un stade Alzheimer donné.
     * Utilisé pour afficher les chiffres dans les cartes statistiques.
     * @param stage stade Alzheimer à compter
     */
    countByStage(stage: string): number {
        return this.consultations.filter(c => c.alzheimerStage === stage).length;
    }

    /**
     * Retourne la classe CSS correspondant au stade Alzheimer pour les pills du tableau.
     * @param stage stade Alzheimer de la consultation
     */
    getStagePillClass(stage: AlzheimerStage): string {
        switch (stage) {
            case AlzheimerStage.PRECLINICAL: return 'stage-preclinical';
            case AlzheimerStage.MILD: return 'stage-mild';
            case AlzheimerStage.MODERATE: return 'stage-moderate';
            case AlzheimerStage.SEVERE: return 'stage-severe';
            default: return '';
        }
    }

    /** Opens the modal in CREATE mode, resetting the form. */
    openAddModal(): void {
        this.modalMode = 'add';
        this.currentConsultationId = undefined;
        this.saveError = '';
        this.consultationForm.reset({
            alzheimerStage: AlzheimerStage.PRECLINICAL
        });
        this.isModalOpen = true;
    }

    /** Opens the modal in EDIT mode, pre-filling the form with the selected consultation's data. */
    openEditModal(consultation: Consultation): void {
        this.modalMode = 'edit';
        this.currentConsultationId = consultation.id;
        this.saveError = '';
        this.consultationForm.patchValue({
            appointmentId: consultation.appointmentId,
            clinicalNotes: consultation.clinicalNotes,
            currentWeight: consultation.currentWeight,
            bloodPressure: consultation.bloodPressure,
            mmseScore: consultation.mmseScore,
            alzheimerStage: consultation.alzheimerStage
        });
        this.isModalOpen = true;
    }

    /** Closes the modal without saving. */
    closeModal(): void {
        this.isModalOpen = false;
        this.saveError = '';
    }

    /** Saves the consultation (create or update depending on current mode). */
    saveConsultation(): void {
        if (this.consultationForm.invalid) {
            this.consultationForm.markAllAsTouched();
            return;
        }

        this.saveError = '';
        const consultationData: Consultation = {
            ...this.consultationForm.value,
            appointmentId: Number(this.consultationForm.value.appointmentId)
        };

        if (this.modalMode === 'add') {
            this.consultationService.create(consultationData).subscribe({
                next: (res) => {
                    console.log('[Consultations] Consultation created:', res);
                    this.loadConsultations();
                    this.closeModal();
                    if (this.router.url.endsWith('/new')) {
                        this.router.navigate(['/admin/consultations']);
                    }
                },
                error: (err) => {
                    console.error('[Consultations] Create error:', err);
                    // HTTP 400 = backend uniqueness rule violated
                    if (err.status === 400) {
                        this.saveError = err.error || 'This appointment is already linked to a consultation. Each appointment can only be used once.';
                    } else {
                        this.saveError = 'Error creating consultation. Status: ' + err.status;
                    }
                    this.cdr.detectChanges();
                }
            });
        } else if (this.currentConsultationId) {
            this.consultationService.update(this.currentConsultationId, consultationData).subscribe({
                next: () => {
                    console.log('[Consultations] Consultation updated.');
                    this.loadConsultations();
                    this.closeModal();
                },
                error: (err) => {
                    console.error('[Consultations] Update error:', err);
                    if (err.status === 400) {
                        this.saveError = err.error || 'This appointment is already linked to a consultation. Each appointment can only be used once.';
                    } else {
                        this.saveError = 'Error updating consultation. Status: ' + err.status;
                    }
                    this.cdr.detectChanges();
                }
            });
        }
    }

    /** Displays form validation errors in the console (debugging tool) */
    logFormErrors(): void {
        Object.keys(this.consultationForm.controls).forEach(key => {
            const controlErrors = this.consultationForm.get(key)?.errors;
            if (controlErrors != null) {
                console.log('Field: ' + key + ', Errors: ', controlErrors);
            }
        });
    }

    /** Supprime une consultation après confirmation de l'utilisateur */
    deleteConsultation(id: number): void {
        if (confirm('Are you sure you want to delete this consultation?')) {
            this.consultationService.delete(id).subscribe({
                next: () => {
                    this.loadConsultations();
                },
                error: (err) => console.error('[Consultations] Delete error:', err)
            });
        }
    }

    /** Retourne la classe CSS Bootstrap correspondant au stade Alzheimer */
    getStageBadgeClass(stage: AlzheimerStage): string {
        switch (stage) {
            case AlzheimerStage.PRECLINICAL: return 'badge-info';
            case AlzheimerStage.MILD: return 'badge-success';
            case AlzheimerStage.MODERATE: return 'badge-warning';
            case AlzheimerStage.SEVERE: return 'badge-danger';
            default: return 'badge-secondary';
        }
    }

    /** Helper to get appointment date */
    getAppointmentDate(appointmentId: number): string | Date | undefined {
        const apt = this.appointments.find(a => a.id === appointmentId);
        return apt ? apt.appointmentDate : undefined;
    }
    /** Helper to get meet link for online appointments */
    getMeetLink(appointmentId: number): string | undefined {
        const apt = this.appointments.find(a => a.id === appointmentId);
        return apt ? apt.meetLink : undefined;
    }
}
