import { Component, Inject, OnInit } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { catchError, finalize } from 'rxjs/operators';
import { of } from 'rxjs';
import { Comment, ForumService, Post } from '../../../core/services/forum.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-view-post-modal',
  templateUrl: './view-post-modal.html',
  styleUrls: ['./view-post-modal.css'],
  standalone: false
})
export class ViewPostModal implements OnInit {
  comments: Comment[] = [];
  commentsLoading = false;
  commentsError: string | null = null;

  constructor(
    public dialogRef: MatDialogRef<ViewPostModal>,
    private authService: AuthService,
    private forumService: ForumService,
    @Inject(MAT_DIALOG_DATA) public data: { post: Post }
  ) {}

  ngOnInit(): void {
    this.loadComments();
  }

  loadComments(): void {
    const postId = this.data.post?.id;
    if (postId == null) {
      return;
    }
    this.commentsLoading = true;
    this.commentsError = null;
    this.forumService
      .getCommentsByPostId(postId)
      .pipe(
        catchError(() => {
          this.commentsError = 'Could not load comments.';
          return of<Comment[]>([]);
        }),
        finalize(() => {
          this.commentsLoading = false;
        })
      )
      .subscribe((rows) => {
        this.comments = [...rows].sort(
          (a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
        );
      });
  }

  trackByCommentId(_i: number, c: Comment): number {
    return c.id;
  }

  get authorInitial(): string {
    const a = this.data.post?.author?.trim();
    if (!a) {
      return 'U';
    }
    return a.charAt(0).toUpperCase();
  }

  get canEditOrDelete(): boolean {
    return this.authService.canEditOrDeleteForumPost(this.data.post);
  }

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
