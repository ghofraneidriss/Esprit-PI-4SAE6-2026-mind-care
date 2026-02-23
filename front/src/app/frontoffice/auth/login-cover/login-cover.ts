import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-auth-login-cover',
  standalone: false,
  templateUrl: './login-cover.html',
  styleUrls: ['./login-cover.css'],
})
export class LoginCoverAuthPage {
  roles = ['PATIENT', 'DOCTOR', 'CAREGIVER', 'ADMIN'];

  credentials = {
    email: '',
    password: '',
    role: 'PATIENT',
  };

  isLoading = false;
  errorMessage = '';

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  onSubmit(): void {
    if (!this.credentials.email || !this.credentials.password || !this.credentials.role) {
      this.errorMessage = 'Email, password and role are required.';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    this.authService.login(this.credentials).subscribe({
      next: (user) => {
        this.isLoading = false;
        const loggedRole = this.authService.normalizeRole(user.role || this.credentials.role);
        if (this.authService.isBackofficeRole(loggedRole)) {
          this.router.navigateByUrl('/admin');
          return;
        }
        this.router.navigateByUrl('/');
      },
      error: (error) => {
        this.isLoading = false;
        this.errorMessage = error?.error?.message || 'Login failed. Please try again.';
      },
    });
  }
}
