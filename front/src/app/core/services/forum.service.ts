import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, of, Subject, tap, catchError } from 'rxjs';
import { forumsApiBase } from '../../../environments/environment';

/** Single response for admin forum dashboard (categories + staff posts). */
/** Top categories by thread count (global forum — from staff KPIs). */
export interface ForumTopCategory {
  categoryId: number;
  categoryName: string;
  postCount: number;
}

/** CVP / public : GET /api/forum/stats */
export interface ForumStatsPayload {
  totalPosts: number;
  totalComments: number;
  inactivePosts: number;
  topCategories: ForumTopCategory[];
}

export interface StaffDashboardPayload {
  categories: Category[];
  posts: Post[];
  /** KPIs from COUNT queries (full forum or doctor scope) — use for cards even when {@link posts} is limited. */
  totalPostCount?: number;
  totalCommentCount?: number;
  totalViewCount?: number;
  publishedPostCount?: number;
  draftPostCount?: number;
  /** Archived threads (hidden from public), forum-wide. */
  inactivePostCount?: number;
  topCategories?: ForumTopCategory[];
}

/** Public forum home — categories + KPIs + threads + top widgets in one call. */
export interface ForumHomePayload {
  categories: Category[];
  posts: Post[];
  totalMemberCount?: number;
  totalThreadCount?: number;
  /** Total rows matching current sort + search (for pagination). */
  totalPostCount?: number;
  topPosts?: Post[];
  topComments?: ForumTopComment[];
}

export interface ForumTopComment {
  id: number;
  postId: number;
  postTitle: string;
  contentPreview: string;
  authorName: string;
  likeCount: number;
  createdAt: string;
}

export interface Category {
  id: number;
  name: string;
  description: string;
  icon?: string;
  /** Hex accent, e.g. #0891b2 — from forums-service / back-office. */
  color?: string;
}

export interface Comment {
  id: number;
  content: string;
  createdAt: string;
  userId: number;
  /** Set by forums-service from users-service (first + last name). */
  authorName?: string;
  /** Present when returned from the forums API (comment on which post). */
  postId?: number;
  likeCount?: number;
  dislikeCount?: number;
  /** When viewerUserId is sent on GET comments/post/:id */
  likedByMe?: boolean;
  dislikedByMe?: boolean;
  /** ≥7 likes → best answer (forums-service). */
  bestAnswer?: boolean;
}

/**
 * Server stored a bracketed moderation placeholder (EN/FR) or mojibake from old UTF-8 French strings.
 * When true, the thread UI shows the modern moderation card instead of raw placeholder text.
 */
export function isModeratedForumCommentContent(content: string | null | undefined): boolean {
  const t = (content ?? '').trim();
  if (!t.startsWith('[')) {
    return false;
  }
  return (
    /Comment removed|Commentaire|community standards|community guidelines|does not meet|inappropri|masqu|langage/i.test(
      t
    ) || /masquÃ|inappropriÃ/i.test(t)
  );
}

export interface ForumBanStatus {
  banned: boolean;
  bannedUntil: string | null;
}

/** Backend enum names are historical; bans are 1 / 3 / 5 minutes (forums-service). */
export type ModerationResolveAction =
  | 'DISMISS'
  | 'LIFT_BAN'
  | 'DELETE_COMMENT'
  | 'BAN_1_DAY'
  | 'BAN_3_DAYS'
  | 'BAN_7_DAYS';

export interface CommentReportItem {
  id: number;
  commentId: number;
  postId: number;
  postAuthorId: number;
  reporterUserId: number;
  reportedUserId: number;
  /** Display name from users-service (falls back to “User #id”). */
  reporterName?: string;
  reportedUserName?: string;
  reason: string;
  status: string;
  createdAt: string;
  commentPreview: string;
  /** Set when resolved — DISMISS, DELETE_COMMENT, BAN_* */
  resolutionAction?: string | null;
  resolvedAt?: string | null;
  /** False when comment was deleted — decision is final. */
  canChangeDecision?: boolean;
}

export type ReactionType =
  | 'LIKE'
  | 'LOVE'
  | 'CARE'
  | 'HAHA'
  | 'WOW'
  | 'SAD'
  | 'ANGRY'
  | 'DISLIKE';

