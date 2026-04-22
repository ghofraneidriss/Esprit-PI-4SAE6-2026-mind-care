import { Component } from '@angular/core';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-auth-forgot-password-cover',
  standalone: false,
  templateUrl: './forgot-password-cover.html',
  styleUrls: ['./forgot-password-cover.css'],
})
export class ForgotPasswordCoverAuthPage {
  email = '';
  isLoading = false;
  successMsg = '';
  errorMsg = '';

  constructor(private readonly http: HttpClient) {}

  onSubmit(): void {
    this.successMsg = '';
    this.errorMsg = '';

    if (!this.email.trim()) {
      this.errorMsg = 'Please enter your email address.';
      return;
    }

    this.isLoading = true;
    this.http.post('http://localhost:8082/api/users/forgot-password',
      { email: this.email },
      { responseType: 'text' }
    ).subscribe({
      next: () => {
        this.isLoading = false;
        this.successMsg = 'Reset link sent! Please check your inbox (and spam folder).';
        this.email = '';
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMsg = err?.error ?? 'Something went wrong. Please try again.';
      }
    });
  }
}
