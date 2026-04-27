import { ChangeDetectorRef, Component, NgZone, OnDestroy, OnInit, ViewRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { catchError, forkJoin, map, of, Subscription } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import {
  AppUser,
  LocationPing,
  MovementAlert,
  PatientLocationStatus,
  PatientMovementService,
  PatientTrackingDashboard,
  SafeZone,
} from '../../core/services/patient-movement.service';
import { environment } from '../../../environments/environment';
import { PatientTrackMapComponent } from './patient-track-map.component';
import { User } from '../../core/models/user.model';
import { MovementAlertAssistantService } from '../../shared/movement-alert-assistant/movement-alert-assistant.service';
import { SpeechService } from '../../core/services/speech.service';
import { ToastService, ToastKind } from '../../core/services/toast.service';

function haversineMeters(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const R = 6371000;
  const toRad = (d: number) => (d * Math.PI) / 180;
  const dLat = toRad(lat2 - lat1);
  const dLon = toRad(lon2 - lon1);
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.sin(dLon / 2) ** 2;
  return 2 * R * Math.asin(Math.min(1, Math.sqrt(a)));
}

@Component({
  selector: 'app-movement-monitoring',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, PatientTrackMapComponent],
  templateUrl: './movement-monitoring.component.html',
  styleUrls: ['./movement-monitoring.component.css']
})
export class MovementMonitoringComponent implements OnInit, OnDestroy {
  user: any;
  patients: AppUser[] = [];
  selectedPatientId = 0;

  latestLocation: LocationPing | null = null;
  history: LocationPing[] = [];
  alerts: MovementAlert[] = [];
  safeZones: SafeZone[] = [];
  trackingDashboard: PatientTrackingDashboard | null = null;
  locationStatus: PatientLocationStatus | null = null;

  /** Staff GPS for in-map pickup route (doctor / caregiver / volunteer). */
  staffJoinPoint: { latitude: number; longitude: number } | null = null;
  /** Bumped when staff overlay changes so the map redraws even between polls. */
  mapOverlayRevision = 0;

  loading = false;
  reporting = false;
  pollRef: any;

  backendOffline = false;
  offlineReason = '';
  consecutiveApiErrors = 0;
  readonly maxApiErrorsBeforePause = 2;
  lastSuccessfulSyncAt: Date | null = null;

  /** Bumped after each successful data sync so the map always redraws (dynamic path / marker). */
  liveSyncRevision = 0;

  readonly pollIntervalMs = 4000;