export interface Post {
  id: number;
  title: string;
  content: string;
  createdAt: string;
  userId: number;
  author: string;
  categoryId: number;
  categoryName: string;
  commentCount: number;
  status?: 'PUBLISHED' | 'DRAFT';
  viewCount?: number;
  reactionCounts?: Record<string, number>;
  myReaction?: string | null;
  averageRating?: number | null;
  ratingCount?: number;
  myRating?: number | null;
  inactive?: boolean;
  inactiveSince?: string | null;
  hasImages?: boolean;
  /** Current user follows this thread (GET with userId). */
  following?: boolean | null;
}

/** Row from {@code GET /posts/{id}/media-meta} (no image bytes). */
export interface PostMediaInfo {
  id: number;
  sortOrder: number;
  contentType: string;
}

/**
 * Max posts returned in one staff-dashboard call for list views.
 * Keeps JSON + DB aggregation (views / media / comments per id) bounded.
 */
export const STAFF_POST_LIST_LIMIT = 100;

@Injectable({
  providedIn: 'root'
})
export class ForumService {
  private refreshSubject = new Subject<void>();
  /** In-memory categories (filled by staff-dashboard / forum-home / getAllCategories). */
  private categoriesCache: Category[] | null = null;

  constructor(private http: HttpClient) {
    this.refreshSubject.subscribe(() => {
      this.categoriesCache = null;
    });
  }

  private forumsBase(): string {
    return forumsApiBase();
  }

  private setCategoriesCache(cats: Category[] | undefined | null): void {
    if (cats && cats.length > 0) {
      this.categoriesCache = cats;
    }
  }

  /** Optional: parent already has categories from dashboard — avoids a second HTTP. */
  primeCategoriesCache(categories: Category[]): void {
    if (categories?.length) {
      this.categoriesCache = [...categories];
    }
  }

  get refresh$(): Observable<void> {
    return this.refreshSubject.asObservable();
  }

  private userParams(userId?: number | null): { params?: HttpParams } {
    if (userId != null && userId > 0) {
      return { params: new HttpParams().set('userId', String(userId)) };
    }
    return {};
  }

  // Categories
  getAllCategories(): Observable<Category[]> {
    if (this.categoriesCache && this.categoriesCache.length > 0) {
      return of([...this.categoriesCache]);
    }
    return this.http.get<Category[]>(`${this.forumsBase()}/categories`).pipe(
      tap((c) => this.setCategoriesCache(c)),
      /** Always complete with a value so dialogs never spin forever on network/CORS errors. */
      catchError(() => of<Category[]>([]))
    );
  }

  /** Admin category management — surfaces HTTP errors instead of returning an empty list. */
  getAllCategoriesStrict(): Observable<Category[]> {
    return this.http.get<Category[]>(`${this.forumsBase()}/categories`).pipe(
      tap((c) => this.setCategoriesCache(c))
    );
  }

  getCategoryById(id: number): Observable<Category> {
    return this.http.get<Category>(`${this.forumsBase()}/categories/${id}`);
  }

  createCategory(category: {
    name: string;
    description?: string;
    icon?: string;
    color?: string;
  }): Observable<Category> {
    return this.http
      .post<Category>(`${this.forumsBase()}/categories`, category)
      .pipe(tap(() => this.refreshSubject.next()));
  }

  updateCategory(
    id: number,
    category: { name: string; description?: string; icon?: string; color?: string }
  ): Observable<Category> {
    return this.http
      .put<Category>(`${this.forumsBase()}/categories/${id}`, category)
      .pipe(tap(() => this.refreshSubject.next()));
  }

  deleteCategory(id: number): Observable<void> {
    return this.http
      .delete<void>(`${this.forumsBase()}/categories/${id}`)
      .pipe(tap(() => this.refreshSubject.next()));
  }

  // Posts
  getAllPosts(userId?: number | null): Observable<Post[]> {
    return this.http.get<Post[]>(`${this.forumsBase()}/posts`, this.userParams(userId));
  }

  /** Posts written by {@link authorId} (back office: each doctor loads only their own). */
  getPostsByAuthor(authorId: number, viewerUserId?: number | null): Observable<Post[]> {
    return this.http.get<Post[]>(
      `${this.forumsBase()}/posts/author/${authorId}`,
      this.userParams(viewerUserId)
    );
  }

