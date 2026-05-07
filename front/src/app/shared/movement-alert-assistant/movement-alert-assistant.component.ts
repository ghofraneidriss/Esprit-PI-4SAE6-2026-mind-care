import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavigationEnd, Router, RouterLink } from '@angular/router';
import { filter } from 'rxjs/operators';
import { Subscription } from 'rxjs';
import { AuthService } from '../../core/services/auth.service';
import {
  MovementAlertAssistantService,
  MovementAssistantMessage,
} from './movement-alert-assistant.service';

@Component({
  selector: 'app-movement-alert-assistant',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './movement-alert-assistant.component.html',
  styleUrls: ['./movement-alert-assistant.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MovementAlertAssistantComponent implements OnInit, OnDestroy {
  private readonly assistant = inject(MovementAlertAssistantService);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly cdr = inject(ChangeDetectorRef);

  private sub = new Subscription();

  panelOpen = false;
  messages: MovementAssistantMessage[] = [];
  unread = 0;
  showWidget = false;

  ngOnInit(): void {
    this.sub.add(
      this.assistant.messages.subscribe((m) => {
        this.messages = m;
        this.cdr.markForCheck();
      })
    );
    this.sub.add(
      this.assistant.unreadCount$.subscribe((n) => {
        this.unread = n;
        this.cdr.markForCheck();
      })
    );
    this.sub.add(
      this.assistant.panelOpen.subscribe((o) => {
        this.panelOpen = o;
        this.cdr.markForCheck();
      })
    );
    this.sub.add(
      this.router.events.pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd)).subscribe(() => {
        this.refreshVisibility();
      })
    );
    this.refreshVisibility();
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }

  private refreshVisibility(): void {
    const u = this.auth.getCurrentUser();
    this.showWidget = !!u && this.assistant.isEligibleRole;
    if (this.showWidget) {
      this.assistant.requestNotificationPermission();
    }
    this.cdr.markForCheck();
  }

  get trackingRoute(): string {
    const r = this.auth.getRole()?.toUpperCase() ?? '';
    return r === 'ADMIN' || r === 'DOCTOR' ? '/admin/patient-movement' : '/patient-movement';
  }

  toggle(): void {
    this.assistant.togglePanel();
  }

  close(): void {
    this.assistant.setPanelOpen(false);
  }

  dismiss(ev: Event, id: string): void {
    ev.stopPropagation();
    this.assistant.dismissMessage(id);
  }

  acknowledge(m: MovementAssistantMessage): void {
    if (m.source !== 'api' || !m.alertId) {
      return;
    }
    this.assistant.acknowledgeApiMessage(m.id);
  }

  itinerary(m: MovementAssistantMessage): void {
    this.assistant.openItineraryToPatient(m);
  }

  trackById(_: number, m: MovementAssistantMessage): string {
    return m.id;
  }

  severityClass(m: MovementAssistantMessage): string {
    switch (m.severity) {
      case 'CRITICAL':
        return 'mov-msg--crit';
      case 'WARNING':
        return 'mov-msg--warn';
      default:
        return 'mov-msg--info';
    }
  }

  formatTime(iso: string): string {
    try {
      return new Date(iso).toLocaleString('en-US', { dateStyle: 'short', timeStyle: 'short' });
    } catch {
      return iso;
    }
  }
}
