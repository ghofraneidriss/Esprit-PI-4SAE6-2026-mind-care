import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';
import {
  MovementAlert,
  PatientMovementService
} from '../../core/services/patient-movement.service';
import { GameResult, GameResultService } from '../../core/services/game-result.service';
import { IncidentService } from '../../core/services/incident.service';
import { Incident } from '../../core/models/incident.model';
import { PatientClinicalPdfService } from '../../core/services/patient-clinical-pdf.service';
import { ToastService } from '../../core/services/toast.service';
import {
  ADMIN_TABLE_PAGE_SIZE,
  padPageRows,
  slicePage,
  totalPageCount
} from '../../core/utils/admin-table-paging';
import {
  CLINICAL_MOVEMENT_ALERTS_CRITICAL_THRESHOLD,
  isCriticalClinicalMovementAlertCount,
  movementRiskBand,
  MOVEMENT_RISK_LABEL_EN,
  MovementRiskBand,
  RISK_THERM_ALERT_LOW_MAX,
  RISK_THERM_ALERT_MEDIUM_MAX
} from '../../core/constants/critical-care.constants';

interface CriticalRow {
  id: number;
  firstname: string;
  lastname: string;
  email: string;
  phone?: string;
  role: string;
  /** All persisted movement alerts (same scope as history / PDF). */
  totalMovementAlerts: number;
  createdAt?: string;
}

type SortKey = 'name' | 'email' | 'exits' | 'risk' | 'created';

@Component({
  selector: 'app-critical-patients',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './critical-patients.component.html',
  styleUrls: ['../quiz-management/quiz-management.component.css', '../mgmt-shared.css', './critical-patients.component.css']
})
export class CriticalPatientsComponent implements OnInit {
  readonly threshold = CLINICAL_MOVEMENT_ALERTS_CRITICAL_THRESHOLD;
  readonly riskLowMax = RISK_THERM_ALERT_LOW_MAX;
  readonly riskMediumMax = RISK_THERM_ALERT_MEDIUM_MAX;

  loading = true;
  movementError = false;
  rows: CriticalRow[] = [];

  filterSearch = '';
  sortKey: SortKey = 'exits';
  sortDir: 'asc' | 'desc' = 'desc';

  pageIndex = 0;
  readonly pageSize = ADMIN_TABLE_PAGE_SIZE;

  /** Row PDF loading state. */
  pdfBusyRowId: number | null = null;

  historyOpen = false;
  historyLoading = false;
  historyPatient: CriticalRow | null = null;
  historyAlerts: MovementAlert[] = [];

  private readonly usersApi = `${environment.apiUrl}/users`;

  constructor(
    private http: HttpClient,
    private movement: PatientMovementService,
    private games: GameResultService,
    private incidents: IncidentService,
    private pdf: PatientClinicalPdfService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.load();
  }

  /** Thermometer band for total alert count. */
  riskBand(total: number): MovementRiskBand {
    return movementRiskBand(total);
  }

  riskLabelEn(total: number): string {
    return MOVEMENT_RISK_LABEL_EN[movementRiskBand(total)];
  }

  /** Sort: low=1, medium=2, high=3 (asc = low → high). */
  private riskSortScore(total: number): number {
    const b = movementRiskBand(total);
    return b === 'high' ? 3 : b === 'medium' ? 2 : 1;
  }

  load(): void {
    this.loading = true;
    this.movementError = false;

    forkJoin({
      users: this.http.get<any[]>(this.usersApi),
      counts: this.movement.getTotalMovementAlertCountsByPatient().pipe(
        catchError(() => {
          this.movementError = true;
          return of({} as Record<string, number>);
        })
      )
    }).subscribe({
      next: ({ users, counts }) => {
        const exitByPatient = new Map<number, number>();
        for (const k of Object.keys(counts || {})) {
          exitByPatient.set(Number(k), Number(counts[k]) || 0);
        }

        const mapped = (users || []).map((u) => ({
          ...u,
          id: u.userId ?? u.id,
          firstname: u.firstName ?? u.firstname,
          lastname: u.lastName ?? u.lastname
        }));

        this.rows = mapped
          .filter((u) => String(u.role || '').toUpperCase() === 'PATIENT')
          .map((u) => ({
            id: u.id,
            firstname: u.firstname,
            lastname: u.lastname,
            email: u.email,
            phone: u.phone,
            role: u.role,
            createdAt: u.createdAt,
            totalMovementAlerts: exitByPatient.get(Number(u.id)) ?? 0
          }))
          .filter((r) => isCriticalClinicalMovementAlertCount(r.totalMovementAlerts));

        this.loading = false;
        this.pageIndex = 0;
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading = false;
        this.toast.show('Could not load users.', 'error');
        this.cdr.markForCheck();
      }
    });
  }

