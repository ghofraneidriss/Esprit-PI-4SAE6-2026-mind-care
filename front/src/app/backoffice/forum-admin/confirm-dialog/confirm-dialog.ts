import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

export type ConfirmDialogVariant = 'default' | 'danger' | 'warning';

export interface ConfirmDialogData {
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  variant?: ConfirmDialogVariant;
}

@Component({
  selector: 'app-confirm-dialog',
  templateUrl: './confirm-dialog.html',
  styleUrls: ['./confirm-dialog.css'],
  standalone: false,
})
export class ConfirmDialog {
  constructor(
    public dialogRef: MatDialogRef<ConfirmDialog, boolean>,
    @Inject(MAT_DIALOG_DATA) public data: ConfirmDialogData
  ) {}

  get variant(): ConfirmDialogVariant {
    return this.data.variant ?? 'default';
  }

  get confirmLabel(): string {
    return this.data.confirmLabel ?? 'Confirm';
  }

  get cancelLabel(): string {
    return this.data.cancelLabel ?? 'Cancel';
  }

  confirm(): void {
    this.dialogRef.close(true);
  }

  cancel(): void {
    this.dialogRef.close(false);
  }
}
