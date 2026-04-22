import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { UserRole } from '../../../core/models/user.model';

@Component({
  selector: 'app-auth-login-cover',
  standalone: false,
  templateUrl: './login-cover.html',
  styleUrls: ['./login-cover.css'],
})
export class LoginCoverAuthPage {
  email = '';
  password = '';
  role: UserRole = 'PATIENT';
  errorMessage = '';
  loading = false;
  roles: { value: UserRole; label: string }[] = [
    { value: 'ADMIN', label: 'Admin' },
    { value: 'PATIENT', label: 'Patient' },
    { value: 'CAREGIVER', label: 'Caregiver' },
    { value: 'VOLUNTEER', label: 'Volunteer' },
    { value: 'DOCTOR', label: 'Doctor' },
  ];

  constructor(private authService: AuthService, private router: Router) {
    if (this.authService.isLoggedIn()) {
      this.router.navigateByUrl(
        this.authService.getLandingRouteForRole(this.authService.getCurrentUser()?.role)
      );
    }
  }

  onLogin(): void {
    if (!this.email || !this.password) {
      this.errorMessage = 'Please fill in all fields.';
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    this.authService.login(this.email, this.password, this.role).subscribe({
      next: (user) => {
        this.loading = false;
        this.router.navigateByUrl(this.authService.getLandingRouteForRole(user.role));
      },
      error: (e: unknown) => {
        this.loading = false;
        const err = e as { status?: number; error?: { message?: string } };
        const message = String(err.error?.message ?? '').trim();
        if (message) {
          this.errorMessage = message;
        } else if (err.status === 401 || err.status === 403 || err.status === 400) {
          this.errorMessage = 'Incorrect email, password, or role.';
        } else if (err.status === 0) {
          this.errorMessage = 'Server unreachable.';
        } else {
          this.errorMessage = 'Incorrect email or password.';
        }
      }
    });
  }

  fill(email: string, pwd: string, role: UserRole): void {
    this.email = email;
    this.password = pwd;
    this.role = role;
  }
}
