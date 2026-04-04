import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { IncidentService } from '../core/services/incident.service';
import { AuthService } from '../core/services/auth.service';
import { IncidentType, IncidentStatus } from '../core/models/incident.model';
import { User } from '../core/models/user.model';

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

    submitted = false;
    successMessage = '';
    errorMessage = '';

    // Caregiver: list of assigned patients
    assignedPatients: User[] = [];
    selectedPatientId: number | null = null;
    loadingPatients = false;

    constructor(
        private fb: FormBuilder,
        private incidentService: IncidentService,
        public authService: AuthService,
        private router: Router
    ) { }

    ngOnInit(): void {
        this.incidentForm = this.fb.group({
            type: ['', Validators.required],
            description: ['', [Validators.required, Validators.minLength(10)]],
            severityLevel: ['MEDIUM', Validators.required],
            incidentDate: [new Date().toISOString()]
        });

        this.loadIncidentTypes();

        // If CAREGIVER, load their assigned patients
        if (this.authService.getRole() === 'CAREGIVER') {
            const caregiverId = this.authService.getUserId();
            if (caregiverId) {
                this.loadAssignedPatients(caregiverId);
            }
        }
    }

    get currentUser() {
        return this.authService.getCurrentUser();
    }

    get isCaregiver(): boolean {
        return this.authService.getRole() === 'CAREGIVER';
    }

    loadIncidentTypes(): void {
        this.incidentService.getAllIncidentTypes().subscribe({
            next: (types) => { this.incidentTypes = types; },
            error: (err: any) => { console.error('Error loading incident types:', err); }
        });
    }

    loadAssignedPatients(caregiverId: number): void {
        this.loadingPatients = true;
        this.authService.getPatientsByCaregiver(caregiverId).subscribe({
            next: (patients) => {
                this.assignedPatients = patients;
                if (patients.length > 0) {
                    this.selectedPatientId = patients[0].userId;
                }
                this.loadingPatients = false;
            },
            error: (err: any) => {
                console.error('Error loading assigned patients:', err);
                this.loadingPatients = false;
            }
        });
    }

    onSubmit(): void {
        if (this.incidentForm.invalid) {
            this.submitted = true;
            return;
        }

        const userId = this.authService.getUserId();
        if (!userId) {
            this.errorMessage = 'Vous devez être connecté pour signaler un incident.';
            return;
        }

        const role = this.authService.getRole();

        // CAREGIVER must select a patient
        if (role === 'CAREGIVER' && !this.selectedPatientId) {
            this.errorMessage = 'Veuillez sélectionner un patient.';
            return;
        }

        this.submitted = true;
        this.errorMessage = '';

        const formValue = this.incidentForm.value;

        const incident = {
            type: { id: Number(formValue.type) },
            description: formValue.description,
            severityLevel: formValue.severityLevel,
            incidentDate: formValue.incidentDate || new Date().toISOString(),
            status: IncidentStatus.OPEN,
            source: role,
            patientId: role === 'PATIENT' ? userId : (role === 'CAREGIVER' ? this.selectedPatientId : null),
            caregiverId: role === 'CAREGIVER' ? userId : null
        };

        this.incidentService.createIncident(incident).subscribe({
            next: () => {
                this.successMessage = 'Incident signalé avec succès !';
                this.incidentForm.reset();
                this.submitted = false;
                setTimeout(() => this.router.navigate(['/incidents/history']), 2000);
            },
            error: (err: any) => {
                console.error('Error creating incident:', err);
                this.errorMessage = 'Erreur lors du signalement. Veuillez réessayer.';
                this.submitted = false;
            }
        });
    }
}
