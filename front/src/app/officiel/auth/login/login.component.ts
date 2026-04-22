import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { UserRole } from '../../../core/models/user.model';

@Component({
  selector: 'app-off-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
})
export class OfficielLoginComponent {
  email = '';
  password = '';
  role: UserRole = 'PATIENT';
  error = '';
  loading = false;
  showPwd = false;
  roles: { value: UserRole; label: string }[] = [
    { value: 'ADMIN', label: 'Admin' },
    { value: 'PATIENT', label: 'Patient' },
    { value: 'CAREGIVER', label: 'Caregiver' },
    { value: 'VOLUNTEER', label: 'Volunteer' },
    { value: 'DOCTOR', label: 'Doctor' },
  ];

  constructor(
    private auth: AuthService,
    private router: Router
  ) {
    if (this.auth.isLoggedIn()) {
      this.router.navigateByUrl(this.auth.getLandingRouteForRole(this.auth.getCurrentUser()?.role));
    }
  }

  submit(): void {
    if (!this.email || !this.password) {
      this.error = 'Please fill in all fields.';
      return;
    }
    this.loading = true;
    this.error = '';
    this.auth.login(this.email, this.password, this.role).subscribe({
      next: (user) => {
        this.loading = false;
        this.router.navigateByUrl(this.auth.getLandingRouteForRole(user.role));
      },
      error: (e: unknown) => {
        this.loading = false;
        const err = e as { status?: number; error?: { message?: string } };
        const message = String(err.error?.message ?? '').trim();
        if (message) {
          this.error = message;
        } else if (err.status === 401 || err.status === 403 || err.status === 400) {
          this.error = 'Incorrect email, password, or role.';
        } else if (err.status === 0) {
          this.error = 'Server unreachable.';
        } else {
          this.error = 'Error.';
        }
      },
    });
  }

  fill(email: string, pwd: string, role: UserRole): void {
    this.email = email;
    this.password = pwd;
    this.role = role;
  }
}
