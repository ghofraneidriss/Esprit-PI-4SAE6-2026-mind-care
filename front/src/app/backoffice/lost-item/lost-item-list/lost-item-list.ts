import { Component, OnInit } from '@angular/core';
import { Location } from '@angular/common';
import { Router } from '@angular/router';
import { LostItem, ItemStatus, ItemCategory, GlobalStats, AlertStats, PatientIntelligence } from '../lost-item.model';
import { LostItemService } from '../lost-item.service';
import { AuthService, AuthUser } from '../../../frontoffice/auth/auth.service';
import { UserApiService, UserSummary } from '../user-api.service';

@Component({
  selector: 'app-lost-item-list',
  standalone: false,
  templateUrl: './lost-item-list.html',
  styleUrls: ['./lost-item-list.css'],
})
export class LostItemListComponent implements OnInit {
  items: LostItem[] = [];
  filteredItems: LostItem[] = [];
  totalElements = 0;
  totalPages = 0;
  currentPage = 0;
  pageSize = 20;

  isLoading = false;
  pageError = '';
  successMsg = '';

  loggedUser: AuthUser | null = null;
  currentUserId: number | null = null;
  currentRole = '';
  isCaregiver = false;
  isAdminOrDoctor = false;

  /** Map userId → 'First Last' for patient name display */
  userNameMap: Record<number, string> = {};
  usersLoaded = false;

  /** New alert count for the badge */
  newAlertCount = 0;

  /** Search text filter */
  searchText = '';

  filters = {
    status: '' as '' | ItemStatus,
    category: '' as '' | ItemCategory,
  };

  readonly statusOptions: Array<{ label: string; value: ItemStatus }> = [
    { label: 'Lost', value: 'LOST' },
    { label: 'Searching', value: 'SEARCHING' },
    { label: 'Found', value: 'FOUND' },
    { label: 'Closed', value: 'CLOSED' },
  ];

  readonly categoryOptions: Array<{ label: string; value: ItemCategory }> = [
    { label: 'Clothing', value: 'CLOTHING' },
    { label: 'Accessory', value: 'ACCESSORY' },
    { label: 'Document', value: 'DOCUMENT' },
    { label: 'Medication', value: 'MEDICATION' },
    { label: 'Electronic', value: 'ELECTRONIC' },
    { label: 'Other', value: 'OTHER' },
  ];

  constructor(
    private readonly lostItemService: LostItemService,
    private readonly authService: AuthService,
    private readonly userApiService: UserApiService,
    private readonly router: Router,
    private readonly location: Location
  ) {}

  ngOnInit(): void {
    this.loggedUser = this.authService.getLoggedUser();
    this.currentUserId = this.loggedUser?.userId ?? null;
    this.currentRole = this.authService.getLoggedRole();
    this.isCaregiver = this.currentRole === 'CAREGIVER';
    this.isAdminOrDoctor = this.currentRole === 'ADMIN' || this.currentRole === 'DOCTOR';
    this.loadUsers();
    this.loadItems();
    this.loadNewAlertCount();
  }

  get isPatient(): boolean { return this.currentRole === 'PATIENT'; }

  get userInitials(): string {
    if (!this.loggedUser) return '?';
    return (this.loggedUser.firstName?.charAt(0) ?? '') + (this.loggedUser.lastName?.charAt(0) ?? '');
  }

  get userFullName(): string {
    if (!this.loggedUser) return 'Unknown';
    return `${this.loggedUser.firstName ?? ''} ${this.loggedUser.lastName ?? ''}`.trim();
  }

  /** Fetch all users to build a name map for patient display */
  loadUsers(): void {
    this.userApiService.getAllUsers().subscribe({
      next: (users: UserSummary[]) => {
        users.forEach(u => {
          this.userNameMap[u.userId] = `${u.firstName} ${u.lastName}`;
        });
        this.usersLoaded = true;
      },
      error: () => { this.usersLoaded = true; }
    });
  }

