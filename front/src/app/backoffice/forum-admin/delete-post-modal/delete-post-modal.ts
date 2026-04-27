import { Component, Inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Post } from '../../../core/services/forum.service';

@Component({
  selector: 'app-delete-post-modal',
  templateUrl: './delete-post-modal.html',
  styleUrls: ['./delete-post-modal.css'],
  standalone: false
})
export class DeletePostModal {
  constructor(
    public dialogRef: MatDialogRef<DeletePostModal>,
    @Inject(MAT_DIALOG_DATA) public data: { post: Post }
  ) { }

  onConfirm(): void {
    this.dialogRef.close(true);
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}
