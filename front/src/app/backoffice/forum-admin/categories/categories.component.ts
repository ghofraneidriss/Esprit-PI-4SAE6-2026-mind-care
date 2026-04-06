import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { ForumService, Category } from '../../../core/services/forum.service';
import { CategoryFormComponent } from './category-form/category-form.component';
import { DeleteConfirmationModalComponent } from '../list-post/delete-confirmation-modal.component';

@Component({
  selector: 'app-categories',
  templateUrl: './categories.component.html',
  styleUrls: ['./categories.component.css'],
  standalone: false
})
export class CategoriesComponent implements OnInit {
  categories: Category[] = [];
  loading: boolean = false;

  constructor(
    private forumService: ForumService,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.loadCategories();
  }

  loadCategories(): void {
    this.loading = true;
    this.forumService.getAllCategories().subscribe({
      next: (categories) => {
        this.categories = categories;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading categories:', err);
        this.loading = false;
      }
    });
  }

  openCreateCategoryModal(): void {
    const dialogRef = this.dialog.open(CategoryFormComponent, {
      data: { category: null },
      width: '500px',
      panelClass: 'category-form-modal'
    });

    dialogRef.afterClosed().subscribe((result: { success: boolean; category?: Category }) => {
      if (result && result.success && result.category) {
        this.categories.push(result.category);
      }
      // Refresh categories list after creation
      this.loadCategories();
    });
  }

  openEditCategoryModal(category: Category): void {
    const dialogRef = this.dialog.open(CategoryFormComponent, {
      data: { category },
      width: '500px',
      panelClass: 'category-form-modal'
    });

    dialogRef.afterClosed().subscribe((result: { success: boolean; category?: Category }) => {
      if (result && result.success && result.category) {
        const index = this.categories.findIndex(c => c.id === result.category!.id);
        if (index !== -1) {
          this.categories[index] = result.category;
        }
      }
    });
  }

  deleteCategory(category: Category): void {
    const dialogRef = this.dialog.open(DeleteConfirmationModalComponent, {
      data: {
        postTitle: category.name,
        postId: category.id
      },
      width: '400px',
      panelClass: 'delete-modal-container'
    });

    dialogRef.afterClosed().subscribe((confirmed: boolean) => {
      if (confirmed) {
        this.forumService.deleteCategory(category.id).subscribe({
          next: () => {
            this.categories = this.categories.filter(c => c.id !== category.id);
          },
          error: (err) => {
            console.error('Error deleting category:', err);
          }
        });
      }
    });
  }

  getCategoryIcon(categoryName: string): string {
    const icons: { [key: string]: string } = {
      'Support': 'ri-heart-pulse-line',
      'Research': 'ri-microscope-line',
      'Discussion': 'ri-discuss-line',
      'News': 'ri-newspaper-line',
      'General': 'ri-folder-line'
    };
    return icons[categoryName] || 'ri-folder-line';
  }

  getCategoryColor(categoryName: string): string {
    const colors: { [key: string]: string } = {
      'Support': '#3B82F6',
      'Research': '#10B981',
      'Discussion': '#8B5CF6',
      'News': '#F59E0B',
      'General': '#6B7280'
    };
    return colors[categoryName] || '#6B7280';
  }
}
