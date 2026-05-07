import { Injectable, NgZone } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, of, Subscription, timer } from 'rxjs';
import { catchError, distinctUntilChanged, map, switchMap, timeout } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export type ForumNotificationKind =
  | 'PATIENT_LOCATION_SHARED'
  | 'FORUM_COMMENT_FOLLOW'
  | 'FORUM_COMMENT_AUTHOR'
  | 'FORUM_LIKE'
  | 'FORUM_REACTION'
  | 'FORUM_RATING'
  | 'FORUM_THREAD_FOLLOW'
  | 'FORUM_BEST_ANSWER'
  | 'FORUM_ARCHIVE_AUTHOR'
  | 'FORUM_ARCHIVE_FOLLOWER'
  | string;

export interface AppNotification {
  id?: number;
  userId: number;
  message: string;
  type: 'INFO' | 'WARNING' | 'CRITICAL';
  read: boolean;
  incidentId?: number;
  postId?: number;
  createdAt?: string;
  eventKind?: ForumNotificationKind;
  actorName?: string | null;
  postTitle?: string | null;
  snippet?: string | null;
  actorUserId?: number | null;
}

/** Same logical event can be stored twice; key collapses duplicates for count + list. */
export function notificationDedupeKey(n: AppNotification): string {
  const p = n.postId ?? 0;
  const k = n.eventKind || 'LEGACY';
  const a = n.actorUserId ?? 0;
  const msg = (n.message || '').slice(0, 160);
  return `${p}|${k}|${a}|${msg}`;
}

export function dedupeNotifications(list: AppNotification[]): AppNotification[] {
  if (!list?.length) {
    return [];
  }
  const sorted = [...list].sort((a, b) => {
    const ta = new Date(a.createdAt || 0).getTime();
    const tb = new Date(b.createdAt || 0).getTime();
    return tb - ta;
  });
  const seen = new Set<string>();
  const out: AppNotification[] = [];
  for (const n of sorted) {
    const key = notificationDedupeKey(n);
    if (seen.has(key)) {
      continue;
    }
    seen.add(key);
    out.push(n);
  }
  return out;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly reqTimeoutMs = 8000;
  private unreadCount$ = new BehaviorSubject<number>(0);

  unreadCount = this.unreadCount$.asObservable();

  private pollingSub?: Subscription;
  private pollUserId: number | null = null;
  private visibilityListener?: () => void;
  private focusListener?: () => void;

  constructor(
    private http: HttpClient,
    private ngZone: NgZone
  ) {}

  /** Évite NG0100 : ne jamais pousser le compteur dans le même tour que la vérif. dev d’Angular. */
  private emitUnreadCount(c: number): void {
    queueMicrotask(() => {
      this.ngZone.run(() => this.unreadCount$.next(c));
    });
  }

  private notificationsBase(): string {
    if (environment.useUsersServiceDirect && environment.usersServiceBaseUrl) {
      const base = environment.usersServiceBaseUrl.replace(/\/$/, '');
      return `${base}/users/notifications`;
    }
    return `${environment.apiUrl}/users/notifications`;
  }

  /**
   * Arrête le polling (logout, destruction du shell) pour éviter requêtes dupliquées et fuites.
   */
  stopPolling(): void {
    this.pollingSub?.unsubscribe();
    this.pollingSub = undefined;
    this.pollUserId = null;
    if (typeof document !== 'undefined' && this.visibilityListener) {
      document.removeEventListener('visibilitychange', this.visibilityListener);
      this.visibilityListener = undefined;
    }
    if (typeof window !== 'undefined' && this.focusListener) {
      window.removeEventListener('focus', this.focusListener);
      this.focusListener = undefined;
    }
  }

  /**
   * Un seul flux : premier tick immédiat (`timer(0, …)`), déduplication, même logique que le badge.
   * Reprise d’onglet / fenêtre : rafraîchit le compteur tout de suite.
   */
  startPolling(userId: number, intervalMs = 4000): void {
    if (this.pollUserId === userId && this.pollingSub) {
      return;
    }
    this.stopPolling();
    this.pollUserId = userId;

    const tick = () =>
      this.getUnread(userId).pipe(
        map((rows) => dedupeNotifications(rows || []).length),
        catchError(() => of(0))
      );

    this.visibilityListener = (): void => {
      if (document.hidden || this.pollUserId == null) {
        return;
      }
      this.refreshUnreadCount(this.pollUserId);
    };
    document.addEventListener('visibilitychange', this.visibilityListener);

    this.focusListener = (): void => {
      if (this.pollUserId != null) {
        this.refreshUnreadCount(this.pollUserId);
      }
    };
    window.addEventListener('focus', this.focusListener);

    this.pollingSub = timer(0, intervalMs)
      .pipe(
        switchMap(() => tick()),
        distinctUntilChanged()
      )
      .subscribe((c) => this.emitUnreadCount(c));
  }

  getByUser(userId: number): Observable<AppNotification[]> {
    const url = `${this.notificationsBase()}/user/${userId}`;
    return this.http.get<AppNotification[]>(url).pipe(
      timeout(this.reqTimeoutMs),
      map((rows) => dedupeNotifications(rows || [])),
      catchError(() => of([]))
    );
  }

  getUnread(userId: number): Observable<AppNotification[]> {
    const url = `${this.notificationsBase()}/user/${userId}/unread`;
    return this.http.get<AppNotification[]>(url).pipe(
      timeout(this.reqTimeoutMs),
      catchError(() => of([]))
    );
  }

  getUnreadCount(userId: number): Observable<{ count: number }> {
    return this.getUnread(userId).pipe(
      map((rows) => ({ count: dedupeNotifications(rows || []).length }))
    );
  }

  markRead(id: number): Observable<AppNotification> {
    return this.http.patch<AppNotification>(`${this.notificationsBase()}/${id}/read`, {}).pipe(
      timeout(this.reqTimeoutMs)
    );
  }

  markAllRead(userId: number): Observable<void> {
    return this.http.patch<void>(`${this.notificationsBase()}/user/${userId}/read-all`, {}).pipe(
      timeout(this.reqTimeoutMs)
    );
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.notificationsBase()}/${id}`).pipe(timeout(this.reqTimeoutMs));
  }

  deleteAllForUser(userId: number): Observable<void> {
    const base = this.notificationsBase();
    const urlPost = `${base}/user/${userId}/delete-all`;
    return this.http.post<void>(urlPost, {}).pipe(
      timeout(this.reqTimeoutMs),
      catchError(() =>
        this.http.delete<void>(`${base}/user/${userId}/all`).pipe(timeout(this.reqTimeoutMs))
      )
    );
  }

  create(notification: Omit<AppNotification, 'id' | 'read' | 'createdAt'>): Observable<AppNotification> {
    return this.http.post<AppNotification>(this.notificationsBase(), notification);
  }

  /** Call after mark read / mark all so the badge matches deduped unread rows. */
  refreshUnreadCount(userId: number): void {
    this.getUnread(userId)
      .pipe(
        map((rows) => dedupeNotifications(rows || []).length),
        catchError(() => of(0))
      )
      .subscribe((c) => this.emitUnreadCount(c));
  }
}
