import { Injectable, inject } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { BehaviorSubject, Observable, Subscription, of } from 'rxjs';
import { catchError, filter, map, switchMap, tap } from 'rxjs/operators';
import { AuthService } from '../../core/services/auth.service';
import { ToastService } from '../../core/services/toast.service';
import { AppUser, PatientMovementService } from '../../core/services/patient-movement.service';
import { User } from '../../core/models/user.model';

export interface MovementAssistantMessage {
  id: string;
  patientId: number;
  patientLabel: string;
  title: string;
  body: string;
  severity: 'CRITICAL' | 'WARNING' | 'INFO';
  createdAt: string;
  alertType?: string;
  acknowledged?: boolean;
  alertId?: number;
  source: 'api' | 'sim';
  uiRead?: boolean;
}

/**
 * Floating movement assistant: demo buttons also **persist** alerts via
 * `POST /api/locations/report` (`SIMULATION_*` sources) so critical lists / PDF / history match the DB.
 */
@Injectable({ providedIn: 'root' })
export class MovementAlertAssistantService {
  private readonly auth = inject(AuthService);
  private readonly movement = inject(PatientMovementService);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  private readonly messagesSubject = new BehaviorSubject<MovementAssistantMessage[]>([]);
  private readonly open$ = new BehaviorSubject(false);

  private routerSub: Subscription | null = null;
  private patientIds: number[] = [];
  private patientLabels = new Map<number, string>();
  /** Avoid redundant patient fetches when route changes but context is unchanged. */
  private lastPatientContextKey = '';
  private alarmInterval: ReturnType<typeof setInterval> | null = null;
  private audioCtx: AudioContext | null = null;
  private alarmAltFreq = false;

  readonly messages = this.messagesSubject.asObservable();
  readonly unreadCount$ = this.messagesSubject.pipe(
    map((m) => m.filter((x) => !x.uiRead).length)
  );
  readonly panelOpen = this.open$.asObservable();

  constructor() {
    this.routerSub = this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe(() => this.syncSession());
    this.syncSession();
  }

  get isEligibleRole(): boolean {
    const r = this.auth.getRole()?.toUpperCase() ?? '';
    return r === 'CAREGIVER' || r === 'VOLUNTEER' || r === 'DOCTOR' || r === 'ADMIN';
  }

  get messagesSnapshot(): MovementAssistantMessage[] {
    return this.messagesSubject.value;
  }

  isPanelOpen(): boolean {
    return this.open$.value;
  }

  setPanelOpen(open: boolean): void {
    this.open$.next(open);
    if (open) {
      this.markAllUiRead();
    }
  }

  togglePanel(): void {
    this.setPanelOpen(!this.open$.value);
  }

  unreadBadgeCount(): number {
    return this.messagesSubject.value.filter((m) => !m.uiRead).length;
  }

  private markAllUiRead(): void {
    const next = this.messagesSubject.value.map((m) => ({ ...m, uiRead: true }));
    this.messagesSubject.next(next);
  }

  private syncSession(): void {
    const u = this.auth.getCurrentUser();
    if (!u || !this.isEligibleRole) {
      this.stop();
      return;
    }
    this.loadPatientContextForDemo().subscribe({
      error: () => {},
    });
  }

  private stop(): void {
    this.lastPatientContextKey = '';
    this.patientIds = [];
    this.patientLabels.clear();
    this.messagesSubject.next([]);
    this.stopAlarmSound();
  }

  /** Resolve linked patients so demo buttons can label the first patient — no API alert polling. */
  private loadPatientContextForDemo(): Observable<void> {
    return this.resolvePatientContext().pipe(
      tap((ctx) => {
        const uid = this.auth.getCurrentUser()?.userId ?? 0;
        const key = `${uid}|${ctx.ids.slice().sort((a, b) => a - b).join(',')}`;
        if (key === this.lastPatientContextKey) {
          return;
        }
        this.lastPatientContextKey = key;
        this.patientIds = ctx.ids;
        this.patientLabels = ctx.labels;
      }),
      map(() => undefined)
    );
  }

  private resolvePatientContext(): Observable<{ ids: number[]; labels: Map<number, string> }> {
    const u = this.auth.getCurrentUser();
    if (!u) {
      return of({ ids: [], labels: new Map() });
    }
    const role = u.role;
    if (role === 'CAREGIVER') {
      return this.auth.getPatientsByCaregiver(u.userId).pipe(
        map((rows) => this.usersToContext(rows)),
        catchError(() => of({ ids: [], labels: new Map() }))
      );
    }
    if (role === 'VOLUNTEER') {
      return this.auth.getPatientsByVolunteer(u.userId).pipe(
        map((rows) => this.usersToContext(rows)),
        catchError(() => of({ ids: [], labels: new Map() }))
      );
    }
    if (role === 'ADMIN' || role === 'DOCTOR') {
      return this.movement.getPatients().pipe(
        map((patients) => this.appUsersToContext(patients)),
        catchError(() => of({ ids: [], labels: new Map() }))
      );
    }
    return of({ ids: [], labels: new Map() });
  }

