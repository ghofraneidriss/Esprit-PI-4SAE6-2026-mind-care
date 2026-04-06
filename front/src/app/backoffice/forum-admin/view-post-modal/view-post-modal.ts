import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatDialogModule } from '@angular/material/dialog';
import { Post } from '../../../core/services/forum.service';

@Component({
  selector: 'app-view-post-modal',
  templateUrl: './view-post-modal.html',
  styleUrls: ['./view-post-modal.css'],
  standalone: true,
  imports: [CommonModule, MatDialogModule]
})
export class ViewPostModal {
  constructor(
    public dialogRef: MatDialogRef<ViewPostModal>,
    @Inject(MAT_DIALOG_DATA) public data: { post: Post }
  ) { }

  onClose(): void {
    this.dialogRef.close();
  }

  onEdit(): void {
    this.dialogRef.close('edit');
  }

  onDelete(): void {
    this.dialogRef.close('delete');
  }
}
