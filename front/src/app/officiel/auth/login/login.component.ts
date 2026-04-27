import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

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
  error = '';
  loading = false;
  showPwd = false;

  constructor(
    private auth: AuthService,
    private router: Router
  ) {
    if (this.auth.isLoggedIn()) {
      this.router.navigate(['/officiel/dashboard']);
    }
  }

  submit(): void {
    if (!this.email || !this.password) {
      this.error = 'Please fill in all fields.';
      return;
    }
    this.loading = true;
    this.error = '';
    this.auth.login(this.email, this.password).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/officiel/dashboard']);
      },
      error: (e: unknown) => {
        this.loading = false;
        const err = e as { status?: number; error?: { message?: string } };
        this.error =
          err.status === 401
            ? 'Incorrect email or password.'
            : err.status === 0
              ? 'Server unreachable.'
              : err.error?.message || 'Error.';
      },
    });
  }

  fill(email: string, pwd: string): void {
    this.email = email;
    this.password = pwd;
  }
}
