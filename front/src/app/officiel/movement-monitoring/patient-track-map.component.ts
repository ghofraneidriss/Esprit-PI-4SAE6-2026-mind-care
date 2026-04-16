import {
  AfterViewInit,
  Component,
  ElementRef,
  Input,
  NgZone,
  OnChanges,
  OnDestroy,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import * as L from 'leaflet';
import { LocationPing, SafeZone } from '../../core/services/patient-movement.service';
import {
  fetchOsrmRouteLonLat,
  haversineMeters,
} from './osrm-routing.util';

@Component({
  selector: 'app-patient-track-map',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './patient-track-map.component.html',
  styleUrls: ['./patient-track-map.component.css'],
})
export class PatientTrackMapComponent implements AfterViewInit, OnChanges, OnDestroy {
  @Input() latestLocation: LocationPing | null = null;
  @Input() history: LocationPing[] = [];
  @Input() safeZones: SafeZone[] = [];
  /** When set (e.g. doctor / caregiver / volunteer GPS), draws pickup route to the patient on this map. */
  @Input() staffJoinPoint: { latitude: number; longitude: number } | null = null;
  /** Parent increments after each successful poll so the map redraws even if object refs look unchanged. */
  @Input() mapRevision = 0;
  /** Shorter hints + legend for the patient-facing screen. */
  @Input() patientMode = false;
  /** Caregiver / volunteer / doctor: taller map using more of the viewport. */
  @Input() staffWideViewport = false;

  @ViewChild('mapContainer', { static: false }) mapContainer?: ElementRef<HTMLDivElement>;

  private map: L.Map | null = null;
  private readonly overlayLayers: L.Layer[] = [];
  private resizeObserver: ResizeObserver | null = null;
  private intersectionObserver: IntersectionObserver | null = null;
  private onWindowResize = (): void => this.invalidateMapSize();
  /** Incremented on each redraw to ignore stale async route results. */
  private routeEpoch = 0;

  private readonly markerIcon = L.icon({
    iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
    iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
    shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
    iconSize: [25, 41],
    iconAnchor: [12, 41],
    popupAnchor: [1, -34],
    shadowSize: [41, 41],
  });

  private readonly staffIcon = L.divIcon({
    className: 'staff-join-marker',
    html: '<div class="staff-join-dot" title="Your position"></div>',
    iconSize: [22, 22],
    iconAnchor: [11, 11],
  });

  constructor(
    private ngZone: NgZone,
    private http: HttpClient
  ) {}

  ngAfterViewInit(): void {
    queueMicrotask(() => this.ensureMap());
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (!this.map) {
      return;
    }
    if (
      changes['latestLocation'] ||
      changes['history'] ||
      changes['safeZones'] ||
      changes['staffJoinPoint'] ||
      changes['mapRevision']
    ) {
      queueMicrotask(() => this.redraw());
    }
  }

  ngOnDestroy(): void {
    this.routeEpoch += 1;
    this.resizeObserver?.disconnect();
    this.resizeObserver = null;
    this.intersectionObserver?.disconnect();
    this.intersectionObserver = null;
    if (typeof window !== 'undefined') {
      window.removeEventListener('resize', this.onWindowResize);
    }
    this.map?.remove();
    this.map = null;
  }

  private invalidateMapSize(): void {
    if (!this.map) {
      return;
    }
    this.ngZone.runOutsideAngular(() => {
      requestAnimationFrame(() => {
        this.map?.invalidateSize({ animate: false });
        requestAnimationFrame(() => this.map?.invalidateSize({ animate: false }));
      });
    });
  }

  private ensureMap(): void {
    const el = this.mapContainer?.nativeElement;
    if (!el || this.map) {
      return;
    }

    const center: L.LatLngExpression = [36.8065, 10.1815];
    this.map = L.map(el, {
      zoomControl: true,
      preferCanvas: false,
    }).setView(center, 13);

    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
      crossOrigin: true,
    }).addTo(this.map);

    this.resizeObserver = new ResizeObserver(() => this.invalidateMapSize());
    this.resizeObserver.observe(el);

    if (typeof window !== 'undefined') {
      window.addEventListener('resize', this.onWindowResize, { passive: true });
    }

    this.intersectionObserver = new IntersectionObserver(
      (entries) => {
        if (entries.some((e) => e.isIntersecting && e.intersectionRatio > 0)) {
          this.invalidateMapSize();
        }
      },
      { root: null, threshold: [0, 0.1, 0.5] }
    );
    this.intersectionObserver.observe(el);

    setTimeout(() => {
      this.invalidateMapSize();
      this.redraw();
    }, 0);
    setTimeout(() => this.invalidateMapSize(), 150);
    setTimeout(() => this.invalidateMapSize(), 400);
    setTimeout(() => this.invalidateMapSize(), 900);
  }

  private redraw(): void {
    if (!this.map) {
      return;
    }

    this.routeEpoch += 1;
    const epoch = this.routeEpoch;

    for (const layer of this.overlayLayers) {
      this.map.removeLayer(layer);
    }
    this.overlayLayers.length = 0;

    const extent: L.LatLng[] = [];
    const zones = this.normalizeSafeZones(this.safeZones);

    for (const z of zones) {
      const circle = L.circle([z.centerLatitude, z.centerLongitude], {
        radius: Math.max(10, z.radius),
        color: '#2563eb',
        weight: 2,
        fillColor: '#3b82f6',
        fillOpacity: 0.12,
      });
      circle.addTo(this.map);
      this.overlayLayers.push(circle);
      extent.push(L.latLng(z.centerLatitude, z.centerLongitude));
    }

    const sortedHistory = [...(this.history || [])].sort(
      (a, b) =>
        new Date(a.recordedAt || 0).getTime() - new Date(b.recordedAt || 0).getTime()
    );
    const path = sortedHistory
      .filter((h) => h.latitude != null && h.longitude != null)
      .map((h) => [h.latitude, h.longitude] as [number, number]);

    if (path.length >= 2) {
      const poly = L.polyline(path, { color: '#64748b', weight: 3, opacity: 0.75 });
      poly.addTo(this.map);
      this.overlayLayers.push(poly);
      path.forEach((p) => extent.push(L.latLng(p[0], p[1])));
    }

    if (this.latestLocation?.latitude != null && this.latestLocation?.longitude != null) {
      const m = L.marker([this.latestLocation.latitude, this.latestLocation.longitude], {
        icon: this.markerIcon,
      });
      m.bindPopup(this.patientMode ? 'Your position' : 'Latest patient position').addTo(this.map);
      this.overlayLayers.push(m);
      extent.push(L.latLng(this.latestLocation.latitude, this.latestLocation.longitude));
    } else if (path.length === 1) {
      extent.push(L.latLng(path[0][0], path[0][1]));
    }

    if (this.staffJoinPoint?.latitude != null && this.staffJoinPoint?.longitude != null) {
      const sm = L.marker([this.staffJoinPoint.latitude, this.staffJoinPoint.longitude], {
        icon: this.staffIcon,
      });
      sm.bindPopup('You (pickup)').addTo(this.map);
      this.overlayLayers.push(sm);
      extent.push(L.latLng(this.staffJoinPoint.latitude, this.staffJoinPoint.longitude));
    }

    // Straight line staff → patient (sync, reliable — does not depend on public OSRM mirrors / CORS).
    if (
      this.staffJoinPoint?.latitude != null &&
      this.staffJoinPoint?.longitude != null &&
      this.latestLocation?.latitude != null &&
      this.latestLocation?.longitude != null
    ) {
      const staffLat = this.staffJoinPoint.latitude;
      const staffLon = this.staffJoinPoint.longitude;
      const patLat = this.latestLocation.latitude;
      const patLon = this.latestLocation.longitude;
      const joinLine = L.polyline(
        [
          [staffLat, staffLon],
          [patLat, patLon],
        ],
        {
          color: '#ea580c',
          weight: 4,
          opacity: 0.9,
          dashArray: '10 7',
        }
      );
      joinLine.bindPopup('Straight line: you → patient (approx.). Use Google Maps for driving directions.');
      joinLine.addTo(this.map);
      this.overlayLayers.push(joinLine);
    }

    if (extent.length > 0) {
      const bounds = L.latLngBounds(extent);
      this.map.fitBounds(bounds.pad(0.15));
    }

    this.invalidateMapSize();
    setTimeout(() => this.invalidateMapSize(), 80);

    void this.runAsyncRoutes(epoch, [...extent], zones);
  }

  private normalizeSafeZones(raw: SafeZone[]): SafeZone[] {
    if (!raw?.length) {
      return [];
    }
    return raw
      .map((z) => {
        const lat = Number((z as any).centerLatitude ?? (z as any).center_lat);
        const lon = Number((z as any).centerLongitude ?? (z as any).center_lon ?? (z as any).centerLng);
        const rad = Number((z as any).radius ?? (z as any).radiusMeters ?? 200);
        if (!Number.isFinite(lat) || !Number.isFinite(lon)) {
          return null;
        }
        return {
          ...z,
          centerLatitude: lat,
          centerLongitude: lon,
          radius: Number.isFinite(rad) && rad > 0 ? rad : 200,
        };
      })
      .filter((z): z is SafeZone => z != null);
  }

  private async runAsyncRoutes(epoch: number, extentSnapshot: L.LatLng[], zones: SafeZone[]): Promise<void> {
    const lat = this.latestLocation?.latitude;
    const lon = this.latestLocation?.longitude;
    const tasks: Promise<void>[] = [];

    if (lat != null && lon != null && zones.length > 0 && this.map) {
      const target = this.pickNearestZone(lat, lon, zones);
      tasks.push(this.applyPatientToSafeZoneRoute(epoch, lat, lon, target, extentSnapshot));
    }

    await Promise.all(tasks);
  }

  private async applyPatientToSafeZoneRoute(
    epoch: number,
    lat: number,
    lon: number,
    target: SafeZone,
    extentBefore: L.LatLng[]
  ): Promise<void> {
    const path = await fetchOsrmRouteLonLat(
      this.http,
      lon,
      lat,
      target.centerLongitude,
      target.centerLatitude
    );
    if (epoch !== this.routeEpoch || !this.map) {
      return;
    }
    if (path && path.length >= 2) {
      const latlngs: L.LatLngExpression[] = path.map(([lo, la]) => [la, lo]);
      const poly = L.polyline(latlngs, { color: '#16a34a', weight: 4, opacity: 0.9 });
      poly.bindPopup('Road route: patient → safe zone');
      poly.addTo(this.map);
      this.overlayLayers.push(poly);
      this.refitBounds([...extentBefore, ...latlngs.map((ll) => L.latLng(ll as [number, number]))]);
    } else {
      this.addStraightLineToZone(lat, lon, target, extentBefore, epoch);
    }
    this.invalidateMapSize();
  }

  private pickNearestZone(lat: number, lon: number, zones: SafeZone[]): SafeZone {
    let best = zones[0];
    let bestD = Number.POSITIVE_INFINITY;
    for (const z of zones) {
      const d = haversineMeters(lat, lon, z.centerLatitude, z.centerLongitude);
      if (d < bestD) {
        bestD = d;
        best = z;
      }
    }
    return best;
  }

  private addStraightLineToZone(
    lat: number,
    lon: number,
    target: SafeZone,
    extentBeforeRoute: L.LatLng[],
    epoch: number
  ): void {
    if (epoch !== this.routeEpoch || !this.map) {
      return;
    }
    const line = L.polyline(
      [
        [lat, lon],
        [target.centerLatitude, target.centerLongitude],
      ],
      { color: '#15803d', weight: 3, opacity: 0.82, dashArray: '10 8' }
    );
    line.bindPopup('Straight line to safe zone (routing servers unavailable)');
    line.addTo(this.map);
    this.overlayLayers.push(line);

    const ext = [...extentBeforeRoute, L.latLng(target.centerLatitude, target.centerLongitude)];
    this.refitBounds(ext);
    this.invalidateMapSize();
  }

  private refitBounds(ext: L.LatLng[]): void {
    if (!this.map || ext.length === 0) {
      return;
    }
    this.map.fitBounds(L.latLngBounds(ext).pad(0.12));
  }
}
