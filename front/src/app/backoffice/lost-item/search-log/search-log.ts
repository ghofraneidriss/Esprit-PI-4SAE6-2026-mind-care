import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';
import { SearchReport, SearchLogStats, SearchTimeline } from '../lost-item.model';
import { LostItemService } from '../lost-item.service';
import { UserApiService, UserSummary } from '../user-api.service';
import { AuthService } from '../../../frontoffice/auth/auth.service';

@Component({
  selector: 'app-search-log',
  standalone: false,
  templateUrl: './search-log.html',
  styleUrls: ['./search-log.css'],
})
export class SearchLogComponent implements OnInit {
  // ── State ───────────────────────────────────────��──────────────────────────
  reports: SearchReport[] = [];
  stats: SearchLogStats | null = null;
  timeline: SearchTimeline | null = null;

  isLoading = false;
  isStatsLoading = false;
  pageError = '';
  successMsg = '';

  // ── Auth context ───────────────────────────────────────────────────────────
  currentRole = '';
  currentUserId: number | null = null;
  isAdminOrDoctor = false;
  isCaregiver = false;
  isPatient = false;

  // ── Filter form ────────────────────────────────────────────────────────────
  filterForm: FormGroup;
  showFilters = true;

  // ── View toggle ────────────────────────────────────────────────────────────
  activeTab: 'list' | 'timeline' | 'stats' = 'list';
  timelineLostItemId: number | null = null;

  // ── Pagination ─────────────────────────────────────────────────────────────
  currentPage = 0;
  pageSize = 15;

  // ── Dropdown options ───────────────────────────────────────────────────────
  readonly resultOptions = [
    { label: 'Not Found',        value: 'NOT_FOUND' },
    { label: 'Found',            value: 'FOUND' },
    { label: 'Partially Found',  value: 'PARTIALLY_FOUND' },
  ];
  readonly statusOptions = [
    { label: 'Open',      value: 'OPEN' },
    { label: 'Closed',    value: 'CLOSED' },
    { label: 'Escalated', value: 'ESCALATED' },
  ];

  patients: UserSummary[] = [];

  constructor(
    private readonly fb: FormBuilder,
    private readonly lostItemService: LostItemService,
    private readonly userApiService: UserApiService,
    private readonly authService: AuthService,
    private readonly cdr: ChangeDetectorRef,
  ) {
    this.filterForm = this.fb.group({
      lostItemId:      [null],
      reportedBy:      [null],
      searchResult:    [''],
      status:          [''],
      locationKeyword: [''],
      from:            [null],
      to:              [null],
    });
  }

  ngOnInit(): void {
    const user = this.authService.getLoggedUser();
    this.currentUserId = user?.userId ?? null;
    this.currentRole   = this.authService.getLoggedRole();
    this.isAdminOrDoctor = this.currentRole === 'ADMIN' || this.currentRole === 'DOCTOR';
    this.isCaregiver   = this.currentRole === 'CAREGIVER';
    this.isPatient     = this.currentRole === 'PATIENT';

    // Patient can only see their own reports → pre-lock reportedBy
    if (this.isPatient && this.currentUserId) {
      this.filterForm.patchValue({ reportedBy: this.currentUserId });
    }

    this.applyFilters();
    this.loadStats();
  }

  applyFilters(): void {
    this.isLoading = true;
    this.pageError = '';
    this.currentPage = 0;

    const v = this.filterForm.value;

    // PATIENT: always scope by their patientId (items belong to them)
    if (this.isPatient && this.currentUserId) {
      this.lostItemService.getReportsByPatient(this.currentUserId).subscribe({
        next: (data) => { this.reports = data; this.isLoading = false; this.cdr.detectChanges(); },
        error: (err) => { this.pageError = err?.error?.message ?? 'Failed to load reports.'; this.isLoading = false; this.cdr.detectChanges(); },
      });
      return;
    }

    // CAREGIVER: scope by reporter (they submitted the reports)
    if (this.isCaregiver && this.currentUserId && !v.reportedBy) {
      v.reportedBy = this.currentUserId;
    }

    this.lostItemService.advancedSearchReports({
      lostItemId:      v.lostItemId  || undefined,
      reportedBy:      v.reportedBy  || undefined,
      searchResult:    v.searchResult || undefined,
      status:          v.status       || undefined,
      locationKeyword: v.locationKeyword || undefined,
      from:            v.from         || undefined,
      to:              v.to           || undefined,
    }).subscribe({
      next: (data) => { this.reports = data; this.isLoading = false; this.cdr.detectChanges(); },
      error: (err) => { this.pageError = err?.error?.message ?? 'Failed to load reports.'; this.isLoading = false; this.cdr.detectChanges(); },
    });
  }

