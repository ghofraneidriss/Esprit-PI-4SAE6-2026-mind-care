import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { User } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly usersApi = 'http://localhost:8082/api/users';

  constructor(private readonly http: HttpClient) {}

  getCurrentUser(): any | null {
    const raw = localStorage.getItem('loggedUser');
    if (!raw) return null;
    try {
      return JSON.parse(raw);
    } catch {
      return null;
    }
  }

  getUserId(): number | null {
    const u = this.getCurrentUser();
    return typeof u?.userId === 'number' ? u.userId : null;
  }

  getRole(): string | null {
    const u = this.getCurrentUser();
    return typeof u?.role === 'string' ? u.role : null;
  }

  getFullName(): string {
    const u = this.getCurrentUser();
    const first = (u?.firstName ?? '').toString().trim();
    const last = (u?.lastName ?? '').toString().trim();
    return `${first} ${last}`.trim();
  }

  getPatientsByCaregiver(caregiverId: number): Observable<User[]> {
    if (!caregiverId) return of([]);
    return this.http.get<User[]>(`${this.usersApi}/caregiver/${caregiverId}/patients`);
  }

  getUserById(userId: number): Observable<User> {
    return this.http.get<User>(`${this.usersApi}/${userId}`);
  }
}
