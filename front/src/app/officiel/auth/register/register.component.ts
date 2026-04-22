import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import type { UserRole } from '../../../core/models/user.model';
import type { PatientRegistrationOption } from '../../../core/models/user.model';

@Component({
  selector: 'app-off-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrls: ['../login/login.component.css'],
})
export class OfficielRegisterComponent {
  firstname = '';
  lastname = '';
  email = '';
  password = '';
  confirm = '';
  phone = '';
  role: UserRole = 'PATIENT';
  error = '';
  success = '';
  loading = false;
  showPwd = false;
  /** Existing patients to link a caregiver or volunteer. */
  patientsForLink: PatientRegistrationOption[] = [];
  loadingPatients = false;
  assignedPatientId: number | null = null;
  roles: { v: UserRole; l: string; d: string }[] = [
    { v: 'PATIENT', l: '🧑 Patient', d: 'Cognitive follow-up' },
    { v: 'CAREGIVER', l: '🤝 Caregiver', d: 'Family / support' },
    { v: 'DOCTOR', l: '🩺 Doctor', d: 'Medical follow-up' },
    { v: 'VOLUNTEER', l: '🙋 Volunteer', d: 'Community help' },
  ];

  constructor(
    private auth: AuthService,
    private router: Router
  ) {
    if (this.auth.isLoggedIn()) {
      this.router.navigate(['/officiel/dashboard']);
    }
  }

  onRoleChange(v: UserRole): void {
    this.role = v;
    this.assignedPatientId = null;
    if (v === 'CAREGIVER' || v === 'VOLUNTEER') {
      this.loadPatientsForLink();
    } else {
      this.patientsForLink = [];
    }
  }

  private loadPatientsForLink(): void {
    this.loadingPatients = true;
    this.auth.getPatientsForRegistration().subscribe({
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
        this.error = 'Could not load the patient list. Please try again later.';
      },
    });
  }

  submit(): void {
    this.error = '';
    this.success = '';
    if (!this.firstname || !this.lastname || !this.email || !this.password) {
      this.error = 'Please fill in all required fields.';
      return;
    }
    if (this.password.length < 4) {
      this.error = 'Password is too short (min 4 characters).';
      return;
    }
    if (this.password !== this.confirm) {
      this.error = 'Passwords do not match.';
      return;
    }
    if ((this.role === 'CAREGIVER' || this.role === 'VOLUNTEER') && this.assignedPatientId == null) {
      this.error = 'Please select a patient to link to your account.';
      return;
    }
    this.loading = true;
    this.auth
      .register({
        firstName: this.firstname,
        lastName: this.lastname,
        email: this.email,
        password: this.password,
        phone: this.phone || undefined,
        role: this.role,
        assignedPatientId:
          this.role === 'CAREGIVER' || this.role === 'VOLUNTEER' ? this.assignedPatientId : undefined,
      })
      .subscribe({
        next: () => {
          this.loading = false;
          this.success = 'Account created! Sign in.';
          setTimeout(() => this.router.navigate(['/officiel/login']), 800);
        },
        error: (e: unknown) => {
          this.loading = false;
          const err = e as { status?: number; error?: { message?: string } };
          this.error =
            err.status === 409
              ? 'Email already in use.'
              : err.error?.message || 'Error.';
        },
      });
  }
}
