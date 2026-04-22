import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ForumService, Category } from '../../../core/services/forum.service';
import { CategoryFormComponent } from './category-form/category-form.component';
import { DeleteConfirmationModalComponent } from '../list-post/delete-confirmation-modal.component';
import { NgZoneUiSync } from '../../../core/services/ng-zone-ui-sync.service';

@Component({
  selector: 'app-categories',
  templateUrl: './categories.component.html',
  styleUrls: ['./categories.component.css'],
  standalone: false,
})
export class CategoriesComponent implements OnInit {
  categories: Category[] = [];
  loading = false;
  loadError: string | null = null;

  constructor(
    private forumService: ForumService,
    private dialog: MatDialog,
    private readonly snack: MatSnackBar,
    private readonly cdr: ChangeDetectorRef,
    private readonly zoneUi: NgZoneUiSync
  ) {}

  ngOnInit(): void {
    this.zoneUi.scheduleInitialLoad(() => this.loadCategories());
  }

  loadCategories(): void {
    this.loading = true;
    this.loadError = null;
    this.forumService.getAllCategoriesStrict().subscribe({
      next: (categories) => {
        this.zoneUi.apply(this.cdr, () => {
          this.categories = categories;
          this.loading = false;
        });
      },
      error: (err) => {
        console.error('Error loading categories:', err);
        this.zoneUi.apply(this.cdr, () => {
          this.loading = false;
          this.loadError = 'Could not load categories. Verify forums-service (8082) and try again.';
        });
      },
    });
  }

  private openFormDialog(category: Category | null): void {
    const dialogRef = this.dialog.open(CategoryFormComponent, {
      data: { category },
      width: '560px',
      maxWidth: '95vw',
      panelClass: 'category-form-modal',
    });

    dialogRef.afterClosed().subscribe((result: { success: boolean; category?: Category } | undefined) => {
      if (result?.success) {
        this.snack.open('Category saved. Icon and color apply on the public forum.', 'OK', { duration: 4000 });
        this.loadCategories();
      }
    });
  }

  openCreateCategoryModal(): void {
    this.openFormDialog(null);
  }

  openEditCategoryModal(category: Category): void {
    this.openFormDialog(category);
  }

  deleteCategory(category: Category): void {
    const dialogRef = this.dialog.open(DeleteConfirmationModalComponent, {
      data: {
        postTitle: category.name,
        postId: category.id,
      },
      width: '400px',
      panelClass: 'delete-modal-container',
    });

    dialogRef.afterClosed().subscribe((confirmed: boolean) => {
      if (confirmed) {
        this.forumService.deleteCategory(category.id).subscribe({
          next: () => {
            this.zoneUi.apply(this.cdr, () => {
              this.categories = this.categories.filter((c) => c.id !== category.id);
            });
            this.snack.open('Category deleted.', 'OK', { duration: 3000 });
          },
          error: (err) => {
            console.error('Error deleting category:', err);
            const msg =
              (err as { error?: { message?: string } })?.error?.message ??
              'Could not delete the category. Check that the forum service is running.';
            this.snack.open(msg, 'OK', { duration: 5000 });
          },
        });
      }
    });
  }

  displayIcon(cat: Category): string {
    const i = cat.icon?.trim();
    return i || 'ri-folder-line';
  }

  displayColor(cat: Category): string {
    const c = cat.color?.trim();
    return c && /^#[0-9A-Fa-f]{6}$/.test(c) ? c : '#6366f1';
  }
}
