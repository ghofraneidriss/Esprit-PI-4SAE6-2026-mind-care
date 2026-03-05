import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Component({
    selector: 'app-patient-follow-up',
    standalone: false,
    templateUrl: './patient-follow-up.html',
    styleUrls: ['./patient-follow-up.css']
})
export class PatientFollowUpComponent implements OnInit {

    degradingPatients: any[] = [];
    severeWithoutFollowUpPatients: any[] = [];

    treatmentSearch: string = '';
    degradationThreshold: number = 2;
    monthsAgo: number = 3;
    isLoadingDegrading: boolean = false;
    isLoadingSevere: boolean = false;
    errorMessage: string = '';

    constructor(private http: HttpClient, private cdr: ChangeDetectorRef) { }

    ngOnInit(): void {
        console.log('Mounting Patient Follow-up...');
        this.loadAllData();
    }

    loadAllData(): void {
        this.loadDegradingPatients();
        this.loadSeverePatients();
    }

    loadDegradingPatients(): void {
        this.isLoadingDegrading = true;
        this.errorMessage = '';
        const treatment = this.treatmentSearch ? this.treatmentSearch.trim() : '';
        const url = `http://localhost:8081/api/profiles/degrading?treatment=${treatment}&threshold=${this.degradationThreshold}`;

        this.http.get<any[]>(url).subscribe({
            next: (data) => {
                console.log('Degrading data received:', data);
                this.degradingPatients = data.map(p => ({
                    ...p,
                    explanation: "Cognitive decline detected under treatment (" + (p.medications || 'N/A') + ")"
                }));
                this.isLoadingDegrading = false;
                this.cdr.detectChanges();
            },
            error: (err) => {
                console.error('Degrading load error:', err);
                this.errorMessage = 'Backend connection error (Degrading)';
                this.isLoadingDegrading = false;
                this.cdr.detectChanges();
            }
        });
    }

    loadSeverePatients(): void {
        this.isLoadingSevere = true;
        const url = `http://localhost:8081/api/profiles/severe-no-followup?months=${this.monthsAgo}`;

        this.http.get<any[]>(url).subscribe({
            next: (data) => {
                console.log('Severe data received:', data);
                this.severeWithoutFollowUpPatients = data.map(p => ({
                    ...p,
                    explanation: "Severe Stage: No follow-up appointment scheduled in the next " + this.monthsAgo + " months."
                }));
                this.isLoadingSevere = false;
                this.cdr.detectChanges();
            },
            error: (err) => {
                console.error('Severe load error:', err);
                this.errorMessage = 'Backend connection error (Severe)';
                this.isLoadingSevere = false;
                this.cdr.detectChanges();
            }
        });
    }

    sendAlert(patient: any): void {
        // Mock sending alert
        alert(`Alert sent to patient/caregiver for Patient ID: ${patient.userId || patient.id}.\nMessage: An appointment is highly recommended based on doctor availability.`);
    }

}
