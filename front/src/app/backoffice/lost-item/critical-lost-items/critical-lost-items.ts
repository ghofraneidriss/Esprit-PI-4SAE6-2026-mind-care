import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { LostItem } from '../lost-item.model';
import { LostItemService } from '../lost-item.service';
import { AuthService, AuthUser } from '../../../frontoffice/auth/auth.service';
import { UserApiService, UserSummary } from '../user-api.service';

@Component({
  selector: 'app-critical-lost-items',
  standalone: false,
  templateUrl: './critical-lost-items.html',
  styleUrls: ['./critical-lost-items.css'],
})
export class CriticalLostItemsComponent implements OnInit {
  items: LostItem[] = [];
  urgentCount = 0;
  isLoading = false;
  pageError = '';

  loggedUser: AuthUser | null = null;
  currentUserId: number | null = null;
  currentRole = '';
  isCaregiver = false;

  userNameMap: Record<number, string> = {};

  constructor(
    private readonly lostItemService: LostItemService,
    private readonly authService: AuthService,
    private readonly userApiService: UserApiService,
    private readonly router: Router,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    this.loggedUser = this.authService.getLoggedUser();
    this.currentUserId = this.loggedUser?.userId ?? null;
    this.currentRole = this.authService.getLoggedRole();
    this.isCaregiver = this.currentRole === 'CAREGIVER';
    this.loadUsers();
    this.loadCritical();
  }

  get userInitials(): string {
    if (!this.loggedUser) return '?';
    return (this.loggedUser.firstName?.charAt(0) ?? '') + (this.loggedUser.lastName?.charAt(0) ?? '');
  }

  get userFullName(): string {
    if (!this.loggedUser) return 'Unknown';
    return `${this.loggedUser.firstName ?? ''} ${this.loggedUser.lastName ?? ''}`.trim();
  }

  loadUsers(): void {
    this.userApiService.getAllUsers().subscribe({
      next: (users: UserSummary[]) => {
        users.forEach(u => { this.userNameMap[u.userId] = `${u.firstName} ${u.lastName}`; });
      },
      error: () => {}
    });
  }

  getPatientName(patientId?: number | null): string {
    if (!patientId) return '—';
    return this.userNameMap[patientId] ?? `Patient #${patientId}`;
  }

  loadCritical(): void {
    if (!this.currentUserId) return;
    this.isLoading = true;

    const request$ = this.isCaregiver
      ? this.lostItemService.getCriticalItemsByCaregiverId(this.currentUserId!)
      : this.lostItemService.getAllCriticalItems();

    request$.subscribe({
      next: (data) => {
        this.items = data.items;
        this.urgentCount = data.urgentCount;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.pageError = err?.error?.message ?? 'Failed to load critical items.';
        this.isLoading = false;
        this.cdr.detectChanges();
      },
    });
  }

  viewItem(id: number): void {
    this.router.navigate(['/admin/lost-items', id]);
  }

  goBack(): void {
    this.router.navigate(['/admin/lost-items']);
  }

  getPriorityClass(priority?: string): string {
    switch (priority) {
      case 'LOW': return 'badge bg-secondary';
      case 'MEDIUM': return 'badge bg-primary';
      case 'HIGH': return 'badge bg-warning text-dark';
      case 'CRITICAL': return 'badge bg-danger';
      default: return 'badge bg-secondary';
    }
  }

  getStatusClass(status?: string): string {
    switch (status) {
      case 'LOST': return 'badge bg-danger';
      case 'SEARCHING': return 'badge bg-warning text-dark';
      case 'FOUND': return 'badge bg-success';
      case 'CLOSED': return 'badge bg-secondary';
      default: return 'badge bg-secondary';
    }
  }

  getRoleClass(role: string): string {
    switch (role) {
      case 'ADMIN': return 'badge bg-danger';
      case 'DOCTOR': return 'badge bg-primary';
      case 'CAREGIVER': return 'badge bg-info text-dark';
      case 'PATIENT': return 'badge bg-success';
      default: return 'badge bg-secondary';
    }
  }
}
