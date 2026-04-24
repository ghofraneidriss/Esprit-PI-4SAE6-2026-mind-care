import { Component, signal, OnInit, OnDestroy } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs/operators';
import { AuthService } from './frontoffice/auth/auth.service';
import { VolunteerService } from './backoffice/volunteering/volunteer.service';

@Component({
  selector: 'app-root',
  templateUrl: './app.html',
  standalone: false,
  styleUrls: ['./app.css']
})
export class App implements OnInit, OnDestroy {
  protected readonly title = signal('mind_care');

  constructor(
      private readonly router: Router,
      private readonly authService: AuthService,
      private readonly volunteerService: VolunteerService
  ) {
    this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe(() => {
        const preloader = document.getElementById('preloader');
        if (preloader) {
          preloader.remove();
        }

        const user = this.authService.getLoggedUser();
        if (!user || this.authService.normalizeRole(user.role) !== 'VOLUNTEER') {
          this.volunteerService.disconnectWebSocket();
        }
      });
  }

  ngOnInit() {
    // Realtime volunteer socket is intentionally disabled.
  }

  ngOnDestroy() {
    this.volunteerService.disconnectWebSocket();
  }
}
