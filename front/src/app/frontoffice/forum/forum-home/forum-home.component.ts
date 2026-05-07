import {
  Component,
  OnInit,
  OnDestroy,
  ChangeDetectorRef,
  ElementRef,
  ViewChild,
} from '@angular/core';
import { Router } from '@angular/router';
import { Subject, Subscription, catchError, of } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import {
  ForumService,
  Category,
  Post,
  ForumHomePayload,
  ForumTopComment,
  isModeratedForumCommentContent,
} from '../../../core/services/forum.service';
import { FORUM_FALLBACK_CATEGORIES } from '../forum-fallback-categories';
import { NgZoneUiSync } from '../../../core/services/ng-zone-ui-sync.service';

type PostSort = 'recent' | 'hot' | 'views';

@Component({
  selector: 'app-forum-home',
  templateUrl: './forum-home.html',
  styleUrls: ['../forum-shared.css', './forum-home.component.css'],
  standalone: false,
})
export class ForumHomeComponent implements OnInit, OnDestroy {
  readonly isModeratedPreview = isModeratedForumCommentContent;

  categories: Category[] = [];
  discussionPosts: Post[] = [];
  topPosts: Post[] = [];
  topComments: ForumTopComment[] = [];
  /** Decorative counts keyed by category id (stable when list is filtered). */
  postCountById: Record<number, number> = {};
  loadingPosts = false;
  loadingMore = false;
  usingDemoCategories = false;
  /** Category filter (API). */
  filterCategoryId: number | null = null;
  /** Debounced thread search (title/body on server). */
  threadSearchInput = '';
  private readonly searchSubject = new Subject<string>();
  private searchSub?: Subscription;
  private refreshSub?: Subscription;
  /** Cancels in-flight list request so sort/filter always wins over an older response. */
  private fetchListSub?: Subscription;
  private observer?: IntersectionObserver;

  postSort: PostSort = 'recent';
  private page = 0;
  readonly pageSize = 12;
  /** CVP « Top threads / Top comments » : afficher uniquement le top 3. */
  readonly topCvpLimit = 3;
  hasMore = false;
  totalPostCount = 0;
  totalMembers = 0;
  totalDiscussions = 0;

  @ViewChild('loadMoreSentinel') loadMoreSentinel?: ElementRef<HTMLElement>;

  constructor(
    private forumService: ForumService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private readonly zoneUi: NgZoneUiSync
  ) {}

  ngOnInit(): void {
    this.searchSub = this.searchSubject
      .pipe(debounceTime(220), distinctUntilChanged())
      .subscribe((q) => {
        this.threadSearchInput = q;
        this.fetchDiscussions(true);
      });

    this.refreshSub = this.forumService.refresh$.subscribe(() => {
      this.fetchDiscussions(true);
    });

    this.fetchDiscussions(true);
  }

  ngOnDestroy(): void {
    this.searchSub?.unsubscribe();
    this.refreshSub?.unsubscribe();
    this.fetchListSub?.unsubscribe();
    this.observer?.disconnect();
  }

  onThreadSearchInput(value: string): void {
    this.searchSubject.next(value);
  }

  setSort(sort: PostSort): void {
    this.postSort = sort;
    this.fetchDiscussions(true);
  }

  onCategoryFilterChange(): void {
    this.fetchDiscussions(true);
  }

  clearAdvancedFilters(): void {
    this.threadSearchInput = '';
    this.filterCategoryId = null;
    this.postSort = 'recent';
    this.fetchDiscussions(true);
  }

  private setupInfiniteScroll(): void {
    if (typeof IntersectionObserver === 'undefined' || !this.loadMoreSentinel?.nativeElement) {
      return;
    }
    this.observer?.disconnect();
    this.observer = new IntersectionObserver(
      (entries) => {
        const hit = entries.some((e) => e.isIntersecting);
        if (hit && this.hasMore && !this.loadingPosts && !this.loadingMore) {
          this.fetchDiscussions(false);
        }
      },
      { root: null, rootMargin: '120px', threshold: 0 }
    );
    this.observer.observe(this.loadMoreSentinel.nativeElement);
  }

  private fetchDiscussions(reset: boolean): void {
    this.fetchListSub?.unsubscribe();

    if (reset) {
      this.page = 0;
      this.loadingPosts = true;
      this.discussionPosts = [];
    } else {
      if (!this.hasMore || this.loadingMore || this.loadingPosts) {
        return;
      }
      this.loadingMore = true;
    }

    const q = this.threadSearchInput.trim();
    const pageForRequest = this.page;

    this.fetchListSub = this.forumService
      .getForumHome({
        postSort: this.postSort,
        postPage: pageForRequest,
        postSize: this.pageSize,
        q: q || undefined,
        categoryId: this.filterCategoryId ?? undefined,
      })
      .pipe(catchError(() => of<ForumHomePayload>({ categories: [], posts: [] })))
      .subscribe({
        next: (payload) => {
          this.zoneUi.apply(this.cdr, () => {
            if (reset) {
              this.totalMembers = payload.totalMemberCount ?? 0;
              this.totalDiscussions = payload.totalThreadCount ?? 0;
              this.totalPostCount = payload.totalPostCount ?? payload.posts?.length ?? 0;
              this.topPosts = (payload.topPosts ?? []).slice(0, this.topCvpLimit);
              this.topComments = (payload.topComments ?? []).slice(0, this.topCvpLimit);
              if (payload.categories?.length) {
                this.categories = payload.categories.slice(0, 6);
                this.usingDemoCategories = false;
              } else {
                this.categories = FORUM_FALLBACK_CATEGORIES;
                this.usingDemoCategories = true;
              }
              this.initializeCountsFromPosts(payload.posts ?? []);
              this.discussionPosts = [...(payload.posts ?? [])];
            } else {
              this.discussionPosts = [...this.discussionPosts, ...(payload.posts ?? [])];
            }

            this.totalPostCount = payload.totalPostCount ?? this.totalPostCount;
            this.hasMore = this.discussionPosts.length < (this.totalPostCount ?? 0);
            this.page++;
            this.loadingPosts = false;
            this.loadingMore = false;
          });
          queueMicrotask(() => {
            this.setupInfiniteScroll();
            this.zoneUi.apply(this.cdr, () => {});
          });
        },
        error: () => {
          this.zoneUi.apply(this.cdr, () => {
            if (reset) {
              this.categories = FORUM_FALLBACK_CATEGORIES;
              this.usingDemoCategories = true;
            }
            this.loadingPosts = false;
            this.loadingMore = false;
          });
        },
      });
  }

