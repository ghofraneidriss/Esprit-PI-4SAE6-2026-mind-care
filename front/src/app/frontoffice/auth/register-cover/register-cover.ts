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
    { value: 'PATIENT', label: 'Patient' },
    { value: 'CAREGIVER', label: 'Caregiver' },
    { value: 'DOCTOR', label: 'Doctor' },
    { value: 'VOLUNTEER', label: 'Volunteer' },
  ];

  constructor(private authService: AuthService, private router: Router) {}

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
      next: () => {
        this.loading = false;
        this.successMessage = 'Account created. Redirecting…';
        setTimeout(() => this.router.navigate(['/login-cover']), 800);
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Registration failed. Email already in use?';
      }
    });
  }
}
