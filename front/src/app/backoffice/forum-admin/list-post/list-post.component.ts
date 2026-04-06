import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { ForumService, Post, Category } from '../../../core/services/forum.service';
import { DeleteConfirmationModalComponent, DeleteConfirmationData } from './delete-confirmation-modal.component';
import { PostForm } from '../post-form/post-form';
import { SuccessAlertComponent } from './success-alert.component';

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

  constructor(
    private forumService: ForumService,
    private dialog: MatDialog
  ) { }

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.error = null;

    // Safety timeout
    const timer = setTimeout(() => {
      if (this.loading) {
        this.loading = false;
        this.error = 'The request is taking too long. Showing local data if available.';
      }
    }, 10000);

    // Load posts
    this.forumService.getAllPosts().subscribe({
      next: (posts) => {
        clearTimeout(timer);
        this.posts = posts.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
        this.calculateStats();
        this.loading = false;
        this.error = null;
      },
      error: (err) => {
        clearTimeout(timer);
        console.error('Error loading posts:', err);
        this.error = 'Unable to connect to forum service (8085).';
        this.loading = false;
      }
    });

    // Load categories (quietly)
    this.forumService.getAllCategories().subscribe({
      next: (categories) => {
        this.categories = categories;
        this.totalCategories = categories.length;
      },
      error: (err) => console.error('Error loading categories:', err)
    });
  }

  calculateStats(): void {
    this.totalPosts = this.posts.length;
    this.publishedPosts = this.posts.filter(p => p.status === 'PUBLISHED').length;
    this.draftPosts = this.posts.filter(p => p.status === 'DRAFT').length;
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
    const dialogRef = this.dialog.open(PostForm, {
      data: { post: null },
      width: '600px',
      panelClass: 'post-form-modal'
    });

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
    const dialogRef = this.dialog.open(PostForm, {
      data: { post },
      width: '600px',
      panelClass: 'post-form-modal'
    });

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
