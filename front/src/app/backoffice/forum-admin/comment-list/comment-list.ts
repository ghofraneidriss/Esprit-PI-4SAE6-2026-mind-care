import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ForumService, Comment } from '../../../core/services/forum.service';

@Component({
    selector: 'app-comment-list',
    templateUrl: './comment-list.html',
    styleUrls: ['./comment-list.css'],
    standalone: true,
    imports: [CommonModule]
})
export class CommentList implements OnInit {
    comments: Comment[] = [];
    loading: boolean = false;
    error: string | null = null;

    constructor(private forumService: ForumService) { }

    ngOnInit(): void {
        this.loadComments();
    }

    loadComments(): void {
        this.loading = true;
        this.error = null;

        console.log('[CommentList] Requesting all comments from Forum Service...');
        this.forumService.getAllComments().subscribe({
            next: (comments: Comment[]) => {
                console.log('[CommentList] Success:', comments.length, 'comments found.');
                this.comments = comments;
                this.loading = false;
            },
            error: (err: any) => {
                console.error('[CommentList] Fatal Error:', err);
                this.error = `Connection failed. Make sure the Forum Service (Port 8085) is running. (${err.message})`;
                this.loading = false;
            }
        });
    }

    deleteComment(id: number): void {
        if (!confirm('Are you sure you want to delete this comment? This action cannot be undone.')) return;

        this.forumService.deleteComment(id).subscribe({
            next: () => {
                this.comments = this.comments.filter(c => c.id !== id);
            },
            error: (err: any) => {
                console.error('Error deleting comment:', err);
            }
        });
    }

    formatDate(date: string): string {
        if (!date) return '';
        const d = new Date(date);
        return d.toLocaleDateString() + ' ' + d.toLocaleTimeString();
    }
}
