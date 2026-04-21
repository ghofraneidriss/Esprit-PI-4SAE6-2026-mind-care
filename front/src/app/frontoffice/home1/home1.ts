import { Component } from '@angular/core';
import { AuthService } from '../auth/auth.service';

@Component({
  selector: 'app-home1',
  standalone: false,
  templateUrl: './home1.html',
  styleUrls: ['./home1.css'],
})
export class Home1 {
  constructor(public readonly authService: AuthService) { }

  get isVolunteerDashboard(): boolean {
    const role = this.authService.getLoggedRole();
    return role === 'VOLUNTEER' || role === 'CAREGIVER';
  }
}
