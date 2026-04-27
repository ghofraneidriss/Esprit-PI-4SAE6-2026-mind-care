import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ForumService, Post, Category } from '../../../core/services/forum.service';
import { AuthService } from '../../../core/services/auth.service';
import { NgZoneUiSync } from '../../../core/services/ng-zone-ui-sync.service';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Component({
  selector: 'app-posts-by-category',
  templateUrl: './posts-by-category.component.html',
  styleUrls: ['../forum-shared.css', './posts-by-category.component.css'],
  standalone: false
})
export class PostsByCategoryComponent implements OnInit {
  posts: Post[] = [];
  category: Category | null = null;
  categoryId: number | null = null;
  loading: boolean = false;

  constructor(
    private forumService: ForumService,
    private authService: AuthService,
    private route: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private readonly zoneUi: NgZoneUiSync
  ) { }

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      console.log('PostsByCategoryComponent - Route ID:', id);
      if (id) {
        this.categoryId = parseInt(id);
        if (!isNaN(this.categoryId)) {
          this.loadCategoryAndPosts();
        } else {
          console.error('Invalid category ID:', id);
          this.router.navigate(['/forum']);
        }
      }
    });
  }

  loadCategoryAndPosts(): void {
    if (!this.categoryId) return;

    this.loading = true;

    console.log('Loading data for category ID:', this.categoryId);

    // catchError on each so a failing category lookup never blocks posts
    const category$ = this.forumService.getCategoryById(this.categoryId).pipe(
      catchError(() => of(null))
    );
    const uid = this.authService.getUserId();
    const posts$ = this.forumService.getPostsByCategory(this.categoryId, uid && uid > 0 ? uid : null).pipe(
      catchError(() => of([] as Post[]))
    );

    forkJoin({
      category: category$,
      posts: posts$
    }).subscribe({
      next: (result) => {
        console.log('Results loaded:', result);
        this.zoneUi.apply(this.cdr, () => {
          this.category = result.category;
          this.posts = result.posts;
          this.loading = false;
        });
      },
      error: (err) => {
        console.error('Error in forkJoin loading category data:', err);
        this.zoneUi.apply(this.cdr, () => {
          this.loading = false;
        });
      }
    });
  }

  navigateToPost(post: Post): void {
    this.router.navigate(['/forum/post', post.id]);
  }

  navigateBack(): void {
    console.log('Navigating back to forum home');
    this.router.navigate(['/forum']);
  }

  formatDate(dateString: string): string {
    if (!dateString) return 'Unkown Date';
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      });
    } catch (e) {
      return dateString;
    }
  }

  getCategoryGradient(categoryName: string): string {
    const gradients: { [key: string]: string } = {
      Support: 'linear-gradient(135deg, #099aa7 0%, #0e7490 100%)',
      Research: 'linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%)',
      Discussion: 'linear-gradient(135deg, #f43f5e 0%, #ec4899 100%)',
      News: 'linear-gradient(135deg, #0891b2 0%, #2563eb 100%)',
      General: 'linear-gradient(135deg, #059669 0%, #0d9488 100%)',
    };
    return (
      gradients[categoryName] ?? 'linear-gradient(135deg, #099aa7 0%, #6366f1 100%)'
    );
  }

  /** Cover thumbnail for list cards — same endpoint as post detail. */
  postCoverSrc(post: Post): string {
    return this.forumService.postCoverImageUrl(post.id);
  }

  onCoverError(event: Event): void {
    const img = event.target as HTMLImageElement | null;
    if (!img) {
      return;
    }
    const wrap = img.closest('.post-card__media');
    if (wrap instanceof HTMLElement) {
      wrap.style.display = 'none';
    }
  }
}
