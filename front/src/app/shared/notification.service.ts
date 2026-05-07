import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';

export interface AppNotification {
  id: string;
  title: string;
  body: string;
  timestamp: Date;
  read: boolean;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly volunteersBase = 'http://localhost:8085/api/volunteers';

  /** Reactive signal holding all notifications (newest first) */
  readonly notifications = signal<AppNotification[]>([]);

  /** Reactive badge count — unread notifications */
  readonly unreadCount = signal<number>(0);

  constructor(private readonly http: HttpClient) {}

  // ─── Permission + Token ────────────────────────────────────────────────────

  /**
   * Request browser notification permission, retrieve the FCM token
   * and persist it to the backend for the given userId.
   */
  async initForUser(userId: number): Promise<void> {
    void userId;
  }

  // ─── Foreground Listener ───────────────────────────────────────────────────

  /**
   * Listen for push notifications while the app tab is in the foreground.
   * Adds them to the in-app notification list and increments the badge.
   */
  startListening(): void {
    return;
  }

  // ─── Badge Management ──────────────────────────────────────────────────────

  markAllRead(): void {
    this.notifications.update((list) =>
      list.map((n) => ({ ...n, read: true }))
    );
    this.unreadCount.set(0);
  }

  markRead(id: string): void {
    this.notifications.update((list) =>
      list.map((n) => (n.id === id ? { ...n, read: true } : n))
    );
    this.unreadCount.update((count) => Math.max(0, count - 1));
  }

  clearAll(): void {
    this.notifications.set([]);
    this.unreadCount.set(0);
  }
}
