import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap, shareReplay, catchError, throwError } from 'rxjs';
import { Router } from '@angular/router';
import { PatientRegistrationOption, User } from '../models/user.model';
import { environment } from '../../../environments/environment';
import { NotificationService } from './notification.service';

@Injectable({ providedIn: 'root' })
export class AuthService {

  private storageKey = 'currentUser';

  /** Cache session : une seule requête HTTP pour la liste patients (inscription). */
  private patientsForRegistration$: Observable<PatientRegistrationOption[]> | null = null;

  constructor(
    private http: HttpClient,
    private router: Router,
    private notificationService: NotificationService
  ) {}

  /** Users API base: direct 8081 in dev (see environment) or /api/users via proxy/gateway. */
  private usersApi(): string {
    if (environment.useUsersServiceDirect && environment.usersServiceBaseUrl) {
      const base = environment.usersServiceBaseUrl.replace(/\/$/, '');
      return `${base}/users`;
    }
    return `${environment.apiUrl}/users`;
  }

  login(email: string, password: string): Observable<User> {
    return this.http.post<User>(`${this.usersApi()}/login`, { email, password }).pipe(
      tap(user => localStorage.setItem(this.storageKey, JSON.stringify(user)))
    );
  }

  register(user: Partial<User> & { password: string; assignedPatientId?: number | null }): Observable<User> {
    return this.http.post<User>(`${this.usersApi()}/register`, user).pipe(
      tap(() => this.clearPatientsForRegistrationCache())
    );
  }

  /** Invalide le cache après inscription ou si la liste doit être rechargée. */
  clearPatientsForRegistrationCache(): void {
    this.patientsForRegistration$ = null;
  }

  logout(): void {
    this.notificationService.stopPolling();
    localStorage.removeItem(this.storageKey);
    this.router.navigateByUrl('/login-cover', { replaceUrl: true }).catch(() => {
      this.router.navigateByUrl('/', { replaceUrl: true });
    });
  }

  getCurrentUser(): User | null {
    const stored = localStorage.getItem(this.storageKey);
    if (!stored) return null;
    try {
      const data = JSON.parse(stored) as Record<string, unknown>;
      const uid = data['userId'] ?? data['id'];
      if (uid == null) return null;
      const rawRole = data['role'];
      let role: User['role'];
      if (typeof rawRole === 'string') {
        const u = rawRole.trim().toUpperCase();
        role = (
          ['ADMIN', 'PATIENT', 'DOCTOR', 'CAREGIVER', 'VOLUNTEER'].includes(u)
            ? u
            : rawRole.trim()
        ) as User['role'];
      } else {
        role = rawRole as User['role'];
      }
      return {
        userId: Number(uid),
        firstName: String(data['firstName'] ?? data['firstname'] ?? ''),
        lastName: String(data['lastName'] ?? data['lastname'] ?? ''),
        email: String(data['email'] ?? ''),
        role,
        phone: data['phone'] as string | undefined,
        caregiverId: (data['caregiverId'] as number | null | undefined) ?? null,
        volunteerId: (data['volunteerId'] as number | null | undefined) ?? null,
      };
    } catch {
      return null;
    }
  }

  /** Convenience alias for screens imported from other branches (e.g. cognitive activities). */
  get currentUser(): User | null {
    return this.getCurrentUser();
  }

  isLoggedIn(): boolean {
    return !!this.getCurrentUser();
  }

  getRole(): string {
    return this.getCurrentUser()?.role || '';
  }

  isAdmin(): boolean {
    return this.getRole() === 'ADMIN';
  }

  isPatient(): boolean {
    return this.getRole() === 'PATIENT';
  }

  /** Forum posts (création / édition / suppression) : réservé aux médecins. */
  isDoctor(): boolean {
    return this.getRole() === 'DOCTOR';
  }

  /** Back-office forum: create/edit/delete posts (doctors and admins). */
  canManageForumPosts(): boolean {
    return this.isAdmin() || this.isDoctor();
  }

  /** Admins manage any post; doctors only their own (userId matches). */
  canEditOrDeleteForumPost(post: { userId: number }): boolean {
    if (!this.canManageForumPosts()) {
      return false;
    }
    if (this.isAdmin()) {
      return true;
    }
    const uid = this.getUserId();
    return uid != null && Number(post.userId) === uid;
  }

  /** ADMIN or DOCTOR — can open MindCare staff tools at `/admin` (same as adminGuard). */
  hasStaffDashboardAccess(): boolean {
    const r = this.getRole();
    return r === 'ADMIN' || r === 'DOCTOR';
  }

  getUserId(): number | null {
    return this.getCurrentUser()?.userId ?? null;
  }

  getFullName(): string {
    const user = this.getCurrentUser();
    if (!user) return '';
    return `${user.firstName} ${user.lastName}`.trim();
  }

  /** Prénom et nom avec capitalisation lisible (ex. youssef ghrir → Youssef Ghrir). */
  getDisplayName(): string {
    const user = this.getCurrentUser();
    if (!user) return '';
    const cap = (s: string) => {
      const t = s.trim();
      if (!t) return '';
      return t.charAt(0).toUpperCase() + t.slice(1).toLowerCase();
    };
    return `${cap(user.firstName)} ${cap(user.lastName)}`.trim();
  }

  getPatientsByCaregiver(caregiverId: number): Observable<User[]> {
    return this.http.get<User[]>(`${this.usersApi()}/caregiver/${caregiverId}/patients`);
  }

  getPatientsByVolunteer(volunteerId: number): Observable<User[]> {
    return this.http.get<User[]>(`${this.usersApi()}/volunteer/${volunteerId}/patients`);
  }

  /**
   * Liste des comptes patient (inscription aidant / bénévole).
   * Mise en cache : bascule Caregiver ↔ Volunteer ne refait pas d’appel HTTP.
   */
  getPatientsForRegistration(): Observable<PatientRegistrationOption[]> {
    if (!this.patientsForRegistration$) {
      this.patientsForRegistration$ = this.http
        .get<PatientRegistrationOption[]>(`${this.usersApi()}/patients`)
        .pipe(
          catchError((err) => {
            this.patientsForRegistration$ = null;
            return throwError(() => err);
          }),
          shareReplay({ bufferSize: 1, refCount: false })
        );
    }
    return this.patientsForRegistration$;
  }

  getUserById(id: number): Observable<User> {
    return this.http.get<User>(`${this.usersApi()}/${id}`);
  }

  getAllUsers(): Observable<User[]> {
    return this.http.get<User[]>(`${this.usersApi()}`);
  }

  updateUser(id: number, data: Partial<User> & { caregiverId?: number | null; volunteerId?: number | null; password?: string }): Observable<User> {
    return this.http.put<User>(`${this.usersApi()}/${id}`, data);
  }

  /** Updates local session after profile edit or refresh. */
  setCurrentUser(user: User): void {
    localStorage.setItem(this.storageKey, JSON.stringify(user));
  }
}
