import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { of } from 'rxjs';
import { catchError, finalize, map, mergeMap } from 'rxjs/operators';
import { AuthService } from '../../core/services/auth.service';
import { User } from '../../core/models/user.model';
import {
  LocalizationUser,
  SafeZone,
  SafeZoneService,
} from '../../core/services/safe-zone.service';
import { SafeZoneOsmMapComponent } from './safe-zone-osm-map/safe-zone-osm-map.component';
import { ToastService } from '../../core/services/toast.service';

/** Same default center as the OSM map when no zone exists yet (Tunisia). */
const DEFAULT_MAP_CENTER = { lat: 36.8065, lng: 10.1815 };

@Component({
  selector: 'app-localization-management',
  standalone: true,
  imports: [CommonModule, FormsModule, SafeZoneOsmMapComponent],
  templateUrl: './localization-management.component.html',
  styleUrls: ['./localization-management.component.css'],
})
export class LocalizationManagementComponent implements OnInit {
  user: User | null = null;
  patients: LocalizationUser[] = [];
  zones: SafeZone[] = [];
  filteredZones: SafeZone[] = [];

  selectedPatientFilter: number | 'ALL' = 'ALL';
  editing = false;
  loading = false;
  /** True while create/update is in flight — blocks duplicate submits. */
  saving = false;
  /** Safe zone id being deleted (disables that row). */
  deletingId: number | null = null;
  /** Map is shown on load so the zone can be placed immediately. */
  mapVisible = true;

  /** Inline validation (English) for the create / edit form. */
  fieldErrors: {
    name?: string;
    patient?: string;
    location?: string;
  } = {};

  form: SafeZone = {
    name: '',
    patientId: 0,
    centerLatitude: DEFAULT_MAP_CENTER.lat,
    centerLongitude: DEFAULT_MAP_CENTER.lng,
    radius: 150,
    homeReference: false,
  };

  constructor(
    private auth: AuthService,
    private localization: SafeZoneService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.user = this.auth.getCurrentUser();
    if (!this.user) {
      this.router.navigate(['/login-cover']);
      return;
    }

    if (!this.canManage) {
      return;
    }

    this.loadAll();
  }

  get canManage(): boolean {
    const role = this.user?.role;
    return (
      role === 'ADMIN' ||
      role === 'DOCTOR' ||
      role === 'CAREGIVER' ||
      role === 'VOLUNTEER'
    );
  }

  private toLocalizationUser(u: User): LocalizationUser {
    return {
      userId: u.userId,
      firstName: u.firstName,
      lastName: u.lastName,
      email: u.email,
      role: u.role,
    };
  }

  clearFieldErrors(): void {
    this.fieldErrors = {};
  }

  onFormFieldChange(): void {
    this.clearFieldErrors();
  }

  onPatientChange(): void {
    this.onFormFieldChange();
    this.form.homeReference = false;
  }

  /** Show registered-home checkbox whenever a patient is selected (create or edit). */
  get showRegisteredHomeOption(): boolean {
    return !!this.form.patientId;
  }

  /**
   * Another zone (same patient) is already the registered home — user can still check this zone
   * to move the home reference; backend clears the previous zone on save.
   */
  get hasOtherRegisteredHome(): boolean {
    const pid = this.form.patientId;
    if (!pid) {
      return false;
    }
    const pidN = this.normalizeId(pid);
    const formId = this.form.id != null ? this.normalizeId(this.form.id) : null;
    return this.zones.some((z) => {
      if (this.normalizeId(z.patientId) !== pidN || !z.homeReference) {
        return false;
      }
      if (formId != null && !Number.isNaN(formId) && this.normalizeId(z.id) === formId) {
        return false;
      }
      return true;
    });
  }

