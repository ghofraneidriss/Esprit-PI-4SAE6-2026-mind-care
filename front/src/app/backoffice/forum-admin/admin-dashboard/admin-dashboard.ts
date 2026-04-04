import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialog } from '@angular/material/dialog';
import { ForumService, Post } from '../../../core/services/forum.service';
import { DeletePostModal } from '../delete-post-modal/delete-post-modal';
import { ViewPostModal } from '../view-post-modal/view-post-modal';
import { PostForm } from '../post-form/post-form';

@Component({
  selector: 'app-admin-dashboard',
  templateUrl: './admin-dashboard.html',
  styleUrls: ['./admin-dashboard.css'],
  standalone: true,
  imports: [CommonModule]
})
export class AdminDashboard implements OnInit {
  stats = [
    { label: 'Total Posts', value: '1,240', change: '+12%', icon: 'ri-file-text-line', color: 'rose' },
    { label: 'Active Users', value: '890', change: '+5%', icon: 'ri-group-line', color: 'amber' },
    { label: 'Categories', value: '6', change: '0%', icon: 'ri-folder-line', color: 'emerald' },
    { label: 'Comments Today', value: '142', change: '+28%', icon: 'ri-chat-3-line', color: 'violet' }
  ];

  recentPosts: Post[] = [];
  loading = false;
  error: string | null = null;

  constructor(
    private forumService: ForumService,
    private dialog: MatDialog
  ) { }

  ngOnInit(): void {
    this.loadPosts();
    this.loadStats();
  }

  loadPosts(): void {
    if (this.recentPosts.length === 0) {
      this.loading = true;
    }
    this.error = null;

    // Safety timeout
    const timer = setTimeout(() => {
      if (this.loading) {
        this.loading = false;
        this.error = "Connection is slow. Showing available data.";
      }
    }, 10000);

    console.log('[Admin Dashboard] Fetching latest posts...');
    this.forumService.getAllPosts().subscribe({
      next: (posts) => {
        clearTimeout(timer);
        this.recentPosts = posts.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()).slice(0, 10);
        this.loading = false;
        this.error = null;
      },
      error: (err) => {
        clearTimeout(timer);
        console.error('[Admin Dashboard] Fetch error:', err);
        this.error = "Forum Service unreachable.";
        this.loading = false;
      }
    });
  }

  loadStats(): void {
    console.log('[Admin Dashboard] Loading stats...');
    this.forumService.getAllCategories().subscribe({
      next: (categories) => {
        this.stats[2].value = categories.length.toString();
      }
    });
    this.forumService.getAllPosts().subscribe({
      next: (posts) => {
        this.stats[0].value = posts.length.toString();
        this.stats[3].value = posts.reduce((acc, p) => acc + (p.commentCount || 0), 0).toString();
      }
    });
  }

  openPostForm(post?: Post): void {
    const dialogRef = this.dialog.open(PostForm, {
      width: '600px',
      data: { post }
    });

    dialogRef.afterClosed().subscribe((result: any) => {
      if (result) {
        console.log('[Admin Dashboard] Post form closed with result:', result);
        this.loadPosts(); // Reload posts from backend to show updates
      }
    });
  }

  viewPost(post: Post): void {
    const dialogRef = this.dialog.open(ViewPostModal, {
      width: '700px',
      data: { post }
    });

    dialogRef.afterClosed().subscribe((result: any) => {
      if (result === 'edit') {
        this.openPostForm(post);
      } else if (result === 'delete') {
        this.deletePost(post);
      }
    });
  }

  deletePost(post: Post): void {
    console.log('[Admin Dashboard] Opening delete modal for post:', post.id);
    const dialogRef = this.dialog.open(DeletePostModal, {
      width: '450px',
      data: { post }
    });

    dialogRef.afterClosed().subscribe((result: any) => {
      if (result === true) {
        console.log('[Admin Dashboard] Deleting post:', post.id);
        this.forumService.deletePost(post.id).subscribe({
          next: () => {
            console.log('[Admin Dashboard] Post deleted successfully');
            this.loadPosts(); // Reload posts
          },
          error: (err) => {
            console.error('[Admin Dashboard] Error deleting post:', err);
          }
        });
      }
    });
  }
}
