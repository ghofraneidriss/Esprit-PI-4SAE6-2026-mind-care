import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { GameResultService, GameResult } from '../../core/services/game-result.service';
import { AuthService } from '../../core/services/auth.service';
import { NgZoneUiSync } from '../../core/services/ng-zone-ui-sync.service';
import {
  ADMIN_TABLE_PAGE_SIZE,
  padPageRows,
  slicePage,
  totalPageCount
} from '../../core/utils/admin-table-paging';

/** Regroupe les sessions d’un même quiz / activité pour la vue patient. */
export interface PatientActivityGroup {
  key: string;
  activityId: number;
  activityType: string;
  title: string;
  sessions: GameResult[];
  avgScore: number;
  bestScore: number;
  lastDate: string;
  sessionCount: number;
}

@Component({
  selector: 'app-off-results',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './results.component.html',
  styleUrls: ['./results.component.css', '../mgmt-shared.css'],
})
export class ResultsComponent implements OnInit {
  results: GameResult[] = [];
  filtered: GameResult[] = [];
  loading = true;
  user: any;
  filterRisk = '';
  filterDiff = '';
  searchText = '';

  /** Staff results table: 10 rows per page */
  staffResultsPageIndex = 0;
  readonly staffResultsPageSize = ADMIN_TABLE_PAGE_SIZE;

  /** Vue moderne (patient / aidant) : un groupe par quiz / activité. */
  activityGroups: PatientActivityGroup[] = [];
  selectedActivityKey: string | null = null;
  /** Session sélectionnée pour le panneau correct / erreurs. */
  selectedSession: GameResult | null = null;

  // -- Risk overview per patient (staff only) --
  patientSummaries: any[] = [];
  activeTab: 'results' | 'risk' = 'results';
  sendingEmail: { [id: number]: boolean } = {};
  emailMsg: { [id: number]: string } = {};

  constructor(
    private grSvc: GameResultService,
    private auth: AuthService,
    private cdr: ChangeDetectorRef,
    private readonly zoneUi: NgZoneUiSync
  ) {}

  ngOnInit() {
    this.user = this.auth.currentUser;
    this.zoneUi.scheduleInitialLoad(() => this.load());
  }

  private roleUpper(): string {
    return String(this.user?.role ?? '').trim().toUpperCase();
  }

  get isStaffResultsView(): boolean {
    const r = this.roleUpper();
    return r === 'ADMIN' || r === 'DOCTOR';
  }

  get isAdminRole(): boolean {
    return this.roleUpper() === 'ADMIN';
  }

  get isPatientPersonalView(): boolean {
    if (this.isStaffResultsView) return false;
    const r = this.roleUpper();
    if (r === 'PATIENT') return true;
    if (r === 'CAREGIVER' || r === 'VOLUNTEER') return false;
    const uid = this.auth.getUserId();
    if (uid == null || !this.results.length) return false;
    if (r !== '') return false;
    return this.results.every((x) => Number(x.patientId) === Number(uid));
  }

  /** Aidant / bénévole : mêmes écrans que le patient (cartes), données filtrées aux patients liés. */
  get isFamilyCaretakerView(): boolean {
    const r = this.roleUpper();
    return r === 'CAREGIVER' || r === 'VOLUNTEER';
  }

  /** Cartes + graphiques (pas le tableau admin). */
  get useModernResultsLayout(): boolean {
    if (this.isStaffResultsView) return false;
    if (this.isPatientPersonalView) return true;
    if (this.isFamilyCaretakerView) return true;
    return false;
  }

  get selectedGroup(): PatientActivityGroup | null {
    if (!this.selectedActivityKey) return null;
    return this.activityGroups.find((g) => g.key === this.selectedActivityKey) ?? null;
  }

  /** Sessions du groupe sélectionné, ancien → récent (pour le graphique). */
  get chartSessions(): GameResult[] {
    const g = this.selectedGroup;
    if (!g) return [];
    return [...g.sessions].sort(
      (a, b) => new Date(a.completedAt || 0).getTime() - new Date(b.completedAt || 0).getTime()
    );
  }

