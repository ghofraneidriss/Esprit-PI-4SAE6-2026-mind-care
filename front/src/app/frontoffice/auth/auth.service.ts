import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export interface AuthUser {
  userId: number;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  role?: string;
  createdAt?: string;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  role: string;
  phone?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
  role: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly apiUrl = 'http://localhost:8082/api/users';
  private readonly storageKey = 'loggedUser';
  private readonly backofficeRoles = new Set(['ADMIN', 'DOCTOR', 'CAREGIVER']);

  constructor(private readonly http: HttpClient) {}

  register(payload: RegisterRequest): Observable<AuthUser> {
    return this.http.post<AuthUser>(`${this.apiUrl}/register`, payload);
  }

  login(payload: LoginRequest): Observable<AuthUser> {
    return this.http
      .post<AuthUser>(`${this.apiUrl}/login`, payload)
      .pipe(tap((user) => localStorage.setItem(this.storageKey, JSON.stringify(user))));
  }

  getLoggedUser(): AuthUser | null {
    const rawUser = localStorage.getItem(this.storageKey);
    if (!rawUser) {
      return null;
    }

    try {
      return JSON.parse(rawUser) as AuthUser;
    } catch {
      localStorage.removeItem(this.storageKey);
      return null;
    }
  }

  getLoggedRole(): string {
    const role = this.getLoggedUser()?.role ?? '';
    return this.normalizeRole(role);
  }

  isAdmin(): boolean {
    return this.getLoggedRole() === 'ADMIN';
  }

  isBackofficeRole(role?: string | null): boolean {
    const normalizedRole = this.normalizeRole(role ?? this.getLoggedRole());
    return this.backofficeRoles.has(normalizedRole);
  }

  isPatient(): boolean {
    return this.getLoggedRole() === 'PATIENT';
  }

  logout(): void {
    localStorage.removeItem(this.storageKey);
  }

  normalizeRole(role: string | undefined | null): string {
    return (role ?? '').trim().toUpperCase().replace(/^ROLE_/, '');
  }
}
