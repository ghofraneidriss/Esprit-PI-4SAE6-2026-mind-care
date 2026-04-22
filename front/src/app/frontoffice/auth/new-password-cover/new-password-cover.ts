import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-auth-new-password-cover',
  standalone: false,
  templateUrl: './new-password-cover.html',
  styleUrls: ['./new-password-cover.css'],
})
export class NewPasswordCoverAuthPage implements OnInit {
  token = '';
  password = '';
  confirmPassword = '';
  isLoading = false;
  successMsg = '';
  errorMsg = '';
  tokenMissing = false;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly http: HttpClient
  ) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') ?? '';
    if (!this.token) {
      this.tokenMissing = true;
    }
  }

  onSubmit(): void {
    this.successMsg = '';
    this.errorMsg = '';

    if (!this.password || this.password.length < 6) {
      this.errorMsg = 'Password must be at least 6 characters.';
      return;
    }
    if (this.password !== this.confirmPassword) {
      this.errorMsg = 'Passwords do not match.';
      return;
    }

    this.isLoading = true;
    this.http.post('http://localhost:8082/api/users/reset-password',
      { token: this.token, password: this.password },
      { responseType: 'text' }
    ).subscribe({
      next: () => {
        this.isLoading = false;
        this.successMsg = 'Password reset successfully! Redirecting to login…';
        setTimeout(() => this.router.navigate(['/auth/login']), 2500);
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMsg = err?.error ?? 'Something went wrong. Please try again.';
      }
    });
  }
}