  getPatientName(patientId?: number | null): string {
    if (!patientId) return '—';
    return this.userNameMap[patientId] ?? `Patient #${patientId}`;
  }

  loadNewAlertCount(): void {
    if (this.isCaregiver && this.currentUserId) {
      this.lostItemService.getAlertsByCaregiverId(this.currentUserId).subscribe({
        next: alerts => { this.newAlertCount = alerts.filter(a => a.status === 'NEW').length; },
        error: () => {}
      });
    } else if (!this.isPatient) {
      this.lostItemService.getAllItemAlerts().subscribe({
        next: alerts => { this.newAlertCount = alerts.filter(a => a.status === 'NEW').length; },
        error: () => {}
      });
    }
  }

  loadItems(): void {
    this.isLoading = true;
    this.pageError = '';

    if (this.isPatient && this.currentUserId) {
      this.lostItemService.getPatientLostItems(
        this.currentUserId, this.filters.status || undefined, this.filters.category || undefined
      ).subscribe({
        next: (page) => {
          this.items = page.content;
          this.applyClientFilters();
          this.isLoading = false;
        },
        error: (err) => {
          this.pageError = err?.error?.message ?? 'Failed to load items.';
          this.isLoading = false;
        }
      });
    } else if (this.isCaregiver && this.currentUserId) {
      this.lostItemService.getItemsByCaregiverId(this.currentUserId).subscribe({
        next: (items) => {
          this.items = items;
          this.applyClientFilters();
          this.isLoading = false;
        },
        error: (err) => {
          this.pageError = err?.error?.message ?? 'Failed to load items.';
          this.isLoading = false;
        }
      });
    } else {
      this.lostItemService.getAllLostItems().subscribe({
        next: (items) => {
          this.items = items;
          this.applyClientFilters();
          this.isLoading = false;
        },
        error: (err) => {
          this.pageError = err?.error?.message ?? 'Failed to load lost items.';
          this.isLoading = false;
        },
      });
    }
  }

  applyClientFilters(): void {
    this.filteredItems = this.items.filter(i => {
      const matchStatus   = !this.filters.status   || i.status   === this.filters.status;
      const matchCategory = !this.filters.category || i.category === this.filters.category;
      const matchSearch   = !this.searchText || i.title.toLowerCase().includes(this.searchText.toLowerCase())
                            || (i.description ?? '').toLowerCase().includes(this.searchText.toLowerCase());
      return matchStatus && matchCategory && matchSearch;
    });
    this.totalElements = this.filteredItems.length;
    this.totalPages = Math.ceil(this.totalElements / this.pageSize);
    if (this.currentPage >= this.totalPages) this.currentPage = 0;
  }

  applyFilters(): void {
    this.currentPage = 0;
    this.applyClientFilters();
  }

