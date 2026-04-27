import { ChangeDetectorRef, Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { forkJoin, of, Subscription } from 'rxjs';
import { switchMap } from 'rxjs/operators';
import { delay } from 'rxjs/operators';
import { AuthService } from '../../core/services/auth.service';
import {
  AppNotification,
  notificationDedupeKey,
  NotificationService,
} from '../../core/services/notification.service';
import {
  notificationFullMessageForDisplay,
  notificationTailForDisplay,
} from '../../core/utils/notification-display.util';
import { playNotificationChime } from '../../core/utils/notification-chime.util';
import { NgZoneUiSync } from '../../core/services/ng-zone-ui-sync.service';

const OFFICIEL_ACTIVITY_PATHS = [
  '/officiel/dashboard',
  '/officiel/quiz-list',
  '/officiel/results',
  '/officiel/performance',
] as const;

const FRONT_NOTIF_POLL_MS = 4000;

@Component({
  selector: 'app-header',
  standalone: false,
  templateUrl: './header.html',
  styleUrl: './header.css',
})
export class Header implements OnInit, OnDestroy {
  unreadCount = 0;
  notifications: AppNotification[] = [];
  showNotifPanel = false;
  private unreadSub?: Subscription;
  private notifUnreadFirstEmit = true;

  constructor(
    public authService: AuthService,
    private router: Router,
    private notifService: NotificationService,
    private readonly cdr: ChangeDetectorRef,
    private readonly zoneUi: NgZoneUiSync
  ) {}

  ngOnInit(): void {
    const userId = this.authService.getUserId();
    if (userId && this.authService.isLoggedIn()) {
      this.notifUnreadFirstEmit = true;
      this.notifService.startPolling(userId, FRONT_NOTIF_POLL_MS);
      this.unreadSub = this.notifService.unreadCount.pipe(delay(0)).subscribe((c) => {
        this.zoneUi.apply(this.cdr, () => {
          const prev = this.unreadCount;
          if (!this.notifUnreadFirstEmit && c > prev) {
            playNotificationChime();
            const uid = this.authService.getUserId();
            if (uid) {
              this.loadNotifications(uid);
            }
          }
          this.notifUnreadFirstEmit = false;
          this.unreadCount = c;
        });
      });
      this.loadNotifications(userId);
    }
  }

  ngOnDestroy(): void {
    this.unreadSub?.unsubscribe();
    this.notifService.stopPolling();
  }

  loadNotifications(userId: number): void {
    this.notifService.getByUser(userId).pipe(delay(0)).subscribe({
      next: (data) => {
        this.zoneUi.apply(this.cdr, () => {
          this.notifications = (data || []).slice(0, 30);
        });
      },
      error: () => {},
    });
  }

  toggleNotifPanel(): void {
    this.showNotifPanel = !this.showNotifPanel;
    if (this.showNotifPanel) {
      const userId = this.authService.getUserId();
      if (userId) {
        this.loadNotifications(userId);
        this.notifService.refreshUnreadCount(userId);
      }
    }
  }

  markAllRead(): void {
    const userId = this.authService.getUserId();
    if (!userId) return;
    this.notifService.markAllRead(userId).subscribe(() => {
      this.notifications = this.notifications.map((n) => ({ ...n, read: true }));
      this.notifService.refreshUnreadCount(userId);
    });
  }

  deleteOne(notif: AppNotification, event: Event): void {
    event.stopPropagation();
    event.preventDefault();
    if (notif.id == null) return;
    const userId = this.authService.getUserId();
    if (!userId) return;
    this.notifService.delete(notif.id).subscribe(() => {
      this.notifications = this.notifications.filter((n) => n.id !== notif.id);
      this.notifService.refreshUnreadCount(userId);
    });
  }

  deleteAll(): void {
    if (this.notifications.length === 0) return;
    const userId = this.authService.getUserId();
    if (!userId) return;
    this.notifService.deleteAllForUser(userId).subscribe(() => {
      this.notifications = [];
      this.notifService.refreshUnreadCount(userId);
    });
  }

  /** Google Maps directions URL stored in `snippet` for live location shares. */
  mapsUrl(n: AppNotification): string | null {
    const s = n.snippet?.trim();
    return s?.startsWith('http') ? s : null;
  }

  /**
   * Dedicated CTA: opens Maps in a new tab without firing the row action; marks read when applicable.
   */
  onLocationMapsLinkClick(notif: AppNotification, event: MouseEvent): void {
    event.stopPropagation();
    const userId = this.authService.getUserId();
    if (!userId || notif.read || notif.id == null) {
      return;
    }
    this.notifService.markRead(notif.id).subscribe({
      next: () => {
        this.notifService.refreshUnreadCount(userId);
        this.loadNotifications(userId);
      },
      error: () => {},
    });
  }

  onNotificationActivate(notif: AppNotification, event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    this.showNotifPanel = false;

    const userId = this.authService.getUserId();
    if (!userId) {
      this.navigateForNotif(notif);
      return;
    }

    if (notif.read || notif.id == null) {
      this.navigateForNotif(notif);
      return;
    }

    const key = notificationDedupeKey(notif);
    this.notifService
      .getUnread(userId)
      .pipe(
        switchMap((unread) => {
          const ids = (unread || [])
            .filter((u) => notificationDedupeKey(u) === key && u.id != null)
            .map((u) => u.id as number);
          if (ids.length === 0) {
            return of(null);
          }
          return forkJoin(ids.map((id) => this.notifService.markRead(id)));
        })
      )
      .subscribe(() => {
        this.notifService.refreshUnreadCount(userId);
        this.loadNotifications(userId);
        this.navigateForNotif(notif);
      });
  }

  private navigateForNotif(notif: AppNotification): void {
    if (notif.eventKind === 'PATIENT_LOCATION_SHARED' && notif.snippet?.trim().startsWith('http')) {
      window.open(notif.snippet.trim(), '_blank', 'noopener,noreferrer');
      const path = this.authService.hasStaffDashboardAccess() ? '/admin/patient-movement' : '/patient-movement';
      void this.router.navigate([path]);
      return;
    }
    if (notif.postId != null && notif.postId > 0) {
      void this.router.navigate(['/forum/post', notif.postId]);
      return;
    }
    if (notif.incidentId != null && notif.incidentId > 0) {
      void this.router.navigate(['/incidents/history']);
    }
  }

  /** Icon per forum event; fallback to severity type. */
  eventIconFor(n: AppNotification): string {
    const k = n.eventKind || '';
    const map: Record<string, string> = {
      PATIENT_LOCATION_SHARED: 'ri-map-pin-user-fill',
      FORUM_COMMENT_FOLLOW: 'ri-chat-3-line',
      FORUM_COMMENT_AUTHOR: 'ri-chat-quote-line',
      FORUM_LIKE: 'ri-heart-3-fill',
      FORUM_REACTION: 'ri-emotion-happy-line',
      FORUM_RATING: 'ri-star-fill',
      FORUM_THREAD_FOLLOW: 'ri-user-add-fill',
      FORUM_BEST_ANSWER: 'ri-medal-fill',
      FORUM_ARCHIVE_AUTHOR: 'ri-archive-line',
      FORUM_ARCHIVE_FOLLOWER: 'ri-archive-drawer-line',
    };
    return map[k] || this.getTypeIcon(n.type);
  }

  eventIconClass(n: AppNotification): string {
    const k = n.eventKind || '';
    if (k === 'PATIENT_LOCATION_SHARED') return 'alzcare-notif-ico--location';
    if (k === 'FORUM_LIKE') return 'alzcare-notif-ico--like';
    if (k.startsWith('FORUM_COMMENT')) return 'alzcare-notif-ico--comment';
    if (k === 'FORUM_REACTION') return 'alzcare-notif-ico--react';
    if (k === 'FORUM_RATING') return 'alzcare-notif-ico--star';
    if (k === 'FORUM_THREAD_FOLLOW') return 'alzcare-notif-ico--follow';
    if (k === 'FORUM_BEST_ANSWER' || k.includes('ARCHIVE')) return 'alzcare-notif-ico--sys';
    return 'alzcare-notif-ico--info';
  }

  /** Text after actor name — clearer wording and reaction (haha) 😄 formatting. */
  messageTail(n: AppNotification): string {
    return notificationTailForDisplay(n);
  }

  plainNotificationMessage(n: AppNotification): string {
    return notificationFullMessageForDisplay(n);
  }

  showActorHighlight(n: AppNotification): boolean {
    if (!n.actorName || !n.message) {
      return false;
    }
    return n.message.trim().startsWith(n.actorName.trim());
  }

  getTypeIcon(type: string): string {
    const icons: Record<string, string> = {
      INFO: 'ri-notification-3-line',
      WARNING: 'ri-error-warning-line',
      CRITICAL: 'ri-alarm-warning-line',
    };
    return icons[type] || 'ri-notification-3-line';
  }

  relativeTimeEn(iso?: string): string {
    if (!iso) return '';
    const t = new Date(iso).getTime();
    if (Number.isNaN(t)) return '';
    const s = Math.floor((Date.now() - t) / 1000);
    if (s < 45) return 'just now';
    if (s < 3600) return `${Math.floor(s / 60)} min`;
    if (s < 86400) return `${Math.floor(s / 3600)} h`;
    return `${Math.floor(s / 86400)} d`;
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    const t = event.target as HTMLElement;
    if (!t.closest('.alzcare-notif-wrap')) {
      this.showNotifPanel = false;
    }
  }

  get activitiesNavLink(): string {
    return this.authService.hasStaffDashboardAccess() ? '/admin' : '/officiel/quiz-list';
  }

  get isActivitiesNavActive(): boolean {
    const path = this.router.url.split('?')[0];
    if (this.authService.hasStaffDashboardAccess()) {
      return path === '/admin' || path.startsWith('/admin/');
    }
    return OFFICIEL_ACTIVITY_PATHS.some((p) => path === p);
  }

  get showIncidentReportLink(): boolean {
    const r = this.authService.getRole();
    return r === 'CAREGIVER' || r === 'VOLUNTEER';
  }

  get showMovementLink(): boolean {
    const r = this.authService.getRole();
    return r === 'PATIENT' || r === 'CAREGIVER' || r === 'VOLUNTEER';
  }

  get showSafeZonesLink(): boolean {
    const r = this.authService.getRole();
    return r === 'CAREGIVER' || r === 'VOLUNTEER';
  }

  get isCaregiverVolunteerIncidentsMenu(): boolean {
    return this.showIncidentReportLink;
  }

  get isIncidentsDropdownActive(): boolean {
    const p = this.router.url.split('?')[0];
    return p.startsWith('/incidents');
  }

  get profileInitials(): string {
    const u = this.authService.getCurrentUser();
    if (!u) return '?';
    const a = (u.firstName || '?').charAt(0);
    const b = (u.lastName || '').charAt(0);
    return (a + b).toUpperCase();
  }

  logout(): void {
    this.authService.logout();
  }
}
