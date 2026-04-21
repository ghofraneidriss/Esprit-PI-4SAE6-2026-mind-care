import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Subject, fromEvent, interval } from 'rxjs';
import { delay, filter, takeUntil } from 'rxjs/operators';
import { PerformanceService, PatientPerformance } from '../../core/services/performance.service';
import { ReportService } from '../../core/services/report.service';
import {
  ADMIN_TABLE_PAGE_SIZE,
  padPageRows,
  slicePage,
  totalPageCount
} from '../../core/utils/admin-table-paging';
import { AuthService } from '../../core/services/auth.service';
import { NgZoneUiSync } from '../../core/services/ng-zone-ui-sync.service';

/** Rafraîchissement silencieux des données (contourne le cache API ~45s). */
const REPORT_AUTO_REFRESH_MS = 30_000;

@Component({
  selector: 'app-off-report',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './report.component.html',
  styleUrls: ['../quiz-management/quiz-management.component.css', '../mgmt-shared.css', './report.component.css']
})
export class ReportComponent implements OnInit, OnDestroy {
  loading = true;
  error = '';

  private readonly destroy$ = new Subject<void>();

  patients: PatientPerformance[] = [];
  reportSearch = '';
  reportPageIndex = 0;
  readonly reportPageSize = ADMIN_TABLE_PAGE_SIZE;
  downloadingId: number | null = null;

  user: any;

  constructor(
    private perfSvc: PerformanceService,
    private reportSvc: ReportService,
    private auth: AuthService,
    private readonly cdr: ChangeDetectorRef,
    private readonly zoneUi: NgZoneUiSync
  ) {}

  ngOnInit() {
    this.user = this.auth.currentUser;
    this.zoneUi.scheduleInitialLoad(() => this.loadPatients());

    interval(REPORT_AUTO_REFRESH_MS)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.loadPatients({ silent: true, forceRefresh: true }));

    fromEvent(document, 'visibilitychange')
      .pipe(
        filter(() => document.visibilityState === 'visible'),
        takeUntil(this.destroy$)
      )
      .subscribe(() => this.loadPatients({ silent: true, forceRefresh: true }));
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadPatients(opts?: { silent?: boolean; forceRefresh?: boolean }) {
    const silent = opts?.silent === true;
    if (!silent) {
      this.loading = true;
    }
    this.perfSvc
      .getAllPerformances({ forceRefresh: opts?.forceRefresh === true })
      .pipe(delay(0))
      .subscribe({
        next: (data) => {
          this.zoneUi.apply(this.cdr, () => {
            this.patients = data ?? [];
            if (!silent) {
              this.loading = false;
            }
          });
        },
        error: () => {
          this.zoneUi.apply(this.cdr, () => {
            if (!silent) {
              this.error = 'Could not load patients';
              this.loading = false;
            }
          });
        }
      });
  }

  downloadPdf(patient: PatientPerformance) {
    this.downloadingId = patient.patientId;

    this.reportSvc.downloadPatientPdf(patient.patientId).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `mindcare-cvp-report-${this.sanitizeName(patient.patientName)}-${patient.patientId}.pdf`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
        this.zoneUi.apply(this.cdr, () => {
          this.downloadingId = null;
        });
      },
      error: () => {
        this.zoneUi.apply(this.cdr, () => {
          this.error = 'Could not generate PDF report';
          this.downloadingId = null;
        });
      }
    });
  }

  getLevelClass(score: number): string {
    if (score >= 80) return 'level-excellent';
    if (score >= 60) return 'level-bon';
    if (score >= 40) return 'level-moyen';
    if (score >= 20) return 'level-faible';
    return 'level-critique';
  }

  getLevelLabel(score: number): string {
    if (score >= 80) return 'Excellent';
    if (score >= 60) return 'Good';
    if (score >= 40) return 'Average';
    if (score >= 20) return 'Low';
    return 'Critical';
  }

  get filteredPatients(): PatientPerformance[] {
    const q = (this.reportSearch || '').trim().toLowerCase();
    if (!q) return this.patients;
    return this.patients.filter(
      (p) =>
        (p.patientName || '').toLowerCase().includes(q) || String(p.patientId).includes(q)
    );
  }

  get pagedReportPatients(): PatientPerformance[] {
    return slicePage(this.filteredPatients, this.reportPageIndex, this.reportPageSize);
  }

  get reportTableRows(): (PatientPerformance | null)[] {
    if (this.loading || !this.filteredPatients.length) return [];
    return padPageRows(this.pagedReportPatients, this.reportPageSize);
  }

  reportRowNumber(rowIndex: number): number {
    return this.reportPageIndex * this.reportPageSize + rowIndex + 1;
  }

  get reportTotalPages(): number {
    return totalPageCount(this.filteredPatients.length, this.reportPageSize);
  }

  get reportRangeLabel(): string {
    const n = this.filteredPatients.length;
    if (!n) return '';
    const start = this.reportPageIndex * this.reportPageSize + 1;
    const end = Math.min(n, (this.reportPageIndex + 1) * this.reportPageSize);
    return `${start}–${end} of ${n}`;
  }

  reportPrevPage() {
    if (this.reportPageIndex > 0) this.reportPageIndex--;
  }

  reportNextPage() {
    if (this.reportPageIndex < this.reportTotalPages - 1) this.reportPageIndex++;
  }

  resetReportSearch() {
    this.reportSearch = '';
    this.reportPageIndex = 0;
  }

  onReportSearchChange() {
    this.reportPageIndex = 0;
  }

  getScoreColor(score: number): string {
    if (score >= 80) return '#22c55e';
    if (score >= 60) return '#667eea';
    if (score >= 40) return '#f6ad55';
    if (score >= 20) return '#fb923c';
    return '#ef4444';
  }

  private sanitizeName(name: string): string {
    return (name || 'patient').replace(/[^a-zA-Z0-9]/g, '-').toLowerCase();
  }
}
