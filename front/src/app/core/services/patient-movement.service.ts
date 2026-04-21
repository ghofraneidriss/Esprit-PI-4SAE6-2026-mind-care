import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, catchError, map, of, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { User } from '../models/user.model';

export interface AppUser {
  id: number;
  firstname: string;
  lastname: string;
  email: string;
  phone: string;
  role: string;
}

export interface SafeZone {
  id?: number;
  name: string;
  centerLatitude: number;
  centerLongitude: number;
  radius: number;
  patientId: number;
  homeReference?: boolean;
}

export interface LocationPing {
  id?: number;
  patientId: number;
  latitude: number;
  longitude: number;
  accuracyMeters?: number;
  speedKmh?: number;
  source?: string;
  recordedAt?: string;
}

export interface MovementAlert {
  id: number;
  patientId: number;
  alertType: string;
  severity: string;
  message: string;
  acknowledged: boolean;
  emailSent: boolean;
  createdAt: string;
  acknowledgedAt?: string;
}

export interface OSMPlace {
  display_name: string;
  lat: string;
  lon: string;
}

/** API métier `/api/tracking` — zone la plus proche du dernier ping. */
export interface NearestSafeZoneInfo {
  safeZoneId?: number;
  name: string;
  distanceMetersToCenter: number;
  radiusMeters: number;
  insideThisZone: boolean;
  distanceMetersToEdge: number;
}

/** Synthèse légère (conformité + fraîcheur GPS). */
export interface PatientLocationStatus {
  patientId: number;
  insideAnySafeZone: boolean | null;
  definedSafeZoneCount: number;
  hasLatestGps: boolean;
  latestGpsRecordedAt?: string;
  minutesSinceLastGps: number | null;
}

/** Dashboard agrégé : zones + GPS + alertes (un seul appel). */
export interface PatientTrackingDashboard {
  patientId: number;
  locationCompliance: string;
  safeZones: SafeZone[];
  latestLocation: LocationPing | null;
  nearestSafeZone: NearestSafeZoneInfo | null;
  unacknowledgedAlertCount: number;
  latestUnacknowledgedAlert: MovementAlert | null;
  recentAlertsPreview: MovementAlert[];
}

@Injectable({ providedIn: 'root' })
export class PatientMovementService {
  private readonly gateway = environment.gatewayBaseUrl.replace(/\/$/, '');
  private readonly nominatimApi = 'https://nominatim.openstreetmap.org/search';

  constructor(private http: HttpClient) {}

  /** Zones sûres : localization-service ou gateway. */
  private safeZoneApiBase(): string {
    if (
      environment.useMovementLocalizationDirect &&
      environment.localizationServiceBaseUrl
    ) {
      return environment.localizationServiceBaseUrl.replace(/\/$/, '');
    }
    return this.gateway;
  }

  /** Positions GPS et alertes mouvement : movement-service ou gateway. */
  private movementApiBase(): string {
    if (environment.useMovementLocalizationDirect && environment.movementServiceBaseUrl) {
      return environment.movementServiceBaseUrl.replace(/\/$/, '');
    }
    return this.gateway;
  }

  private usersListUrl(): string {
    if (environment.useUsersServiceDirect && environment.usersServiceBaseUrl) {
      return `${environment.usersServiceBaseUrl.replace(/\/$/, '')}/users`;
    }
    return `${this.gateway}/users`;
  }

  getPatients(): Observable<AppUser[]> {
    return this.http.get<User[]>(this.usersListUrl()).pipe(
      map((users) =>
        (users || [])
          .filter((u) => u.role === 'PATIENT')
          .map((u) => ({
            id: u.userId,
            firstname: u.firstName,
            lastname: u.lastName,
            email: u.email,
            phone: u.phone ?? '',
            role: u.role,
          }))
      )
    );
  }

  resolvePatientWhatsAppPhone(patientUserId: number): Observable<string | null> {
    return this.getPatients().pipe(
      map((patients) => {
        const patient = (patients || []).find((p) => p.id === patientUserId);
        return this.normalizePhoneForWhatsapp(patient?.phone || '');
      }),
      catchError(() => of(null))
    );
  }

  /** Digits only (no +), suitable for `https://wa.me/{digits}`. */
  phoneDigitsForWhatsApp(value: string): string | null {
    return this.normalizePhoneForWhatsapp(value);
  }

  /** `tel:+…` href for the native phone app (mobile / desktop). */
  phoneHrefForTel(value: string): string | null {
    const digits = this.normalizePhoneForWhatsapp(value);
    if (!digits) {
      return null;
    }
    return `tel:+${digits}`;
  }

  getSafeZones(): Observable<SafeZone[]> {
    return this.http.get<SafeZone[]>(`${this.safeZoneApiBase()}/safezones`);
  }

  getSafeZonesByPatient(patientId: number): Observable<SafeZone[]> {
    return this.http.get<SafeZone[]>(`${this.safeZoneApiBase()}/safezones/patient/${patientId}`);
  }

  createSafeZone(zone: SafeZone): Observable<SafeZone> {
    return this.http.post<SafeZone>(`${this.safeZoneApiBase()}/safezones`, zone);
  }

  updateSafeZone(id: number, zone: SafeZone): Observable<SafeZone> {
    return this.http.put<SafeZone>(`${this.safeZoneApiBase()}/safezones/${id}`, zone);
  }

