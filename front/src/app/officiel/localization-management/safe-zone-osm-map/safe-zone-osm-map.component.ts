import {
  AfterViewInit,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  NgZone,
  OnChanges,
  OnDestroy,
  Output,
  SimpleChanges,
  ViewChild,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import * as L from 'leaflet';

/** Same default as movement tracking when no coordinates exist yet (Tunisia). */
const DEFAULT_CENTER: L.LatLngTuple = [36.8065, 10.1815];

@Component({
  selector: 'app-safe-zone-osm-map',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './safe-zone-osm-map.component.html',
  styleUrls: ['./safe-zone-osm-map.component.css'],
})
export class SafeZoneOsmMapComponent implements AfterViewInit, OnChanges, OnDestroy {
  @Input() centerLatitude = 0;
  @Input() centerLongitude = 0;
  @Input() radius = 150;

  @Output() centerChange = new EventEmitter<{ lat: number; lng: number }>();
  @Output() radiusChange = new EventEmitter<number>();

  @ViewChild('mapEl', { static: false }) mapEl?: ElementRef<HTMLDivElement>;

  mapReady = false;
  tileError = false;
  radiusModel = 150;

  private map: L.Map | null = null;
  private marker: L.Marker | null = null;
  private circle: L.Circle | null = null;
  private resizeObserver: ResizeObserver | null = null;
  private readonly onWinResize = (): void => this.invalidateMapSize();

  private readonly markerIcon = L.icon({
    iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
    iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
    shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
    iconSize: [25, 41],
    iconAnchor: [12, 41],
    popupAnchor: [1, -34],
    shadowSize: [41, 41],
  });

  constructor(private ngZone: NgZone) {}

  ngAfterViewInit(): void {
    queueMicrotask(() => this.ensureMap());
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (!this.mapReady || !this.map || !this.marker || !this.circle) {
      return;
    }
    if (changes['centerLatitude'] || changes['centerLongitude'] || changes['radius']) {
      this.applyInputsToMap();
    }
  }

  ngOnDestroy(): void {
    window.removeEventListener('resize', this.onWinResize);
    this.resizeObserver?.disconnect();
    this.resizeObserver = null;
    this.map?.remove();
    this.map = null;
    this.marker = null;
    this.circle = null;
  }

  onRadiusSliderInput(value: number): void {
    const r = Math.max(20, Math.min(5000, Number(value) || 150));
    this.radiusModel = r;
    this.circle?.setRadius(r);
    this.radiusChange.emit(r);
  }

  private ensureMap(): void {
    const el = this.mapEl?.nativeElement;
    if (!el || this.map) {
      return;
    }

    const start = this.getInitialCenter();
    this.radiusModel = Math.max(20, Number(this.radius) || 150);
    this.tileError = false;

    this.map = L.map(el, { zoomControl: true }).setView(start, 14);

    const tiles = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
      crossOrigin: true,
    });
    tiles.on('tileerror', () => {
      this.ngZone.run(() => {
        this.tileError = true;
      });
    });
    tiles.addTo(this.map);

    this.marker = L.marker(start, { draggable: true, icon: this.markerIcon }).addTo(this.map);

    this.circle = L.circle(start, {
      radius: this.radiusModel,
      color: '#e11d48',
      weight: 2,
      fillColor: '#f43f5e',
      fillOpacity: 0.2,
    }).addTo(this.map);

    this.map.on('click', (e: L.LeafletMouseEvent) => {
      this.ngZone.run(() => this.emitCenter(e.latlng.lat, e.latlng.lng));
    });

    this.marker.on('dragend', () => {
      const ll = this.marker?.getLatLng();
      if (ll) {
        this.ngZone.run(() => this.emitCenter(ll.lat, ll.lng));
      }
    });

    this.resizeObserver = new ResizeObserver(() => this.invalidateMapSize());
    this.resizeObserver.observe(el);
    window.addEventListener('resize', this.onWinResize);

    this.map.whenReady(() => {
      this.mapReady = true;
      this.applyInputsToMap();
      const lat = Number(this.centerLatitude);
      const lng = Number(this.centerLongitude);
      if (!this.isValidLatLng(lat, lng)) {
        const c = this.getInitialCenter();
        this.ngZone.run(() => this.emitCenter(c[0], c[1]));
      }
      this.scheduleInvalidate();
    });
  }

  private applyInputsToMap(): void {
    if (!this.map || !this.marker || !this.circle) {
      return;
    }
    const lat = Number(this.centerLatitude);
    const lng = Number(this.centerLongitude);
    if (this.isValidLatLng(lat, lng)) {
      const ll = L.latLng(lat, lng);
      this.marker.setLatLng(ll);
      this.circle.setLatLng(ll);
      this.map.setView(ll, 15, { animate: false });
    }
    const r = Math.max(20, Number(this.radius) || 150);
    this.radiusModel = r;
    this.circle.setRadius(r);
  }

  private getInitialCenter(): L.LatLngTuple {
    const lat = Number(this.centerLatitude);
    const lng = Number(this.centerLongitude);
    if (this.isValidLatLng(lat, lng)) {
      return [lat, lng];
    }
    return [DEFAULT_CENTER[0], DEFAULT_CENTER[1]];
  }

  private isValidLatLng(lat: number, lng: number): boolean {
    if (Number.isNaN(lat) || Number.isNaN(lng)) {
      return false;
    }
    if (lat === 0 && lng === 0) {
      return false;
    }
    return lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
  }

  private emitCenter(lat: number, lng: number): void {
    const ll = L.latLng(lat, lng);
    this.marker?.setLatLng(ll);
    this.circle?.setLatLng(ll);
    this.map?.panTo(ll);
    this.centerChange.emit({ lat, lng });
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

  private scheduleInvalidate(): void {
    for (const ms of [0, 100, 250, 500, 900]) {
      setTimeout(() => this.invalidateMapSize(), ms);
    }
  }
}