  /**
   * @param silent If true, refreshes zone list without full-page skeleton (e.g. after save).
   */
  loadAll(silent = false): void {
    if (!silent) {
      this.loading = true;
    }
    const role = this.user?.role;
    const uid = this.user?.userId;

    const patients$ =
      role === 'CAREGIVER' && uid
        ? this.auth.getPatientsByCaregiver(uid).pipe(
            map((list) => (list || []).map((u) => this.toLocalizationUser(u)))
          )
        : role === 'VOLUNTEER' && uid
          ? this.auth.getPatientsByVolunteer(uid).pipe(
              map((list) => (list || []).map((u) => this.toLocalizationUser(u)))
            )
          : this.localization.getPatients();

    patients$
      .pipe(
        catchError((err: HttpErrorResponse) => {
          this.showMsg(this.httpErrorMessage(err, 'Failed to load patients.'), 'err');
          return of<LocalizationUser[]>([]);
        }),
        mergeMap((patients) => {
          this.patients = patients || [];
          if (this.patients.length && !this.form.patientId) {
            this.form.patientId = this.patients[0].userId;
          }
          return this.localization.getSafeZones().pipe(
            catchError((err: HttpErrorResponse) => {
              this.showMsg(this.httpErrorMessage(err, 'Failed to load safe zones.'), 'err');
              return of<SafeZone[]>([]);
            })
          );
        })
      )
      .subscribe({
        next: (zones) => {
          let list = zones || [];
          if (role === 'CAREGIVER' || role === 'VOLUNTEER') {
            const allowed = new Set(this.patients.map((p) => this.normalizeId(p.userId)));
            list = list.filter((z) => allowed.has(this.normalizeId(z.patientId)));
          }
          this.zones = list;
          this.applyFilter();
          if (!silent) {
            this.loading = false;
          }
          this.cdr.detectChanges();
        },
        error: () => {
          if (!silent) {
            this.loading = false;
          }
          this.cdr.detectChanges();
        },
      });
  }

  /** API may return patient ids as string or number — keeps filters and Set.has() consistent. */
  private normalizeId(v: number | string | undefined | null): number {
    const n = Number(v);
    return Number.isFinite(n) ? n : NaN;
  }

  trackByZoneId(index: number, z: SafeZone): number {
    return z.id ?? index;
  }

  private httpErrorMessage(err: HttpErrorResponse, fallback: string): string {
    const body = err.error;
    if (typeof body === 'string' && body.trim()) {
      return body.length > 200 ? fallback : body;
    }
    if (body && typeof body === 'object') {
      const m = (body as { message?: string; error?: string }).message;
      if (m && typeof m === 'string') {
        return m.length > 200 ? fallback : m;
      }
      const e = (body as { error?: string }).error;
      if (e && typeof e === 'string') {
        return e.length > 200 ? fallback : e;
      }
    }
    if (err.status === 0) {
      return 'Network error. Check your connection and try again.';
    }
    return fallback;
  }

  applyFilter(): void {
    if (this.selectedPatientFilter === 'ALL') {
      this.filteredZones = [...this.zones];
      return;
    }
    const fid = this.normalizeId(this.selectedPatientFilter as number);
    this.filteredZones = this.zones.filter((z) => this.normalizeId(z.patientId) === fid);
  }

  revealMap(): void {
    this.mapVisible = true;
  }

  resetForm(): void {
    this.editing = false;
    this.mapVisible = true;
    this.clearFieldErrors();
    this.form = {
      name: '',
      patientId: this.patients[0]?.userId || 0,
      centerLatitude: DEFAULT_MAP_CENTER.lat,
      centerLongitude: DEFAULT_MAP_CENTER.lng,
      radius: 150,
      homeReference: false,
    };
  }

  editZone(zone: SafeZone): void {
    this.clearFieldErrors();
    this.editing = true;
    this.form = { ...zone, homeReference: !!zone.homeReference };
    this.mapVisible = true;
  }

