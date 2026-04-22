import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DomSanitizer, SafeStyle } from '@angular/platform-browser';
import { Subscription } from 'rxjs';
import { RecoveryStrategy } from '../lost-item.model';
import { LostItemService } from '../lost-item.service';
import { AuthService, AuthUser } from '../../../frontoffice/auth/auth.service';

@Component({
  selector: 'app-recovery-strategy',
  standalone: false,
  templateUrl: './recovery-strategy.html',
  styleUrls: ['./recovery-strategy.css'],
})
export class RecoveryStrategyComponent implements OnInit, OnDestroy {
  strategy: RecoveryStrategy | null = null;
  isLoading = false;
  pageError = '';
  itemId: number | null = null;

  loggedUser: AuthUser | null = null;
  currentRole = '';

  private routeSub?: Subscription;

  constructor(
    private readonly lostItemService: LostItemService,
    private readonly authService: AuthService,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly cdr: ChangeDetectorRef,
    private readonly sanitizer: DomSanitizer,
  ) {}

  ngOnInit(): void {
    this.loggedUser = this.authService.getLoggedUser();
    this.currentRole = this.authService.getLoggedRole();
    // Subscribe to params so re-navigation always re-fetches
    this.routeSub = this.route.params.subscribe(params => {
      const id = params['id'];
      if (id) {
        this.itemId = +id;
        this.strategy = null;
        this.pageError = '';
        this.loadStrategy(this.itemId);
      }
    });
  }

  ngOnDestroy(): void {
    this.routeSub?.unsubscribe();
  }

  loadStrategy(id: number): void {
    this.isLoading = true;
    this.pageError = '';
    this.cdr.detectChanges();
    this.lostItemService.getRecoveryStrategy(id).subscribe({
      next: (data) => {
        this.strategy = data;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.pageError = err?.error?.message ?? 'Failed to compute recovery strategy. Make sure the service is running.';
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  refresh(): void {
    if (this.itemId) this.loadStrategy(this.itemId);
  }

  // ── Gauge ────────────────────────────────────────────────────────────────

  get gaugeStyle(): SafeStyle {
    const pct = this.strategy?.recoveryProbability ?? 0;
    const color = this.probabilityColor;
    return this.sanitizer.bypassSecurityTrustStyle(
      `conic-gradient(${color} ${pct}%, #e9ecef ${pct}%)`
    );
  }

  get probabilityColor(): string {
    if (!this.strategy) return '#6b1fad';
    switch (this.strategy.probabilityLevel) {
      case 'HIGH':     return '#198754';
      case 'MODERATE': return '#fd7e14';
      case 'LOW':      return '#dc3545';
      case 'CRITICAL': return '#6f42c1';
      default:         return '#6b1fad';
    }
  }

  get probabilityBg(): string {
    if (!this.strategy) return '#f8f4fc';
    switch (this.strategy.probabilityLevel) {
      case 'HIGH':     return '#d1f5d3';
      case 'MODERATE': return '#fff3cd';
      case 'LOW':      return '#f8d7da';
      case 'CRITICAL': return '#ede7f6';
      default:         return '#f8f4fc';
    }
  }

  // ── Location bars ────────────────────────────────────────────────────────

  getBarWidth(rate: number): number {
    if (!this.strategy?.recommendedLocations?.length) return 0;
    const maxRate = this.strategy.recommendedLocations[0].successRate;
    return maxRate > 0 ? Math.round((rate / maxRate) * 100) : 0;
  }

  getBarColor(rate: number): string {
    if (rate >= 60) return '#198754';
    if (rate >= 35) return '#fd7e14';
    return '#dc3545';
  }

  getRankIcon(rank: number): string {
    if (rank === 1) return '🥇';
    if (rank === 2) return '🥈';
    if (rank === 3) return '🥉';
    return `#${rank}`;
  }

  // ── Badge helpers ────────────────────────────────────────────────────────

  getPriorityClass(priority?: string): string {
    switch (priority) {
      case 'CRITICAL': return 'badge bg-danger';
      case 'HIGH':     return 'badge bg-warning text-dark';
      case 'MEDIUM':   return 'badge bg-primary';
      default:         return 'badge bg-secondary';
    }
  }

  getStatusClass(status?: string): string {
    switch (status) {
      case 'LOST':      return 'badge bg-danger';
      case 'SEARCHING': return 'badge bg-warning text-dark';
      case 'FOUND':     return 'badge bg-success';
      case 'CLOSED':    return 'badge bg-secondary';
      default:          return 'badge bg-secondary';
    }
  }

  // ── User info ────────────────────────────────────────────────────────────

  get userInitials(): string {
    if (!this.loggedUser) return '?';
    return (this.loggedUser.firstName?.charAt(0) ?? '') + (this.loggedUser.lastName?.charAt(0) ?? '');
  }

  get userFullName(): string {
    if (!this.loggedUser) return 'Unknown';
    return `${this.loggedUser.firstName ?? ''} ${this.loggedUser.lastName ?? ''}`.trim();
  }

  // ── Navigation ───────────────────────────────────────────────────────────

  goBack(): void {
    if (this.itemId) this.router.navigate(['/admin/lost-items', this.itemId]);
    else this.router.navigate(['/admin/lost-items']);
  }
}
