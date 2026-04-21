import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import {
    ForumService,
    Comment,
    isModeratedForumCommentContent,
} from '../../../core/services/forum.service';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { Subscription, of } from 'rxjs';
import { catchError, filter, finalize, timeout } from 'rxjs/operators';
import { ConfirmDialog, ConfirmDialogData } from '../confirm-dialog/confirm-dialog';
import { NgZoneUiSync } from '../../../core/services/ng-zone-ui-sync.service';

@Component({
    selector: 'app-comment-list',
    templateUrl: './comment-list.html',
    styleUrls: ['./comment-list.css'],
    standalone: false
})
export class CommentList implements OnInit, OnDestroy {
    readonly isModeratedCommentContent = isModeratedForumCommentContent;

    comments: Comment[] = [];
    loading: boolean = false;
    error: string | null = null;
    private refreshSub?: Subscription;

    private static readonly REQUEST_MS = 15000;

    constructor(
        private forumService: ForumService,
        public authService: AuthService,
        private dialog: MatDialog,
        private toast: ToastService,
        private readonly cdr: ChangeDetectorRef,
        private readonly zoneUi: NgZoneUiSync
    ) { }

    ngOnInit(): void {
        this.loadComments();
        this.refreshSub = this.forumService.refresh$.subscribe(() => this.loadComments());
    }

    ngOnDestroy(): void {
        this.refreshSub?.unsubscribe();
    }

    loadComments(): void {
        this.loading = true;
        this.error = null;

        const uid = this.authService.getUserId();
        const doctorScope = this.authService.getRole() === 'DOCTOR' && uid != null;
        const req$ = doctorScope
            ? this.forumService.getCommentsByPostAuthor(uid!)
            : this.forumService.getAllComments();
        req$
            .pipe(
                timeout(CommentList.REQUEST_MS),
                catchError((err: unknown) => {
                    const msg =
                        (err as { name?: string })?.name === 'TimeoutError'
                            ? 'Request timed out. Check forums-service (8082) or the API gateway.'
                            : `Connection failed. Check API Gateway (8080) and forums-service (8082). (${(err as Error)?.message ?? ''})`;
                    this.zoneUi.apply(this.cdr, () => {
                        this.error = msg;
                    });
                    return of<Comment[]>([]);
                }),
                finalize(() => {
                    this.zoneUi.apply(this.cdr, () => {
                        this.loading = false;
                    });
                })
            )
            .subscribe((comments: Comment[]) => {
                this.zoneUi.apply(this.cdr, () => {
                    this.comments = comments;
                });
            });
    }

    deleteComment(id: number): void {
        const data: ConfirmDialogData = {
            title: 'Delete comment?',
            message: 'This action cannot be undone.',
            confirmLabel: 'Delete',
            cancelLabel: 'Cancel',
            variant: 'danger',
        };
        this.dialog
            .open(ConfirmDialog, { width: 'min(440px, 94vw)', maxWidth: '96vw', data })
            .afterClosed()
            .pipe(filter((v): v is true => v === true))
            .subscribe(() => {
                this.forumService.deleteComment(id).subscribe({
                    next: () => {
                        this.comments = this.comments.filter((c) => c.id !== id);
                    },
                    error: (err: unknown) => this.toast.showHttpError(err, 'Could not delete comment.'),
                });
            });
    }

    formatDate(date: string): string {
        if (!date) return '';
        const d = new Date(date);
        return d.toLocaleDateString('en-US') + ' ' + d.toLocaleTimeString('en-US');
    }
}