  get trackingComplianceLabel(): string {
    const compliance = (this.trackingDashboard?.locationCompliance || '').trim();
    if (compliance) {
      return compliance.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (c) => c.toUpperCase());
    }
    if (this.locationStatus?.insideAnySafeZone === true) {
      return 'Inside safe zone';
    }
    if (this.locationStatus?.insideAnySafeZone === false) {
      return 'Outside safe zone';
    }
    return 'Unknown';
  }

  /** Linked caregiver / volunteer display names (from profile). */
  linkedCaregiverName: string | null = null;
  linkedVolunteerName: string | null = null;

  /** Index in `safeZones` for “directions to safe zone” when several zones exist. */
  safeZoneDirectionIndex = 0;
  private lastSafeZonesListKey = '';

  /** Modal: zones only (patient) or patient + zones (caregiver / volunteer / staff). */
  directionsModalOpen = false;
  /** `patient` = list safe zones only. `staff` = choose patient position or a safe zone. */
  directionsModalMode: 'patient' | 'staff' = 'patient';
  /** Staff: navigate to patient pin or to safe zone index. */
  staffDirectionsTarget: 'patient' | number = 'patient';

  /** Speech-to-text: pick destination in the directions modal (same engine as quiz answers). */
  readonly directionsVoiceSupported: boolean;
  directionsVoiceListening = false;
  directionsVoiceTranscript = '';
  directionsVoiceFeedback = '';
  directionsVoiceFeedbackType: 'success' | 'error' | '' = '';
  private directionsVoiceSubs: Subscription[] = [];

  /** Help text when `offlineReason` is empty (e.g. non-network error). */
  get offlineHelpHint(): string {
    if (environment.useMovementLocalizationDirect) {
      return 'Start localization-service (8085), movement-service (8086), and MySQL, then try again.';
    }
    return 'Start Eureka, the API Gateway (8080), and movement-service (8086), then try again.';
  }

  private browserNotificationsEnabled = false;
  private readonly notificationRepeatMs = 30000;
  private lastNotificationAtByAlertId = new Map<number, number>();

  constructor(
    private auth: AuthService,
    private movement: PatientMovementService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private ngZone: NgZone,
    private movementAssistant: MovementAlertAssistantService,
    private speechSvc: SpeechService,
    private toast: ToastService
  ) {
    this.directionsVoiceSupported = this.speechSvc.isSupported;
  }

  ngOnInit(): void {
    this.user = this.auth.currentUser;
    if (!this.user) {
      this.router.navigate(['/login-cover']);
      return;
    }

    this.initDirectionsVoice();

    if (!this.isPatient) {
      this.initializeBrowserNotifications();
    }

    if (this.isCaregiver) {
      this.loadCaregiverPatients();
      return;
    }

    if (this.isVolunteer) {
      this.loadVolunteerPatients();
      return;
    }

    if (this.isDoctorOrAdmin) {
      this.movement.getPatients().subscribe({
        next: (patients) => {
          this.patients = patients || [];
          if (!this.patients.length) {
            this.showMsg('No patient available for tracking.');
            this.loading = false;
            this.selectedPatientId = 0;
            this.refreshView();
            return;
          }
          // One patient: auto-select. Several: explicit choice required.
          this.selectedPatientId =
            this.patients.length === 1 ? this.patients[0].id : 0;
          if (!this.selectedPatientId) {
            this.showMsg('Please select a patient to load tracking and alerts.', 'warn');
            this.loading = false;
            this.refreshView();
            return;
          }
          this.refresh();
          this.startPolling();
          this.refreshView();
        },
        error: () => {
          this.showMsg('Could not load the patient list.');
          this.loading = false;
          this.refreshView();
        }
      });
      return;
    }

    this.initializePatientView();
  }

  ngOnDestroy(): void {
    this.stopPolling();
    this.speechSvc.stop();
    this.directionsVoiceSubs.forEach((s) => s.unsubscribe());
  }

  get isDoctorOrAdmin(): boolean {
    const role = this.user?.role;
    return role === 'ADMIN' || role === 'DOCTOR';
  }

  get isCaregiver(): boolean {
    return this.user?.role === 'CAREGIVER';
  }

  get isVolunteer(): boolean {
    return this.user?.role === 'VOLUNTEER';
  }

  /** Jury demo: English simulation buttons → floating movement assistant. */
  get showMovementDemoSim(): boolean {
    return !this.isPatient && (this.isCaregiver || this.isVolunteer || this.isDoctorOrAdmin);
  }

  simulateDemoOutOfZone(): void {
    const pid = this.selectedPatientId;
    if (!pid) {
      this.toast.show('Select a patient in the dropdown first.', 'info');
      return;
    }
    this.movementAssistant.simulateOutOfZone(pid);
  }

  simulateDemoImmobile(): void {
    const pid = this.selectedPatientId;
    if (!pid) {
      this.toast.show('Select a patient in the dropdown first.', 'info');
      return;
    }
    this.movementAssistant.simulateImmobile(pid);
  }

  /** Shows patient picker: staff, caregiver, or volunteer. */
  get isAdminOrDoctor(): boolean {
    const role = this.user?.role;
    return role === 'ADMIN' || role === 'DOCTOR' || role === 'CAREGIVER' || role === 'VOLUNTEER';
  }

  /** Can use Call / WhatsApp links on movement alerts (not the patient themselves). */
  get canContactPatientFromAlerts(): boolean {
    return this.isAdminOrDoctor;
  }

  get isAdmin(): boolean {
    return this.user?.role === 'ADMIN';
  }

  get isPatient(): boolean {
    return this.user?.role === 'PATIENT';
  }

  get mapLink(): string {
    if (!this.latestLocation) return '';
    const la = this.latestLocation.latitude;
    const lo = this.latestLocation.longitude;
    return `https://www.openstreetmap.org/?mlat=${la}&mlon=${lo}#map=16/${la}/${lo}`;
  }

  /** Google Maps (turn-by-turn). External OSM page does not show app safe zones or routes. */
  get googleMapsPatientUrl(): string {
    if (!this.latestLocation) return '';
    const { latitude, longitude } = this.latestLocation;
    return `https://www.google.com/maps/dir/?api=1&destination=${latitude},${longitude}&travelmode=driving`;
  }

  get showSafeZonesLink(): boolean {
    const r = this.user?.role;
    return r === 'ADMIN' || r === 'DOCTOR' || r === 'CAREGIVER' || r === 'VOLUNTEER';
  }

  get safeZonesRouterLink(): string {
    const r = this.user?.role;
    return r === 'ADMIN' || r === 'DOCTOR' ? '/admin/safe-zones' : '/safe-zones';
  }

  formatTimeShort(iso: string | undefined): string {
    if (!iso) return '—';
    try {
      const d = new Date(iso);
      return d.toLocaleString('en-US', { dateStyle: 'short', timeStyle: 'short' });
    } catch {
      return iso;
    }
  }

  formatSpeed(n: number | undefined | null): string {
    if (n == null || Number.isNaN(Number(n))) return '—';
    return `${Number(n).toFixed(1)} km/h`;
  }

  /** Human-readable alert title (English). */
  formatAlertTitle(alertType: string): string {
    const titles: Record<string, string> = {
      OUT_OF_SAFE_ZONE: 'Left safe zone',
      LEFT_REGISTERED_HOME: 'Left registered home',
      RAPID_OR_UNUSUAL_MOVEMENT: 'Rapid or unusual movement',
      GPS_NO_DATA: 'No GPS signal',
      IMMOBILE_TOO_LONG: 'Immobile for too long',
    };
    const t = alertType?.trim();
    if (titles[t]) return titles[t];
    return t
      ? t
          .replace(/_/g, ' ')
          .toLowerCase()
          .replace(/\b\w/g, (c) => c.toUpperCase())
      : 'Alert';
  }

  /** Normalize legacy French API messages to English for display. */
  formatAlertMessage(message: string): string {
    let m = (message || '').trim();
    const replacements: [RegExp, string][] = [
      [
        /Changement brusque detecte\s*\(([^)]+)\)/i,
        'Sudden movement detected ($1)',
      ],
      [/Changement brusque simule\s*\(([^)]+)\)/i, 'Simulated sudden movement ($1)'],
      [/Changement brusque simule\.?/i, 'Simulated sudden movement.'],
      [/Alerte test:\s*sortie de zone simulee\.?/i, 'Test: simulated safe-zone exit.'],
      [/Alerte test:\s*changement brusque simule/i, 'Test: simulated abrupt movement'],
      [/Alerte test de mouvement simule\.?/i, 'Test: simulated movement.'],
      [/Le patient est sorti des zones autorisees\.?/i, 'The patient left all safe zones.'],
      [/Patient immobile depuis plus de (\d+) secondes\.?/i, 'Patient immobile for more than $1 seconds.'],
      [/Aucune donnee GPS recue pour ce patient\.?/i, 'No GPS data received for this patient.'],
      [/Le GPS n'envoie plus de donnees depuis (\d+) minutes\.?/i, 'No GPS data for $1 minutes.'],
    ];
    for (const [re, rep] of replacements) {
      m = m.replace(re, rep);
    }
    return m;
  }

  /** Label for a safe zone chip (name or fallback). */
  safeZoneChipLabel(z: SafeZone, index: number): string {
    const n = (z.name || '').trim();
    const base = n || `Zone ${index + 1}`;
    return z.homeReference ? `Home: ${base}` : base;
  }

  /** Currently selected safe zone for Google Maps directions. */
  get selectedSafeZoneForDirections(): SafeZone | null {
    const zones = this.safeZones || [];
    if (!zones.length) {
      return null;
    }
    const i = Math.min(Math.max(0, this.safeZoneDirectionIndex), zones.length - 1);
    return zones[i] ?? null;
  }

  selectSafeZoneForDirections(index: number): void {
    const n = this.safeZones?.length ?? 0;
    if (index >= 0 && index < n) {
      this.safeZoneDirectionIndex = index;
      this.refreshView();
    }
  }

  /** Patient: pick a safe zone → Google Maps. */
  openSafeZoneDirectionsModal(): void {
    if (this.backendOffline || !this.safeZones?.length) {
      return;
    }
    this.directionsModalMode = 'patient';
    this.directionsModalOpen = true;
    this.directionsVoiceFeedback = '';
    this.directionsVoiceFeedbackType = '';
    this.directionsVoiceTranscript = '';
    this.refreshView();
  }

  /** Caregiver / volunteer / doctor: pick patient position or a safe zone → Google Maps. */
  openStaffDirectionsModal(): void {
    if (this.backendOffline) {
      return;
    }
    if (!this.latestLocation && !this.safeZones?.length) {
      return;
    }
    this.directionsModalMode = 'staff';
    if (this.latestLocation) {
      this.staffDirectionsTarget = 'patient';
    } else {
      this.staffDirectionsTarget = 0;
    }
    this.directionsModalOpen = true;
    this.directionsVoiceFeedback = '';
    this.directionsVoiceFeedbackType = '';
    this.directionsVoiceTranscript = '';
    this.refreshView();
  }

  setStaffDirectionsTarget(target: 'patient' | number): void {
    this.staffDirectionsTarget = target;
    this.refreshView();
  }

  isStaffZoneSelected(index: number): boolean {
    return typeof this.staffDirectionsTarget === 'number' && this.staffDirectionsTarget === index;
  }

  onDirectionsZoneRadioChange(index: number): void {
    if (this.directionsModalMode === 'staff') {
      this.setStaffDirectionsTarget(index);
    } else {
      this.selectSafeZoneForDirections(index);
    }
  }

  closeDirectionsModal(): void {
    this.directionsModalOpen = false;
    this.speechSvc.stop();
    this.directionsVoiceTranscript = '';
    this.directionsVoiceFeedback = '';
    this.directionsVoiceFeedbackType = '';
    this.refreshView();
  }

  /** Labels for speech matching (staff: two English phrases for patient position, then each zone). */
  get directionsVoiceLabels(): string[] {
    const zoneLabels = (this.safeZones || []).map((z, i) => this.safeZoneChipLabel(z, i));
    if (this.directionsModalMode === 'staff' && this.latestLocation) {
      return ['Patient current position', 'Patient last position', ...zoneLabels];
    }
    return zoneLabels;
  }

  toggleDirectionsVoice(): void {
    if (!this.directionsVoiceSupported || !this.directionsModalOpen) {
      return;
    }
    this.directionsVoiceFeedback = '';
    this.directionsVoiceFeedbackType = '';
    this.speechSvc.toggle('en-US');
  }

  private applyDirectionsVoiceMatch(listIndex: number): void {
    if (this.directionsModalMode === 'staff' && this.latestLocation) {
      if (listIndex === 0 || listIndex === 1) {
        this.setStaffDirectionsTarget('patient');
      } else {
        this.setStaffDirectionsTarget(listIndex - 2);
      }
    } else {
      this.selectSafeZoneForDirections(listIndex);
    }
  }

  private initDirectionsVoice(): void {
    if (!this.directionsVoiceSupported) {
      return;
    }
    this.directionsVoiceSubs.push(
      this.speechSvc.onResult.subscribe((r) => {
        if (!this.directionsModalOpen) {
          return;
        }
        this.ngZone.run(() => {
          this.directionsVoiceTranscript = r.transcript;
          if (!r.isFinal) {
            this.refreshView();
            return;
          }
          const labels = this.directionsVoiceLabels;
          if (!labels.length) {
            return;
          }
          const idx = this.speechSvc.matchAnswer(r.transcript, labels);
          if (idx < 0) {
            this.directionsVoiceFeedback = `Not recognized (“${r.transcript}”) — try again`;
            this.directionsVoiceFeedbackType = 'error';
            this.refreshView();
            return;
          }
          this.applyDirectionsVoiceMatch(idx);
          this.directionsVoiceFeedback = `Selected: ${labels[idx]}`;
          this.directionsVoiceFeedbackType = 'success';
          this.refreshView();
        });
      })
    );
    this.directionsVoiceSubs.push(
      this.speechSvc.onStateChange.subscribe((state) => {
        if (!this.directionsModalOpen) {
          return;
        }
        this.ngZone.run(() => {
          this.directionsVoiceListening = state === 'listening';
          this.refreshView();
        });
      })
    );
    this.directionsVoiceSubs.push(
      this.speechSvc.onError.subscribe(() => {
        if (!this.directionsModalOpen) {
          return;
        }
        this.ngZone.run(() => {
          this.directionsVoiceListening = false;
          this.directionsVoiceFeedback = 'Microphone error — check browser permissions';
          this.directionsVoiceFeedbackType = 'error';
          this.refreshView();
        });
      })
    );
  }

  /** Confirm modal: patient mode = one zone; staff mode = patient or zone. */
  confirmDirectionsModal(): void {
    if (this.directionsModalMode === 'patient') {
      const z = this.selectedSafeZoneForDirections;
      this.closeDirectionsModal();
      if (z) {
        this.openNavigationToSafeZone(z);
      }
      return;
    }
    const target = this.staffDirectionsTarget;
    this.closeDirectionsModal();
    if (target === 'patient') {
      this.openGoogleDirectionsToPatient();
    } else if (typeof target === 'number') {
      const z = this.safeZones[target];
      if (z) {
        this.openNavigationToSafeZone(z);
      }
    }
  }

  /**
   * When the list of zones changes (patient switch or new data), default to nearest zone;
   * otherwise keep the user’s chip selection.
   */
  private syncSafeZoneDirectionSelection(): void {
    const zones = this.safeZones || [];
    if (!zones.length) {
      this.safeZoneDirectionIndex = 0;
      this.lastSafeZonesListKey = '';
      return;
    }
    const key = zones
      .map((z) => `${z.id ?? 'x'}:${z.centerLatitude}:${z.centerLongitude}:${z.name ?? ''}`)
      .join('|');
    if (key !== this.lastSafeZonesListKey) {
      this.lastSafeZonesListKey = key;
      this.safeZoneDirectionIndex = this.primarySafeZoneIndexInList();
    } else if (this.safeZoneDirectionIndex >= zones.length) {
      this.safeZoneDirectionIndex = Math.max(0, zones.length - 1);
    }
  }

  private primarySafeZoneIndexInList(): number {
    const p = this.primarySafeZone;
    const zones = this.safeZones || [];
    if (!p || !zones.length) {
      return 0;
    }
    const idx = zones.findIndex(
      (z) =>
        (z.id != null && p.id != null && z.id === p.id) ||
        (z.centerLatitude === p.centerLatitude && z.centerLongitude === p.centerLongitude)
    );
    return idx >= 0 ? idx : 0;
  }

  onPatientChange(): void {
    this.staffJoinPoint = null;
    this.mapOverlayRevision += 1;
    this.lastSafeZonesListKey = '';
    this.safeZoneDirectionIndex = 0;
    this.directionsModalOpen = false;
    if (!this.selectedPatientId) {
      this.showMsg('Please select a patient to load tracking and alerts.', 'warn');
      this.refreshView();
      return;
    }
    this.refresh();
  }

  /** Selected monitored patient has a usable phone (calls + WhatsApp). */
  selectedPatientHasPhone(): boolean {
    const p = this.patients.find((x) => x.id === this.selectedPatientId);
    return !!this.movement.phoneHrefForTel(p?.phone || '');
  }

  getContactTelHref(alert: MovementAlert): string | null {
    const p = this.patients.find((x) => x.id === alert.patientId);
    return this.movement.phoneHrefForTel(p?.phone || '');
  }

  getContactWhatsAppHref(alert: MovementAlert): string | null {
    const p = this.patients.find((x) => x.id === alert.patientId);
    const digits = this.movement.phoneDigitsForWhatsApp(p?.phone || '');
    if (!digits) {
      return null;
    }
    const text = encodeURIComponent(this.buildAlertContactMessage(alert));
    return `https://wa.me/${digits}?text=${text}`;
  }

  private buildAlertContactMessage(alert: MovementAlert): string {
    const patientLabel = this.getPatientLabel(alert.patientId);
    const lines = [
      'Patient movement alert',
      `Patient: ${patientLabel}`,
      `Type: ${this.formatAlertTitle(alert.alertType)}`,
      `Message: ${this.formatAlertMessage(alert.message)}`,
      `Time: ${alert.createdAt}`,
    ];
    const raw = alert.message || '';
    const hasMapsInMessage = /google\.com\/maps|maps\.google/i.test(raw);
    if (!hasMapsInMessage) {
      const ping =
        alert.patientId === this.selectedPatientId && this.latestLocation
          ? this.latestLocation
          : null;
      if (ping?.latitude != null && ping?.longitude != null) {
        lines.push(`Maps: https://www.google.com/maps?q=${ping.latitude},${ping.longitude}`);
      }
    }
    return lines.join('\n');
  }

  refresh(options?: { silent?: boolean }): void {
    if (!this.selectedPatientId) return;
    const silent = options?.silent === true;
    if (!silent) {
      this.loading = true;
      this.refreshView();
    }

    const dashboard$ = this.movement.getPatientTrackingDashboard(this.selectedPatientId).pipe(
      map((data) => ({ data, failed: false, error: null as any })),
      catchError((error) => of({ data: null as PatientTrackingDashboard | null, failed: true, error }))
    );

    const status$ = this.movement.getPatientLocationStatus(this.selectedPatientId).pipe(
      map((data) => ({ data, failed: false, error: null as any })),
      catchError((error) => of({ data: null as PatientLocationStatus | null, failed: true, error }))
    );

    const latest$ = this.movement.getLatestLocation(this.selectedPatientId).pipe(
      map((data) => ({ data, failed: false, error: null as any })),
      catchError((error) => of({ data: null as LocationPing | null, failed: true, error }))
    );

    const history$ = this.movement.getLocationHistory(this.selectedPatientId, 240)
      .pipe(
        map((data) => ({ data, failed: false, error: null as any })),
        catchError((error) => of({ data: [] as LocationPing[], failed: true, error }))
      );

    /** Patients do not load or see movement alerts — only caregiver / volunteer / doctor / admin. */
    const alerts$ = this.isPatient
      ? of({ data: [] as MovementAlert[], failed: false, error: null as any })
      : (this.isDoctorOrAdmin
          ? this.movement.getAlerts(false)
          : this.movement.getPatientAlerts(this.selectedPatientId)
        ).pipe(
          map((data) => ({ data, failed: false, error: null as any })),
          catchError((error) => of({ data: [] as MovementAlert[], failed: true, error }))
        );

    const zones$ = this.movement.getSafeZonesByPatient(this.selectedPatientId).pipe(
      map((data) => ({ data, failed: false, error: null as any })),
      catchError((error) => of({ data: [] as SafeZone[], failed: true, error }))
    );

    forkJoin({
      dashboard: dashboard$,
      status: status$,
      latest: latest$,
      history: history$,
      alerts: alerts$,
      zones: zones$
    }).subscribe({
      next: ({ dashboard, status, latest, history, alerts, zones }) => {
        const usedLegacyFallback = dashboard.failed;
        const results = usedLegacyFallback
          ? [dashboard, status, latest, history, alerts, zones]
          : [dashboard, status, history];
        const failedErrors = results.filter((x) => x.failed).map((x) => x.error);
        const failureCount = failedErrors.length;
        const anyNetworkDown = failedErrors.some(
          (e) => e?.status === 0 || e?.status === undefined
        );
        const allFailed = failureCount === results.length;

        if (!dashboard.failed && dashboard.data) {
          this.trackingDashboard = dashboard.data;
          this.latestLocation = dashboard.data.latestLocation;
          this.safeZones = dashboard.data.safeZones || [];
          this.alerts = this.sortAlertsForDisplay(dashboard.data.recentAlertsPreview || []);
          this.syncSafeZoneDirectionSelection();
          if (!this.isPatient) {
            this.notifyForCriticalAlerts(this.alerts);
          }
        } else {
          this.trackingDashboard = null;
          if (!latest.failed) {
            this.latestLocation = latest.data;
          }
          if (!alerts.failed) {
            this.alerts = this.sortAlertsForDisplay(alerts.data || []);
            if (!this.isPatient) {
              this.notifyForCriticalAlerts(this.alerts);
            }
          }
          if (!zones.failed) {
            this.safeZones = zones.data || [];
            this.syncSafeZoneDirectionSelection();
          }
        }

        this.locationStatus = !status.failed ? status.data : null;

        if (!history.failed) {
          this.history = (history.data || []).slice().reverse();
        }

        if (failureCount === 0) {
          this.consecutiveApiErrors = 0;
          this.backendOffline = false;
          this.offlineReason = '';
          this.lastSuccessfulSyncAt = new Date();
          this.liveSyncRevision += 1;
          if (!this.pollRef) {
            this.startPolling();
          }
        } else if (anyNetworkDown || allFailed) {
          this.consecutiveApiErrors += 1;
          if (this.consecutiveApiErrors >= this.maxApiErrorsBeforePause) {
            this.backendOffline = true;
            this.offlineReason = this.formatNetworkReason(failedErrors[0]);
            this.stopPolling();
            if (!silent) {
              this.showMsg(this.offlineReason, 'warn');
            }
          }
        } else {
          // Partial API failure: still show what we got; keep polling & map updating.
          this.consecutiveApiErrors = 0;
          this.backendOffline = false;
          this.lastSuccessfulSyncAt = new Date();
          this.liveSyncRevision += 1;
          if (!this.pollRef) {
            this.startPolling();
          }
        }

        this.loading = false;
        this.refreshView();
      },
      error: () => {
        this.consecutiveApiErrors += 1;
        this.backendOffline = true;
        this.offlineReason = 'The movement service is unavailable (API Gateway or movement-service).';
        this.stopPolling();
        this.showMsg(this.offlineReason, 'err');
        this.loading = false;
        this.refreshView();
      }
    });
  }

  /**
   * Real tracking (not the demo simulation buttons): reads browser GPS once and POSTs it to movement-service.
   * Same endpoint as simulations — `POST /api/locations/report` — but `source` is `BROWSER_GPS`, so the backend does
   * **not** run `createSimulationAlert`; it only runs live rules: safe-zone check, home exit, abrupt movement,
   * immobility, scheduled no-GPS checks. Alerts are persisted in `movement_alert` like simulations; staff map refreshes
   * via polling (`refresh` / auto poll). The patient must tap again after moving (no background watchPosition loop).
   */
  reportMyLiveGps(): void {
    if (!this.isPatient) {
      this.showMsg('Only the patient can send their position.', 'warn');
      return;
    }

    if (!navigator.geolocation || !this.selectedPatientId) {
      this.showMsg('Browser GPS is unavailable.', 'err');
      return;
    }

    this.reporting = true;
    this.refreshView();
    navigator.geolocation.getCurrentPosition(
      (pos: GeolocationPosition) => {
        this.movement.reportLocation({
          patientId: this.selectedPatientId,
          latitude: pos.coords.latitude,
          longitude: pos.coords.longitude,
          accuracyMeters: pos.coords.accuracy,
          source: 'BROWSER_GPS'
        }).subscribe({
          next: () => {
            this.reporting = false;
            this.consecutiveApiErrors = 0;
            this.backendOffline = false;
            this.showMsg(
              this.isPatient
                ? 'Location sent.'
                : 'Live location shared — your care team sees your updated position on the map.',
              'info'
            );
            this.refresh();
            this.refreshView();
          },
          error: (error) => {
            this.reporting = false;
            this.handleApiError(error, 'Could not share live location. Try again.');
            this.refreshView();
          }
        });
      },
      () => {
        this.reporting = false;
        this.showMsg(this.isPatient ? 'Could not read GPS.' : 'Could not read GPS position.', 'err');
        this.refreshView();
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 0
      }
    );
  }

  /**
   * Safe zone for navigation: nearest to patient when GPS known, otherwise first zone for this patient.
   */
  get primarySafeZone(): SafeZone | null {
    if (!this.safeZones?.length) {
      return null;
    }
    const lat = this.latestLocation?.latitude;
    const lon = this.latestLocation?.longitude;
    if (lat != null && lon != null) {
      return this.pickNearestSafeZone(lat, lon);
    }
    return this.safeZones[0];
  }

  /**
   * Google Maps turn-by-turn to the patient: uses your GPS as start when available,
   * otherwise opens directions to the patient only (same as the “Google Maps” link above).
   */
  openGoogleDirectionsToPatient(): void {
    if (!this.latestLocation) {
      this.showMsg('No patient position yet.', 'warn');
      return;
    }
    const dLat = this.latestLocation.latitude;
    const dLon = this.latestLocation.longitude;
    const open = (url: string) => window.open(url, '_blank', 'noopener,noreferrer');

    if (!navigator.geolocation) {
      open(this.googleMapsPatientUrl);
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
      () => {
        open(this.googleMapsPatientUrl);
      },
      { enableHighAccuracy: true, timeout: 12000, maximumAge: 0 }
    );
  }

  /**
   * Places your GPS on the map and draws an immediate straight line to the patient (no external routing API).
   * For turn-by-turn driving, use “Directions (Google Maps)”.
   */
  plotStaffPickupOnMap(): void {
    if (!this.latestLocation) {
      this.showMsg('No patient position on the map yet.', 'warn');
      return;
    }
    if (!navigator.geolocation) {
      this.showMsg('Geolocation is not available in this browser.', 'warn');
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        this.ngZone.run(() => {
          this.staffJoinPoint = {
            latitude: pos.coords.latitude,
            longitude: pos.coords.longitude,
          };
          this.mapOverlayRevision += 1;
          this.showMsg(
            'Your position is on the map; orange dashed line = straight distance to the patient. Use “Directions (Google Maps)” for driving route.',
            'info'
          );
          this.refreshView();
        });
      },
      () => this.showMsg('Could not read your GPS position.', 'err'),
      { enableHighAccuracy: true, timeout: 15000, maximumAge: 0 }
    );
  }

  clearStaffPickupOnMap(): void {
    this.staffJoinPoint = null;
    this.mapOverlayRevision += 1;
    this.refreshView();
  }

  /**
   * Turn-by-turn in Google Maps: your GPS → chosen safe zone center.
   * Uses `selectedSafeZoneForDirections` when `zone` is omitted.
   */
  openNavigationToSafeZone(zone?: SafeZone | null): void {
    const z = zone ?? this.selectedSafeZoneForDirections ?? this.primarySafeZone;
    if (!z) {
      this.showMsg('No safe zone defined for this patient.', 'warn');
      return;
    }
    const dLat = z.centerLatitude;
    const dLon = z.centerLongitude;
    const dest = `${dLat},${dLon}`;
    const open = (url: string) => window.open(url, '_blank', 'noopener,noreferrer');

    if (!navigator.geolocation) {
      open(
        `https://www.google.com/maps/dir/?api=1&destination=${encodeURIComponent(dest)}&travelmode=driving`
      );
      return;
    }

    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const oLat = pos.coords.latitude;
        const oLon = pos.coords.longitude;
        const url = `https://www.google.com/maps/dir/?api=1&origin=${oLat},${oLon}&destination=${dLat},${dLon}&travelmode=driving`;
        open(url);
      },
      () => {
        open(
          `https://www.google.com/maps/dir/?api=1&destination=${encodeURIComponent(dest)}&travelmode=driving`
        );
      },
      { enableHighAccuracy: true, timeout: 12000, maximumAge: 0 }
    );
  }

  acknowledge(alert: MovementAlert): void {
    this.movement.acknowledgeAlert(alert.id).subscribe({
      next: (updated) => {
        const ix = this.alerts.findIndex((x) => x.id === alert.id);
        if (ix >= 0) {
          const merged = { ...this.alerts[ix], ...updated };
          const next = [...this.alerts];
          next[ix] = merged;
          this.alerts = this.sortAlertsForDisplay(next);
        }
        this.refreshView();
      },
      error: () => this.showMsg('Could not acknowledge this alert.', 'err')
    });
  }

  removeAlert(alert: MovementAlert): void {
    this.movement.deleteMovementAlert(alert.id).subscribe({
      next: () => {
        this.alerts = this.alerts.filter((x) => x.id !== alert.id);
        this.lastNotificationAtByAlertId.delete(alert.id);
        if (this.trackingDashboard) {
          const prev = this.trackingDashboard.recentAlertsPreview || [];
          const nextPrev = this.sortAlertsForDisplay(prev.filter((x) => x.id !== alert.id));
          const unack = nextPrev.filter((x) => !x.acknowledged);
          this.trackingDashboard = {
            ...this.trackingDashboard,
            recentAlertsPreview: nextPrev,
            unacknowledgedAlertCount: unack.length,
            latestUnacknowledgedAlert: unack.length ? unack[0] : null,
          };
        }
        this.refreshView();
      },
      error: () => this.showMsg('Could not remove this alert.', 'err'),
    });
  }

  clearAllAlerts(): void {
    if (!this.alerts.length || this.backendOffline) {
      return;
    }
    if (!window.confirm('Clear all movement alerts from this list?')) {
      return;
    }
    const req = this.isDoctorOrAdmin
      ? this.movement.deleteAllMovementAlerts()
      : this.movement.deleteAllMovementAlertsForPatient(this.selectedPatientId);
    req.subscribe({
      next: () => {
        this.alerts = [];
        this.lastNotificationAtByAlertId.clear();
        if (this.trackingDashboard) {
          this.trackingDashboard = {
            ...this.trackingDashboard,
            recentAlertsPreview: [],
            unacknowledgedAlertCount: 0,
            latestUnacknowledgedAlert: null,
          };
        }
        this.refreshView();
      },
      error: () => this.showMsg('Could not clear alerts.', 'err'),
    });
  }

  get unacknowledgedAlertCount(): number {
    return this.alerts.filter((a) => !a.acknowledged).length;
  }

  private sortAlertsForDisplay(list: MovementAlert[]): MovementAlert[] {
    return [...list].sort((a, b) => {
      if (a.acknowledged !== b.acknowledged) {
        return a.acknowledged ? 1 : -1;
      }
      const ta = new Date(a.createdAt || 0).getTime();
      const tb = new Date(b.createdAt || 0).getTime();
      return tb - ta;
    });
  }

  getAlertClass(alert: MovementAlert): string {
    switch (alert.severity) {
      case 'CRITICAL': return 'crit';
      case 'WARNING': return 'warn';
      default: return 'info';
    }
  }

  retryConnection(): void {
    this.backendOffline = false;
    this.consecutiveApiErrors = 0;
    this.offlineReason = '';
    this.showMsg('Reconnecting to the movement service…', 'info');
    this.refresh();
    this.startPolling();
  }

  private pickNearestSafeZone(lat: number, lon: number): SafeZone {
    let best = this.safeZones[0];
    let bestD = Number.POSITIVE_INFINITY;
    for (const z of this.safeZones) {
      const d = haversineMeters(lat, lon, z.centerLatitude, z.centerLongitude);
      if (d < bestD) {
        bestD = d;
        best = z;
      }
    }
    return best;
  }

  private initializeBrowserNotifications(): void {
    if (typeof window === 'undefined' || !('Notification' in window)) {
      return;
    }

    if (Notification.permission === 'granted') {
      this.browserNotificationsEnabled = true;
      return;
    }

    if (Notification.permission === 'default') {
      Notification.requestPermission().then((permission) => {
        this.browserNotificationsEnabled = permission === 'granted';
      });
    }
  }

  private notifyForCriticalAlerts(alerts: MovementAlert[]): void {
    if (!this.browserNotificationsEnabled) {
      return;
    }

    const currentAlerts = alerts || [];
    const notificationTypes = new Set([
      'OUT_OF_SAFE_ZONE',
      'LEFT_REGISTERED_HOME',
      'RAPID_OR_UNUSUAL_MOVEMENT',
      'IMMOBILE_TOO_LONG',
    ]);
    const activeAlertIds = new Set<number>();
    const now = Date.now();

    for (const alert of currentAlerts) {
      if (!notificationTypes.has(alert.alertType) || alert.acknowledged) {
        continue;
      }

      activeAlertIds.add(alert.id);
      const lastNotifiedAt = this.lastNotificationAtByAlertId.get(alert.id) ?? 0;
      if (now - lastNotifiedAt < this.notificationRepeatMs) {
        continue;
      }

      this.lastNotificationAtByAlertId.set(alert.id, now);
      try {
        new Notification(`Patient alert #${alert.patientId}`, {
          body: alert.message,
          tag: `movement-alert-${alert.id}`
        });
      } catch {
        // Ignore Notification API errors on unsupported devices/browsers.
      }
    }

    for (const existingId of Array.from(this.lastNotificationAtByAlertId.keys())) {
      if (!activeAlertIds.has(existingId)) {
        this.lastNotificationAtByAlertId.delete(existingId);
      }
    }
  }

  private loadVolunteerPatients(): void {
    const volunteerId = Number(this.user?.userId || 0);
    if (!volunteerId) {
      this.showMsg('Invalid volunteer session. Please sign in again.', 'err');
      this.loading = false;
      this.refreshView();
      return;
    }

    this.auth.getPatientsByVolunteer(volunteerId).subscribe({
      next: (users: any[]) => {
        this.patients = (users || [])
          .map((u) => this.normalizeAppUser(u))
          .filter((u): u is AppUser => !!u);

        this.selectedPatientId = this.patients[0]?.id || 0;
        if (!this.selectedPatientId) {
          this.showMsg('No patient linked to this volunteer.', 'warn');
          this.loading = false;
          this.refreshView();
          return;
        }

        this.refresh();
        this.startPolling();
        this.refreshView();
      },
      error: () => {
        this.showMsg('Could not load linked patients.', 'err');
        this.loading = false;
        this.refreshView();
      }
    });
  }

  private loadCaregiverPatients(): void {
    const caregiverId = Number(this.user?.userId || 0);
    if (!caregiverId) {
      this.showMsg('Invalid caregiver session. Please sign in again.', 'err');
      this.loading = false;
      this.refreshView();
      return;
    }

    this.auth.getPatientsByCaregiver(caregiverId).subscribe({
      next: (users: any[]) => {
        this.patients = (users || [])
          .map((u) => this.normalizeAppUser(u))
          .filter((u): u is AppUser => !!u);

        this.selectedPatientId = this.patients[0]?.id || 0;
        if (!this.selectedPatientId) {
          this.showMsg('No patient linked to this caregiver.', 'warn');
          this.loading = false;
          this.refreshView();
          return;
        }

        this.refresh();
        this.startPolling();
        this.refreshView();
      },
      error: () => {
        this.showMsg('Could not load linked patients.', 'err');
        this.loading = false;
        this.refreshView();
      }
    });
  }

  /** Charge aidant / bénévole associés au compte patient (IDs dans la session). */
  private loadLinkedSupportStaffForPatient(): void {
    const session = this.auth.getCurrentUser();
    if (!session || session.role !== 'PATIENT') {
      return;
    }
    const cid = session.caregiverId;
    const vid = session.volunteerId;
    const c$ =
      cid != null && cid > 0
        ? this.auth.getUserById(cid).pipe(
            map((u) => this.formatPersonDisplayName(u)),
            catchError(() => of(null))
          )
        : of(null);
    const v$ =
      vid != null && vid > 0
        ? this.auth.getUserById(vid).pipe(
            map((u) => this.formatPersonDisplayName(u)),
            catchError(() => of(null))
          )
        : of(null);

    forkJoin({ caregiver: c$, volunteer: v$ }).subscribe({
      next: ({ caregiver, volunteer }) => {
        this.linkedCaregiverName = caregiver;
        this.linkedVolunteerName = volunteer;
        this.refreshView();
      },
    });
  }

  private formatPersonDisplayName(u: User | null): string | null {
    if (!u) {
      return null;
    }
    const cap = (s: string) => {
      const t = (s || '').trim();
      if (!t) {
        return '';
      }
      return t.charAt(0).toUpperCase() + t.slice(1).toLowerCase();
    };
    const name = `${cap(u.firstName)} ${cap(u.lastName)}`.trim();
    return name || null;
  }

  private normalizeAppUser(raw: any): AppUser | null {
    const id = Number(raw?.id ?? raw?.userId ?? 0);
    if (!id || Number.isNaN(id)) {
      return null;
    }

    return {
      id,
      firstname: (raw?.firstname ?? raw?.firstName ?? '').toString(),
      lastname: (raw?.lastname ?? raw?.lastName ?? '').toString(),
      email: (raw?.email ?? '').toString(),
      phone: (raw?.phone ?? '').toString(),
      role: (raw?.role ?? '').toString(),
    };
  }

  private initializePatientView(): void {
    this.loadLinkedSupportStaffForPatient();

    const directId = Number(this.user?.userId);
    if (directId && !Number.isNaN(directId)) {
      this.selectedPatientId = directId;
      this.refresh();
      this.startPolling();
      this.reportMyLiveGps();
      this.refreshView();
      return;
    }

    // Fallback for sessions where patient id is not present in the auth payload.
    this.movement.getPatients().subscribe({
      next: (patients) => {
        const byEmail = (patients || []).find((p) => p.email === this.user?.email);
        this.selectedPatientId = byEmail?.id ?? 0;
        if (!this.selectedPatientId) {
          this.showMsg('Could not determine your patient id. Please sign in again.', 'err');
          this.loading = false;
          this.refreshView();
          return;
        }
        this.refresh();
        this.startPolling();
        this.reportMyLiveGps();
        this.refreshView();
      },
      error: () => {
        this.showMsg('Failed to resolve patient profile.', 'err');
        this.loading = false;
        this.refreshView();
      }
    });
  }

  private startPolling(): void {
    if (this.backendOffline) {
      return;
    }
    this.stopPolling();
    this.ngZone.runOutsideAngular(() => {
      this.pollRef = setInterval(() => {
        this.ngZone.run(() => this.refresh({ silent: true }));
      }, this.pollIntervalMs);
    });
  }

  private stopPolling(): void {
    if (this.pollRef) {
      clearInterval(this.pollRef);
      this.pollRef = null;
    }
  }

  private handleApiError(error: any, fallbackMessage: string): void {
    if (error?.status === 0) {
      this.backendOffline = true;
      this.consecutiveApiErrors = this.maxApiErrorsBeforePause;
      this.offlineReason = this.formatNetworkReason(error);
      this.stopPolling();
      this.showMsg(this.offlineReason, 'err');
      return;
    }

    const backendMessage = error?.error?.message;
    this.showMsg(backendMessage || fallbackMessage, 'err');
  }

  private formatNetworkReason(error: any): string {
    if (error?.status === 0) {
      if (environment.useMovementLocalizationDirect) {
        return 'Cannot reach localization-service (:8085) or movement-service (:8086). Start them and MySQL, then try again.';
      }
      return 'Cannot reach the API (gateway :8080, movement-service :8086). Check Eureka and the microservices.';
    }
    return 'The movement service is temporarily unavailable.';
  }

  private showMsg(message: string, type: 'info' | 'warn' | 'err' = 'info'): void {
    let kind: ToastKind = 'info';
    if (type === 'err') {
      kind = 'error';
    } else if (type === 'info' && /sent\.|successfully|shared —|Reconnecting/i.test(message)) {
      kind = 'success';
    }
    const durationMs = type === 'err' ? 6500 : type === 'warn' ? 4800 : 4200;
    this.toast.show(message, kind, durationMs);
    this.refreshView();
  }

  getPatientLabel(patientId: number): string {
    const patient = this.patients.find((p) => p.id === patientId);
    if (!patient) {
      return `Patient ${patientId}`;
    }
    const name = `${patient.firstname || ''} ${patient.lastname || ''}`.trim();
    if (!name) return `Patient ${patientId}`;
    return name
      .split(/\s+/)
      .filter(Boolean)
      .map((w) => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase())
      .join(' ');
  }

  private refreshView(): void {
    const view = this.cdr as ViewRef;
    if (!view.destroyed) {
      this.cdr.detectChanges();
    }
  }
}
