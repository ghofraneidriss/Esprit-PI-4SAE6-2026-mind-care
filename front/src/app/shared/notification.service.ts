import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { messaging, VAPID_KEY } from '../firebase-config';
import { getToken, onMessage, MessagePayload } from 'firebase/messaging';

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
    try {
      if (typeof Notification === 'undefined') {
        return;
      }

      // If browser has already blocked notifications, do not re-prompt.
      // This avoids repeated browser warnings in console.
      if (Notification.permission === 'denied') {
        return;
      }

      const permission =
        Notification.permission === 'granted'
          ? 'granted'
          : await Notification.requestPermission();
      if (permission !== 'granted') {
        // User denied notifications; silently skip FCM setup.
        return;
      }

      const token = await getToken(messaging, { vapidKey: VAPID_KEY });
      if (!token) {
        console.warn('[FCM] Failed to get FCM token.');
        return;
      }

      console.log('FCM Token generated:', token);

      // Persist to backend — POST /api/volunteers/token
      this.http
        .post<void>(`${this.volunteersBase}/token`, { userId, token })
        .subscribe({
          next: () => console.log('FCM Token saved successfully'),
          error: (err) => console.error('[FCM] Failed to save token:', err),
        });

      // Also save via the /fcm-token path as fallback
      this.http
        .post<void>(`${this.volunteersBase}/${userId}/fcm-token`, { token })
        .subscribe({ error: () => {} }); // silent fallback

    } catch (err) {
      console.error('[FCM] Error initializing notifications:', err);
    }
  }

  // ─── Foreground Listener ───────────────────────────────────────────────────

  /**
   * Listen for push notifications while the app tab is in the foreground.
   * Adds them to the in-app notification list and increments the badge.
   */
  startListening(): void {
    onMessage(messaging, (payload: MessagePayload) => {
      console.log('Notification received:', payload);

      const title = payload.notification?.title ?? 'Notification';
      const body  = payload.notification?.body  ?? '';

      // UI Message: Alert
      alert("New Mission Assigned: " + title);

      // Add to in-app list
      const newNotification: AppNotification = {
        id: crypto.randomUUID(),
        title,
        body,
        timestamp: new Date(),
        read: false,
      };

      this.notifications.update((existing) => [newNotification, ...existing]);
      this.unreadCount.update((n) => n + 1);

      // Also show browser Notification API toast if visible
      if (document.visibilityState === 'visible' && Notification.permission === 'granted') {
        new Notification(title, { body });
      }
    });
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
