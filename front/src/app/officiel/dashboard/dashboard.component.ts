import {
  Component,
  OnInit,
  OnDestroy,
  ChangeDetectorRef,
  ElementRef,
  ViewChild,
  Injector,
  afterNextRender,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { Chart, registerables } from 'chart.js';
import { AuthService } from '../../core/services/auth.service';
import { GameResultService, GameResult } from '../../core/services/game-result.service';
import { QuizService } from '../../core/services/quiz.service';
import { PhotoService } from '../../core/services/photo.service';
import { AnalyticsService, IncidentStats } from '../../core/services/analytics.service';
import type { User } from '../../core/models/user.model';
import { NgZoneUiSync } from '../../core/services/ng-zone-ui-sync.service';

Chart.register(...registerables);

@Component({
  selector: 'app-off-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css', '../mgmt-shared.css'],
})
export class DashboardComponent implements OnInit, OnDestroy {
  /** CVP route prefix: `/admin` (staff) or `/officiel` (patient shell). */
  cvpPrefix = '/officiel';

  user: User | null = null;
  stats: {
    totalQuizzes?: number;
    totalPhotos?: number;
    avgScore?: number;
    criticalCount?: number;
    totalUsers?: number;
    totalResults?: number;
  } = {};
  recentResults: any[] = [];
  loading = true;

  /** Back-office home: analytics only (no quiz embed). */
  incidentStats: IncidentStats | null = null;
  private charts: Chart[] = [];
  private chartsBuilt = false;
  /** Reprise si les canvas n'étaient pas encore dans le DOM (évite un écran vide jusqu'au F5). */
  private chartLayoutAttempts = 0;
  private static readonly MAX_CHART_LAYOUT_ATTEMPTS = 30;
  private allResultsSnapshot: GameResult[] = [];

  @ViewChild('chartRisk') chartRiskRef?: ElementRef<HTMLCanvasElement>;
  @ViewChild('chartScoreTrend') chartScoreTrendRef?: ElementRef<HTMLCanvasElement>;
  @ViewChild('chartActivity') chartActivityRef?: ElementRef<HTMLCanvasElement>;
  @ViewChild('chartIncidents') chartIncidentsRef?: ElementRef<HTMLCanvasElement>;

  constructor(
    private auth: AuthService,
    private gameResult: GameResultService,
    private quiz: QuizService,
    private photoSvc: PhotoService,
    private analytics: AnalyticsService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private injector: Injector,
    private readonly zoneUi: NgZoneUiSync
  ) {}

  get isStaffAnalyticsHome(): boolean {
    return this.cvpPrefix === '/admin';
  }

  get hasIncidentSeverityData(): boolean {
    const s = this.incidentStats?.bySeverity;
    return !!s && Object.keys(s).length > 0;
  }

  ngOnInit(): void {
    this.user = this.auth.getCurrentUser();
    this.cvpPrefix = this.router.url.startsWith('/admin') ? '/admin' : '/officiel';
    this.zoneUi.scheduleInitialLoad(() => this.loadData());
  }

  ngOnDestroy(): void {
    this.destroyCharts();
  }

  loadData(): void {
    this.loading = true;
    const role = this.auth.getRole();
    const patientId = this.auth.getUserId();

    if (this.isStaffAnalyticsHome) {
      this.loadStaffAnalytics(role);
      return;
    }

    this.quiz.getQuizzes().subscribe({
      next: (q: any) => {
        this.zoneUi.apply(this.cdr, () => {
          this.stats.totalQuizzes = q?.length || 0;
        });
      },
      error: () => {
        this.zoneUi.apply(this.cdr, () => {
          this.stats.totalQuizzes = 0;
        });
      },
    });

    this.photoSvc.getPhotos().subscribe({
      next: (p) => {
        this.zoneUi.apply(this.cdr, () => {
          this.stats.totalPhotos = p?.length || 0;
        });
      },
      error: () => {
        this.zoneUi.apply(this.cdr, () => {
          this.stats.totalPhotos = 0;
        });
      },
    });

    this.gameResult.getAllResults().subscribe({
      next: (results: any[]) => {
        const filtered =
          role === 'PATIENT' && patientId != null
            ? results.filter((r) => r.patientId === patientId)
            : results;
        this.zoneUi.apply(this.cdr, () => {
          this.applyResultStats(filtered);
          this.recentResults = filtered.slice(-5).reverse();
          this.loading = false;
        });
      },
      error: () => {
        this.zoneUi.apply(this.cdr, () => {
          this.loading = false;
        });
      },
    });

    if (role === 'ADMIN') {
      this.auth.getAllUsers().subscribe({
        next: (u) => {
          this.zoneUi.apply(this.cdr, () => {
            this.stats.totalUsers = u?.length || 0;
          });
        },
        error: () => {
          this.zoneUi.apply(this.cdr, () => {
            this.stats.totalUsers = 0;
          });
        },
      });
    }
  }