  private usersToContext(users: User[]): { ids: number[]; labels: Map<number, string> } {
    const labels = new Map<number, string>();
    for (const x of users || []) {
      const id = Number(x.userId);
      if (!Number.isFinite(id) || id <= 0) continue;
      labels.set(id, `${x.firstName ?? ''} ${x.lastName ?? ''}`.trim() || `Patient #${id}`);
    }
    return { ids: [...labels.keys()], labels };
  }

  private appUsersToContext(patients: AppUser[]): { ids: number[]; labels: Map<number, string> } {
    const labels = new Map<number, string>();
    for (const p of patients || []) {
      const id = Number(p.id);
      if (!Number.isFinite(id) || id <= 0) continue;
      labels.set(id, `${p.firstname ?? ''} ${p.lastname ?? ''}`.trim() || `Patient #${id}`);
    }
    return { ids: [...labels.keys()], labels };
  }

  /** ~5 s alternating beeps (demo buttons only). Stops any previous run. */
  playAttentionAlarm(): void {
    this.stopAlarmSound();
    if (typeof window === 'undefined') {
      return;
    }
    try {
      const Ctor = window.AudioContext || (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
      if (!Ctor) {
        return;
      }
      if (!this.audioCtx) {
        this.audioCtx = new Ctor();
      }
      if (this.audioCtx.state === 'suspended') {
        void this.audioCtx.resume();
      }

      const extraBeepsAfterFirst = 10;
      const stepMs = 500;

      const beep = () => {
        if (!this.audioCtx) {
          return;
        }
        this.alarmAltFreq = !this.alarmAltFreq;
        const freq = this.alarmAltFreq ? 880 : 520;
        const t0 = this.audioCtx.currentTime;
        const osc = this.audioCtx.createOscillator();
        const gain = this.audioCtx.createGain();
        osc.type = 'sine';
        osc.frequency.value = freq;
        gain.gain.setValueAtTime(0.0001, t0);
        gain.gain.exponentialRampToValueAtTime(0.12, t0 + 0.02);
        gain.gain.exponentialRampToValueAtTime(0.0001, t0 + 0.14);
        osc.connect(gain);
        gain.connect(this.audioCtx.destination);
        osc.start(t0);
        osc.stop(t0 + 0.16);
      };

      let ticks = 0;
      beep();
      this.alarmInterval = window.setInterval(() => {
        ticks += 1;
        if (ticks > extraBeepsAfterFirst) {
          this.stopAlarmSound();
          return;
        }
        beep();
      }, stepMs);
    } catch {
      // ignore (no autoplay / unsupported)
    }
  }

  private stopAlarmSound(): void {
    if (this.alarmInterval != null) {
      clearInterval(this.alarmInterval);
      this.alarmInterval = null;
    }
  }

  /**
   * Persists `OUT_OF_SAFE_ZONE` via `POST /api/locations/report` (`SIMULATION_OUT_OF_ZONE`).
   * @param explicitPatientId optional — use the patient selected on Movement (admin/doctor); otherwise first linked patient.
   */
  simulateOutOfZone(explicitPatientId?: number): void {
    const pid = explicitPatientId ?? this.patientIds[0];
    if (!pid) {
      this.toast.show('No patient — select a patient on Movement or link a patient first.', 'info');
      return;
    }
    const label = this.patientLabels.get(pid) ?? `Patient #${pid}`;
    this.movement
      .getLatestLocation(pid)
      .pipe(
        catchError(() => of(null)),
        switchMap((loc) => {
          const baseLat = loc?.latitude != null ? Number(loc.latitude) : 36.85;
          const baseLon = loc?.longitude != null ? Number(loc.longitude) : 10.25;
          return this.movement.reportLocation({
            patientId: pid,
            latitude: baseLat + 0.08,
            longitude: baseLon + 0.08,
            source: 'SIMULATION_OUT_OF_ZONE',
            accuracyMeters: 12,
          });
        })
      )
      .subscribe({
        next: () => {
          const msg: MovementAssistantMessage = {
            id: `sim-${Date.now()}-zone`,
            patientId: pid,
            patientLabel: label,
            title: 'Distance / safe zone',
            body:
              'Simulation saved to server: safe-zone exit test alert. Open itinerary if you need directions.',
            severity: 'CRITICAL',
            createdAt: new Date().toISOString(),
            source: 'sim',
            uiRead: false,
          };
          this.unshift(msg);
          this.playAttentionAlarm();
          this.open$.next(true);
          this.toast.show('Simulation recorded — movement alert stored (traceable in Critical patients).', 'success');
        },
        error: (e) => {
          this.toast.showHttpError(e, 'Could not save simulation — is movement-service running?');
        },
      });
  }

  /** Persists `IMMOBILE_TOO_LONG` via `SIMULATION_IMMOBILE`. */
  simulateImmobile(explicitPatientId?: number): void {
    const pid = explicitPatientId ?? this.patientIds[0];
    if (!pid) {
      this.toast.show('No patient — select a patient on Movement or link a patient first.', 'info');
      return;
    }
    const label = this.patientLabels.get(pid) ?? `Patient #${pid}`;
    this.movement
      .getLatestLocation(pid)
      .pipe(
        catchError(() => of(null)),
        switchMap((loc) => {
          const lat = loc?.latitude != null ? Number(loc.latitude) : 36.85;
          const lon = loc?.longitude != null ? Number(loc.longitude) : 10.25;
          return this.movement.reportLocation({
            patientId: pid,
            latitude: lat,
            longitude: lon,
            source: 'SIMULATION_IMMOBILE',
            accuracyMeters: 8,
          });
        })
      )
      .subscribe({
        next: () => {
          const msg: MovementAssistantMessage = {
            id: `sim-${Date.now()}-still`,
            patientId: pid,
            patientLabel: label,
            title: 'No movement (10 min)',
            body: 'Simulation saved to server: immobility test alert.',
            severity: 'WARNING',
            createdAt: new Date().toISOString(),
            source: 'sim',
            uiRead: false,
          };
          this.unshift(msg);
          this.playAttentionAlarm();
          this.open$.next(true);
          this.toast.show('Simulation recorded — movement alert stored.', 'success');
        },
        error: (e) => {
          this.toast.showHttpError(e, 'Could not save simulation — is movement-service running?');
        },
      });
  }

  private unshift(m: MovementAssistantMessage): void {
    const cur = this.messagesSubject.value.filter((x) => x.id !== m.id);
    this.messagesSubject.next([m, ...cur].slice(0, 50));
  }

  dismissMessage(id: string): void {
    this.messagesSubject.next(this.messagesSubject.value.filter((m) => m.id !== id));
  }

  acknowledgeApiMessage(id: string): void {
    const m = this.messagesSubject.value.find((x) => x.id === id && x.alertId != null);
    if (!m?.alertId) {
      return;
    }
    this.movement.acknowledgeAlert(m.alertId).subscribe({
      next: (updated) => {
        const next = this.messagesSubject.value.map((x) =>
          x.id === id ? { ...x, acknowledged: updated.acknowledged } : x
        );
        this.messagesSubject.next(next);
      },
    });
  }

  openItineraryToPatient(msg: MovementAssistantMessage): void {
    this.movement.getLatestLocation(msg.patientId).subscribe({
      next: (loc) => {
        const open = (url: string) => window.open(url, '_blank', 'noopener,noreferrer');
        if (!loc) {
          open(
            `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(msg.patientLabel)}`
          );
          return;
        }
        const dLat = loc.latitude;
        const dLon = loc.longitude;
        if (!navigator.geolocation) {
          open(`https://www.google.com/maps/dir/?api=1&destination=${dLat},${dLon}&travelmode=driving`);
          return;
        }
        navigator.geolocation.getCurrentPosition(
          (pos) => {
            const oLat = pos.coords.latitude;
            const oLon = pos.coords.longitude;
            open(
              `https://www.google.com/maps/dir/?api=1&origin=${oLat},${oLon}&destination=${dLat},${dLon}&travelmode=driving`
            );
          },
          () => open(`https://www.google.com/maps/dir/?api=1&destination=${dLat},${dLon}&travelmode=driving`),
          { enableHighAccuracy: true, timeout: 12000, maximumAge: 0 }
        );
      },
      error: () => {
        window.open(
          `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(msg.patientLabel)}`,
          '_blank',
          'noopener,noreferrer'
        );
      },
    });
  }

  requestNotificationPermission(): void {
    if (typeof window === 'undefined' || !('Notification' in window)) {
      return;
    }
    if (Notification.permission === 'default') {
      Notification.requestPermission().catch(() => {});
    }
  }
}
