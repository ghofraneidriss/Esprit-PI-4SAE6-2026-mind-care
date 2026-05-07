import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { UserRole, PatientRegistrationOption } from '../../../core/models/user.model';

@Component({
  selector: 'app-auth-register-cover',
  standalone: false,
  templateUrl: './register-cover.html',
  styleUrls: ['./register-cover.css'],
})
export class RegisterCoverAuthPage {
  firstName = '';
  lastName = '';
  email = '';
  password = '';
  role: UserRole = 'PATIENT';
  errorMessage = '';
  successMessage = '';
  loading = false;

  patientsForLink: PatientRegistrationOption[] = [];
  loadingPatients = false;
  assignedPatientId: number | null = null;

  roles: { value: UserRole; label: string }[] = [
    { value: 'ADMIN', label: 'Admin' },
    { value: 'PATIENT', label: 'Patient' },
    { value: 'CAREGIVER', label: 'Caregiver' },
    { value: 'VOLUNTEER', label: 'Volunteer' },
    { value: 'DOCTOR', label: 'Doctor' },
  ];

  constructor(private authService: AuthService, private router: Router) {
    if (this.authService.isLoggedIn()) {
      this.router.navigateByUrl(this.authService.getLandingRouteForRole(this.authService.getCurrentUser()?.role));
    }
  }

  onRoleChange(): void {
    this.assignedPatientId = null;
    if (this.role === 'CAREGIVER' || this.role === 'VOLUNTEER') {
      this.loadPatientsForLink();
    } else {
      this.patientsForLink = [];
    }
  }

  private loadPatientsForLink(): void {
    this.loadingPatients = true;
    this.authService.getPatientsForRegistration().subscribe({
      next: (list) => {
        this.patientsForLink = list || [];
        if (this.assignedPatientId == null && this.patientsForLink.length > 0) {
          this.assignedPatientId = this.patientsForLink[0].userId;
        }
        this.loadingPatients = false;
      },
      error: () => {
        this.patientsForLink = [];
        this.loadingPatients = false;
        this.errorMessage = 'Could not load the patient list. Try again later.';
      },
    });
  }

  onRegister(): void {
    if (!this.firstName || !this.lastName || !this.email || !this.password) {
      this.errorMessage = 'Please fill in all required fields.';
      return;
    }
    if ((this.role === 'CAREGIVER' || this.role === 'VOLUNTEER') && this.assignedPatientId == null) {
      this.errorMessage = 'Select a patient to link to your account.';
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.authService.register({
      firstName: this.firstName,
      lastName: this.lastName,
      email: this.email,
      password: this.password,
      role: this.role,
      assignedPatientId:
        this.role === 'CAREGIVER' || this.role === 'VOLUNTEER' ? this.assignedPatientId : undefined,
    }).subscribe({
      next: (user) => {
        this.loading = false;
        this.authService.setCurrentUser(user);
        this.successMessage = 'Account created. Redirecting to your space...';
        setTimeout(() => this.router.navigateByUrl(this.authService.getLandingRouteForRole(user.role)), 500);
      },
      error: (err: unknown) => {
        this.loading = false;
        const e = err as { status?: number; error?: { message?: string } };
        const message = String(e.error?.message ?? '').trim();
        if (message) {
          this.errorMessage = message;
        } else if (e.status === 409) {
          this.errorMessage = 'Email already in use.';
        } else {
          this.errorMessage = 'Registration failed. Email already in use?';
        }
      }
    });
  }
}
