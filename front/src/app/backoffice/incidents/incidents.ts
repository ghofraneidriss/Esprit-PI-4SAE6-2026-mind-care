import { Component } from '@angular/core';
import { AuthService } from '../../frontoffice/auth/auth.service';

@Component({
  selector: 'app-incidents',
  standalone: false,
  templateUrl: './incidents.html',
  styleUrls: ['./incidents.css'],
})
export class IncidentsPage {
  readonly incidentItems = [
    { title: 'Medication delay alert', status: 'OPEN', priority: 'HIGH', owner: 'Dr. Salem' },
    { title: 'Sensor disconnection', status: 'IN_PROGRESS', priority: 'MEDIUM', owner: 'Ops Team' },
    { title: 'Unexpected patient fall', status: 'OPEN', priority: 'CRITICAL', owner: 'Nurse Team' },
    { title: 'Notification timeout', status: 'RESOLVED', priority: 'LOW', owner: 'Platform Team' },
  ];

  constructor(public readonly authService: AuthService) {}
}
