import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { IncidentService } from '../../core/services/incident.service';
import { AuthService } from '../../core/services/auth.service';
import { Incident } from '../../core/models/incident.model';

@Component({
    selector: 'app-patient-incidents',
    standalone: true,
    imports: [CommonModule, RouterModule, FormsModule],
    templateUrl: './patient-incidents.html',
    styleUrls: ['./patient-incidents.css']
})
export class PatientIncidentsComponent implements OnInit {

    incidents: Incident[] = [];
    filteredIncidents: Incident[] = [];
    loading = true;

    searchQuery = '';

    selectedIncident: Incident | null = null;
    isModalOpen = false;

    currentPage = 1;
    pageSize = 5;

    constructor(
        private incidentService: IncidentService,
        public authService: AuthService
    ) { }

    ngOnInit(): void {
        this.loadHistory();
    }

    get currentUser() {
        return this.authService.getCurrentUser();
    }

    loadHistory(): void {
        this.loading = true;
        const userId = this.authService.getUserId();
        const role = this.authService.getRole();

        if (!userId) {
            this.loading = false;
            this.incidents = [];
            this.applyFilters();
            return;
        }

        const request$ = role === 'CAREGIVER'
            ? this.incidentService.getIncidentsByCaregiver(userId)
            : this.incidentService.getIncidentsByPatient(userId);

        request$.subscribe({
            next: (data) => {
                this.loading = false;
                this.incidents = data;
                this.applyFilters();
            },
            error: () => {
                this.loading = false;
                this.incidents = [];
                this.applyFilters();
            }
        });
    }

    applyFilters(): void {
        const query = this.searchQuery.toLowerCase();
        this.filteredIncidents = this.incidents.filter(i =>
            i.type?.name?.toLowerCase().includes(query) ||
            i.description?.toLowerCase().includes(query)
        );
        this.currentPage = 1;
    }

    get paginatedIncidents(): Incident[] {
        const start = (this.currentPage - 1) * this.pageSize;
        return this.filteredIncidents.slice(start, start + this.pageSize);
    }

    get totalPages(): number {
        return Math.ceil(this.filteredIncidents.length / this.pageSize) || 1;
    }

    openDetailModal(incident: Incident): void {
        this.selectedIncident = incident;
        this.isModalOpen = true;
    }

    closeModal(): void {
        this.isModalOpen = false;
        this.selectedIncident = null;
    }

    getSeverityColor(severity: string | undefined): string {
        switch (severity) {
            case 'HIGH': return '#EF4444';
            case 'MEDIUM': return '#F59E0B';
            case 'LOW': return '#10B981';
            default: return '#6B7280';
        }
    }

    getStatusColor(status: string | undefined): string {
        switch (status) {
            case 'OPEN': return '#8B5CF6';
            case 'IN_PROGRESS': return '#3B82F6';
            case 'RESOLVED': return '#10B981';
            default: return '#6B7280';
        }
    }
}