  load() {
    this.loading = true;
    const r = this.roleUpper();
    const pid = this.auth.getUserId();

    if (r === 'PATIENT' && pid != null) {
      this.grSvc.getResultsByPatient(pid).subscribe({
        next: (d) => this.onDataLoaded(d || []),
        error: () => this.finishLoadError(),
      });
      return;
    }

    if (r === 'CAREGIVER' && pid != null) {
      this.auth.getPatientsByCaregiver(pid).subscribe({
        next: (patients) => {
          const ids = new Set<number>(patients.map((p) => p.userId));
          ids.add(pid);
          this.grSvc.getAllResults().subscribe({
            next: (d) => {
              const data = (d || []).filter((row) => ids.has(Number(row.patientId)));
              this.onDataLoaded(data);
            },
            error: () => this.finishLoadError(),
          });
        },
        error: () => {
          this.grSvc.getAllResults().subscribe({
            next: (d) => {
              const data = (d || []).filter((row) => Number(row.patientId) === Number(pid));
              this.onDataLoaded(data);
            },
            error: () => this.finishLoadError(),
          });
        },
      });
      return;
    }

    if (r === 'VOLUNTEER' && pid != null) {
      this.auth.getPatientsByVolunteer(pid).subscribe({
        next: (patients) => {
          const ids = new Set<number>(patients.map((p) => p.userId));
          ids.add(pid);
          this.grSvc.getAllResults().subscribe({
            next: (d) => {
              const data = (d || []).filter((row) => ids.has(Number(row.patientId)));
              this.onDataLoaded(data);
            },
            error: () => this.finishLoadError(),
          });
        },
        error: () => {
          this.grSvc.getAllResults().subscribe({
            next: (d) => {
              const data = (d || []).filter((row) => Number(row.patientId) === Number(pid));
              this.onDataLoaded(data);
            },
            error: () => this.finishLoadError(),
          });
        },
      });
      return;
    }

    this.grSvc.getAllResults().subscribe({
      next: (d: GameResult[]) => {
        let data = d || [];
        const filterToSelf =
          r === 'PATIENT' ||
          (r === '' &&
            pid != null &&
            data.length > 0 &&
            data.every((row) => Number(row.patientId) === Number(pid)));
        if (filterToSelf && pid != null) {
          data = data.filter((row) => Number(row.patientId) === Number(pid));
        }
        this.onDataLoaded(data);
      },
      error: () => this.finishLoadError(),
    });
  }

  private finishLoadError(): void {
    this.zoneUi.apply(this.cdr, () => {
      this.loading = false;
    });
  }

  private onDataLoaded(data: GameResult[]): void {
    this.zoneUi.apply(this.cdr, () => {
      this.results = data;
      this.applyFilter();
      if (this.isStaffResultsView) {
        this.buildPatientSummaries();
      } else if (this.useModernResultsLayout) {
        this.selectedActivityKey = null;
        this.selectedSession = null;
        this.buildActivityGroups();
      }
      this.loading = false;
    });
  }

  buildActivityGroups(): void {
    const map = new Map<string, GameResult[]>();
    const splitByPatient = this.isFamilyCaretakerView;
    for (const r of this.results) {
      const k = splitByPatient
        ? `${r.activityType || 'ACT'}_${r.activityId ?? 0}_p${r.patientId ?? 0}`
        : `${r.activityType || 'ACT'}_${r.activityId ?? 0}`;
      if (!map.has(k)) map.set(k, []);
      map.get(k)!.push(r);
    }
    this.activityGroups = Array.from(map.entries()).map(([key, sessions]) => {
      sessions.sort(
        (a, b) =>
          new Date(b.completedAt || 0).getTime() - new Date(a.completedAt || 0).getTime()
      );
      const latest = sessions[0];
      const scores = sessions.map((s) => s.weightedScore ?? 0);
      const titleBase = latest.activityTitle || latest.activityType || 'Activity';
      const title =
        splitByPatient && latest.patientName
          ? `${titleBase} · ${latest.patientName}`
          : titleBase;
      return {
        key,
        activityId: latest.activityId,
        activityType: latest.activityType,
        title,
        sessions,
        avgScore: Math.round(scores.reduce((a, b) => a + b, 0) / scores.length),
        bestScore: Math.max(...scores, 0),
        lastDate: latest.completedAt || '',
        sessionCount: sessions.length,
      };
    });
    this.activityGroups.sort(
      (a, b) => new Date(b.lastDate).getTime() - new Date(a.lastDate).getTime()
    );
    this.selectedActivityKey = this.activityGroups[0]?.key ?? null;
    this.selectedSession = this.selectedGroup?.sessions?.[0] ?? null;
  }