  deleteSafeZone(id: number): Observable<void> {
    return this.http.delete<void>(`${this.safeZoneApiBase()}/safezones/${id}`);
  }

  /**
   * Single entry point for all location pings: real GPS (`BROWSER_GPS`), mobile app, or `SIMULATION_*` demos.
   * Backend: `MovementMonitoringService.reportLocation` — saves ping, then evaluates alerts (zone, immobility, etc.).
   */
  reportLocation(payload: Partial<LocationPing>): Observable<LocationPing> {
    return this.http.post<LocationPing>(`${this.movementApiBase()}/locations/report`, payload);
  }

  /**
   * Latest GPS ping, or `null` if the patient has no location yet (backend returns 404).
   * Treating 404 as null keeps polling alive; otherwise every poll looked like a failure.
   */
  getLatestLocation(patientId: number): Observable<LocationPing | null> {
    const url = `${this.movementApiBase()}/locations/patient/${patientId}/latest`;
    return this.http.get<LocationPing>(url).pipe(
      catchError((err: unknown) => {
        const status = err instanceof HttpErrorResponse ? err.status : undefined;
        if (status === 404) {
          return of(null);
        }
        return throwError(() => err);
      })
    );
  }

  getLocationHistory(patientId: number, minutes = 180): Observable<LocationPing[]> {
    const params = new HttpParams().set('minutes', String(minutes));
    return this.http.get<LocationPing[]>(
      `${this.movementApiBase()}/locations/patient/${patientId}/history`,
      { params }
    );
  }

  getAlerts(unacknowledgedOnly = false): Observable<MovementAlert[]> {
    const params = new HttpParams().set('unacknowledgedOnly', String(unacknowledgedOnly));
    return this.http.get<MovementAlert[]>(`${this.movementApiBase()}/alerts`, { params });
  }

  /**
   * Full OUT_OF_SAFE_ZONE counts per patient (DB aggregate). Prefer this over {@link getAlerts}
   * for dashboards — {@code GET /alerts} returns only the 200 most recent alerts globally.
   */
  getOutOfSafeZoneExitCountsByPatient(): Observable<Record<string, number>> {
    return this.http.get<Record<string, number>>(
      `${this.movementApiBase()}/alerts/stats/out-of-safe-zone-by-patient`
    );
  }

  /**
   * Total movement alerts per patient (all alert types — same data as patient alert history / PDF).
   * Use for “critical patients” and user-management counts.
   */
  getTotalMovementAlertCountsByPatient(): Observable<Record<string, number>> {
    return this.http.get<Record<string, number>>(
      `${this.movementApiBase()}/alerts/stats/total-by-patient`
    );
  }

  getPatientAlerts(patientId: number): Observable<MovementAlert[]> {
    return this.http.get<MovementAlert[]>(`${this.movementApiBase()}/alerts/patient/${patientId}`);
  }

  acknowledgeAlert(alertId: number): Observable<MovementAlert> {
    return this.http.put<MovementAlert>(`${this.movementApiBase()}/alerts/${alertId}/ack`, {});
  }

  deleteMovementAlert(alertId: number): Observable<void> {
    return this.http.delete<void>(`${this.movementApiBase()}/alerts/${alertId}`);
  }

  deleteAllMovementAlertsForPatient(patientId: number): Observable<void> {
    return this.http.delete<void>(`${this.movementApiBase()}/alerts/patient/${patientId}/all`);
  }

  /** Clears every movement alert in the system (staff “all patients” view). */
  deleteAllMovementAlerts(): Observable<void> {
    return this.http.delete<void>(`${this.movementApiBase()}/alerts/bulk/all`);
  }

  /**
   * API métier avancée (movement-service) : zones + dernière position + alertes.
   * Même base que les locations (`movementApiBase`), derrière gateway : `/api/tracking/...`.
   */
  getPatientTrackingDashboard(patientId: number): Observable<PatientTrackingDashboard> {
    return this.http.get<PatientTrackingDashboard>(
      `${this.movementApiBase()}/tracking/patient/${patientId}/dashboard`
    );
  }

  getPatientLocationStatus(patientId: number): Observable<PatientLocationStatus> {
    return this.http.get<PatientLocationStatus>(
      `${this.movementApiBase()}/tracking/patient/${patientId}/status`
    );
  }

  searchPlace(query: string): Observable<OSMPlace[]> {
    const params = new HttpParams().set('format', 'jsonv2').set('limit', '6').set('q', query);
    const headers = new HttpHeaders({ 'Accept-Language': 'fr' });
    return this.http.get<OSMPlace[]>(this.nominatimApi, { params, headers });
  }

  private normalizePhoneForWhatsapp(value: string): string | null {
    const raw = (value || '').trim();
    if (!raw || raw.includes('@')) {
      return null;
    }

    let normalized = raw.replace(/[^\d+]/g, '');
    if (!normalized) {
      return null;
    }

    if (normalized.startsWith('+')) {
      normalized = normalized.slice(1);
    }
    if (normalized.startsWith('00')) {
      normalized = normalized.slice(2);
    }

    const digits = normalized.replace(/\D/g, '');
    if (digits.length === 8) {
      return `216${digits}`;
    }

    if (digits.length === 9 && digits.startsWith('0')) {
      return `216${digits.slice(1)}`;
    }

    return digits.length >= 10 ? digits : null;
  }
}
