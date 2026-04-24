import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';
import { VolunteerService } from '../../backoffice/volunteering/volunteer.service';

@Component({
  selector: 'app-header',
  standalone: false,
  templateUrl: './header.html',
  styleUrl: './header.css',
})
export class Header {

  constructor(
    private readonly router: Router,
    private readonly authService: AuthService,
    private readonly volunteerService: VolunteerService
  ) { }

  get isLoggedIn(): boolean {
    return !!localStorage.getItem('loggedUser');
  }

  get isVolunteer(): boolean {
    const user = this.authService.getLoggedUser();
    return !!(user && this.authService.normalizeRole(user.role) === 'VOLUNTEER');
  }

  logout(): void {
    const user = this.authService.getLoggedUser();
    if (user && this.authService.normalizeRole(user.role) === 'VOLUNTEER') {
      this.volunteerService.markOffline(user.userId).subscribe({
        error: (err) => console.warn('Failed to mark offline', err),
      });
      this.volunteerService.disconnectWebSocket();
      this.volunteerService.stopSessionHeartbeat();
    }
    this.authService.logout();
    this.router.navigate(['/auth/signup']);
  }
}
