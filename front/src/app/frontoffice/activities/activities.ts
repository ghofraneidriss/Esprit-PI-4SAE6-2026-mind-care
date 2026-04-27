import { Component } from '@angular/core';

@Component({
  selector: 'app-frontoffice-activities',
  standalone: false,
  templateUrl: './activities.html',
  styleUrls: ['./activities.css'],
})
export class FrontofficeActivitiesPage {
  readonly items = [
    { title: 'Mindfulness Workshop', date: 'Friday, 10:00 AM', location: 'Room A / Online' },
    { title: 'Caregiver Support Circle', date: 'Saturday, 2:00 PM', location: 'Main Hall' },
    { title: 'Healthy Sleep Session', date: 'Monday, 6:30 PM', location: 'Virtual Session' },
  ];
}
