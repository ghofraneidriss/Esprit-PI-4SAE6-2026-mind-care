import { Component } from '@angular/core';

import { AuthService } from '../../frontoffice/auth/auth.service';

@Component({
  selector: 'app-home2',
  standalone: false,
  templateUrl: './home2.html',
  styleUrls: ['./home2.css'],
})
export class Home2 {
  constructor(public authService: AuthService) { }

  get isVolunteer(): boolean {
    return this.authService.isVolunteer();
  }
}
