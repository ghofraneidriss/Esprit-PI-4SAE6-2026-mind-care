import { ChangeDetectorRef, Component, NgZone, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import {
  ForumService,
  Post,
  Category,
  StaffDashboardPayload,
  STAFF_POST_LIST_LIMIT,
} from '../../../core/services/forum.service';
import { DeleteConfirmationModalComponent, DeleteConfirmationData } from './delete-confirmation-modal.component';
import { PostForm } from '../post-form/post-form';
import { forumPostFormDialogConfig } from '../forum-post-form-dialog.config';
import { SuccessAlertComponent } from './success-alert.component';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-list-post',
  templateUrl: './list-post.component.html',
  styleUrls: ['./list-post.component.css'],
  standalone: false
})
export class ListPostComponent implements OnInit {
  posts: Post[] = [];
  categories: Category[] = [];
  searchTerm: string = '';
  selectedCategory: string = '';
  showFilters = false;

  // Statistics
  totalPosts = 0;
  publishedPosts = 0;
  draftPosts = 0;
  totalCategories = 0;

  loading = false;
  error: string | null = null;
  showSuccessAlert: boolean = false;
  successMessage: string = '';

  /** KPIs from server COUNTs (list may be capped). */
  private dashSummary: StaffDashboardPayload | null = null;

  constructor(
    private forumService: ForumService,
    private authService: AuthService,
    private dialog: MatDialog,
    private readonly cdr: ChangeDetectorRef,
    private readonly ngZone: NgZone
  ) {}

  ngOnInit(): void {
    this.forumService.getAllCategories().subscribe();
    setTimeout(() => this.loadData(), 0);
  }

  loadData(): void {
    this.loading = true;
    this.error = null;
    this.cdr.detectChanges();

    // Safety timeout
    const timer = setTimeout(() => {
      this.ngZone.run(() => {
        if (this.loading) {
          this.loading = false;
          this.error = 'The request is taking too long. Showing local data if available.';
          this.cdr.detectChanges();
        }
      });
    }, 10000);

    const uid = this.authService.getUserId();
    const doctorScope = this.authService.getRole() === 'DOCTOR' && uid != null;

    this.forumService.getStaffDashboard(doctorScope ? uid! : null, STAFF_POST_LIST_LIMIT).subscribe({
      next: (payload) => {
        const { categories, posts } = payload;
        clearTimeout(timer);
        this.ngZone.run(() => {
          this.dashSummary = payload;
          this.posts = posts.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
          this.categories = categories;
          this.totalCategories = categories.length;
          this.calculateStats();
          this.loading = false;
          this.error = null;
          this.cdr.detectChanges();
        });
      },
      error: (err) => {
        clearTimeout(timer);
        console.error('Error loading posts:', err);
        this.ngZone.run(() => {
          this.error = 'Unable to connect to forum API (gateway 8080 / forums-service 8082).';
          this.loading = false;
          this.cdr.detectChanges();
        });
      },
    });
  }

  calculateStats(): void {
    const s = this.dashSummary;
    if (s?.totalPostCount != null) {
      this.totalPosts = s.totalPostCount;
      this.publishedPosts =
        s.publishedPostCount ?? this.posts.filter((p) => p.status === 'PUBLISHED').length;
      this.draftPosts = s.draftPostCount ?? this.posts.filter((p) => p.status === 'DRAFT').length;
    } else {
      this.totalPosts = this.posts.length;
      this.publishedPosts = this.posts.filter((p) => p.status === 'PUBLISHED').length;
      this.draftPosts = this.posts.filter((p) => p.status === 'DRAFT').length;
    }
  }

  getFilteredPosts(): Post[] {
    return this.posts.filter(post => {
      const matchesSearch = post.title.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        post.content.toLowerCase().includes(this.searchTerm.toLowerCase());
      const matchesCategory = !this.selectedCategory || post.categoryId === parseInt(this.selectedCategory);
      return matchesSearch && matchesCategory;
    });
  }

  getCategoryName(categoryId: number): string {
    const category = this.categories.find(c => c.id === categoryId);
    return category ? category.name : 'Unknown';
  }

  getCategoryColor(categoryName: string): string {
    const colors: { [key: string]: string } = {
      'Support': '#3B82F6',      // Blue
      'Research': '#10B981',     // Green
      'Discussion': '#8B5CF6',   // Purple
      'News': '#F59E0B',         // Orange
      'General': '#6B7280'       // Gray
    };
    return colors[categoryName] || '#6B7280';
  }

  getStatusColor(status?: string): string {
    if (!status) return '#6B7280'; // Gray for undefined
    return status === 'PUBLISHED' ? '#10B981' : '#F59E0B';
  }

  getStatusText(status?: string): string {
    if (!status) return 'Unknown';
    return status === 'PUBLISHED' ? 'Published' : 'Draft';
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  createNewPost(): void {
    const dialogRef = this.dialog.open(
      PostForm,
      forumPostFormDialogConfig({ post: undefined, categories: this.categories })
    );

    dialogRef.afterClosed().subscribe((result: { success: boolean; post?: Post }) => {
      if (result && result.success && result.post) {
        // Add new post to the beginning of the array
        this.posts.unshift(result.post);
        this.calculateStats();
        this.showSuccessAlertMessage('Post created successfully!');
      }
    });
  }

  editPost(post: Post): void {
    const dialogRef = this.dialog.open(PostForm, forumPostFormDialogConfig({ post, categories: this.categories }));

    dialogRef.afterClosed().subscribe((result: { success: boolean; post?: Post }) => {
      if (result && result.success && result.post) {
        // Update the post in the array
        const index = this.posts.findIndex(p => p.id === result.post!.id);
        if (index !== -1) {
          this.posts[index] = result.post;
          this.calculateStats();
          this.showSuccessAlertMessage('Post updated successfully!');
        }
      }
    });
  }

  deletePost(post: Post): void {
    const dialogRef = this.dialog.open(DeleteConfirmationModalComponent, {
      data: {
        postTitle: post.title,
        postId: post.id
      },
      width: '400px',
      panelClass: 'delete-modal-container'
    });

    dialogRef.afterClosed().subscribe((confirmed: boolean) => {
      if (confirmed) {
        this.forumService.deletePost(post.id).subscribe({
          next: () => {
            this.loadData();
          },
          error: (err) => {
            console.error('Error deleting post:', err);
          }
        });
      }
    });
  }

  toggleFilters(): void {
    this.showFilters = !this.showFilters;
  }

  showSuccessAlertMessage(message: string): void {
    this.successMessage = message;
    this.showSuccessAlert = true;

    // Auto-hide after 2 seconds
    setTimeout(() => {
      this.showSuccessAlert = false;
    }, 2000);
  }
}