  resetFilters(): void {
    this.filterForm.reset({ searchResult: '', status: '' });
    if (this.isPatient && this.currentUserId) {
      this.filterForm.patchValue({ reportedBy: this.currentUserId });
    }
    this.applyFilters();
  }

  loadStats(): void {
    this.isStatsLoading = true;
    this.lostItemService.getSearchLogStats().subscribe({
      next: (s) => { this.stats = s; this.isStatsLoading = false; this.cdr.detectChanges(); },
      error: () => { this.isStatsLoading = false; this.cdr.detectChanges(); },
    });
  }

  loadTimeline(): void {
    if (!this.timelineLostItemId) return;
    this.isLoading = true;
    this.lostItemService.getSearchTimeline(this.timelineLostItemId).subscribe({
      next: (t) => { this.timeline = t; this.isLoading = false; this.cdr.detectChanges(); },
      error: (err) => { this.pageError = err?.error?.message ?? 'Failed to load timeline.'; this.isLoading = false; this.cdr.detectChanges(); },
    });
  }

  setTab(tab: 'list' | 'timeline' | 'stats'): void {
    this.activeTab = tab;
    if (tab === 'stats' && !this.stats) this.loadStats();
  }

  deleteReport(id: number): void {
    if (!confirm('Delete this search report?')) return;
    this.lostItemService.deleteSearchReport(id).subscribe({
      next: () => {
        this.successMsg = 'Report deleted.';
        this.applyFilters();
        setTimeout(() => (this.successMsg = ''), 3000);
      },
      error: (err) => { this.pageError = err?.error?.message ?? 'Failed to delete.'; },
    });
  }

  // ── Pagination ─────────────────────���───────────────────��───────────────────
  get pagedReports(): SearchReport[] {
    const s = this.currentPage * this.pageSize;
    return this.reports.slice(s, s + this.pageSize);
  }
  get totalPages(): number { return Math.ceil(this.reports.length / this.pageSize); }
  get pages(): number[] { return Array.from({ length: this.totalPages }, (_, i) => i); }
  goToPage(p: number): void {
    if (p >= 0 && p < this.totalPages) this.currentPage = p;
  }

  // ── Helpers ──────────────────────────────────────────────────────────────���─
  getResultClass(result?: string): string {
    switch (result) {
      case 'FOUND':            return 'badge bg-success';
      case 'PARTIALLY_FOUND':  return 'badge bg-warning text-dark';
      case 'NOT_FOUND':        return 'badge bg-danger';
      default:                 return 'badge bg-secondary';
    }
  }

  getResultLabel(result?: string): string {
    switch (result) {
      case 'FOUND':            return 'Found';
      case 'PARTIALLY_FOUND':  return 'Partially Found';
      case 'NOT_FOUND':        return 'Not Found';
      default:                 return result ?? '—';
    }
  }

  getStatusClass(status?: string): string {
    switch (status) {
      case 'OPEN':      return 'badge bg-primary';
      case 'CLOSED':    return 'badge bg-secondary';
      case 'ESCALATED': return 'badge bg-danger';
      default:          return 'badge bg-secondary';
    }
  }

  countByResult(result: string): number {
    return this.reports.filter(r => r.searchResult === result as any).length;
  }

  countByStatus(status: string): number {
    return this.reports.filter(r => r.status === status as any).length;
  }

  objectKeys(obj: Record<string, number> | null | undefined): string[] {
    return obj ? Object.keys(obj) : [];
  }

  getBarWidth(value: number, max: number): string {
    return max === 0 ? '0%' : Math.round((value / max) * 100) + '%';
  }

  get maxResultCount(): number {
    if (!this.stats?.resultDistribution) return 1;
    return Math.max(...Object.values(this.stats.resultDistribution), 1);
  }
}
