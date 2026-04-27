import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, ActivatedRoute } from '@angular/router';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { IncidentService } from '../../../core/services/incident.service';
import { Incident, IncidentType, SeverityLevel, IncidentStatus } from '../../../core/models/incident.model';
import { Subscription } from 'rxjs';

@Component({
    selector: 'app-incident-list',
    standalone: true,
    imports: [CommonModule, RouterModule, FormsModule, ReactiveFormsModule],
    templateUrl: './incident-list.component.html',
    styleUrls: ['./incident-list.component.css']
})
export class IncidentListComponent implements OnInit, OnDestroy {

    incidents: Incident[] = [];
    incidentTypes: IncidentType[] = [];
    isHistoryMode = false;
    showCaregiverReports = false;
    private refreshSub?: Subscription;

    // Modal State
    isModalOpen = false;
    isDeleteModalOpen = false;
    isEditMode = false;
    selectedIncidentForDelete: number | null = null;
    currentIncidentId: number | null = null;

    // Form
    incidentForm: FormGroup;

    // Enums for Template
    severityLevels = Object.values(SeverityLevel);
    statuses = Object.values(IncidentStatus);

    constructor(
        private incidentService: IncidentService,
        private fb: FormBuilder,
        private route: ActivatedRoute
    ) {
        this.incidentForm = this.fb.group({
            id: [null],
            type: [null, Validators.required],
            description: ['', Validators.required],
            severityLevel: [SeverityLevel.LOW, Validators.required],
            status: [IncidentStatus.OPEN, Validators.required],
            patientId: [1, Validators.required],
            incidentDate: [new Date(), Validators.required]
        });
    }

    ngOnInit(): void {
        this.route.data.subscribe(data => {
            this.isHistoryMode = data['mode'] === 'history';
            this.loadIncidents();
        });

        this.loadTypes();

        this.refreshSub = this.incidentService.refresh$.subscribe(() => {
            this.loadIncidents();
        });
    }

    ngOnDestroy(): void {
        this.refreshSub?.unsubscribe();
    }

    loadIncidents(): void {
        const obs = this.isHistoryMode
            ? this.incidentService.getIncidentHistory()
            : this.incidentService.getAllActiveIncidents();

        obs.subscribe(data => {
            if (this.showCaregiverReports) {
                // Filter incidents where source is CAREGIVER (or simulated matching)
                this.incidents = data.filter(i => (i as any).source === 'CAREGIVER' || i.description?.toLowerCase().includes('caregiver'));
            } else {
                this.incidents = data;
            }
        });
    }

    toggleCaregiverReports(): void {
        this.showCaregiverReports = !this.showCaregiverReports;
        this.loadIncidents();
    }

    loadTypes(): void {
        this.incidentService.getAllIncidentTypes().subscribe(data => {
            this.incidentTypes = data;
        });
    }

    // --- MODAL ---

    openAddModal(): void {
        this.isEditMode = false;
        this.currentIncidentId = null;
        this.incidentForm.reset({
            severityLevel: SeverityLevel.LOW,
            status: IncidentStatus.OPEN,
            incidentDate: new Date(),
            patientId: 1
        });
        this.isModalOpen = true;
    }

    openEditModal(incident: Incident): void {
        this.isEditMode = true;
        this.currentIncidentId = incident.id ? incident.id : null;

        this.incidentForm.patchValue({
            id: incident.id,
            type: incident.type,
            description: incident.description,
            severityLevel: incident.severityLevel,
            status: incident.status,
            patientId: incident.patientId,
            incidentDate: incident.incidentDate
        });
        this.isModalOpen = true;
    }

    closeModal(): void {
        this.isModalOpen = false;
    }

    saveIncident(): void {
        if (this.incidentForm.invalid) return;

        const formValue = this.incidentForm.value;

        if (this.isEditMode && this.currentIncidentId) {
            this.incidentService.updateIncident(this.currentIncidentId, formValue).subscribe(() => {
                // Refresh handled by Subscription
                this.closeModal();
            });
        } else {
            this.incidentService.createIncident(formValue).subscribe(() => {
                // Refresh handled by Subscription
                this.closeModal();
            });
        }
    }

    // --- DELETE ---

    confirmDelete(id: number): void {
        this.selectedIncidentForDelete = id;
        this.isDeleteModalOpen = true;
    }

    deleteIncident(): void {
        if (this.selectedIncidentForDelete) {
            this.incidentService.deleteIncident(this.selectedIncidentForDelete).subscribe(() => {
                // Refresh handled by Subscription
                this.isDeleteModalOpen = false;
                this.selectedIncidentForDelete = null;
            });
        }
    }
}
