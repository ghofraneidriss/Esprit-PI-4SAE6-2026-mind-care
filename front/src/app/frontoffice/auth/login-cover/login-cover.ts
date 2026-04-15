import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';
import { NotificationService } from '../../../shared/notification.service';

@Component({
  selector: 'app-auth-login-cover',
  standalone: false,
  templateUrl: './login-cover.html',
  styleUrls: ['./login-cover.css'],
})
export class LoginCoverAuthPage {
  roles = ['PATIENT', 'VOLUNTEER', 'DOCTOR', 'CAREGIVER', 'ADMIN'];

  credentials = {
    email: '',
    password: '',
    role: 'PATIENT',
  };

  isLoading = false;
  errorMessage = '';

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly notificationService: NotificationService
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

        if (loggedRole === 'VOLUNTEER') {
          // Register FCM token + start foreground listener
          this.notificationService.initForUser(user.userId);
          this.notificationService.startListening();
        }

        if (this.authService.isBackofficeRole(loggedRole)) {
          this.router.navigateByUrl('/admin');
          return;
        }

        if (loggedRole === 'VOLUNTEER') {
          this.router.navigateByUrl('/admin');
          return;
        }

        if (loggedRole === 'PATIENT') {
          this.router.navigateByUrl('/reports');
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
