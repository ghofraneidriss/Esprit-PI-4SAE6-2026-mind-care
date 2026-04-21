import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-off-layout',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './layout.component.html',
  styleUrls: ['./layout.component.css']
})
export class LayoutComponent implements OnInit {
  user: any = null;
  sidebarOpen = true;
  navItems: any[] = [];

  constructor(private auth: AuthService, private router: Router) {}

  ngOnInit() {
    this.user = this.auth.currentUser;
    if (!this.user) { this.router.navigate(['/officiel/login']); return; }
    this.buildNav();
  }

  buildNav() {
    const role = String(this.user?.role ?? '').toUpperCase();
    this.navItems = [
      { icon: '📊', label: 'Dashboard', link: '/officiel/dashboard', roles: ['ADMIN', 'DOCTOR', 'PATIENT', 'CAREGIVER'] },
      { icon: '🧩', label: 'Quiz management', link: '/officiel/quiz-management', roles: ['ADMIN', 'DOCTOR'] },
      { icon: '📸', label: 'Photo discrimination', link: '/officiel/photo-management', roles: ['ADMIN', 'DOCTOR'] },
      { icon: '🎮', label: 'My quizzes', link: '/officiel/quiz-list', roles: ['PATIENT', 'CAREGIVER'] },
      { icon: '📈', label: 'Results & risk', link: '/officiel/results', roles: ['ADMIN', 'DOCTOR', 'PATIENT'] },
      { icon: '📄', label: 'PDF reports', link: '/officiel/reports', roles: ['ADMIN', 'DOCTOR'] },
      { icon: '👥', label: 'Users', link: '/officiel/users', roles: ['ADMIN', 'DOCTOR'] },
    ].filter(n => n.roles.includes(String(role ?? '').toUpperCase()));
  }

  toggleSidebar() { this.sidebarOpen = !this.sidebarOpen; }

  logout() {
    this.auth.logout();
    this.router.navigate(['/officiel/quiz-list']);
  }

  get initials(): string {
    if (!this.user) return '?';
    return (this.user.firstname?.[0] || '') + (this.user.lastname?.[0] || '');
  }

  get roleBadge(): string {
    const m: any = { ADMIN: '🛡️ Admin', DOCTOR: '🩺 Doctor', PATIENT: '🧠 Patient', CAREGIVER: '🤝 Caregiver' };
    return m[this.user?.role] || this.user?.role;
  }
}
