import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { PerformanceService, PatientPerformance, ThemeScore, Recommendation } from '../../core/services/performance.service';
import { AuthService } from '../../core/services/auth.service';
import { NgZoneUiSync } from '../../core/services/ng-zone-ui-sync.service';

@Component({
  selector: 'app-off-performance',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './performance.component.html',
  styleUrls: ['../mgmt-shared.css', './performance.component.css']
})
export class PerformanceComponent implements OnInit {
  loading = true;
  error = '';

  // Admin/Doctor: all patients
  allPerformances: PatientPerformance[] = [];
  selectedPatient: PatientPerformance | null = null;

  // Patient: own performance
  myPerformance: PatientPerformance | null = null;

  user: any;
  isAdminOrDoctor = false;

  constructor(
    private perfSvc: PerformanceService,
    private auth: AuthService,
    private cdr: ChangeDetectorRef,
    private readonly zoneUi: NgZoneUiSync
  ) {}

  ngOnInit() {
    this.user = this.auth.currentUser;
    const r = String(this.user?.role ?? '').trim().toUpperCase();
    this.isAdminOrDoctor = r === 'ADMIN' || r === 'DOCTOR';

    this.zoneUi.scheduleInitialLoad(() => {
      if (this.isAdminOrDoctor) {
        this.loadAll();
      } else {
        this.loadMyPerformance();
      }
    });
  }

  loadAll() {
    this.loading = true;
    this.error = '';
    this.perfSvc.getAllPerformances().subscribe({
      next: (data) => {
        this.zoneUi.apply(this.cdr, () => {
          this.allPerformances = data ?? [];
          this.loading = false;
        });
      },
      error: (err: unknown) => {
        this.zoneUi.apply(this.cdr, () => {
          this.error = this.mapPerfHttpError(err);
          this.loading = false;
        });
      },
    });
  }

  loadMyPerformance() {
    this.loading = true;
    this.error = '';
    const pid = this.auth.getUserId();
    if (pid == null) {
      this.zoneUi.apply(this.cdr, () => {
        this.error = 'Invalid session — please sign in again.';
        this.loading = false;
      });
      return;
    }
    this.perfSvc.getPatientPerformance(pid).subscribe({
      next: (data) => {
        this.zoneUi.apply(this.cdr, () => {
          this.myPerformance = data;
          this.loading = false;
        });
      },
      error: (err: unknown) => {
        this.zoneUi.apply(this.cdr, () => {
          this.error = this.mapPerfHttpError(err);
          this.loading = false;
        });
      },
    });
  }

  retryLoad(): void {
    this.error = '';
    this.perfSvc.clearPerformanceCaches();
    if (this.isAdminOrDoctor) {
      if (this.selectedPatient) {
        return;
      }
      this.loadAll();
    } else {
      this.loadMyPerformance();
    }
  }

  private mapPerfHttpError(err: unknown): string {
    const e = err as { code?: string; message?: string; status?: number; error?: unknown };
    if (e?.code === 'PERF_TIMEOUT' || e?.message === 'PERF_TIMEOUT') {
      return 'Timed out: the activities service is not responding. Ensure the gateway (port 8080), Eureka, and the activities microservice are running, then try again.';
    }
    if (e?.message?.includes?.('Timeout')) {
      return 'Timed out: the server took too long to respond. Please try again.';
    }
    if (e?.status === 0) {
      return 'Could not reach the server (network or proxy). Check that the backend is running on the expected port.';
    }
    if (e?.status === 404) {
      return 'Resource not found — check the /api/performance API route.';
    }
    if (e?.status === 503 || e?.status === 502) {
      return 'Service temporarily unavailable (gateway or microservice). Try again shortly.';
    }
    return 'Error loading performance analysis.';
  }

  selectPatient(p: PatientPerformance) {
    this.selectedPatient = p;
  }

  closeDetail() {
    this.selectedPatient = null;
  }

  getLevelClass(level: string): string {
    const map: any = { EXCELLENT: 'level-excellent', BON: 'level-bon', MOYEN: 'level-moyen', FAIBLE: 'level-faible', CRITIQUE: 'level-critique' };
    return map[level] || '';
  }

  getLevelLabel(level: string): string {
    const map: any = { EXCELLENT: 'Excellent', BON: 'Good', MOYEN: 'Average', FAIBLE: 'Low', CRITIQUE: 'Critical' };
    return map[level] || level;
  }

  getTrendIconClass(trend: string): string[] {
    const map: Record<string, string[]> = {
      UP: ['ri-arrow-up-circle-fill', 'trend-up'],
      DOWN: ['ri-arrow-down-circle-fill', 'trend-down'],
      STABLE: ['ri-subtract-line', 'trend-stable'],
      INSUFFICIENT: ['ri-question-line', 'trend-muted'],
    };
    return map[trend] || ['ri-question-line', 'trend-muted'];
  }

  /** Icône thème “chic” (Remix) selon le libellé — API peut encore envoyer un emoji dans icon */
  getThemeIconClass(label: string): string {
    const l = (label || '').toLowerCase();
    if (l.includes('mém') || l.includes('memo') || l.includes('memory')) return 'ri-mental-health-line';
    if (l.includes('log')) return 'ri-node-tree';
    if (l.includes('math') || l.includes('calcul')) return 'ri-calculator-line';
    if (l.includes('lang') || l.includes('voc')) return 'ri-translate-2';
    if (l.includes('attention') || l.includes('focus')) return 'ri-focus-3-line';
    return 'ri-questionnaire-line';
  }

  getPriorityRemixIcon(priority: string): string {
    const map: Record<string, string> = {
      CRITICAL: 'ri-alarm-warning-fill',
      HIGH: 'ri-error-warning-fill',
      MEDIUM: 'ri-lightbulb-flash-line',
      LOW: 'ri-checkbox-circle-fill',
    };
    return map[priority] || 'ri-flag-line';
  }

  getTrendLabel(trend: string): string {
    const map: any = { UP: 'Up', DOWN: 'Down', STABLE: 'Stable', INSUFFICIENT: 'Insufficient data' };
    return map[trend] || trend;
  }

  /** Vue patient CVP : textes sans nom affiché. */
  get isPatientRole(): boolean {
    return String(this.user?.role ?? '').trim().toUpperCase() === 'PATIENT';
  }

  getPriorityClass(priority: string): string {
    const map: any = { CRITICAL: 'priority-critical', HIGH: 'priority-high', MEDIUM: 'priority-medium', LOW: 'priority-low' };
    return map[priority] || '';
  }

  getScoreColor(score: number): string {
    if (score >= 80) return '#22c55e';
    if (score >= 60) return '#667eea';
    if (score >= 40) return '#f6ad55';
    if (score >= 20) return '#fb923c';
    return '#ef4444';
  }

}
