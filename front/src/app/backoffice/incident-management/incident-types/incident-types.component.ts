import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { IncidentService } from '../../../core/services/incident.service';
import { IncidentType } from '../../../core/models/incident.model';

@Component({
    selector: 'app-incident-types',
    standalone: true,
    imports: [CommonModule, RouterModule, FormsModule, ReactiveFormsModule],
    templateUrl: './incident-types.component.html',
    styleUrls: ['./incident-types.component.css']
})
export class IncidentTypesComponent implements OnInit {

    incidentTypes: IncidentType[] = [];

    // Modal State
    isModalOpen = false;
    isDeleteModalOpen = false;
    isEditMode = false;
    selectedTypeForDelete: number | null = null;
    currentTypeId: number | undefined;
    loading = false;
    error: string | null = null;

    // Form
    typeForm: FormGroup;

    constructor(
        private incidentService: IncidentService,
        private fb: FormBuilder
    ) {
        this.typeForm = this.fb.group({
            id: [null],
            name: ['', Validators.required],
            description: [''],
            defaultSeverity: ['MEDIUM', Validators.required],
            points: [10, [Validators.required, Validators.min(1), Validators.max(50)]]
        });
    }

    ngOnInit(): void {
        this.loadTypes();
    }

    loadTypes(): void {
        this.incidentService.getAllIncidentTypes().subscribe(data => {
            this.incidentTypes = data;
        });
    }

    // --- MODAL ---

    openAddModal(): void {
        this.isEditMode = false;
        this.currentTypeId = undefined;
        this.typeForm.reset();
        this.isModalOpen = true;
    }

    openEditModal(type: IncidentType): void {
        this.isEditMode = true;
        this.currentTypeId = type.id;
        this.typeForm.patchValue({
            id: type.id,
            name: type.name,
            description: type.description,
            defaultSeverity: type.defaultSeverity || 'MEDIUM',
            points: type.points || 10
        });
        this.isModalOpen = true;
    }

    closeModal(): void {
        this.isModalOpen = false;
    }

    saveType(): void {
        console.log('[Incident Types] Attempting to save:', this.typeForm.value);
        if (this.typeForm.invalid) {
            console.warn('[Incident Types] Form is invalid');
            return;
        }

        this.loading = true;
        this.error = null;

        const timer = setTimeout(() => {
            if (this.loading) {
                this.loading = false;
                this.error = 'Connection timeout. Check service on 8087.';
            }
        }, 10000);

        const formValue = this.typeForm.value;
        const request = (this.isEditMode && this.currentTypeId)
            ? this.incidentService.updateIncidentType(this.currentTypeId, formValue)
            : this.incidentService.createIncidentType(formValue);

        request.subscribe({
            next: () => {
                clearTimeout(timer);
                console.log('[Incident Types] Save successful');
                this.loadTypes();
                this.closeModal();
                this.loading = false;
            },
            error: (err) => {
                clearTimeout(timer);
                console.error('[Incident Types] Save error:', err);
                this.error = 'Service unreachable (Port 8087).';
                this.loading = false;
            }
        });
    }

    // --- DELETE ---

    confirmDelete(id: number): void {
        this.selectedTypeForDelete = id;
        this.isDeleteModalOpen = true;
    }

    deleteType(): void {
        if (this.selectedTypeForDelete) {
            this.incidentService.deleteIncidentType(this.selectedTypeForDelete).subscribe(() => {
                this.loadTypes();
                this.isDeleteModalOpen = false;
                this.selectedTypeForDelete = null;
            });
        }
    }
}