  /**
   * Fast staff listing: no HTML body, no reactions/ratings aggregation — for /admin forum tables.
   */
  getAllPostsStaff(): Observable<Post[]> {
    const params = new HttpParams().set('staffList', 'true');
    return this.http.get<Post[]>(`${this.forumsBase()}/posts`, { params });
  }

  getPostsByAuthorStaff(authorId: number): Observable<Post[]> {
    const params = new HttpParams().set('staffList', 'true');
    return this.http.get<Post[]>(`${this.forumsBase()}/posts/author/${authorId}`, { params });
  }

  /**
   * One request for back-office forum overview (replaces parallel categories + staff posts).
   * Pass authorId for doctor scope (only their threads).
   * @param postLimit max rows in `posts` (KPIs still use full COUNTs); use a small value for the overview page, larger for list views.
   */
  getStaffDashboard(authorId?: number | null, postLimit?: number): Observable<StaffDashboardPayload> {
    let params = new HttpParams();
    if (authorId != null && authorId > 0) {
      params = params.set('authorId', String(authorId));
    }
    if (postLimit != null && postLimit > 0) {
      params = params.set('limit', String(postLimit));
    }
    return this.http
      .get<StaffDashboardPayload>(`${this.forumsBase()}/posts/staff-dashboard`, { params })
      .pipe(tap((p) => this.setCategoriesCache(p.categories)));
  }

  /** KPIs + categories only (fast). Pair with {@link getStaffRecentPosts} on the admin overview. */
  getStaffDashboardKpis(authorId?: number | null): Observable<StaffDashboardPayload> {
    let params = new HttpParams();
    if (authorId != null && authorId > 0) {
      params = params.set('authorId', String(authorId));
    }
    return this.http
      .get<StaffDashboardPayload>(`${this.forumsBase()}/posts/staff-dashboard/kpis`, { params })
      .pipe(tap((p) => this.setCategoriesCache(p.categories)));
  }

  /** Recent staff rows only (heavier). Loads in parallel with {@link getStaffDashboardKpis}. */
  getStaffRecentPosts(authorId?: number | null, limit = 12): Observable<Post[]> {
    let params = new HttpParams().set('limit', String(limit));
    if (authorId != null && authorId > 0) {
      params = params.set('authorId', String(authorId));
    }
    return this.http.get<Post[]>(`${this.forumsBase()}/posts/staff-dashboard/recent`, { params });
  }

  /**
   * Public MindCare forum home (categories + KPIs + sorted threads + top posts/comments).
   * @param postSort recent | hot | views
   */
  getForumHome(options?: {
    postSort?: string;
    postPage?: number;
    postSize?: number;
    q?: string;
    categoryId?: number;
  }): Observable<ForumHomePayload> {
    /** Always send sort/pagination so the API never uses a stale default; _cb avoids browser/proxy GET cache. */
    let params = new HttpParams()
      .set('postSort', options?.postSort ?? 'recent')
      .set('postPage', String(options?.postPage ?? 0))
      .set('postSize', String(options?.postSize ?? 12))
      .set('_cb', String(Date.now()));
    if (options?.q != null && options.q.trim() !== '') {
      params = params.set('q', options.q.trim());
    }
    if (options?.categoryId != null && options.categoryId > 0) {
      params = params.set('categoryId', String(options.categoryId));
    }
    return this.http
      .get<ForumHomePayload>(`${this.forumsBase()}/forum/home`, { params })
      .pipe(tap((p) => this.setCategoriesCache(p.categories)));
  }

  /** Global forum KPIs + top categories (front office / CVP — not admin). */
  getForumStats(): Observable<ForumStatsPayload> {
    const params = new HttpParams().set('_cb', String(Date.now()));
    return this.http.get<ForumStatsPayload>(`${this.forumsBase()}/forum/stats`, { params });
  }

  /**
   * @param includeInactive back-office preview of archived threads only (public GET returns 404 if inactive).
   */
  getPostById(
    id: number,
    userId?: number | null,
    opts?: { includeInactive?: boolean }
  ): Observable<Post> {
    let params = new HttpParams();
    if (userId != null && userId > 0) {
      params = params.set('userId', String(userId));
    }
    if (opts?.includeInactive) {
      params = params.set('includeInactive', 'true');
    }
    return this.http.get<Post>(`${this.forumsBase()}/posts/${id}`, { params });
  }

