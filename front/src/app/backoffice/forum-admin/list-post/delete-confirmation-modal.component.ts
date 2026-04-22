import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';

export interface DeleteConfirmationData {
  postTitle: string;
  postId: number;
}

@Component({
  selector: 'app-delete-confirmation-modal',
  templateUrl: './delete-confirmation-modal.component.html',
  styleUrls: ['./delete-confirmation-modal.component.css'],
  standalone: false
})
export class DeleteConfirmationModalComponent {
  constructor(
    public dialogRef: MatDialogRef<DeleteConfirmationModalComponent>,
    @Inject(MAT_DIALOG_DATA) public data: DeleteConfirmationData
  ) {}

  onConfirm(): void {
    this.dialogRef.close(true);
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}