  get filteredSorted(): CriticalRow[] {
    let list = [...this.rows];
    const q = (this.filterSearch || '').trim().toLowerCase();
    if (q) {
      list = list.filter(
        (r) =>
          `${r.firstname} ${r.lastname}`.toLowerCase().includes(q) ||
          (r.email || '').toLowerCase().includes(q) ||
          String(r.id).includes(q)
      );
    }
    const dir = this.sortDir === 'asc' ? 1 : -1;
    const key = this.sortKey;
    list.sort((a, b) => {
      let cmp = 0;
      if (key === 'name') {
        cmp = `${a.firstname} ${a.lastname}`.localeCompare(`${b.firstname} ${b.lastname}`);
      } else if (key === 'email') {
        cmp = (a.email || '').localeCompare(b.email || '');
      } else if (key === 'exits') {
        cmp = a.totalMovementAlerts - b.totalMovementAlerts;
      } else if (key === 'risk') {
        cmp =
          this.riskSortScore(a.totalMovementAlerts) - this.riskSortScore(b.totalMovementAlerts);
      } else if (key === 'created') {
        const ta = a.createdAt ? new Date(a.createdAt).getTime() : 0;
        const tb = b.createdAt ? new Date(b.createdAt).getTime() : 0;
        cmp = ta - tb;
      }
      return cmp * dir;
    });
    return list;
  }

  get pagedRows(): CriticalRow[] {
    return slicePage(this.filteredSorted, this.pageIndex, this.pageSize);
  }

  get tableRows(): (CriticalRow | null)[] {
    if (this.loading || !this.filteredSorted.length) return [];
    return padPageRows(this.pagedRows, this.pageSize);
  }

  get totalPages(): number {
    return totalPageCount(this.filteredSorted.length, this.pageSize);
  }

  get rangeLabel(): string {
    const n = this.filteredSorted.length;
    if (!n) return '';
    const start = this.pageIndex * this.pageSize + 1;
    const end = Math.min(n, (this.pageIndex + 1) * this.pageSize);
    return `${start}–${end} of ${n}`;
  }

  setSort(key: SortKey): void {
    if (this.sortKey === key) {
      this.sortDir = this.sortDir === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortKey = key;
      this.sortDir = key === 'exits' || key === 'risk' ? 'desc' : 'asc';
    }
    this.pageIndex = 0;
  }

  sortIndicator(key: SortKey): string {
    if (this.sortKey !== key) return '';
    return this.sortDir === 'asc' ? '↑' : '↓';
  }

  onSearchChange(): void {
    this.pageIndex = 0;
  }

  prevPage(): void {
    if (this.pageIndex > 0) this.pageIndex--;
  }

  nextPage(): void {
    if (this.pageIndex < this.totalPages - 1) this.pageIndex++;
  }

  private clinicalBundle$(patientId: number) {
    return forkJoin({
      alerts: this.movement.getPatientAlerts(patientId).pipe(catchError(() => of<MovementAlert[]>([]))),
      games: this.games.getResultsByPatient(patientId).pipe(catchError(() => of<GameResult[]>([]))),
      inc: this.incidents.getPatientIncidentsHistory(patientId).pipe(catchError(() => of<Incident[]>([])))
    });
  }

