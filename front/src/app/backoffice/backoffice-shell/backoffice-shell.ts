import { Component } from '@angular/core';

import { AuthService } from '../../frontoffice/auth/auth.service';

@Component({
  selector: 'app-backoffice-shell',
  standalone: false,
  templateUrl: './backoffice-shell.html',
  styleUrls: ['./backoffice-shell.css'],
})
export class BackofficeShellComponent {
  constructor(public authService: AuthService) { }
}