  saveZone(): void {
    if (this.saving) {
      return;
    }
    this.clearFieldErrors();

    if (!this.form.name?.trim()) {
      this.fieldErrors.name = 'Please enter a name for this safe zone.';
    }

    if (!this.patients.length) {
      this.fieldErrors.patient =
        'No patients are available. You cannot create a safe zone until a patient is linked to your account.';
    } else if (!this.form.patientId) {
      this.fieldErrors.patient = 'Please select a patient to assign this zone to.';
    }

    const lat = Number(this.form.centerLatitude);
    const lng = Number(this.form.centerLongitude);
    if (
      Number.isNaN(lat) ||
      Number.isNaN(lng) ||
      lat < -90 ||
      lat > 90 ||
      lng < -180 ||
      lng > 180
    ) {
      this.fieldErrors.location =
        'The zone center is invalid. Click the map or drag the pin to set a valid location.';
    }

    if (Object.keys(this.fieldErrors).length) {
      this.showMsg('Please fix the errors below.', 'err');
      return;
    }

    const payload: SafeZone = {
      ...this.form,
      radius: Number(this.form.radius),
      centerLatitude: Number(this.form.centerLatitude),
      centerLongitude: Number(this.form.centerLongitude),
      homeReference: !!this.form.homeReference,
    };

    this.saving = true;

    if (this.editing && payload.id) {
      this.localization
        .updateSafeZone(payload.id, payload)
        .pipe(finalize(() => (this.saving = false)))
        .subscribe({
          next: () => {
            this.showMsg('Safe zone updated successfully.', 'ok');
            this.resetForm();
            this.loadAll(true);
          },
          error: (err: HttpErrorResponse) =>
            this.showMsg(this.httpErrorMessage(err, 'Failed to update the safe zone.'), 'err'),
        });
      return;
    }

    this.localization
      .createSafeZone(payload)
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: () => {
          this.showMsg('Safe zone created successfully.', 'ok');
          this.resetForm();
          this.loadAll(true);
        },
        error: (err: HttpErrorResponse) =>
          this.showMsg(this.httpErrorMessage(err, 'Failed to create the safe zone.'), 'err'),
      });
  }

  deleteZone(zone: SafeZone): void {
    if (!zone.id || this.deletingId != null) {
      return;
    }
    if (!confirm('Delete this safe zone? This cannot be undone.')) return;

    this.deletingId = zone.id;
    this.localization
      .deleteSafeZone(zone.id)
      .pipe(finalize(() => (this.deletingId = null)))
      .subscribe({
        next: () => {
          this.showMsg('Safe zone deleted.', 'ok');
          this.loadAll(true);
        },
        error: (err: HttpErrorResponse) =>
          this.showMsg(this.httpErrorMessage(err, 'Failed to delete the safe zone.'), 'err'),
      });
  }

  onMapCenter(ev: { lat: number; lng: number }): void {
    this.form.centerLatitude = ev.lat;
    this.form.centerLongitude = ev.lng;
    if (this.fieldErrors.location) {
      delete this.fieldErrors.location;
    }
  }

  onMapRadiusMeters(r: number): void {
    this.form.radius = r;
  }

  getPatientLabel(patientId: number): string {
    const id = this.normalizeId(patientId);
    const p = this.patients.find((x) => this.normalizeId(x.userId) === id);
    return p ? `${p.firstName} ${p.lastName}` : `Patient ${patientId}`;
  }

  formatCoord(n: number | undefined): string {
    if (n == null || Number.isNaN(Number(n))) {
      return '—';
    }
    return Number(n).toFixed(5);
  }

  openZoneOnOsm(zone: SafeZone): void {
    const la = zone.centerLatitude;
    const lo = zone.centerLongitude;
    const url = `https://www.openstreetmap.org/?mlat=${la}&mlon=${lo}#map=16/${la}/${lo}`;
    window.open(url, '_blank');
  }

  private showMsg(message: string, type: 'ok' | 'err'): void {
    const kind = type === 'err' ? 'error' : 'success';
    const durationMs = type === 'err' ? 6500 : 4200;
    this.toast.show(message, kind, durationMs);
    this.cdr.detectChanges();
  }
}