  private applySortedClinicalBundle(
    alerts: MovementAlert[] | undefined,
    games: GameResult[] | undefined,
    inc: Incident[] | undefined
  ): {
    allMovementAlerts: MovementAlert[];
    safeZoneExitAlerts: MovementAlert[];
    gameResults: GameResult[];
    incidents: Incident[];
  } {
    const list = [...(alerts || [])].sort(
      (a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
    );
    return {
      allMovementAlerts: list,
      safeZoneExitAlerts: list.filter((a) => String(a.alertType).toUpperCase() === 'OUT_OF_SAFE_ZONE'),
      gameResults: games || [],
      incidents: inc || []
    };
  }

  openHistory(row: CriticalRow): void {
    this.historyPatient = row;
    this.historyOpen = true;
    this.historyLoading = true;
    this.historyAlerts = [];
    this.movement.getPatientAlerts(row.id).subscribe({
      next: (alerts) => {
        this.historyAlerts = [...(alerts || [])].sort(
          (a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
        );
        this.historyLoading = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.historyLoading = false;
        this.toast.show('Could not load movement history.', 'error');
        this.cdr.markForCheck();
      }
    });
  }

  closeHistory(): void {
    this.historyOpen = false;
    this.historyPatient = null;
    this.historyAlerts = [];
  }

  /** Google Maps link embedded in server message text. */
  extractGoogleMapsUrl(message: string | undefined | null): string | null {
    if (!message) return null;
    const match = message.match(/https:\/\/www\.google\.com\/maps\?[^\s)]+/i);
    return match ? match[0] : null;
  }

  /** Short English label for alert type (history modal). */
  alertKindTitle(a: MovementAlert): string {
    const t = String(a.alertType || '').toUpperCase();
    const labels: Record<string, string> = {
      OUT_OF_SAFE_ZONE: 'Left safe zone',
      LEFT_REGISTERED_HOME: 'Left home area',
      IMMOBILE_TOO_LONG: 'No movement',
      GPS_NO_DATA: 'No GPS',
      RAPID_OR_UNUSUAL_MOVEMENT: 'Fast / odd move',
    };
    return labels[t] || t.replace(/_/g, ' ').toLowerCase();
  }

  /** Optional extra line after removing URL and coordinates tail. */
  alertNoteText(a: MovementAlert): string | null {
    let s = (a.message || '').trim();
    s = s.replace(/https:\/\/[^\s)]+/g, '').trim();
    s = s.replace(/\s*[—–-]\s*Position\s*\(patient\):.*$/is, '').trim();
    s = s.replace(/^Test alert:\s*/i, '').replace(/^Simulated alert:\s*/i, '');
    if (/simulated safe-zone exit/i.test(s)) return 'Demo — not live GPS.';
    if (/simulated prolonged immobility/i.test(s)) return 'Demo — not live GPS.';
    if (/simulated abrupt movement/i.test(s)) return 'Demo — speed test.';
    if (/The patient left all safe zones/i.test(s)) return 'Outside every safe area.';
    if (/No GPS data for/i.test(s)) return 'No signal for a while.';
    if (/Patient immobile for more than/i.test(s)) return 'Almost no move in the zone.';
    if (!s) return null;
    return s.length > 90 ? `${s.slice(0, 87)}…` : s;
  }

  /** Compact coordinates when present. */
  alertCoordsLine(a: MovementAlert): string | null {
    const m = (a.message || '').match(/Position\s*\(patient\):\s*([0-9.,\s]+)/i);
    if (m) return m[1].trim().replace(/\s+/g, ' ');
    const url = this.extractGoogleMapsUrl(a.message || '');
    if (url) {
      try {
        const q = new URL(url).searchParams.get('q');
        if (q) return q.replace(/\s+/g, ' ');
      } catch {
        /* ignore */
      }
    }
    return null;
  }

  severityLabel(sev: string | undefined): string {
    const u = String(sev || '').toUpperCase();
    if (u === 'CRITICAL') return 'High';
    if (u === 'WARNING') return 'Watch';
    return u || '—';
  }

  downloadPdfRow(row: CriticalRow): void {
    this.pdfBusyRowId = row.id;
    this.clinicalBundle$(row.id).subscribe({
      next: async ({ alerts, games, inc }) => {
        const b = this.applySortedClinicalBundle(alerts, games, inc);
        try {
          await this.pdf.downloadClinicalSummary({
            patientName: `${row.firstname} ${row.lastname}`,
            patientId: row.id,
            email: row.email,
            phone: row.phone,
            safeZoneExitAlerts: b.safeZoneExitAlerts,
            allMovementAlerts: b.allMovementAlerts,
            gameResults: b.gameResults,
            incidents: b.incidents
          });
          this.toast.show('PDF generated', 'success');
        } catch {
          this.toast.show('Could not generate PDF.', 'error');
        } finally {
          this.pdfBusyRowId = null;
          this.cdr.markForCheck();
        }
      },
      error: () => {
        this.pdfBusyRowId = null;
        this.toast.show('Could not load data for PDF.', 'error');
        this.cdr.markForCheck();
      }
    });
  }
}