  private loadStaffAnalytics(role: string | null): void {
    const needsUsers = role === 'ADMIN';
    forkJoin({
      quizzes: this.quiz.getQuizzes().pipe(catchError(() => of([]))),
      photos: this.photoSvc.getPhotos().pipe(catchError(() => of([]))),
      results: this.gameResult.getAllResults().pipe(catchError(() => of([]))),
      incidents: this.analytics.getIncidentStats().pipe(catchError(() => of(null))),
      users: needsUsers
        ? this.auth.getAllUsers().pipe(catchError(() => of([])))
        : of(null),
    }).subscribe({
      next: ({ quizzes, photos, results, incidents, users }) => {
        this.zoneUi.apply(this.cdr, () => {
          const list = (results as GameResult[]) ?? [];
          this.allResultsSnapshot = list;
          this.stats.totalQuizzes = (quizzes as any[])?.length ?? 0;
          this.stats.totalPhotos = (photos as any[])?.length ?? 0;
          this.applyResultStats(list);
          this.recentResults = list.slice(-5).reverse();
          this.incidentStats = incidents;
          if (users && Array.isArray(users)) {
            this.stats.totalUsers = users.length;
          }
          this.loading = false;
          this.chartsBuilt = false;
          this.chartLayoutAttempts = 0;
        });
        this.scheduleStaffChartsBuild();
      },
      error: () => {
        this.zoneUi.apply(this.cdr, () => {
          this.loading = false;
        });
      },
    });
  }

  private applyResultStats(filtered: GameResult[]): void {
    this.stats.totalResults = filtered.length;
    this.stats.avgScore = filtered.length
      ? Math.round(
          filtered.reduce((s, r) => s + (r.weightedScore || 0), 0) / filtered.length
        )
      : 0;
    this.stats.criticalCount = filtered.filter(
      (r) => r.riskLevel === 'CRITICAL' || r.riskLevel === 'HIGH'
    ).length;
  }

  private destroyCharts(): void {
    this.charts.forEach((c) => c.destroy());
    this.charts = [];
  }

  /** Après le rendu du `*ngIf="!loading"` pour que les #chart* existent dans le DOM. */
  private scheduleStaffChartsBuild(): void {
    afterNextRender(
      () => {
        this.buildStaffCharts();
      },
      { injector: this.injector }
    );
  }

  private buildStaffCharts(): void {
    if (!this.isStaffAnalyticsHome) return;

    const riskEl = this.chartRiskRef?.nativeElement;
    const trendEl = this.chartScoreTrendRef?.nativeElement;
    const activityEl = this.chartActivityRef?.nativeElement;
    const incEl = this.chartIncidentsRef?.nativeElement;

    if (!riskEl || !trendEl || !activityEl || !incEl) {
      if (this.chartLayoutAttempts < DashboardComponent.MAX_CHART_LAYOUT_ATTEMPTS) {
        this.chartLayoutAttempts++;
        setTimeout(() => this.buildStaffCharts(), 0);
      }
      return;
    }

    if (this.chartsBuilt) return;

    this.destroyCharts();
    const results = this.allResultsSnapshot;

    const risk = this.aggregateRisk(results);
    const ctxR = this.chartRiskRef?.nativeElement;
    if (ctxR && risk.labels.length) {
      this.charts.push(
        new Chart(ctxR, {
          type: 'doughnut',
          data: {
            labels: risk.labels,
            datasets: [
              {
                data: risk.data,
                backgroundColor: risk.colors,
                borderWidth: 2,
              },
            ],
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { position: 'bottom' } },
          },
        })
      );
    }

    const trend = this.monthlyAvgScoreTrend(results);
    const ctxT = this.chartScoreTrendRef?.nativeElement;
    if (ctxT && trend.labels.length) {
      this.charts.push(
        new Chart(ctxT, {
          type: 'line',
          data: {
            labels: trend.labels,
            datasets: [
              {
                label: 'Avg. score %',
                data: trend.data,
                borderColor: '#099aa7',
                backgroundColor: 'rgba(9, 154, 167, 0.12)',
                fill: true,
                tension: 0.35,
                pointRadius: 4,
              },
            ],
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: {
              y: { beginAtZero: true, max: 100 },
            },
          },
        })
      );
    }

