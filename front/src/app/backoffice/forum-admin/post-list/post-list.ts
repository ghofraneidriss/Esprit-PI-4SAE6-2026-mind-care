import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ForumService, Post } from '../../../core/services/forum.service';
import { PostForm } from '../post-form/post-form';
import { DeletePostModal } from '../delete-post-modal/delete-post-modal';

@Component({
  selector: 'app-post-list',
  templateUrl: './post-list.html',
  styleUrls: ['./post-list.css'],
  standalone: true,
  imports: [CommonModule, FormsModule]
})
export class PostList implements OnInit {
  posts: Post[] = [];
  loading = true;
  searchTerm = '';
  selectedCategory: number | null = null;

  constructor(
    private forumService: ForumService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) { }

  ngOnInit(): void {
    this.loadPosts();
  }

  loadPosts(): void {
    this.loading = true;
    console.log('[Post List] Loading posts from backend...');
    this.forumService.getAllPosts().subscribe({
      next: (posts) => {
        console.log('[Post List] Posts received:', posts);
        this.posts = posts;
        this.loading = false;
      },
      error: (err) => {
        console.error('[Post List] Error loading posts:', err);
        this.loading = false;
        this.showSnackBar('Failed to load posts. Please try again.', 'error');
      }
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
    return this.posts.reduce((acc, p) => acc + (p.commentCount || 0), 0);
  }

  get totalCategoriesCount(): number {
    return new Set(this.posts.map(p => p.categoryId)).size || 6;
  }

  openPostForm(post?: Post): void {
    const dialogRef = this.dialog.open(PostForm, {
      width: '650px',
      data: { post },
      panelClass: 'forum-dialog'
    });

    dialogRef.afterClosed().subscribe((result: any) => {
      if (result) {
        console.log('[Post List] Post form closed with result:', result);
        this.showSnackBar(post ? 'Post updated successfully!' : 'Post created successfully!', 'success');
        setTimeout(() => { this.loadPosts(); }, 0);
      }
    });
  }

  deletePost(post: Post): void {
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
            setTimeout(() => { this.loadPosts(); }, 0);
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
    return date.toLocaleDateString('fr-FR', { 
      day: '2-digit', 
      month: 'short', 
      year: 'numeric' 
    });
  }

  getCategoryColor(categoryId: number): string {
    const colors: { [key: number]: string } = {
      1: 'bg-rose-100 text-rose-700',
      2: 'bg-amber-100 text-amber-700',
      3: 'bg-emerald-100 text-emerald-700',
      4: 'bg-violet-100 text-violet-700',
      5: 'bg-blue-100 text-blue-700',
      6: 'bg-orange-100 text-orange-700'
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
