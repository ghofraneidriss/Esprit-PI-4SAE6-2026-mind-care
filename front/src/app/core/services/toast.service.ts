import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { friendlyHttpError } from '../utils/friendly-http-error';

export type ToastKind = 'success' | 'error' | 'info';

export interface ToastItem {
  id: string;
  message: string;
  kind: ToastKind;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private readonly items = new BehaviorSubject<ToastItem[]>([]);
  readonly toasts$ = this.items.asObservable();
  private seq = 0;

  /** Shows a toast that auto-dismisses after `durationMs` (default 4000). */
  show(message: string, kind: ToastKind = 'info', durationMs = 4000) {
    const id = `toast-${++this.seq}`;
    this.items.next([...this.items.value, { id, message, kind }]);
    setTimeout(() => this.dismiss(id), durationMs);
  }

  /**
   * Maps HTTP/backend errors to a short English message (no SQL or stack traces).
   * Errors stay visible a bit longer so they are easier to read.
   */
  showHttpError(error: unknown, fallback = 'Something went wrong. Please try again.', durationMs = 6000) {
    this.show(friendlyHttpError(error, fallback), 'error', durationMs);
  }

  dismiss(id: string) {
    this.items.next(this.items.value.filter((t) => t.id !== id));
  }
}
