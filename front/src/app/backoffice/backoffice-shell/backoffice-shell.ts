import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { RouterModule } from '@angular/router';

import { AuthService } from '../../frontoffice/auth/auth.service';

@Component({
  selector: 'app-backoffice-shell',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './backoffice-shell.html',
  styleUrls: ['./backoffice-shell.css'],
})
export class BackofficeShellComponent {
  constructor(
    public authService: AuthService,
    private readonly router: Router
  ) { }

  get isVolunteer(): boolean {
    return this.authService.isVolunteer();
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }
}
