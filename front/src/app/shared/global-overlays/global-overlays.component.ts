import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService } from '../../core/services/toast.service';
import { ConfirmDialogService } from '../../core/services/confirm-dialog.service';
import { MovementAlertAssistantComponent } from '../movement-alert-assistant/movement-alert-assistant.component';

/** Hosts app-wide toasts (3s dismiss) and confirm dialogs — add once per root layout. */
@Component({
  selector: 'app-global-overlays',
  standalone: true,
  imports: [CommonModule, MovementAlertAssistantComponent],
  templateUrl: './global-overlays.component.html',
  styleUrls: ['./global-overlays.component.css'],
})
export class GlobalOverlaysComponent {
  private readonly toastSvc = inject(ToastService);
  readonly confirm = inject(ConfirmDialogService);
  readonly toasts$ = this.toastSvc.toasts$;
  readonly confirm$ = this.confirm.dialog$;

  dismissToast(id: string) {
    this.toastSvc.dismiss(id);
  }
}
