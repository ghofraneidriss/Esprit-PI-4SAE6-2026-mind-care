import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface ConfirmOptions {
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  danger?: boolean;
}

@Injectable({ providedIn: 'root' })
export class ConfirmDialogService {
  private readonly state = new BehaviorSubject<ConfirmOptions | null>(null);
  readonly dialog$ = this.state.asObservable();
  private resolver: ((value: boolean) => void) | null = null;

  confirm(options: ConfirmOptions): Promise<boolean> {
    return new Promise((resolve) => {
      this.resolver = resolve;
      this.state.next({
        title: options.title,
        message: options.message,
        confirmText: options.confirmText ?? 'OK',
        cancelText: options.cancelText ?? 'Cancel',
        danger: options.danger ?? false,
      });
    });
  }

  close(result: boolean) {
    this.resolver?.(result);
    this.resolver = null;
    this.state.next(null);
  }
}