  resetFilters(): void {
    this.filters = { status: '', category: '' };
    this.searchText = '';
    this.currentPage = 0;
    this.applyClientFilters();
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages) return;
    this.currentPage = page;
  }

  get pagedItems(): LostItem[] {
    const start = this.currentPage * this.pageSize;
    return this.filteredItems.slice(start, start + this.pageSize);
  }

  viewItem(id: number): void {
    this.router.navigate(['/admin/lost-items', id]);
  }

  editItem(id: number): void {
    this.router.navigate(['/admin/lost-items', id, 'edit']);
  }

  deleteItem(id: number): void {
    if (!confirm('Close this lost item? All open search reports will also be closed.')) return;
    this.lostItemService.deleteLostItem(id).subscribe({
      next: () => {
        this.successMsg = 'Item closed successfully.';
        this.loadItems();
        setTimeout(() => (this.successMsg = ''), 3000);
      },
      error: (err) => {
        this.pageError = err?.error?.message ?? 'Failed to close item.';
      },
    });
  }

  markFound(id: number): void {
    if (!confirm('Mark this item as FOUND? All open search reports will be closed.')) return;
    this.lostItemService.markAsFound(id).subscribe({
      next: () => {
        this.successMsg = 'Item marked as FOUND.';
        this.loadItems();
        setTimeout(() => (this.successMsg = ''), 3000);
      },
      error: (err) => {
        this.pageError = err?.error?.message ?? 'Failed to mark item as found.';
      },
    });
  }

  goBack(): void {
    this.location.back();
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

  get pages(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i);
  }

  /** Quick summary counts for the stats bar */
  get lostCount(): number { return this.items.filter(i => i.status === 'LOST').length; }
  get searchingCount(): number { return this.items.filter(i => i.status === 'SEARCHING').length; }
  get foundCount(): number { return this.items.filter(i => i.status === 'FOUND').length; }

  // ── Statistics Modal ─────────────────────────────────────────────────────

  showStatsModal = false;
  globalStats: GlobalStats | null = null;
  alertStats: AlertStats | null = null;
  statsLoading = false;
  statsError = '';

  openStats(): void {
    this.showStatsModal = true;
    document.body.style.overflow = 'hidden';
    if (!this.globalStats) this.loadStats();
  }

  closeStats(): void {
    this.showStatsModal = false;
    document.body.style.overflow = '';
  }

  loadStats(): void {
    this.statsLoading = true;
    this.statsError = '';
    this.lostItemService.getGlobalStatistics().subscribe({
      next: data => {
        this.globalStats = data;
        this.statsLoading = false;
      },
      error: () => {
        this.statsError = 'Failed to load statistics.';
        this.statsLoading = false;
      }
    });
    this.lostItemService.getAlertStatistics().subscribe({
      next: data => { this.alertStats = data; },
      error: () => {}
    });
  }

  refreshStats(): void {
    this.globalStats = null;
    this.alertStats = null;
    this.loadStats();
  }

  categoryEntries(): { key: string; value: number }[] {
    if (!this.globalStats?.categoryDistribution) return [];
    return Object.entries(this.globalStats.categoryDistribution)
      .map(([key, value]) => ({ key, value }))
      .sort((a, b) => b.value - a.value);
  }

  priorityEntries(): { key: string; value: number }[] {
    if (!this.globalStats?.priorityDistribution) return [];
    return Object.entries(this.globalStats.priorityDistribution)
      .map(([key, value]) => ({ key, value }))
      .sort((a, b) => b.value - a.value);
  }

  alertLevelEntries(): { key: string; value: number }[] {
    if (!this.alertStats?.levelDistribution) return [];
    return Object.entries(this.alertStats.levelDistribution)
      .map(([key, value]) => ({ key, value }))
      .sort((a, b) => b.value - a.value);
  }

  categoryBarWidth(value: number): number {
    if (!this.globalStats) return 0;
    const max = Math.max(...Object.values(this.globalStats.categoryDistribution), 1);
    return Math.round((value / max) * 100);
  }

  priorityBarWidth(value: number): number {
    if (!this.globalStats) return 0;
    const max = Math.max(...Object.values(this.globalStats.priorityDistribution), 1);
    return Math.round((value / max) * 100);
  }

  alertLevelBarWidth(value: number): number {
    if (!this.alertStats) return 0;
    const max = Math.max(...Object.values(this.alertStats.levelDistribution), 1);
    return Math.round((value / max) * 100);
  }

  getPriorityBarColor(key: string): string {
    switch (key) {
      case 'CRITICAL': return '#dc3545';
      case 'HIGH':     return '#ffc107';
      case 'MEDIUM':   return '#0d6efd';
      default:         return '#6c757d';
    }
  }

  getRecoveryColor(): string {
    const rate = this.globalStats?.recoveryRate ?? 0;
    if (rate >= 70) return '#22c55e';
    if (rate >= 40) return '#f59e0b';
    return '#ef4444';
  }

  // ── AI Patient Intelligence Modal ────────────────────────────────────────

  showIntelligenceModal = false;
  intelligence: PatientIntelligence | null = null;
  intelligenceLoading = false;
  intelligenceError = '';
  intelligencePatientId: number | null = null;

  /** Unique patient IDs visible in current list */
  get uniquePatientIds(): number[] {
    const ids = new Set<number>();
    this.items.forEach(i => { if (i.patientId) ids.add(i.patientId); });
    return Array.from(ids).sort((a, b) => a - b);
  }

  openIntelligence(): void {
    this.showIntelligenceModal = true;
    document.body.style.overflow = 'hidden';
    // Default to first patient if not selected yet
    if (!this.intelligencePatientId && this.uniquePatientIds.length > 0) {
      this.intelligencePatientId = this.uniquePatientIds[0];
    }
    if (this.intelligencePatientId && !this.intelligence) {
      this.loadIntelligence();
    }
  }

  closeIntelligence(): void {
    this.showIntelligenceModal = false;
    document.body.style.overflow = '';
  }

  loadIntelligence(): void {
    if (!this.intelligencePatientId) return;
    this.intelligenceLoading = true;
    this.intelligenceError = '';
    this.intelligence = null;
    this.lostItemService.getPatientIntelligence(this.intelligencePatientId).subscribe({
      next: data => {
        this.intelligence = data;
        this.intelligenceLoading = false;
      },
      error: () => {
        this.intelligenceError = 'Failed to load AI intelligence. Make sure the service is running.';
        this.intelligenceLoading = false;
      }
    });
  }

  onIntelligencePatientChange(): void {
    this.intelligence = null;
    this.loadIntelligence();
  }

  getRiskColor(level?: string): string {
    switch (level) {
      case 'CRITICAL': return '#dc3545';
      case 'HIGH':     return '#fd7e14';
      case 'MODERATE': return '#ffc107';
      default:         return '#198754';
    }
  }

  getTrendIcon(dir?: string): string {
    switch (dir) {
      case 'INCREASING': return 'bi-graph-up-arrow';
      case 'DECREASING': return 'bi-graph-down-arrow';
      default:           return 'bi-dash-lg';
    }
  }

  getTrendColor(dir?: string): string {
    switch (dir) {
      case 'INCREASING': return '#dc3545';
      case 'DECREASING': return '#198754';
      default:           return '#6c757d';
    }
  }

  getCategoryRiskColor(level?: string): string {
    switch (level) {
      case 'HIGH':   return '#dc3545';
      case 'MEDIUM': return '#ffc107';
      default:       return '#198754';
    }
  }

  monthlyBarMax(): number {
    if (!this.intelligence?.monthlyTrend) return 1;
    return Math.max(...this.intelligence.monthlyTrend.map(m => m.count), 1);
  }

  monthlyBarWidth(count: number): number {
    return Math.round((count / this.monthlyBarMax()) * 100);
  }

  /** Parse aiAnalysis into labelled sections */
  get aiSections(): Array<{ label: string; content: string }> {
    const text = this.intelligence?.aiAnalysis;
    if (!text) return [];
    const labels = [
      'CLINICAL ASSESSMENT',
      'RISK LEVEL',
      'KEY PATTERNS',
      'CARE TEAM RECOMMENDATIONS',
      'COGNITIVE INDICATOR'
    ];
    const sections: Array<{ label: string; content: string }> = [];
    for (let i = 0; i < labels.length; i++) {
      const start = text.indexOf(labels[i] + ':');
      if (start === -1) continue;
      const end = i < labels.length - 1
        ? text.indexOf(labels[i + 1] + ':')
        : text.length;
      const content = text.slice(start + labels[i].length + 1, end === -1 ? text.length : end).trim();
      sections.push({ label: labels[i], content });
    }
    return sections.length > 0 ? sections : [{ label: 'AI ANALYSIS', content: text }];
  }
}
