import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AuthService } from '../auth/auth.service';

@Component({
    selector: 'app-patient-reports',
    standalone: false,
    templateUrl: './reports.html',
    styleUrls: ['./reports.css']
})
export class PatientReportsPage implements OnInit {
    reports: any[] = [];
    isLoading = true;
    error = '';
    patientName = '';

    constructor(
        private http: HttpClient,
        private authService: AuthService
    ) { }

    ngOnInit(): void {
        console.log('PatientReportsPage initializing...');
        const user = this.authService.getLoggedUser();
        console.log('Logged user context:', user);

        if (user && user.userId) {
            const firstName = user.firstName ? user.firstName.charAt(0).toUpperCase() + user.firstName.slice(1).toLowerCase() : '';
            const lastName = user.lastName ? user.lastName.charAt(0).toUpperCase() + user.lastName.slice(1).toLowerCase() : '';
            this.patientName = `${firstName} ${lastName}`.trim();
            console.log(`User confirmed: ${this.patientName} (ID: ${user.userId})`);
            this.loadReports(user.userId);
        } else if (user && !user.userId) {
            console.error('User found but userId is missing!', user);
            this.isLoading = false;
            this.error = 'Profile integrity error. Please logout and login again.';
        } else {
            console.warn('No logged user found. Redirecting to error state.');
            this.isLoading = false;
            this.error = 'Please login to view your reports.';
        }
    }

    loadReports(patientId: number): void {
        console.log(`Fetching reports for patient ${patientId}...`);
        this.http.get<any[]>(`http://localhost:8083/api/medical-reports/patient/${patientId}`)
            .subscribe({
                next: (data) => {
                    console.log('Reports received:', data);
                    this.reports = data;
                    this.isLoading = false;
                },
                error: (err) => {
                    console.error('Failed to load reports:', err);
                    this.error = 'Failed to load reports. Please try again later.';
                    this.isLoading = false;
                }
            });
    }

    downloadReport(url: string): void {
        if (url) {
            window.open(url, '_blank');
        }
    }
}