  get displayCategories(): Category[] {
    return this.categories.slice(0, 6);
  }

  initializeCountsFromPosts(posts: Post[]): void {
    this.postCountById = {};
    for (const p of posts) {
      const cid = p.categoryId;
      if (cid != null) {
        this.postCountById[cid] = (this.postCountById[cid] || 0) + 1;
      }
    }
  }

  getPostCountForCategory(cat: Category): number {
    if (cat.id == null) {
      return 0;
    }
    return this.postCountById[cat.id] ?? 0;
  }

  navigateToCategory(cat: Category): void {
    if (cat?.id != null) {
      this.router.navigate(['/forum/category', cat.id]);
    }
  }

  navigateToPost(post: Post): void {
    if (post?.id != null) {
      this.router.navigate(['/forum/post', post.id]);
    }
  }

  navigateToPostId(postId: number | undefined): void {
    if (postId != null) {
      this.router.navigate(['/forum/post', postId]);
    }
  }

  formatDate(iso: string): string {
    try {
      return new Date(iso).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
      });
    } catch {
      return iso;
    }
  }

  /** Handles ISO string or Jackson LocalDateTime array so the template never breaks. */
  formatPostDate(post: Post): string {
    const v = post.createdAt as unknown;
    if (v == null || v === '') {
      return '—';
    }
    if (typeof v === 'string') {
      return this.formatDate(v);
    }
    if (Array.isArray(v) && v.length >= 3) {
      const y = Number(v[0]);
      const mo = Number(v[1]) - 1;
      const d = Number(v[2]);
      const h = v.length > 3 ? Number(v[3]) : 0;
      const mi = v.length > 4 ? Number(v[4]) : 0;
      const sec = v.length > 5 ? Number(v[5]) : 0;
      const dt = new Date(y, mo, d, h, mi, sec);
      if (!isNaN(dt.getTime())) {
        return dt.toLocaleDateString('en-US', {
          year: 'numeric',
          month: 'short',
          day: 'numeric',
        });
      }
    }
    return String(v);
  }

  trackCategory(_index: number, cat: Category): number | string {
    return cat.id ?? cat.name;
  }

  categoryVisualIndex(cat: Category): number {
    if (cat.id != null) {
      return cat.id;
    }
    let h = 0;
    for (let i = 0; i < cat.name.length; i++) {
      h = (h * 31 + cat.name.charCodeAt(i)) >>> 0;
    }
    return h;
  }

  getCategoryIcon(i: number): string {
    const icons = [
      'ri-heart-pulse-line',
      'ri-microscope-line',
      'ri-discuss-line',
      'ri-newspaper-line',
      'ri-folder-line',
      'ri-question-line',
    ];
    return icons[i % icons.length];
  }

  getCategoryGradient(i: number): string {
    const gradients = [
      'linear-gradient(135deg, #099aa7 0%, #0e7490 100%)',
      'linear-gradient(135deg, #f43f5e 0%, #ec4899 100%)',
      'linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%)',
      'linear-gradient(135deg, #059669 0%, #0d9488 100%)',
      'linear-gradient(135deg, #0891b2 0%, #2563eb 100%)',
      'linear-gradient(135deg, #db2777 0%, #c026d3 100%)',
    ];
    return gradients[i % gradients.length];
  }

  /** Uses stored {@link Category.color} when set (back-office), else rotating palette. */
  getCategoryCardStyle(cat: Category): string {
    const c = cat.color?.trim();
    if (c && /^#[0-9A-Fa-f]{6}$/.test(c)) {
      return `linear-gradient(135deg, ${c} 0%, ${c}cc 55%, ${c}99 100%)`;
    }
    return this.getCategoryGradient(this.categoryVisualIndex(cat));
  }

  getPostCategoryStyle(name: string | undefined): string {
    const key = name ?? '';
    let h = 0;
    for (let i = 0; i < key.length; i++) {
      h = (h + key.charCodeAt(i) * (i + 1)) % 6;
    }
    return this.getCategoryGradient(h);
  }

  postExcerpt(post: Post): string {
    const raw = post.content ?? '';
    const text = raw.replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim();
    return text.length > 220 ? text.slice(0, 217) + '…' : text;
  }

  trackPost(_index: number, post: Post): number {
    return post.id;
  }
}
