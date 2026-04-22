import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { LostItemService } from '../lost-item.service';
import { LostItemAlert, AlertLevel, AlertStatus } from '../lost-item.model';
import { AuthService, AuthUser } from '../../../frontoffice/auth/auth.service';

@Component({
  selector: 'app-item-alerts',
  standalone: false,
  templateUrl: './item-alerts.html',
  styleUrls: ['./item-alerts.css']
})
export class ItemAlertsComponent implements OnInit {

  alerts: LostItemAlert[] = [];
  filteredAlerts: LostItemAlert[] = [];
  loggedUser: AuthUser | null = null;
  isLoading = false;
  pageError = '';
  successMsg = '';

  currentUserId: number | null = null;
  currentRole = '';
  isCaregiver = false;

  filterLevel: AlertLevel | '' = '';
  filterStatus: AlertStatus | '' = '';

  readonly levelOptions: AlertLevel[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
  readonly statusOptions: AlertStatus[] = ['NEW', 'VIEWED', 'RESOLVED'];

  constructor(
    private readonly svc: LostItemService,
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.loggedUser = this.authService.getLoggedUser();
    this.currentUserId = this.loggedUser?.userId ?? null;
    this.currentRole = this.authService.getLoggedRole();
    this.isCaregiver = this.currentRole === 'CAREGIVER';
    this.loadAlerts();
  }

  loadAlerts(): void {
    this.isLoading = true;
    this.pageError = '';

    const request$ = (this.isCaregiver && this.currentUserId)
      ? this.svc.getAlertsByCaregiverId(this.currentUserId)
      : this.svc.getAllItemAlerts();

    request$.subscribe({
      next: data => {
        this.alerts = data;
        this.applyFilters();
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.pageError = 'Failed to load alerts.';
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  applyFilters(): void {
    this.filteredAlerts = this.alerts.filter(a => {
      const matchLevel  = !this.filterLevel  || a.level  === this.filterLevel;
      const matchStatus = !this.filterStatus || a.status === this.filterStatus;
      return matchLevel && matchStatus;
    });
  }

  resetFilters(): void {
    this.filterLevel  = '';
    this.filterStatus = '';
    this.applyFilters();
  }

  markViewed(alert: LostItemAlert): void {
    if (!alert.id || alert.status !== 'NEW') return;
    this.svc.markAlertViewed(alert.id).subscribe({
      next: updated => {
        const idx = this.alerts.findIndex(a => a.id === alert.id);
        if (idx !== -1) this.alerts[idx] = updated;
        this.applyFilters();
        this.successMsg = 'Alert marked as viewed.';
        setTimeout(() => this.successMsg = '', 3000);
      },
      error: () => { this.pageError = 'Failed to mark alert as viewed.'; }
    });
  }

  resolveAlert(alert: LostItemAlert): void {
    if (!alert.id) return;
    this.svc.resolveAlert(alert.id).subscribe({
      next: updated => {
        const idx = this.alerts.findIndex(a => a.id === alert.id);
        if (idx !== -1) this.alerts[idx] = updated;
        this.applyFilters();
        this.successMsg = 'Alert resolved.';
        setTimeout(() => this.successMsg = '', 3000);
      },
      error: () => { this.pageError = 'Failed to resolve alert.'; }
    });
  }

  escalateAlert(alert: LostItemAlert): void {
    if (!alert.id || alert.level === 'CRITICAL') return;
    this.svc.escalateAlert(alert.id).subscribe({
      next: updated => {
        const idx = this.alerts.findIndex(a => a.id === alert.id);
        if (idx !== -1) this.alerts[idx] = updated;
        this.applyFilters();
        this.successMsg = `Alert escalated to ${updated.level}.`;
        setTimeout(() => this.successMsg = '', 3000);
      },
      error: err => {
        this.pageError = err?.error?.message || 'Cannot escalate further.';
      }
    });
  }

  deleteAlert(id: number | undefined): void {
    if (!id || !confirm('Delete this alert permanently?')) return;
    this.svc.deleteItemAlert(id).subscribe({
      next: () => {
        this.alerts = this.alerts.filter(a => a.id !== id);
        this.applyFilters();
        this.successMsg = 'Alert deleted.';
        setTimeout(() => this.successMsg = '', 3000);
      },
      error: () => { this.pageError = 'Failed to delete alert.'; }
    });
  }

  viewItem(lostItemId: number): void {
    if (!lostItemId || lostItemId === 0) return;
    this.router.navigate(['/admin/lost-items', lostItemId]);
  }

  goBack(): void {
    this.router.navigate(['/admin/lost-items']);
  }

  get userInitials(): string {
    if (!this.loggedUser) return '?';
    return (this.loggedUser.firstName?.charAt(0) ?? '') + (this.loggedUser.lastName?.charAt(0) ?? '');
  }

  getLevelClass(level?: AlertLevel): string {
    switch (level) {
      case 'CRITICAL': return 'badge bg-danger';
      case 'HIGH':     return 'badge bg-warning text-dark';
      case 'MEDIUM':   return 'badge bg-primary';
      default:         return 'badge bg-secondary';
    }
  }

  getStatusClass(status?: AlertStatus): string {
    switch (status) {
      case 'NEW':      return 'badge bg-danger';
      case 'VIEWED':   return 'badge bg-info text-dark';
      case 'RESOLVED': return 'badge bg-success';
      default:         return 'badge bg-secondary';
    }
  }

  get newCount(): number {
    return this.alerts.filter(a => a.status === 'NEW').length;
  }

  get criticalCount(): number {
    return this.alerts.filter(a => a.level === 'CRITICAL' && a.status !== 'RESOLVED').length;
  }
}
