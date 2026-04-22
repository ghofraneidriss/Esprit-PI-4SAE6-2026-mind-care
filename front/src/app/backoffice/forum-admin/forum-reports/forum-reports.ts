import { ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import {
  ForumService,
  CommentReportItem,
  ModerationResolveAction,
} from '../../../core/services/forum.service';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { NgZoneUiSync } from '../../../core/services/ng-zone-ui-sync.service';
import { Subscription } from 'rxjs';
import { filter, take } from 'rxjs/operators';
import { ConfirmDialog, ConfirmDialogData } from '../confirm-dialog/confirm-dialog';

@Component({
  selector: 'app-forum-reports',
  templateUrl: './forum-reports.html',
  styleUrls: ['./forum-reports.css'],
  standalone: false,
})
export class ForumReports implements OnInit, OnDestroy {
  reports: CommentReportItem[] = [];
  loading = false;
  error: string | null = null;

  private refreshSub?: Subscription;
  private loadSub?: Subscription;
  private loadTimer?: ReturnType<typeof setTimeout>;

  private static readonly LOAD_MS = 12000;

  constructor(
    private forumService: ForumService,
    public authService: AuthService,
    private cdr: ChangeDetectorRef,
    private dialog: MatDialog,
    private toast: ToastService,
    private readonly zoneUi: NgZoneUiSync
  ) {}

  ngOnInit(): void {
    this.loadReports();
    this.refreshSub = this.forumService.refresh$.subscribe(() => {
      if (this.isDoctor) {
        this.loadReports();
      }
    });
  }

  ngOnDestroy(): void {
    this.cancelLoad();
    this.refreshSub?.unsubscribe();
  }

  get isDoctor(): boolean {
    return this.authService.getRole() === 'DOCTOR' && this.authService.getUserId() != null;
  }

  get pendingCount(): number {
    return this.reports.filter((r) => r.status === 'PENDING').length;
  }

  isPending(r: CommentReportItem): boolean {
    return r.status === 'PENDING';
  }

  canAct(r: CommentReportItem): boolean {
    if (r.canChangeDecision === false) {
      return false;
    }
    if (r.canChangeDecision === true) {
      return true;
    }
    return r.status === 'PENDING' || r.resolutionAction !== 'DELETE_COMMENT';
  }

  /** Highlights the action button that matches the saved server decision (not always “7d”). */
  isActionSelected(r: CommentReportItem, action: ModerationResolveAction): boolean {
    if (r.status === 'PENDING') {
      return false;
    }
    return r.resolutionAction === action;
  }

  actionLabel(action: ModerationResolveAction): string {
    const map: Record<ModerationResolveAction, string> = {
      DISMISS: 'Remove ban',
      LIFT_BAN: 'Remove ban (early)',
      DELETE_COMMENT: 'Delete comment',
      BAN_1_DAY: 'Ban 1 minute',
      BAN_3_DAYS: 'Ban 3 minutes',
      BAN_7_DAYS: 'Ban 5 minutes',
    };
    return map[action];
  }

  resolutionBadge(r: CommentReportItem): string {
    if (r.status === 'PENDING') {
      return 'Pending';
    }
    const a = r.resolutionAction;
    if (!a) {
      return 'Resolved';
    }
    if (a === 'DISMISS') {
      return 'Cleared';
    }
    if (a === 'DELETE_COMMENT') {
      return 'Comment removed';
    }
    if (a === 'BAN_1_DAY') {
      return 'Ban 1m';
    }
    if (a === 'BAN_3_DAYS') {
      return 'Ban 3m';
    }
    if (a === 'BAN_7_DAYS') {
      return 'Ban 5m';
    }
    if (a === 'LIFT_BAN') {
      return 'Ban lifted';
    }
    return a;
  }

  private cancelLoad(): void {
    if (this.loadTimer != null) {
      clearTimeout(this.loadTimer);
      this.loadTimer = undefined;
    }
    this.loadSub?.unsubscribe();
    this.loadSub = undefined;
  }

  loadReports(): void {
    const uid = this.authService.getUserId();
    if (!this.isDoctor || uid == null) {
      this.cancelLoad();
      this.zoneUi.apply(this.cdr, () => {
        this.reports = [];
        this.error = null;
        this.loading = false;
      });
      return;
    }

    this.cancelLoad();
    this.zoneUi.apply(this.cdr, () => {
      this.loading = true;
      this.error = null;
    });

    this.loadTimer = setTimeout(() => {
      this.loadTimer = undefined;
      if (!this.loading) {
        return;
      }
      this.loadSub?.unsubscribe();
      this.loadSub = undefined;
      this.zoneUi.apply(this.cdr, () => {
        this.error =
          'Request timed out. Ensure forums-service is running (port 8082) and check the Network tab for /moderation/comment-reports.';
        this.reports = [];
        this.loading = false;
      });
    }, ForumReports.LOAD_MS);

    this.loadSub = this.forumService
      .getPendingCommentReports(uid)
      .pipe(take(1))
      .subscribe({
        next: (rows) => {
          if (this.loadTimer != null) {
            clearTimeout(this.loadTimer);
            this.loadTimer = undefined;
          }
          this.zoneUi.apply(this.cdr, () => {
            this.reports = rows ?? [];
            this.loading = false;
          });
        },
        error: (err: unknown) => {
          if (this.loadTimer != null) {
            clearTimeout(this.loadTimer);
            this.loadTimer = undefined;
          }
          const e = err as { message?: string };
          this.zoneUi.apply(this.cdr, () => {
            this.error = `Could not load reports. ${e?.message ?? ''}`.trim();
            this.reports = [];
            this.loading = false;
          });
        },
      });
  }

  resolveReport(r: CommentReportItem, action: ModerationResolveAction): void {
    const uid = this.authService.getUserId();
    if (uid == null) {
      return;
    }
    const changing = r.status === 'RESOLVED';
    let message: string;
    let title: string;
    let variant: 'default' | 'danger' | 'warning';
    let confirmLabel: string;

    if (action === 'LIFT_BAN') {
      title = 'Remove ban early';
      message =
        'End this user’s forum ban now (before the scheduled end). You can apply another duration later if needed.';
      variant = 'warning';
      confirmLabel = 'Remove ban';
    } else if (changing) {
      title = 'Update decision';
      message = `Apply “${this.actionLabel(action)}”? This replaces the previous moderation effect where relevant (e.g. adjusts or clears the ban).`;
      variant = action === 'DELETE_COMMENT' ? 'danger' : 'warning';
      confirmLabel = 'Update';
    } else if (action === 'DISMISS') {
      title = 'Remove ban / clear report';
      message =
        'Close this report without further sanction. If a temporary ban was applied from this report, it will be lifted.';
      variant = 'default';
      confirmLabel = 'Remove ban';
    } else if (action === 'DELETE_COMMENT') {
      title = 'Delete comment';
      message =
        'Delete this comment permanently? You will not be able to change this decision afterwards.';
      variant = 'danger';
      confirmLabel = 'Delete';
    } else {
      title = 'Apply ban';
      message = 'Apply this ban to the comment author’s account?';
      variant = 'warning';
      confirmLabel = 'Confirm';
    }

    const data: ConfirmDialogData = {
      title,
      message,
      confirmLabel,
      cancelLabel: 'Cancel',
      variant,
    };
    this.dialog
      .open(ConfirmDialog, {
        width: 'min(440px, 94vw)',
        maxWidth: '96vw',
        autoFocus: 'dialog',
        data,
      })
      .afterClosed()
      .pipe(filter((v): v is true => v === true))
      .subscribe(() => {
        this.forumService.resolveCommentReport(r.id, uid, action).subscribe({
          next: () => {
            this.toast.show('Decision applied.', 'success');
            this.loadReports();
          },
          error: (err) => this.toast.showHttpError(err, 'Action could not be completed.'),
        });
      });
  }
}
