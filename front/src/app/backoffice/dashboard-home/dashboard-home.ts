import {
  Component,
  Input,
  OnInit,
  AfterViewInit,
  OnDestroy,
  ElementRef,
  ViewChild,
  ChangeDetectorRef,
  Injector,
  afterNextRender,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Chart, registerables } from 'chart.js';
import { AnalyticsService, IncidentStats } from '../../core/services/analytics.service';
import { NgZoneUiSync } from '../../core/services/ng-zone-ui-sync.service';

Chart.register(...registerables);

@Component({
  selector: 'app-dashboard-home',
  templateUrl: './dashboard-home.html',
  styleUrls: ['../../officiel/mgmt-shared.css', './dashboard-home.css'],
  standalone: true,
  imports: [CommonModule],
})
export class DashboardHomeComponent implements OnInit, AfterViewInit, OnDestroy {
  /** When true, hides the page title block (e.g. analytics opened inside a modal). */
  @Input() embedInModal = false;

  @ViewChild('severityChart') severityChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('statusChart') statusChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('trendChart') trendChartRef!: ElementRef<HTMLCanvasElement>;

  stats: IncidentStats | null = null;
  loading = true;
  chartsRendered = false;
  private chartLayoutAttempts = 0;
  private static readonly MAX_CHART_LAYOUT_ATTEMPTS = 30;

  private charts: Chart[] = [];

  constructor(
    private analyticsService: AnalyticsService,
    private cdr: ChangeDetectorRef,
    private injector: Injector,
    private readonly zoneUi: NgZoneUiSync
  ) {}

  ngOnInit(): void {
    this.analyticsService.getIncidentStats().subscribe({
      next: (data) => {
        this.zoneUi.apply(this.cdr, () => {
          this.stats = data;
          this.loading = false;
          this.chartsRendered = false;
          this.chartLayoutAttempts = 0;
        });
        afterNextRender(() => this.renderCharts(), { injector: this.injector });
      },
      error: () => {
        this.zoneUi.apply(this.cdr, () => {
          this.loading = false;
        });
      },
    });
  }

  ngAfterViewInit(): void {}

  ngOnDestroy(): void {
    this.charts.forEach(c => c.destroy());
  }

  renderCharts(): void {
    if (!this.stats || this.chartsRendered) return;

    const severityCtx = this.severityChartRef?.nativeElement;
    const statusCtx = this.statusChartRef?.nativeElement;
    const trendCtx = this.trendChartRef?.nativeElement;

    if (!severityCtx || !statusCtx || !trendCtx) {
      if (this.chartLayoutAttempts < DashboardHomeComponent.MAX_CHART_LAYOUT_ATTEMPTS) {
        this.chartLayoutAttempts++;
        setTimeout(() => this.renderCharts(), 0);
      }
      return;
    }

    this.chartLayoutAttempts = 0;
    this.charts.forEach((c) => c.destroy());
    this.charts = [];

    if (severityCtx) {
      this.charts.push(new Chart(severityCtx, {
        type: 'bar',
        data: {
          labels: Object.keys(this.stats.bySeverity),
          datasets: [{
            label: 'Incidents',
            data: Object.values(this.stats.bySeverity),
            backgroundColor: ['#198754', '#ffc107', '#fd7e14', '#dc3545'],
            borderRadius: 6
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          layout: { padding: { left: 4, right: 8, top: 4, bottom: 4 } },
          plugins: { legend: { display: false } },
          scales: {
            x: { grid: { display: false } },
            y: { beginAtZero: true, ticks: { stepSize: 1 } }
          }
        }
      }));
    }

    if (statusCtx) {
      this.charts.push(new Chart(statusCtx, {
        type: 'doughnut',
        data: {
          labels: Object.keys(this.stats.byStatus),
          datasets: [{
            data: Object.values(this.stats.byStatus),
            backgroundColor: ['#dc3545', '#0d6efd', '#198754'],
            borderWidth: 2
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          layout: { padding: { left: 4, right: 8, top: 8, bottom: 8 } },
          plugins: { legend: { position: 'bottom' } }
        }
      }));
    }

    if (trendCtx) {
      this.charts.push(new Chart(trendCtx, {
        type: 'line',
        data: {
          labels: Object.keys(this.stats.byMonth),
          datasets: [{
            label: 'Incidents / Month',
            data: Object.values(this.stats.byMonth),
            borderColor: '#0d6efd',
            backgroundColor: 'rgba(13,110,253,0.1)',
            fill: true,
            tension: 0.4,
            pointBackgroundColor: '#0d6efd',
            pointRadius: 5
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          layout: { padding: { left: 4, right: 16, top: 4, bottom: 4 } },
          plugins: { legend: { display: false } },
          scales: {
            x: {
              ticks: {
                maxRotation: 45,
                autoSkip: true,
                maxTicksLimit: 12
              },
              grid: { display: true }
            },
            y: { beginAtZero: true, ticks: { stepSize: 1 } }
          }
        }
      }));
    }

    this.chartsRendered = true;
    queueMicrotask(() => this.charts.forEach((c) => c.resize()));
  }
}
