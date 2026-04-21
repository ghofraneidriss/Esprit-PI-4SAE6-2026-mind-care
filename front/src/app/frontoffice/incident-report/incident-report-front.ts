import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { IncidentService } from '../../core/services/incident.service';
import { AuthService } from '../../core/services/auth.service';
import { IncidentType, IncidentStatus } from '../../core/models/incident.model';
import { User } from '../../core/models/user.model';

@Component({
    selector: 'app-incident-report-front',
    standalone: true,
    imports: [CommonModule, RouterModule, FormsModule, ReactiveFormsModule],
    templateUrl: './incident-report-front.html',
    styleUrls: ['./incident-report-front.css']
})
export class IncidentReportFrontPage implements OnInit {

    incidentForm!: FormGroup;
    incidentTypes: IncidentType[] = [];
    severityLevels = ['LOW', 'MEDIUM', 'HIGH'];

    /** True after user tried to submit; drives validation messages only. */
    attemptedSubmit = false;
    /** True while HTTP create is in flight. */
    isSubmitting = false;
    successMessage = '';
    errorMessage = '';
    warningMessage = '';
    loadingTypes = false;

    // Caregiver: list of assigned patients
    assignedPatients: User[] = [];
    selectedPatientId: number | null = null;
    loadingPatients = false;

    constructor(
        private fb: FormBuilder,
        private incidentService: IncidentService,
        public authService: AuthService,
        private router: Router,
        private cdr: ChangeDetectorRef
    ) { }

    ngOnInit(): void {
        this.incidentForm = this.fb.group({
            type: ['', Validators.required],
            description: ['', [Validators.required, Validators.minLength(10)]],
            severityLevel: ['MEDIUM', Validators.required],
            incidentDate: [new Date().toISOString()]
        });

        this.loadIncidentTypes();

        const role = this.authService.getRole();
        const uid = this.authService.getUserId();
        if (role === 'CAREGIVER' && uid) {
            this.loadAssignedPatientsForCaregiver(uid);
        } else if (role === 'VOLUNTEER' && uid) {
            this.loadAssignedPatientsForVolunteer(uid);
        }
    }

    get currentUser() {
        return this.authService.getCurrentUser();
    }

    /** Same initials logic as navbar profile button */
    get reportingInitials(): string {
        const u = this.currentUser;
        if (!u) return '?';
        const a = (u.firstName || '?').charAt(0);
        const b = (u.lastName || '').charAt(0);
        return (a + b).toUpperCase();
    }

    get isCaregiver(): boolean {
        return this.authService.getRole() === 'CAREGIVER';
    }

    get isVolunteer(): boolean {
        return this.authService.getRole() === 'VOLUNTEER';
    }

    /** Aidant ou bénévole : sélection du patient pour le signalement */
    get isCaregiverOrVolunteer(): boolean {
        return this.isCaregiver || this.isVolunteer;
    }

    loadIncidentTypes(): void {
        this.loadingTypes = true;
        this.warningMessage = '';
        this.incidentService.getAllIncidentTypes().pipe(
            finalize(() => {
                this.loadingTypes = false;
                this.cdr.detectChanges();
            })
        ).subscribe({
            next: (types) => {
                this.incidentTypes = types;
                if (types.length === 0) {
                    this.warningMessage = 'No incident types in the database yet. Add them in the admin backoffice or reset the incident_db.';
                }
                this.cdr.detectChanges();
            },
            error: () => {
                this.incidentTypes = [];
                this.warningMessage = 'Could not load incident types. Ensure incident-service is running (port 8083) and MySQL (incident_db) is up.';
                this.cdr.detectChanges();
            }
        });
    }

    private loadAssignedPatientsForCaregiver(caregiverId: number): void {
        this.loadingPatients = true;
        this.authService.getPatientsByCaregiver(caregiverId).subscribe({
            next: (patients) => {
                this.assignedPatients = patients;
                if (patients.length > 0) {
                    this.selectedPatientId = patients[0].userId;
                }
                this.loadingPatients = false;
                this.cdr.detectChanges();
            },
            error: () => {
                this.assignedPatients = [];
                this.warningMessage = 'Could not load assigned patients right now.';
                this.loadingPatients = false;
                this.cdr.detectChanges();
            }
        });
    }

    private loadAssignedPatientsForVolunteer(volunteerId: number): void {
        this.loadingPatients = true;
        this.authService.getPatientsByVolunteer(volunteerId).subscribe({
            next: (patients) => {
                this.assignedPatients = patients;
                if (patients.length > 0) {
                    this.selectedPatientId = patients[0].userId;
                }
                this.loadingPatients = false;
                this.cdr.detectChanges();
            },
            error: () => {
                this.assignedPatients = [];
                this.warningMessage = 'Could not load assigned patients right now.';
                this.loadingPatients = false;
                this.cdr.detectChanges();
            }
        });
    }

    onSubmit(): void {
        this.attemptedSubmit = true;

        if (this.incidentForm.invalid) {
            return;
        }

        const userId = this.authService.getUserId();
        if (!userId) {
            this.errorMessage = 'You must be signed in to report an incident.';
            return;
        }

        const role = this.authService.getRole();

        if ((role === 'CAREGIVER' || role === 'VOLUNTEER') && !this.selectedPatientId) {
            this.errorMessage = 'Please select a patient.';
            return;
        }

        this.isSubmitting = true;
        this.errorMessage = '';

        const formValue = this.incidentForm.value;

        const incident = {
            type: { id: Number(formValue.type) },
            description: formValue.description,
            severityLevel: formValue.severityLevel,
            incidentDate: formValue.incidentDate || new Date().toISOString(),
            status: IncidentStatus.OPEN,
            source: role,
            patientId: role === 'CAREGIVER' || role === 'VOLUNTEER' ? this.selectedPatientId : userId,
            caregiverId: role === 'CAREGIVER' ? userId : null,
            volunteerId: role === 'VOLUNTEER' ? userId : null
        };

        this.incidentService.createIncident(incident).subscribe({
            next: () => {
                this.successMessage = 'Incident reported successfully.';
                this.incidentForm.reset({
                    type: '',
                    description: '',
                    severityLevel: 'MEDIUM',
                    incidentDate: new Date().toISOString()
                });
                this.attemptedSubmit = false;
                this.isSubmitting = false;
                setTimeout(() => this.router.navigate(['/incidents/history']), 2000);
            },
            error: (err: any) => {
                console.error('Error creating incident:', err);
                this.errorMessage = 'Could not submit the report. Please try again.';
                this.isSubmitting = false;
            }
        });
    }
}
