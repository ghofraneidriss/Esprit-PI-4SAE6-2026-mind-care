import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { IncidentService } from '../../core/services/incident.service';
import { AuthService } from '../../core/services/auth.service';
import { Incident } from '../../core/models/incident.model';

@Component({
    selector: 'app-patient-incidents-history',
    standalone: true,
    imports: [CommonModule, RouterModule],
    templateUrl: './patient-incidents-history.html',
    styleUrls: ['./patient-incidents-history.css']
})
export class PatientIncidentsHistoryComponent implements OnInit {

    incidents: Incident[] = [];
    loading = true;

    constructor(
        private incidentService: IncidentService,
        public authService: AuthService
    ) { }

    get canReportIncident(): boolean {
        const r = this.authService.getRole();
        return r === 'CAREGIVER' || r === 'VOLUNTEER';
    }

    ngOnInit(): void {
        this.incidentService.getPatientIncidentsHistory(1).subscribe({
            next: (data) => {
                this.incidents = data;
                this.loading = false;
            },
            error: () => this.loading = false
        });
    }
}
