import { ChangeDetectorRef, Component, NgZone, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, of } from 'rxjs';
import { timeout } from 'rxjs/operators';
import {
  ForumService,
  Post,
  Category,
  StaffDashboardPayload,
  STAFF_POST_LIST_LIMIT,
} from '../../../core/services/forum.service';
import { AuthService } from '../../../core/services/auth.service';
import { PostForm } from '../post-form/post-form';
import { forumPostFormDialogConfig } from '../forum-post-form-dialog.config';
import { DeletePostModal } from '../delete-post-modal/delete-post-modal';

@Component({
  selector: 'app-post-list',
  templateUrl: './post-list.html',
  styleUrls: ['./post-list.css'],
  standalone: false
})
export class PostList implements OnInit {
  posts: Post[] = [];
  categories: Category[] = [];
  loading = true;
  searchTerm = '';
  selectedCategory: number | null = null;

  private dashSummary: StaffDashboardPayload | null = null;

  constructor(
    private forumService: ForumService,
    private authService: AuthService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private readonly cdr: ChangeDetectorRef,
    private readonly ngZone: NgZone
  ) {}

  get canManageForum(): boolean {
    return this.authService.canManageForumPosts();
  }

  canEditPost(post: Post): boolean {
    return this.authService.canEditOrDeleteForumPost(post);
  }

  get totalViews(): number {
    if (this.dashSummary?.totalViewCount != null) {
      return this.dashSummary.totalViewCount;
    }
    return this.posts.reduce((acc, p) => acc + (p.viewCount ?? 0), 0);
  }

  ngOnInit(): void {
    this.forumService.getAllCategories().subscribe();
    /** Next macrotask: same fix as admin dashboard — router-outlet + HTTP subscribe sometimes skip CD until another UI event (e.g. dialog). */
    setTimeout(() => this.loadPosts(), 0);
  }

  loadPosts(): void {
    this.loading = true;
    this.cdr.detectChanges();
    const uid = this.authService.getUserId();
    const doctorScope = this.authService.getRole() === 'DOCTOR' && uid != null;
    this.forumService
      .getStaffDashboard(doctorScope ? uid! : null, STAFF_POST_LIST_LIMIT)
      .pipe(
        timeout(12000),
        catchError(() =>
          of({
            categories: [] as Category[],
            posts: [] as Post[],
            totalPostCount: 0,
            totalCommentCount: 0,
            totalViewCount: 0,
            publishedPostCount: 0,
            draftPostCount: 0,
          } as StaffDashboardPayload)
        )
      )
      .subscribe({
        next: (payload) => {
          this.ngZone.run(() => {
            this.dashSummary = payload;
            this.categories = payload.categories;
            this.posts = payload.posts;
            this.loading = false;
            this.cdr.detectChanges();
          });
        },
        error: () => {
          this.ngZone.run(() => {
            this.loading = false;
            this.cdr.detectChanges();
            this.showSnackBar('Failed to load posts. Please try again.', 'error');
          });
        },
      });
  }

  get filteredPosts(): Post[] {
    return this.posts.filter(post => {
      const matchesSearch = !this.searchTerm || 
        post.title.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        post.author.toLowerCase().includes(this.searchTerm.toLowerCase());
      const matchesCategory = !this.selectedCategory || post.categoryId === this.selectedCategory;
      return matchesSearch && matchesCategory;
    });
  }

  get totalComments(): number {
    if (this.dashSummary?.totalCommentCount != null) {
      return this.dashSummary.totalCommentCount;
    }
    return this.posts.reduce((acc, p) => acc + (p.commentCount || 0), 0);
  }

  get totalCategoriesCount(): number {
    return new Set(this.posts.map(p => p.categoryId)).size || 9;
  }

  openPostForm(post?: Post): void {
    if (post && !this.authService.canEditOrDeleteForumPost(post)) {
      return;
    }
    const dialogRef = this.dialog.open(
      PostForm,
      forumPostFormDialogConfig({ post, categories: this.categories })
    );

    dialogRef.afterClosed().subscribe((result: any) => {
      if (result) {
        console.log('[Post List] Post form closed with result:', result);
        this.showSnackBar(post ? 'Post updated successfully!' : 'Post created successfully!', 'success');
        this.ngZone.run(() => this.loadPosts());
      }
    });
  }

  deletePost(post: Post): void {
    if (!this.authService.canEditOrDeleteForumPost(post)) {
      return;
    }
    const dialogRef = this.dialog.open(DeletePostModal, {
      width: '450px',
      data: { post },
      panelClass: 'forum-dialog'
    });

    dialogRef.afterClosed().subscribe((result: any) => {
      if (result === true) {
        console.log('[Post List] Deleting post:', post.id);
        this.forumService.deletePost(post.id).subscribe({
          next: () => {
            console.log('[Post List] Post deleted successfully');
            this.showSnackBar('Post deleted successfully!', 'success');
            this.ngZone.run(() => {
              this.loadPosts();
            });
          },
          error: (err) => {
            console.error('[Post List] Error deleting post:', err);
            this.showSnackBar('Failed to delete post. Please try again.', 'error');
          }
        });
      }
    });
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
    });
  }

  getCategoryColor(categoryId: number): string {
    const colors: { [key: number]: string } = {
      1: 'bg-rose-100 text-rose-700',
      2: 'bg-amber-100 text-amber-700',
      3: 'bg-emerald-100 text-emerald-700',
      4: 'bg-violet-100 text-violet-700',
      5: 'bg-blue-100 text-blue-700',
      6: 'bg-orange-100 text-orange-700',
      7: 'bg-cyan-100 text-cyan-700',
      8: 'bg-indigo-100 text-indigo-700',
      9: 'bg-teal-100 text-teal-700',
    };
    return colors[categoryId] || 'bg-gray-100 text-gray-700';
  }

  private showSnackBar(message: string, type: 'success' | 'error'): void {
    this.snackBar.open(message, 'Close', {
      duration: 3000,
      panelClass: type === 'success' ? ['snackbar-success'] : ['snackbar-error'],
      horizontalPosition: 'right',
      verticalPosition: 'top'
    });
  }
}
