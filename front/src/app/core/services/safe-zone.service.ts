import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { environment } from '../../../environments/environment';
import { User } from '../models/user.model';

export interface LocalizationUser {
  userId: number;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  role: string;
}

export interface SafeZone {
  id?: number;
  name: string;
  centerLatitude: number;
  centerLongitude: number;
  radius: number;
  patientId: number;
  /** Registered home — reference for caregivers/volunteers; alerts on every exit from this circle. */
  homeReference?: boolean;
}

export interface OSMPlace {
  display_name: string;
  lat: string;
  lon: string;
}

@Injectable({ providedIn: 'root' })
export class SafeZoneService {
  private readonly gateway = environment.gatewayBaseUrl.replace(/\/$/, '');
  private readonly nominatimApi = 'https://nominatim.openstreetmap.org/search';

  constructor(private http: HttpClient) {}

  private safeZoneApiBase(): string {
    if (
      environment.useMovementLocalizationDirect &&
      environment.localizationServiceBaseUrl
    ) {
      return environment.localizationServiceBaseUrl.replace(/\/$/, '');
    }
    return this.gateway;
  }

  private usersListUrl(): string {
    if (environment.useUsersServiceDirect && environment.usersServiceBaseUrl) {
      return `${environment.usersServiceBaseUrl.replace(/\/$/, '')}/users`;
    }
    return `${this.gateway}/users`;
  }

  getPatients(): Observable<LocalizationUser[]> {
    return this.http.get<User[]>(this.usersListUrl()).pipe(
      map((users) =>
        (users || [])
          .filter((u) => u.role === 'PATIENT')
          .map((u) => ({
            userId: u.userId,
            firstName: u.firstName,
            lastName: u.lastName,
            email: u.email,
            phone: u.phone,
            role: u.role,
          }))
      )
    );
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

  searchPlace(query: string): Observable<OSMPlace[]> {
    const params = new HttpParams().set('format', 'jsonv2').set('limit', '6').set('q', query);
    const headers = new HttpHeaders({ 'Accept-Language': 'fr' });
    return this.http.get<OSMPlace[]>(this.nominatimApi, { params, headers });
  }
}