  selectActivity(key: string): void {
    this.selectedActivityKey = key;
    this.selectedSession = this.selectedGroup?.sessions?.[0] ?? null;
    this.cdr.detectChanges();
  }

  selectSession(s: GameResult): void {
    this.selectedSession = s;
  }

  /** Questions non réussies (données agrégées — pas le détail par question en base). */
  mistakeCount(s: GameResult): number {
    const total = s.totalQuestions ?? 0;
    const correct = s.correctAnswers ?? 0;
    return Math.max(0, total - correct);
  }

  correctPct(s: GameResult): number {
    const total = s.totalQuestions ?? 0;
    if (!total) return 0;
    return Math.round(((s.correctAnswers ?? 0) / total) * 100);
  }

  /** Répartition des niveaux de risque pour le graphique (vue patient). */
  riskCounts(g: PatientActivityGroup): { level: string; count: number }[] {
    const levels = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
    return levels
      .map((level) => ({
        level,
        count: g.sessions.filter((s) => s.riskLevel === level).length,
      }))
      .filter((x) => x.count > 0);
  }

  riskPct(count: number, total: number): number {
    if (!total) return 0;
    return Math.round((count / total) * 100);
  }

  buildPatientSummaries() {
    const map = new Map<number, any>();
    for (const r of this.results) {
      const pid = r.patientId;
      if (!map.has(pid)) {
        map.set(pid, {
          patientId: pid,
          patientName: r.patientName || 'Patient #' + pid,
          patientEmail: r.patientEmail || '',
          results: [],
          totalGames: 0,
          avgScore: 0,
          lastRisk: '',
          criticalCount: 0,
          highCount: 0,
          lastDate: '',
        });
      }
      const s = map.get(pid)!;
      s.results.push(r);
      s.totalGames++;
      if (r.riskLevel === 'CRITICAL') s.criticalCount++;
      if (r.riskLevel === 'HIGH') s.highCount++;
    }
    map.forEach((s) => {
      s.avgScore = Math.round(
        s.results.reduce((a: number, r: GameResult) => a + (r.weightedScore || 0), 0) / s.totalGames
      );
      s.results.sort(
        (a: GameResult, b: GameResult) =>
          new Date(b.completedAt || 0).getTime() - new Date(a.completedAt || 0).getTime()
      );
      s.lastRisk = s.results[0]?.riskLevel || 'N/A';
      s.lastDate = s.results[0]?.completedAt || '';
      const recent = s.results.slice(0, 3).map((r: GameResult) => r.weightedScore || 0);
      if (recent.length >= 2) {
        const diff = recent[0] - recent[recent.length - 1];
        s.trend = diff > 5 ? 'IMPROVING' : diff < -5 ? 'DECLINING' : 'STABLE';
      } else {
        s.trend = 'STABLE';
      }
    });
    this.patientSummaries = Array.from(map.values());
    this.patientSummaries.sort((a, b) => {
      const pri: any = { CRITICAL: 0, HIGH: 1, MEDIUM: 2, LOW: 3, 'N/A': 4 };
      const pa = pri[a.lastRisk] ?? 4,
        pb = pri[b.lastRisk] ?? 4;
      if (pa !== pb) return pa - pb;
      return a.avgScore - b.avgScore;
    });
  }

  applyFilter() {
    this.staffResultsPageIndex = 0;
    if (this.useModernResultsLayout) {
      this.filtered = [...this.results];
      return;
    }
    this.filtered = this.results.filter((r) => {
      if (this.filterRisk && r.riskLevel !== this.filterRisk) return false;
      if (this.filterDiff && r.difficulty !== this.filterDiff) return false;
      if (this.searchText) {
        const s = this.searchText.toLowerCase();
        return (
          (r.patientName || '').toLowerCase().includes(s) ||
          (r.activityTitle || '').toLowerCase().includes(s)
        );
      }
      return true;
    });
  }

  get pagedStaffResults(): GameResult[] {
    return slicePage(this.filtered, this.staffResultsPageIndex, this.staffResultsPageSize);
  }

  get staffResultsTableRows(): (GameResult | null)[] {
    if (this.loading || !this.filtered.length) return [];
    return padPageRows(this.pagedStaffResults, this.staffResultsPageSize);
  }

