import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { GlobalOverlaysComponent } from '../../shared/global-overlays/global-overlays.component';

export interface OfficielActivityNavItem {
  path: string;
  label: string;
  icon: string;
}

@Component({
  selector: 'app-officiel-activities-shell',
  standalone: true,
  imports: [CommonModule, RouterModule, GlobalOverlaysComponent],
  templateUrl: './officiel-activities-shell.component.html',
  styleUrls: ['./officiel-activities-shell.component.css'],
})
export class OfficielActivitiesShellComponent {
  readonly navItems: OfficielActivityNavItem[] = [
    { path: '/officiel/dashboard', label: 'Dashboard', icon: 'ri-dashboard-3-line' },
    { path: '/officiel/quiz-list', label: 'My quizzes', icon: 'ri-gamepad-line' },
    { path: '/officiel/results', label: 'Results & risk', icon: 'ri-line-chart-line' },
    { path: '/officiel/performance', label: 'Performance analysis', icon: 'ri-trophy-line' },
  ];
}
