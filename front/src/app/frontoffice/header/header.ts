import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';

@Component({
  selector: 'app-header',
  standalone: false,
  templateUrl: './header.html',
  styleUrl: './header.css',
})
export class Header {

  constructor(
    private readonly router: Router,
    private readonly authService: AuthService
  ) {}

  get isLoggedIn(): boolean {
    return !!localStorage.getItem('loggedUser');
  }

  get isPatient(): boolean {
    return this.authService.getLoggedRole() === 'PATIENT';
  }

  get isCaregiver(): boolean {
    return this.authService.getLoggedRole() === 'CAREGIVER';
  }

  get isBackofficeUser(): boolean {
    const role = this.authService.getLoggedRole();
    return role === 'ADMIN' || role === 'DOCTOR' || role === 'CAREGIVER';
  }

  get loggedUserName(): string {
    const user = this.authService.getLoggedUser();
    if (!user) return '';
    return user.firstName ?? '';
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }
}
