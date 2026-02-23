import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService, RegisterRequest } from '../auth.service';

@Component({
  selector: 'app-auth-register-cover',
  standalone: false,
  templateUrl: './register-cover.html',
  styleUrls: ['./register-cover.css'],
})
export class RegisterCoverAuthPage {
  roles = ['PATIENT', 'DOCTOR', 'CAREGIVER', 'ADMIN'];

  model: RegisterRequest = {
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    role: 'PATIENT',
    phone: '',
  };

  acceptedTerms = false;
  isLoading = false;
  errorMessage = '';

  constructor(
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  onSubmit(): void {
    if (!this.acceptedTerms) {
      this.errorMessage = 'Please accept privacy policy and terms.';
      return;
    }

    if (!this.model.firstName || !this.model.lastName || !this.model.email || !this.model.password || !this.model.role) {
      this.errorMessage = 'First name, last name, email, password and role are required.';
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    this.authService.register(this.model).subscribe({
      next: () => {
        this.isLoading = false;
        this.router.navigateByUrl('/auth/login');
      },
      error: (error) => {
        this.isLoading = false;
        this.errorMessage = error?.error?.message || 'Registration failed. Please try again.';
      },
    });
  }
}
