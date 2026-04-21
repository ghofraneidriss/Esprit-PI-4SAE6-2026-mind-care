import { ChangeDetectorRef, Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
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

const BO_NOTIF_POLL_MS = 4000;

@Component({
  selector: 'app-backoffice-header',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './header.html',
  styleUrl: './header.css',
})
export class BackofficeHeader implements OnInit, OnDestroy {
  unreadCount = 0;
  notifications: AppNotification[] = [];
  showDropdown = false;
  private sub!: Subscription;
  private notifUnreadFirstEmit = true;

  constructor(
    public authService: AuthService,
    private notifService: NotificationService,
    private router: Router,
    private readonly cdr: ChangeDetectorRef,
    private readonly zoneUi: NgZoneUiSync
  ) {}

  ngOnInit(): void {
    const userId = this.authService.getUserId();
    if (userId) {
      this.notifUnreadFirstEmit = true;
      this.notifService.startPolling(userId, BO_NOTIF_POLL_MS);
      this.sub = this.notifService.unreadCount.pipe(delay(0)).subscribe((c) => {
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
    this.sub?.unsubscribe();
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

  toggleDropdown(): void {
    this.showDropdown = !this.showDropdown;
    if (this.showDropdown) {
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

  mapsUrl(n: AppNotification): string | null {
    const s = n.snippet?.trim();
    return s?.startsWith('http') ? s : null;
  }

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

  onNotificationRowClick(notif: AppNotification, event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    this.showDropdown = false;

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
      void this.router.navigate(['/admin/patient-movement']);
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
    return map[k] || this.getTypeIconClassOnly(n.type);
  }

  getTypeIconClassOnly(type: string): string {
    const icons: Record<string, string> = {
      INFO: 'ri-notification-3-line',
      WARNING: 'ri-error-warning-line',
      CRITICAL: 'ri-alarm-warning-line',
    };
    return icons[type] || 'ri-notification-3-line';
  }

  eventIconClass(n: AppNotification): string {
    const k = n.eventKind || '';
    if (k === 'PATIENT_LOCATION_SHARED') return 'bo-notif-ico--location';
    if (k === 'FORUM_LIKE') return 'bo-notif-ico--like';
    if (k.startsWith('FORUM_COMMENT')) return 'bo-notif-ico--comment';
    if (k === 'FORUM_REACTION') return 'bo-notif-ico--react';
    if (k === 'FORUM_RATING') return 'bo-notif-ico--star';
    if (k === 'FORUM_THREAD_FOLLOW') return 'bo-notif-ico--follow';
    if (k === 'FORUM_BEST_ANSWER' || k.includes('ARCHIVE')) return 'bo-notif-ico--sys';
    return 'bo-notif-ico--info';
  }

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
      INFO: 'ri-chat-3-line',
      WARNING: 'ri-error-warning-line text-warning',
      CRITICAL: 'ri-alarm-warning-line text-danger',
    };
    return icons[type] || 'ri-notification-3-line text-secondary';
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
    const target = event.target as HTMLElement;
    if (!target.closest('.notif-dropdown-wrapper')) {
      this.showDropdown = false;
    }
  }

  logout(): void {
    this.authService.logout();
  }

  get userInitials(): string {
    const u = this.authService.getCurrentUser();
    if (!u) return '?';
    const a = (u.firstName?.trim()?.charAt(0) || '').toUpperCase();
    const b = (u.lastName?.trim()?.charAt(0) || '').toUpperCase();
    if (a && b) return a + b;
    if (a) return a.length >= 2 ? a.slice(0, 2) : a;
    if (u.email) return u.email.charAt(0).toUpperCase();
    return '?';
  }
}
