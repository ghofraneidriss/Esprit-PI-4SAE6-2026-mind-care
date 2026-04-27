import { ChangeDetectorRef, Component, NgZone, OnDestroy, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { NavigationEnd, Router } from '@angular/router';
import { catchError, of, Subscription, interval } from 'rxjs';
import { filter, timeout, map, distinctUntilChanged, finalize } from 'rxjs/operators';
import {
  ForumService,
  Post,
  Category,
  Comment,
  StaffDashboardPayload,
} from '../../../core/services/forum.service';
import { AuthService } from '../../../core/services/auth.service';
import { DeletePostModal } from '../delete-post-modal/delete-post-modal';
import { ViewPostModal } from '../view-post-modal/view-post-modal';
import { PostForm } from '../post-form/post-form';
import { forumPostFormDialogConfig } from '../forum-post-form-dialog.config';

interface ForumStatCard {
  label: string;
  value: string;
  detail: string;
  icon: string;
}

@Component({
  selector: 'app-admin-dashboard',
  templateUrl: './admin-dashboard.html',
  styleUrls: ['./admin-dashboard.css'],
  standalone: false,
})
export class AdminDashboard implements OnInit, OnDestroy {
  stats: ForumStatCard[] = [
    { label: 'Total posts', value: '—', detail: 'Loading…', icon: 'ri-file-text-line' },
    { label: 'Categories', value: '—', detail: 'Loading…', icon: 'ri-folder-line' },
    { label: 'Total comments', value: '—', detail: 'Loading…', icon: 'ri-chat-3-line' },
    { label: 'Total views', value: '—', detail: 'Loading…', icon: 'ri-eye-line' },
  ];
  statsLoading = true;

  recentPosts: Post[] = [];
  /** Passed to “New post” so the modal does not reload categories (same payload as dashboard). */
  private dashboardCategories: Category[] = [];
  /** Table rows only — loads after KPIs or in parallel (slower aggregation). */
  postsLoading = false;
  error: string | null = null;

  private pollSub?: Subscription;
  private routeSub?: Subscription;
  /** Évite deux appels simultanés (ex. ngOnInit + NavigationEnd sur la même page). */
  private dashboardInFlight = false;
  private pendingDashboardRefresh = false;

  constructor(
    private forumService: ForumService,
    private authService: AuthService,
    private dialog: MatDialog,
    private router: Router,
    private readonly cdr: ChangeDetectorRef,
    private readonly ngZone: NgZone
  ) {}

  get canManageForum(): boolean {
    return this.authService.canManageForumPosts();
  }

  canEditPost(post: Post): boolean {
    return this.authService.canEditOrDeleteForumPost(post);
  }

  ngOnInit(): void {
    /** Next macrotask: avoids stuck template when parent layout / router hasn’t finished a CD cycle yet. */
    setTimeout(() => this.loadDashboard(false), 0);
    this.pollSub = interval(45_000).subscribe(() => this.loadDashboard(true));
    this.routeSub = this.router.events
      .pipe(
        filter((e): e is NavigationEnd => e instanceof NavigationEnd),
        map((e) => e.urlAfterRedirects),
        distinctUntilChanged(),
        filter((url) => url === '/admin/forum' || url.endsWith('/admin/forum'))
      )
      .subscribe(() => this.loadDashboard(true));
  }

  ngOnDestroy(): void {
    this.pollSub?.unsubscribe();
    this.routeSub?.unsubscribe();
  }

  trackByPostId(_index: number, post: Post): number {
    return post.id;
  }

  /** KPIs respond first (counts + categories); recent posts can be slower. */
  private static readonly KPI_MS = 5000;
  private static readonly RECENT_MS = 15000;

  /** @param silent when true, skip full-page spinners (poll / return navigation). */
  loadDashboard(silent = false): void {
    if (this.dashboardInFlight) {
      if (!silent) {
        this.pendingDashboardRefresh = true;
      }
      return;
    }
    this.dashboardInFlight = true;
    if (!silent && this.recentPosts.length === 0) {
      this.postsLoading = true;
    }
    if (!silent) {
      this.statsLoading = true;
    }
    if (!silent) {
      this.error = null;
    }

    const uid = this.authService.getUserId();
    const doctorScope = this.authService.getRole() === 'DOCTOR' && uid != null;
    const authorId = doctorScope ? uid! : null;

    const kpisFallback: StaffDashboardPayload = {
      categories: [] as Category[],
      posts: [] as Post[],
      totalPostCount: 0,
      totalCommentCount: 0,
      totalViewCount: 0,
      publishedPostCount: 0,
      draftPostCount: 0,
    };

    const kpis$ = this.forumService.getStaffDashboardKpis(authorId).pipe(
      timeout(AdminDashboard.KPI_MS),
      catchError(() => of(kpisFallback))
    );

    const recent$ = this.forumService.getStaffRecentPosts(authorId, 10).pipe(
      timeout(AdminDashboard.RECENT_MS),
      catchError(() => of([] as Post[]))
    );

    let partsDone = 0;
    const onPartDone = () => {
      this.ngZone.run(() => {
        partsDone++;
        if (partsDone >= 2) {
          this.finishDashboardLoad();
        }
      });
    };

    kpis$.pipe(finalize(() => onPartDone())).subscribe({
      next: (payload) => {
        this.ngZone.run(() => {
          this.dashboardCategories = payload.categories;
          this.stats = this.buildStats(payload.categories, [], [], doctorScope, payload);
          if (!silent) {
            this.statsLoading = false;
          }
          this.error = null;
          this.cdr.detectChanges();
        });
      },
    });

    recent$.pipe(finalize(() => onPartDone())).subscribe({
      next: (posts) => {
        this.ngZone.run(() => {
          this.recentPosts = posts.slice(0, 10);
          if (!silent) {
            this.postsLoading = false;
          }
          this.cdr.detectChanges();
        });
      },
    });
  }

  private finishDashboardLoad(): void {
    this.dashboardInFlight = false;
    this.cdr.detectChanges();
    if (this.pendingDashboardRefresh) {
      this.pendingDashboardRefresh = false;
      this.loadDashboard(false);
    }
  }

  private buildStats(
    categories: Category[],
    posts: Post[],
    comments: Comment[],
    doctorScope: boolean,
    dash?: StaffDashboardPayload
  ): ForumStatCard[] {
    const fromApi = dash?.totalPostCount != null && dash.totalCommentCount != null && dash.totalViewCount != null;
    const totalPosts = fromApi ? dash!.totalPostCount! : posts.length;
    const categoryCount = categories.length;
    const totalViews = fromApi ? dash!.totalViewCount! : posts.reduce((sum, p) => sum + (p.viewCount ?? 0), 0);

    const sumCommentCounts = posts.reduce((sum, p) => sum + (p.commentCount ?? 0), 0);
    const totalComments = fromApi
      ? dash!.totalCommentCount!
      : comments.length > 0
        ? comments.length
        : sumCommentCounts;

    const startOfToday = new Date();
    startOfToday.setHours(0, 0, 0, 0);
    const commentsToday =
      comments.length > 0
        ? comments.filter((c) => new Date(c.createdAt).getTime() >= startOfToday.getTime()).length
        : null;

    const uniqueAuthors = new Set(posts.map((p) => p.userId).filter((id) => id != null && !Number.isNaN(Number(id))))
      .size;

    const fmt = (n: number) => n.toLocaleString();

    const scopeNote = doctorScope ? 'Your forum activity' : 'Whole forum';

    return [
      {
        label: doctorScope ? 'Your posts' : 'Total posts',
        value: fmt(totalPosts),
        detail: totalPosts === 0 ? 'No posts yet' : scopeNote,
        icon: 'ri-file-text-line',
      },
      {
        label: 'Categories',
        value: fmt(categoryCount),
        detail: categoryCount === 0 ? 'Create categories if empty' : 'Available categories',
        icon: 'ri-folder-line',
      },
      {
        label: doctorScope ? 'Comments on your posts' : 'Total comments',
        value: fmt(totalComments),
        detail: fromApi
          ? doctorScope
            ? 'On threads you authored'
            : 'Forum-wide total'
          : commentsToday != null
            ? commentsToday > 0
              ? `${fmt(commentsToday)} new today`
              : 'None posted today'
            : comments.length === 0 && sumCommentCounts > 0
              ? 'From post comment counts (list API unavailable)'
              : doctorScope
                ? 'On threads you authored'
                : 'All comments returned by the API',
        icon: 'ri-chat-3-line',
      },
      {
        label: 'Total views',
        value: fmt(totalViews),
        detail: fromApi
          ? doctorScope
            ? 'Recorded views on your posts'
            : 'Forum-wide total'
          : uniqueAuthors > 0
            ? doctorScope
              ? `${fmt(uniqueAuthors)} author (you)`
              : `${fmt(uniqueAuthors)} unique authors`
            : 'Sum of view counts on listed posts',
        icon: 'ri-eye-line',
      },
    ];
  }

  openPostForm(post?: Post): void {
    if (post && !this.authService.canEditOrDeleteForumPost(post)) {
      return;
    }
    const dialogRef = this.dialog.open(
      PostForm,
      forumPostFormDialogConfig({ post, categories: this.dashboardCategories })
    );

    dialogRef.afterClosed().subscribe((result: unknown) => {
      if (result) {
        this.loadDashboard(false);
      }
    });
  }

  viewPost(post: Post): void {
    const viewerId = this.authService.getUserId();
    const open = (p: Post) => {
      const dialogRef = this.dialog.open(ViewPostModal, {
        width: 'min(960px, 98vw)',
        maxWidth: '98vw',
        maxHeight: '92vh',
        panelClass: 'view-post-modal-panel',
        data: { post: p },
      });
      dialogRef.afterClosed().subscribe((result: unknown) => {
        if (result === 'edit') {
          this.openPostForm(p);
        } else if (result === 'delete') {
          this.deletePost(p);
        }
      });
    };

    if (!post.content?.trim()) {
      this.forumService.getPostById(post.id, viewerId).subscribe({
        next: (full) => open(full),
        error: () => open(post),
      });
    } else {
      open(post);
    }
  }

  deletePost(post: Post): void {
    if (!this.authService.canEditOrDeleteForumPost(post)) {
      return;
    }
    const dialogRef = this.dialog.open(DeletePostModal, {
      width: '450px',
      data: { post },
    });

    dialogRef.afterClosed().subscribe((result: unknown) => {
      if (result === true) {
        this.forumService.deletePost(post.id).subscribe({
          next: () => this.loadDashboard(false),
          error: (err) => console.error('[Admin Dashboard] Error deleting post:', err),
        });
      }
    });
  }

  reactivatePost(post: Post): void {
    if (!post.inactive || !this.authService.canEditOrDeleteForumPost(post)) {
      return;
    }
    this.forumService.reactivatePost(post.id).subscribe({
      next: () => this.loadDashboard(false),
      error: (err) => console.error('[Admin Dashboard] Error reactivating post:', err),
    });
  }
}