  get staffResultsTotalPages(): number {
    return totalPageCount(this.filtered.length, this.staffResultsPageSize);
  }

  get staffResultsRangeLabel(): string {
    const n = this.filtered.length;
    if (!n) return '';
    const start = this.staffResultsPageIndex * this.staffResultsPageSize + 1;
    const end = Math.min(n, (this.staffResultsPageIndex + 1) * this.staffResultsPageSize);
    return `${start}–${end} of ${n}`;
  }

  staffResultsPrevPage() {
    if (this.staffResultsPageIndex > 0) this.staffResultsPageIndex--;
  }

  staffResultsNextPage() {
    if (this.staffResultsPageIndex < this.staffResultsTotalPages - 1) this.staffResultsPageIndex++;
  }

  get resultsTableColspan(): number {
    if (this.isStaffResultsView) return 9;
    return 7;
  }

  get patientAttentionSessions(): number {
    if (!this.isPatientPersonalView) return 0;
    return this.filtered.filter((r) => r.riskLevel === 'CRITICAL' || r.riskLevel === 'HIGH')
      .length;
  }

  /** Moyenne globale sur toutes les activités (patient). */
  get patientOverallAvg(): number {
    if (!this.results.length) return 0;
    return Math.round(
      this.results.reduce((s, r) => s + (r.weightedScore || 0), 0) / this.results.length
    );
  }

  delete(id: number) {
    if (!confirm('Delete this result?')) return;
    this.grSvc.deleteResult(id).subscribe({ next: () => this.load(), error: () => this.cdr.detectChanges() });
  }

  sendAlert(resultId: number) {
    this.sendingEmail[resultId] = true;
    this.emailMsg[resultId] = '';
    this.cdr.detectChanges();
    this.grSvc.sendAlert(resultId).subscribe({
      next: (res: any) => {
        this.sendingEmail[resultId] = false;
        this.emailMsg[resultId] = res.sent ? 'Email sent' : 'Failed';
        this.cdr.detectChanges();
        setTimeout(() => {
          this.emailMsg[resultId] = '';
          this.cdr.detectChanges();
        }, 4000);
      },
      error: () => {
        this.sendingEmail[resultId] = false;
        this.emailMsg[resultId] = 'Error';
        this.cdr.detectChanges();
        setTimeout(() => {
          this.emailMsg[resultId] = '';
          this.cdr.detectChanges();
        }, 4000);
      },
    });
  }

  getRiskClass(l: string) {
    return { LOW: 'risk-low', MEDIUM: 'risk-med', HIGH: 'risk-high', CRITICAL: 'risk-crit' }[l] || '';
  }

  getRiskIconClass(l: string): string {
    const map: Record<string, string> = {
      LOW: 'ri-checkbox-circle-fill',
      MEDIUM: 'ri-alert-line',
      HIGH: 'ri-error-warning-fill',
      CRITICAL: 'ri-alarm-warning-fill',
    };
    return map[l] || 'ri-question-line';
  }

  getTrendIconClass(t: string): string[] {
    const map: Record<string, string[]> = {
      IMPROVING: ['ri-arrow-up-circle-fill', 'trend-up'],
      DECLINING: ['ri-arrow-down-circle-fill', 'trend-down'],
      STABLE: ['ri-subtract-line', 'trend-stable'],
    };
    return map[t] || ['ri-subtract-line', 'trend-stable'];
  }

  getTrendLabel(t: string) {
    return { IMPROVING: 'Improving', DECLINING: 'Declining', STABLE: 'Stable' }[t] || t;
  }

  get avgScore() {
    if (!this.filtered.length) return 0;
    return Math.round(
      this.filtered.reduce((s, r) => s + (r.weightedScore || 0), 0) / this.filtered.length
    );
  }

  get criticalPatients() {
    return this.patientSummaries.filter((p) => p.lastRisk === 'CRITICAL' || p.lastRisk === 'HIGH')
      .length;
  }

  /** Human-readable label for analytics (quiz vs photo discrimination). */
  activityTypeLabel(t: string | undefined): string {
    const u = String(t || '').toUpperCase();
    if (u === 'QUIZ') return 'Quiz';
    if (u === 'PHOTO') return 'Photo discrimination';
    return t || '—';
  }
}
