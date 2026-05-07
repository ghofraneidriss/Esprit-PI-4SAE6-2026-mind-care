import { Component, OnDestroy, OnInit, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import {
  ForumService,
  Post,
  PostMediaInfo,
  Comment,
  ReactionType,
  ForumBanStatus,
  isModeratedForumCommentContent,
} from '../../../core/services/forum.service';
import { ConfirmDialogService } from '../../../core/services/confirm-dialog.service';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { Subscription, switchMap, of, catchError } from 'rxjs';
import { finalize } from 'rxjs/operators';

@Component({
  selector: 'app-post-details',
  templateUrl: './post-details.html',
  styleUrls: ['./post-details.component.css'],
  standalone: false
})
export class PostDetailsComponent implements OnInit, OnDestroy {
  /** Re-export for template: moderated comments styled in red. */
  readonly isModeratedCommentContent = isModeratedForumCommentContent;

  post: Post | null = null;
  comments: Comment[] = [];
  newCommentContent: string = '';
  editingCommentId: number | null = null;
  editCommentContent: string = '';
  get currentUserId(): number { return this.authService.getUserId() ?? 0; }
  loading: boolean = true;
  error: string | null = null;
  private refreshSub?: Subscription;
  forumBan: ForumBanStatus | null = null;
  private banCountdownTimer?: ReturnType<typeof setInterval>;
  commentBusyId: number | null = null;
  /** Blocks double Post clicks and duplicate submissions while the request is in flight. */
  commentSubmitting = false;

  /** Gallery: from {@link ForumService.getPostMediaMeta}; fallback to cover when {@link Post.hasImages}. */
  postMedia: PostMediaInfo[] = [];
  postImageUrls: string[] = [];
  currentImageIndex = 0;

  /** In-app report modal (replaces window.prompt). */
  reportModalOpen = false;
  reportTargetComment: Comment | null = null;
  reportReasonKey = 'spam';
  reportOtherDetail = '';
  reportInlineError: string | null = null;
  reportSubmitting = false;
  reportToast: { kind: 'success' | 'error'; text: string } | null = null;
  private reportToastTimer?: ReturnType<typeof setTimeout>;

  /** Centered notice when a banned user tries to comment, react, or rate (3s). */
  forumBanNotice: string | null = null;
  private forumBanNoticeTimer?: ReturnType<typeof setTimeout>;

  readonly reportReasons: { id: string; label: string }[] = [
    { id: 'spam', label: 'Spam or misleading content' },
    { id: 'harassment', label: 'Harassment or hate speech' },
    { id: 'misinformation', label: 'Misinformation or harmful advice' },
    { id: 'off_topic', label: 'Off-topic or irrelevant' },
    { id: 'inappropriate', label: 'Inappropriate or explicit content' },
    { id: 'other', label: 'Other (describe below)' },
  ];

  ratingBusy = false;
  reactionBusy = false;
  followBusy = false;

  readonly starOptions = [1, 2, 3, 4, 5] as const;

  /** Facebook-style picker (no dislike in the hover row; counts still include all types from API). */
  readonly facebookReactions: { type: ReactionType; icon: string; label: string }[] = [
    { type: 'LIKE', icon: 'ri-thumb-up-fill', label: 'Like' },
    { type: 'LOVE', icon: 'ri-heart-fill', label: 'Love' },
    { type: 'CARE', icon: 'ri-hand-heart-fill', label: 'Care' },
    { type: 'HAHA', icon: 'ri-emotion-laugh-line', label: 'Haha' },
    { type: 'WOW', icon: 'ri-sparkling-line', label: 'Wow' },
    { type: 'SAD', icon: 'ri-emotion-sad-line', label: 'Sad' },
    { type: 'ANGRY', icon: 'ri-angry-fill', label: 'Angry' },
  ];

  readonly reactions: { type: ReactionType; icon: string; label: string }[] = [
    ...this.facebookReactions,
    { type: 'DISLIKE', icon: 'ri-thumb-down-fill', label: 'Dislike' },
  ];

  constructor(
    private forumService: ForumService,
    private route: ActivatedRoute,
    public router: Router,
    private cdr: ChangeDetectorRef,
    private authService: AuthService,
    private confirm: ConfirmDialogService,
    private toast: ToastService
  ) { }

  ngOnInit(): void {
    this.loading = true;
    this.error = null;
    this.post = null;

    this.route.paramMap.subscribe(params => {
      const idStr = params.get('id');
      if (idStr) {
        const postId = parseInt(idStr, 10);
        if (isNaN(postId)) {
          this.error = 'Invalid post id.';
          this.loading = false;
        } else {
          this.loadPostData(postId);
        }
      } else {
        this.error = 'No post specified.';
        this.loading = false;
      }
    });

    this.refreshSub = this.forumService.refresh$.subscribe(() => {
      if (this.post?.id) {
        const uid = this.currentUserId > 0 ? this.currentUserId : null;
        /* Short delay so POST finishes before GET (avoids stale list). */
        setTimeout(() => {
          if (!this.post?.id) return;
          this.loadComments(this.post.id);
          this.forumService.getPostById(this.post.id, uid).subscribe({
            next: (postData) => (this.post = postData),
          });
        }, 80);
      }
    });
  }

  ngOnDestroy(): void {
    this.refreshSub?.unsubscribe();
    if (this.banCountdownTimer) {
      clearInterval(this.banCountdownTimer);
    }
    if (this.reportToastTimer) {
      clearTimeout(this.reportToastTimer);
    }
    if (this.forumBanNoticeTimer) {
      clearTimeout(this.forumBanNoticeTimer);
    }
  }

  private reloadPost(postId: number): void {
    const uid = this.currentUserId > 0 ? this.currentUserId : null;
    this.forumService.getPostById(postId, uid).subscribe({
      next: (p) => {
        this.post = p;
        this.cdr.markForCheck();
      },
    });
  }

  /** Follow thread → users-service notifications on each new comment (backend). */
  toggleFollow(): void {
    if (!this.post || this.currentUserId <= 0 || this.followBusy) return;
    this.followBusy = true;
    const pid = this.post.id;
    const uid = this.currentUserId;
    const isFollowing = !!this.post.following;
    const obs = isFollowing
      ? this.forumService.unfollowPost(pid, uid)
      : this.forumService.followPost(pid, uid);
    obs
      .pipe(
        finalize(() => {
          this.followBusy = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: () => {
          if (this.post && this.post.id === pid) {
            this.post.following = !isFollowing;
          }
          this.reloadPost(pid);
        },
      });
  }

  loadPostData(postId: number): void {
    this.loading = true;
    this.error = null;
    const uid = this.currentUserId > 0 ? this.currentUserId : null;

    const loadingTimeout = setTimeout(() => {
      if (this.loading && !this.post) {
        this.error = 'Loading is taking too long. The server is not responding.';
        this.loading = false;
      }
    }, 10000);

    const load$ =
      uid != null
        ? this.forumService.recordPostView(postId, uid).pipe(
            catchError(() => of(undefined)),
            switchMap(() => this.forumService.getPostById(postId, uid))
          )
        : this.forumService.getPostById(postId);

    load$.subscribe({
      next: (postData) => {
        clearTimeout(loadingTimeout);
        this.post = postData;
        this.postMedia = [];
        this.currentImageIndex = 0;
        this.rebuildPostImageUrls();
        this.loading = false;
        this.cdr.detectChanges();
        this.loadPostMedia(postId);
        this.loadComments(postId);
        this.loadForumBan();
      },
      error: () => {
        clearTimeout(loadingTimeout);
        this.error = 'Discussion not found or server error.';
        this.loading = false;
        this.cdr.detectChanges();
      },
    });
  }

  private rebuildPostImageUrls(): void {
    this.postImageUrls = [];
    if (!this.post) {
      return;
    }
    const pid = this.post.id;
    if (this.postMedia.length > 0) {
      this.postImageUrls = this.postMedia.map((m) => this.forumService.postMediaUrl(pid, m.id));
    } else if (this.post.hasImages) {
      this.postImageUrls = [this.forumService.postCoverImageUrl(pid)];
    }
    if (this.currentImageIndex >= this.postImageUrls.length) {
      this.currentImageIndex = 0;
    }
  }

  private loadPostMedia(postId: number): void {
    this.forumService.getPostMediaMeta(postId).subscribe({
      next: (rows) => {
        this.postMedia = Array.isArray(rows)
          ? [...rows].sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
          : [];
        this.rebuildPostImageUrls();
        this.cdr.markForCheck();
      },
      error: () => {
        this.postMedia = [];
        this.rebuildPostImageUrls();
        this.cdr.markForCheck();
      },
    });
  }

  prevPostImage(): void {
    const n = this.postImageUrls.length;
    if (n <= 1) {
      return;
    }
    this.currentImageIndex = (this.currentImageIndex - 1 + n) % n;
  }

  nextPostImage(): void {
    const n = this.postImageUrls.length;
    if (n <= 1) {
      return;
    }
    this.currentImageIndex = (this.currentImageIndex + 1) % n;
  }

  loadComments(postId: number): void {
    const viewer = this.currentUserId > 0 ? this.currentUserId : undefined;
    this.forumService.getCommentsByPostId(postId, viewer).subscribe({
      next: (commentsData) => {
        this.comments = Array.isArray(commentsData) ? [...commentsData] : [];
        this.cdr.markForCheck();
        this.cdr.detectChanges();
      },
      error: () => {
        /* Keep existing list on transient errors. */
        this.cdr.markForCheck();
      },
    });
  }

  private loadForumBan(): void {
    if (this.currentUserId <= 0) {
      this.forumBan = null;
      return;
    }
    this.forumService.getForumBanStatus(this.currentUserId).subscribe({
      next: (b) => {
        this.forumBan = b;
        this.startBanCountdownTicker();
        this.cdr.markForCheck();
      },
      error: () => {
        this.forumBan = null;
      },
    });
  }

  private startBanCountdownTicker(): void {
    if (this.banCountdownTimer) {
      clearInterval(this.banCountdownTimer);
    }
    if (!this.forumBan?.banned) return;
    this.banCountdownTimer = setInterval(() => {
      if (this.remainingBanSeconds() <= 0) {
        this.loadForumBan();
        if (this.banCountdownTimer) clearInterval(this.banCountdownTimer);
      }
      this.cdr.markForCheck();
    }, 1000);
  }

  remainingBanSeconds(): number {
    if (!this.forumBan?.banned || !this.forumBan.bannedUntil) return 0;
    const end = new Date(this.forumBan.bannedUntil).getTime();
    return Math.max(0, Math.floor((end - Date.now()) / 1000));
  }

  banCountdownLabel(): string {
    const s = this.remainingBanSeconds();
    if (s <= 0) return '';
    const d = Math.floor(s / 86400);
    const h = Math.floor((s % 86400) / 3600);
    const m = Math.floor((s % 3600) / 60);
    const sec = s % 60;
    if (d > 0) return `${d}d ${h}h ${m}m ${sec}s left`;
    if (h > 0) return `${h}h ${m}m ${sec}s left`;
    if (m > 0) return `${m}m ${sec}s left`;
    return `${sec}s left`;
  }

  isForumBannedActive(): boolean {
    return !!this.forumBan?.banned && this.remainingBanSeconds() > 0;
  }

  private showForumBanNotice(): void {
    if (this.forumBanNoticeTimer) {
      clearTimeout(this.forumBanNoticeTimer);
    }
    this.forumBanNotice =
      "You're banned from the forum for now — you can't comment or react until the ban ends.";
    this.cdr.markForCheck();
    this.forumBanNoticeTimer = setTimeout(() => {
      this.forumBanNotice = null;
      this.forumBanNoticeTimer = undefined;
      this.cdr.markForCheck();
    }, 3000);
  }

  navigateBack(): void {
    if (this.post && this.post.categoryId) {
      this.router.navigate(['/forum/category', this.post.categoryId]);
    } else {
      this.router.navigate(['/forum']);
    }
  }

  reactionCount(type: ReactionType): number {
    const v = this.post?.reactionCounts?.[type];
    return typeof v === 'number' ? v : 0;
  }

  totalReactionCount(): number {
    if (!this.post?.reactionCounts) return 0;
    return Object.values(this.post.reactionCounts).reduce(
      (sum, v) => sum + (typeof v === 'number' ? v : 0),
      0
    );
  }

  /** Top reaction types for the stacked summary (Facebook-style). */
  topReactionTypesForPreview(): ReactionType[] {
    if (!this.post?.reactionCounts) return [];
    return this.reactions
      .map((r) => ({ type: r.type, n: this.reactionCount(r.type) }))
      .filter((e) => e.n > 0)
      .sort((a, b) => b.n - a.n)
      .slice(0, 3)
      .map((e) => e.type);
  }

  reactionIconFor(type: ReactionType | string | null | undefined): string {
    if (!type) return 'ri-thumb-up-line';
    const key = String(type).toUpperCase() as ReactionType;
    const r = this.reactions.find((x) => x.type === key);
    return r?.icon ?? 'ri-thumb-up-line';
  }

  currentReactionLabel(): string {
    const raw = this.post?.myReaction;
    if (raw == null || raw === '') return 'Like';
    const key = String(raw).toUpperCase() as ReactionType;
    const r = this.reactions.find((x) => x.type === key);
    return r?.label ?? 'Like';
  }

  /** Main bar: tap again on the same reaction to remove it; use emoji row to switch anytime. */
  primaryReactionClick(): void {
    if (!this.post || this.currentUserId <= 0 || this.reactionBusy) return;
    if (this.isForumBannedActive()) {
      this.showForumBanNotice();
      return;
    }
    const raw = this.post.myReaction;
    const t =
      raw != null && raw !== ''
        ? (String(raw).toUpperCase() as ReactionType)
        : 'LIKE';
    this.pickReaction(t);
  }

  scrollToComments(): void {
    if (this.currentUserId > 0 && this.isForumBannedActive()) {
      this.showForumBanNotice();
      return;
    }
    document.getElementById('forum-comments-thread')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    setTimeout(() => document.getElementById('forum-comment-input')?.focus(), 450);
  }

  sharePost(): void {
    if (!this.post) return;
    const url = window.location.href;
    if (typeof navigator !== 'undefined' && navigator.share) {
      navigator.share({ title: this.post.title, url }).catch(() => this.copyUrlToClipboard(url));
    } else {
      this.copyUrlToClipboard(url);
    }
  }

  private copyUrlToClipboard(url: string): void {
    void navigator.clipboard.writeText(url).then(
      () => {
        this.toast.show('Link copied to clipboard.', 'success');
        this.cdr.markForCheck();
      },
      () => {
        this.toast.show('Could not copy the link.', 'error');
        this.cdr.markForCheck();
      }
    );
  }

  pickReaction(type: ReactionType): void {
    if (!this.post || this.currentUserId <= 0 || this.reactionBusy) return;
    if (this.isForumBannedActive()) {
      this.showForumBanNotice();
      return;
    }
    this.reactionBusy = true;
    const current = this.post.myReaction != null ? String(this.post.myReaction).toUpperCase() : null;
    const same = current === type;
    const req = same
      ? this.forumService.clearPostReaction(this.post.id, this.currentUserId)
      : this.forumService.setPostReaction(this.post.id, this.currentUserId, type);
    req.subscribe({
      next: () => {
        this.reactionBusy = false;
        this.reloadPost(this.post!.id);
      },
      error: (err) => {
        this.reactionBusy = false;
        if (err?.status === 403) {
          this.loadForumBan();
          this.showForumBanNotice();
        }
      },
    });
  }

  setStarRating(stars: number): void {
    if (!this.post || this.currentUserId <= 0 || this.ratingBusy) return;
    if (this.isForumBannedActive()) {
      this.showForumBanNotice();
      return;
    }
    if (stars < 1 || stars > 5) return;
    this.ratingBusy = true;
    this.forumService.setPostRating(this.post.id, this.currentUserId, stars).subscribe({
      next: () => {
        this.ratingBusy = false;
        this.reloadPost(this.post!.id);
      },
      error: (err) => {
        this.ratingBusy = false;
        if (err?.status === 403) {
          this.loadForumBan();
          this.showForumBanNotice();
        }
      },
    });
  }

  addComment(): void {
    if (!this.newCommentContent.trim() || !this.post) return;
    if (this.currentUserId <= 0) {
      this.toast.show('Sign in to comment.', 'info');
      this.cdr.markForCheck();
      return;
    }
    if (this.isForumBannedActive()) {
      this.showForumBanNotice();
      return;
    }
    if (this.commentSubmitting) {
      return;
    }

    this.commentSubmitting = true;
    this.cdr.markForCheck();

    const commentBody = {
      content: this.newCommentContent.trim(),
      userId: this.currentUserId
    };

    this.forumService
      .addComment(commentBody, this.post.id)
      .pipe(
        finalize(() => {
          this.commentSubmitting = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (newComment) => {
          this.newCommentContent = '';
          const pid = this.post?.id;
          if (this.post) {
            this.post.commentCount++;
          }
          /* Show new comment immediately from POST (GET may lag or be cached). */
          if (newComment?.id != null) {
            const withoutDup = this.comments.filter((c) => c.id !== newComment.id);
            this.comments = [newComment, ...withoutDup];
            this.cdr.markForCheck();
            this.cdr.detectChanges();
          }
          if (pid != null) {
            /* Re-sync from server shortly after (avoids racing refresh$). */
            setTimeout(() => this.loadComments(pid), 120);
          }
          this.loadForumBan();
        },
        error: (err) => {
          if (err?.status === 403) {
            this.loadForumBan();
            this.showForumBanNotice();
          } else {
            this.toast.showHttpError(err, 'Could not post comment.');
          }
        },
      });
  }

  toggleCommentLike(comment: Comment): void {
    if (!this.post || this.currentUserId <= 0) return;
    if (this.isForumBannedActive()) {
      this.showForumBanNotice();
      return;
    }
    this.commentBusyId = comment.id;
    this.forumService.toggleCommentLike(comment.id, this.currentUserId).subscribe({
      next: () => {
        this.commentBusyId = null;
        this.loadComments(this.post!.id);
      },
      error: (err) => {
        this.commentBusyId = null;
        if (err?.status === 403) {
          this.loadForumBan();
          this.showForumBanNotice();
        }
      },
    });
  }

  toggleCommentDislike(comment: Comment): void {
    if (!this.post || this.currentUserId <= 0) return;
    if (this.isForumBannedActive()) {
      this.showForumBanNotice();
      return;
    }
    this.commentBusyId = comment.id;
    this.forumService.toggleCommentDislike(comment.id, this.currentUserId).subscribe({
      next: () => {
        this.commentBusyId = null;
        this.loadComments(this.post!.id);
      },
      error: (err) => {
        this.commentBusyId = null;
        if (err?.status === 403) {
          this.loadForumBan();
          this.showForumBanNotice();
        }
      },
    });
  }

  openReportModal(comment: Comment): void {
    if (this.currentUserId <= 0 || comment.userId === this.currentUserId) return;
    this.reportTargetComment = comment;
    this.reportReasonKey = 'spam';
    this.reportOtherDetail = '';
    this.reportInlineError = null;
    this.reportModalOpen = true;
    this.cdr.markForCheck();
  }

  closeReportModal(): void {
    this.reportModalOpen = false;
    this.reportTargetComment = null;
    this.reportInlineError = null;
    this.reportSubmitting = false;
    this.cdr.markForCheck();
  }

  onReportBackdropClick(ev: MouseEvent): void {
    if (ev.target === ev.currentTarget) {
      this.closeReportModal();
    }
  }

  private showReportToast(kind: 'success' | 'error', text: string): void {
    if (this.reportToastTimer) {
      clearTimeout(this.reportToastTimer);
    }
    this.reportToast = { kind, text };
    this.cdr.markForCheck();
    this.reportToastTimer = setTimeout(() => {
      this.reportToast = null;
      this.cdr.markForCheck();
    }, 4200);
  }

  submitReport(): void {
    const comment = this.reportTargetComment;
    if (!comment || this.currentUserId <= 0) return;

    let reason = '';
    if (this.reportReasonKey === 'other') {
      const detail = this.reportOtherDetail.trim();
      if (!detail) {
        this.reportInlineError = 'Please describe the issue when you choose Other.';
        this.cdr.markForCheck();
        return;
      }
      this.reportInlineError = null;
      reason = `Other: ${detail}`;
    } else {
      this.reportInlineError = null;
      const opt = this.reportReasons.find((r) => r.id === this.reportReasonKey);
      reason = opt?.label ?? '';
    }

    this.reportSubmitting = true;
    this.cdr.markForCheck();
    this.forumService.reportComment(comment.id, this.currentUserId, reason).subscribe({
      next: () => {
        this.reportSubmitting = false;
        this.closeReportModal();
        this.showReportToast('success', 'Report sent to the thread moderator.');
      },
      error: () => {
        this.reportSubmitting = false;
        this.showReportToast('error', 'Could not send the report.');
        this.cdr.markForCheck();
      },
    });
  }

  startEditComment(comment: Comment): void {
    if (this.isModeratedCommentContent(comment.content)) {
      return;
    }
    this.editingCommentId = comment.id;
    this.editCommentContent = comment.content;
  }

  saveEditComment(comment: Comment): void {
    if (!this.editCommentContent.trim()) return;

    this.forumService.updateComment(comment.id, { content: this.editCommentContent }).subscribe({
      next: (updated) => {
        const idx = this.comments.findIndex(c => c.id === comment.id);
        if (idx !== -1) {
          this.comments[idx] = updated;
        }
        this.cancelEditComment();
        this.toast.show('Comment updated.', 'success');
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.toast.showHttpError(err, 'Could not update comment.');
        this.cdr.markForCheck();
      }
    });
  }

  cancelEditComment(): void {
    this.editingCommentId = null;
    this.editCommentContent = '';
  }

  async deleteComment(id: number): Promise<void> {
    const ok = await this.confirm.confirm({
      title: 'Delete comment',
      message: 'Are you sure you want to delete this comment? This cannot be undone.',
      confirmText: 'Delete',
      cancelText: 'Cancel',
      danger: true,
    });
    if (!ok) {
      return;
    }

    this.forumService.deleteComment(id).subscribe({
      next: () => {
        this.comments = this.comments.filter(c => c.id !== id);
        if (this.post) this.post.commentCount--;
        this.toast.show('Comment deleted.', 'success');
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.toast.showHttpError(err, 'Could not delete comment.');
        this.cdr.markForCheck();
      }
    });
  }

  isCommentOwner(comment: Comment): boolean {
    return comment.userId === this.currentUserId;
  }

  /** First letter for avatar chips — uses real name when available. */
  displayNameInitial(displayName: string | undefined | null): string {
    const s = (displayName || 'U').trim();
    if (!s) return 'U';
    return s.charAt(0).toUpperCase();
  }

  commentAuthorLabel(comment: Comment): string {
    const n = comment.authorName?.trim();
    if (n) return n;
    return `User ${comment.userId}`;
  }

  /** API may return reaction string with different casing — normalize for UI state. */
  myReactionIs(type: ReactionType): boolean {
    const m = this.post?.myReaction;
    if (m == null || m === '') return false;
    return String(m).toUpperCase() === type;
  }
}