  /** Suivre le fil : notification à chaque nouveau commentaire (followers). */
  followPost(postId: number, userId: number): Observable<void> {
    const params = new HttpParams().set('userId', String(userId));
    return this.http.post<void>(`${this.forumsBase()}/posts/${postId}/follow`, null, { params });
  }

  unfollowPost(postId: number, userId: number): Observable<void> {
    const params = new HttpParams().set('userId', String(userId));
    return this.http.delete<void>(`${this.forumsBase()}/posts/${postId}/follow`, { params });
  }

  /** Historique staff : publications retirées du forum public (inactives). */
  getInactivePostsHistory(viewerUserId?: number | null, limit = 200): Observable<Post[]> {
    let params = new HttpParams().set('limit', String(limit));
    if (viewerUserId != null && viewerUserId > 0) {
      params = params.set('userId', String(viewerUserId));
    }
    return this.http.get<Post[]>(`${this.forumsBase()}/posts/inactive-history`, { params });
  }

  getPostsByCategory(categoryId: number, userId?: number | null): Observable<Post[]> {
    return this.http.get<Post[]>(`${this.forumsBase()}/posts/category/${categoryId}`, this.userParams(userId));
  }

  /** Records one view per logged-in user (once per post). */
  recordPostView(postId: number, userId: number): Observable<void> {
    return this.http.post<void>(`${this.forumsBase()}/posts/${postId}/view`, { userId });
  }

  setPostReaction(postId: number, userId: number, type: ReactionType): Observable<void> {
    return this.http
      .put<void>(`${this.forumsBase()}/posts/${postId}/reaction`, { userId, type })
      .pipe(tap(() => this.refreshSubject.next()));
  }

  clearPostReaction(postId: number, userId: number): Observable<void> {
    const params = new HttpParams().set('userId', String(userId));
    return this.http
      .delete<void>(`${this.forumsBase()}/posts/${postId}/reaction`, { params })
      .pipe(tap(() => this.refreshSubject.next()));
  }

  setPostRating(postId: number, userId: number, value: number): Observable<void> {
    return this.http
      .put<void>(`${this.forumsBase()}/posts/${postId}/rating`, { userId, value })
      .pipe(tap(() => this.refreshSubject.next()));
  }

  createPost(
    post: { title: string; content: string; userId: number; status?: string },
    categoryId: number
  ): Observable<Post> {
    return this.http
      .post<Post>(`${this.forumsBase()}/posts/category/${categoryId}`, post)
      .pipe(tap(() => this.refreshSubject.next()));
  }

  /** Photos stored as LONGBLOB in {@code post_media} (multipart). */
  createPostWithPhotos(
    categoryId: number,
    post: { title: string; content: string; userId: number; status?: string },
    files: File[]
  ): Observable<Post> {
    const fd = new FormData();
    fd.append('title', post.title);
    fd.append('content', post.content);
    fd.append('userId', String(post.userId));
    fd.append('status', post.status ?? 'PUBLISHED');
    for (const f of files) {
      fd.append('photos', f, f.name);
    }
    return this.http
      .post<Post>(`${this.forumsBase()}/posts/category/${categoryId}/with-photos`, fd)
      .pipe(tap(() => this.refreshSubject.next()));
  }

  /** URL for first image (cover) of a post — use in `<img [src]>` with forums base. */
  postCoverImageUrl(postId: number): string {
    return `${this.forumsBase()}/posts/${postId}/media/cover`;
  }

  /** Single attachment image URL (by media row id). */
  postMediaUrl(postId: number, mediaId: number): string {
    return `${this.forumsBase()}/posts/${postId}/media/${mediaId}`;
  }

  getPostMediaMeta(postId: number): Observable<PostMediaInfo[]> {
    return this.http.get<PostMediaInfo[]>(`${this.forumsBase()}/posts/${postId}/media-meta`);
  }

  appendPostMedia(postId: number, files: File[]): Observable<void> {
    const fd = new FormData();
    for (const f of files) {
      fd.append('photos', f, f.name);
    }
    return this.http
      .post<void>(`${this.forumsBase()}/posts/${postId}/media`, fd)
      .pipe(tap(() => this.refreshSubject.next()));
  }

  deletePostMedia(postId: number, mediaId: number): Observable<void> {
    return this.http
      .delete<void>(`${this.forumsBase()}/posts/${postId}/media/${mediaId}`)
      .pipe(tap(() => this.refreshSubject.next()));
  }