    const act = this.countByActivityType(results);
    const ctxA = this.chartActivityRef?.nativeElement;
    if (ctxA && act.labels.length) {
      this.charts.push(
        new Chart(ctxA, {
          type: 'bar',
          data: {
            labels: act.labels,
            datasets: [
              {
                label: 'Sessions',
                data: act.data,
                backgroundColor: '#6366f1',
                borderRadius: 6,
              },
            ],
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: {
              y: { beginAtZero: true, ticks: { stepSize: 1 } },
            },
          },
        })
      );
    }

    const inc = this.incidentStats;
    const ctxI = this.chartIncidentsRef?.nativeElement;
    if (ctxI && inc && Object.keys(inc.bySeverity ?? {}).length) {
      this.charts.push(
        new Chart(ctxI, {
          type: 'bar',
          data: {
            labels: Object.keys(inc.bySeverity),
            datasets: [
              {
                label: 'Incidents',
                data: Object.values(inc.bySeverity),
                backgroundColor: ['#198754', '#ffc107', '#fd7e14', '#dc3545'],
                borderRadius: 6,
              },
            ],
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: {
              y: { beginAtZero: true, ticks: { stepSize: 1 } },
            },
          },
        })
      );
    }

    this.chartsBuilt = true;
    this.chartLayoutAttempts = 0;
    queueMicrotask(() => this.charts.forEach((c) => c.resize()));
  }

  private aggregateRisk(results: GameResult[]): {
    labels: string[];
    data: number[];
    colors: string[];
  } {
    const palette = {
      low: '#10b981',
      med: '#f59e0b',
      high: '#f97316',
      crit: '#ef4444',
      unk: '#94a3b8',
    };
    const order = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
    const map = new Map<string, number>();
    for (const r of results) {
      const k = (r.riskLevel || 'UNKNOWN').toUpperCase();
      map.set(k, (map.get(k) ?? 0) + 1);
    }
    const labels: string[] = [];
    const data: number[] = [];
    const colors: string[] = [];
    const colorFor = (k: string) => {
      if (k === 'LOW') return palette.low;
      if (k === 'MEDIUM') return palette.med;
      if (k === 'HIGH') return palette.high;
      if (k === 'CRITICAL') return palette.crit;
      return palette.unk;
    };
    for (const k of order) {
      const n = map.get(k) ?? 0;
      if (n > 0) {
        labels.push(k);
        data.push(n);
        colors.push(colorFor(k));
      }
    }
    const unk = map.get('UNKNOWN') ?? 0;
    if (unk > 0) {
      labels.push('Other');
      data.push(unk);
      colors.push(palette.unk);
    }
    return { labels, data, colors };
  }

  private monthlyAvgScoreTrend(results: GameResult[]): { labels: string[]; data: number[] } {
    const map = new Map<string, { sum: number; n: number }>();
    for (const r of results) {
      if (!r.completedAt) continue;
      const d = new Date(r.completedAt);
      const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
      const cur = map.get(key) ?? { sum: 0, n: 0 };
      cur.sum += r.weightedScore ?? 0;
      cur.n += 1;
      map.set(key, cur);
    }
    const keys = [...map.keys()].sort();
    const last = keys.slice(-8);
    return {
      labels: last,
      data: last.map((k) => {
        const v = map.get(k)!;
        return v.n ? Math.round(v.sum / v.n) : 0;
      }),
    };
  }

  private countByActivityType(results: GameResult[]): { labels: string[]; data: number[] } {
    const map = new Map<string, number>();
    const labelFor = (raw: string | undefined) => {
      const t = (raw || 'OTHER').toUpperCase();
      if (t === 'QUIZ') return 'Quiz';
      if (t === 'PHOTO') return 'Photo discrimination';
      return t.charAt(0) + t.slice(1).toLowerCase();
    };
    for (const r of results) {
      const key = labelFor(r.activityType);
      map.set(key, (map.get(key) ?? 0) + 1);
    }
    const priority = ['Quiz', 'Photo discrimination'];
    const labels = [...map.keys()].sort((a, b) => {
      const ia = priority.indexOf(a);
      const ib = priority.indexOf(b);
      if (ia >= 0 && ib >= 0) return ia - ib;
      if (ia >= 0) return -1;
      if (ib >= 0) return 1;
      return a.localeCompare(b);
    });
    return { labels, data: labels.map((k) => map.get(k) ?? 0) };
  }

  getRiskClass(level: string): string {
    const m: Record<string, string> = {
      LOW: 'risk-low',
      MEDIUM: 'risk-med',
      HIGH: 'risk-high',
      CRITICAL: 'risk-crit',
    };
    return m[level] || '';
  }

  get greeting(): string {
    const h = new Date().getHours();
    if (h < 12) return 'Good morning';
    if (h < 18) return 'Good afternoon';
    return 'Good evening';
  }
}
