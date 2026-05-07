import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, of } from 'rxjs';
import { ForumService, Category } from '../../../core/services/forum.service';
import { FORUM_FALLBACK_CATEGORIES } from '../forum-fallback-categories';
import { NgZoneUiSync } from '../../../core/services/ng-zone-ui-sync.service';

@Component({
  selector: 'app-category-list',
  templateUrl: './category-list.component.html',
  styleUrls: ['../forum-shared.css', './category-list.component.css'],
  standalone: false,
})
export class CategoryListComponent implements OnInit {
  categories: Category[] = [];
  loading = false;
  usingDemo = false;

  constructor(
    private forumService: ForumService,
    private router: Router,
    private readonly cdr: ChangeDetectorRef,
    private readonly zoneUi: NgZoneUiSync
  ) {}

  ngOnInit(): void {
    this.zoneUi.scheduleInitialLoad(() => this.loadCategories());
  }

  loadCategories(): void {
    this.loading = true;
    this.forumService
      .getAllCategories()
      .pipe(catchError(() => of<Category[]>([])))
      .subscribe({
        next: (categories) => {
          this.zoneUi.apply(this.cdr, () => {
            if (categories.length > 0) {
              this.categories = categories.slice(0, 6);
              this.usingDemo = false;
            } else {
              this.categories = FORUM_FALLBACK_CATEGORIES;
              this.usingDemo = true;
            }
            this.loading = false;
          });
        },
      });
  }

  navigateToCategory(category: Category): void {
    this.router.navigate(['/forum/category', category.id]);
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
      'ri-mental-health-line',
      'ri-capsule-line',
    ];
    return icons[i % icons.length];
  }

  trackById(_i: number, cat: Category): number | string {
    return cat.id ?? cat.name;
  }

  getCategoryGradient(i: number): string {
    const gradients = [
      'linear-gradient(135deg, #099aa7 0%, #0e7490 100%)',
      'linear-gradient(135deg, #f43f5e 0%, #ec4899 100%)',
      'linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%)',
      'linear-gradient(135deg, #059669 0%, #0d9488 100%)',
      'linear-gradient(135deg, #0891b2 0%, #2563eb 100%)',
      'linear-gradient(135deg, #db2777 0%, #c026d3 100%)',
      'linear-gradient(135deg, #0d9488 0%, #099aa7 100%)',
      'linear-gradient(135deg, #7c3aed 0%, #6366f1 100%)',
    ];
    return gradients[i % gradients.length];
  }

  getCategoryCardStyle(cat: Category): string {
    const c = cat.color?.trim();
    if (c && /^#[0-9A-Fa-f]{6}$/.test(c)) {
      return `linear-gradient(135deg, ${c} 0%, ${c}cc 55%, ${c}99 100%)`;
    }
    return this.getCategoryGradient(this.categoryVisualIndex(cat));
  }
}