  updatePost(
    id: number,
    post: { title?: string; content?: string; userId?: number; category?: { id: number }; status?: string }
  ): Observable<Post> {
    return this.http
      .put<Post>(`${this.forumsBase()}/posts/${id}`, post)
      .pipe(tap(() => this.refreshSubject.next()));
  }

  deletePost(id: number): Observable<void> {
    return this.http
      .delete<void>(`${this.forumsBase()}/posts/${id}`)
      .pipe(tap(() => this.refreshSubject.next()));
  }

  /** Restore an auto-archived thread to the public forum (staff). */
  reactivatePost(id: number): Observable<void> {
    return this.http
      .post<void>(`${this.forumsBase()}/posts/${id}/reactivate`, {})
      .pipe(tap(() => this.refreshSubject.next()));
  }

  // Comments
  getAllComments(): Observable<Comment[]> {
    return this.http.get<Comment[]>(`${this.forumsBase()}/comments`);
  }

  /** Comments on threads owned by {@link authorUserId} (doctor moderation scope). */
  getCommentsByPostAuthor(authorUserId: number): Observable<Comment[]> {
    const params = new HttpParams().set('_cb', String(Date.now()));
    return this.http.get<Comment[]>(`${this.forumsBase()}/comments/by-post-author/${authorUserId}`, {
      params,
    });
  }

  addComment(comment: Omit<Comment, 'id' | 'createdAt'>, postId: number): Observable<Comment> {
    return this.http
      .post<Comment>(`${this.forumsBase()}/comments/post/${postId}`, comment)
      .pipe(tap(() => this.refreshSubject.next()));
  }

  getCommentsByPostId(postId: number, viewerUserId?: number | null): Observable<Comment[]> {
    /** Bust caches (browser/proxy) so the list always reflects the latest after POST. */
    let params = new HttpParams().set('_cb', String(Date.now()));
    if (viewerUserId != null && viewerUserId > 0) {
      params = params.set('viewerUserId', String(viewerUserId));
    }
    return this.http.get<Comment[]>(`${this.forumsBase()}/comments/post/${postId}`, { params });
  }

  getForumBanStatus(userId: number): Observable<ForumBanStatus> {
    const params = new HttpParams().set('userId', String(userId));
    return this.http.get<ForumBanStatus>(`${this.forumsBase()}/forum/ban-status`, { params });
  }

  toggleCommentLike(commentId: number, userId: number): Observable<Comment> {
    const params = new HttpParams().set('userId', String(userId));
    return this.http
      .post<Comment>(`${this.forumsBase()}/comments/${commentId}/like`, null, { params })
      .pipe(tap(() => this.refreshSubject.next()));
  }

  toggleCommentDislike(commentId: number, userId: number): Observable<Comment> {
    const params = new HttpParams().set('userId', String(userId));
    return this.http
      .post<Comment>(`${this.forumsBase()}/comments/${commentId}/dislike`, null, { params })
      .pipe(tap(() => this.refreshSubject.next()));
  }

  reportComment(commentId: number, reporterUserId: number, reason: string): Observable<unknown> {
    return this.http.post(`${this.forumsBase()}/comments/${commentId}/report`, {
      reporterUserId,
      reason: reason || '',
    });
  }

  getPendingCommentReports(doctorId: number): Observable<CommentReportItem[]> {
    const params = new HttpParams()
      .set('doctorId', String(doctorId))
      .set('_cb', String(Date.now()));
    return this.http.get<CommentReportItem[]>(`${this.forumsBase()}/moderation/comment-reports`, { params });
  }

  resolveCommentReport(
    reportId: number,
    doctorId: number,
    action: ModerationResolveAction
  ): Observable<unknown> {
    return this.http
      .post(`${this.forumsBase()}/moderation/comment-reports/${reportId}/resolve`, {
        doctorId,
        action,
      })
      .pipe(tap(() => this.refreshSubject.next()));
  }

  updateComment(id: number, comment: { content: string }): Observable<Comment> {
    return this.http
      .put<Comment>(`${this.forumsBase()}/comments/${id}`, comment)
      .pipe(tap(() => this.refreshSubject.next()));
  }

  deleteComment(id: number): Observable<void> {
    return this.http
      .delete<void>(`${this.forumsBase()}/comments/${id}`)
      .pipe(tap(() => this.refreshSubject.next()));
  }
}
