import { Component, Inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ForumService, Category } from '../../../../core/services/forum.service';

export interface CategoryFormData {
  category: Category | null;
}

/** Same palette as the public forum cards — pick icon + color for a modern look. */
const PRESETS: { icon: string; color: string; label: string }[] = [
  { icon: 'ri-heart-pulse-line', color: '#0891b2', label: 'Care' },
  { icon: 'ri-hand-heart-line', color: '#f43f5e', label: 'Support' },
  { icon: 'ri-microscope-line', color: '#6366f1', label: 'Science' },
  { icon: 'ri-home-heart-line', color: '#059669', label: 'Home' },
  { icon: 'ri-scales-3-line', color: '#d97706', label: 'Legal' },
  { icon: 'ri-cup-line', color: '#db2777', label: 'Community' },
  { icon: 'ri-chat-3-line', color: '#8b5cf6', label: 'Discuss' },
  { icon: 'ri-book-read-line', color: '#0ea5e9', label: 'Read' },
];

@Component({
  selector: 'app-category-form',
  templateUrl: './category-form.component.html',
  styleUrls: ['./category-form.component.css'],
  standalone: false,
})
export class CategoryFormComponent {
  readonly presets = PRESETS;
  categoryForm: FormGroup;
  isEditMode: boolean = false;

  constructor(
    private fb: FormBuilder,
    private forumService: ForumService,
    private readonly snack: MatSnackBar,
    public dialogRef: MatDialogRef<CategoryFormComponent>,
    @Inject(MAT_DIALOG_DATA) public data: CategoryFormData
  ) {
    this.categoryForm = this.fb.group({
      name: ['', Validators.required],
      description: [''],
      icon: ['ri-folder-line'],
      color: ['#6366f1'],
    });

    this.isEditMode = !!data.category;

    if (this.isEditMode && data.category) {
      this.categoryForm.patchValue({
        name: data.category.name,
        description: data.category.description || '',
        icon: data.category.icon || 'ri-folder-line',
        color: data.category.color || '#6366f1',
      });
    }
  }

  applyPreset(p: { icon: string; color: string }): void {
    this.categoryForm.patchValue({ icon: p.icon, color: p.color });
  }

  onSave(): void {
    if (this.categoryForm.valid) {
      const v = this.categoryForm.value;
      const categoryData = {
        name: v.name,
        description: v.description,
        icon: (v.icon as string)?.trim() || 'ri-folder-line',
        color: (v.color as string)?.trim() || '#6366f1',
      };

      if (this.isEditMode && this.data.category) {
        this.forumService.updateCategory(this.data.category.id, categoryData).subscribe({
          next: (updatedCategory) => {
            this.dialogRef.close({ success: true, category: updatedCategory });
          },
          error: (err) => {
            console.error('Error updating category:', err);
            const msg =
              (err as { error?: { message?: string } })?.error?.message ?? 'Could not update the category.';
            this.snack.open(msg, 'OK', { duration: 5000 });
          },
        });
      } else {
        this.forumService.createCategory(categoryData).subscribe({
          next: (newCategory) => {
            this.dialogRef.close({ success: true, category: newCategory });
          },
          error: (err) => {
            console.error('Error creating category:', err);
            const msg =
              (err as { error?: { message?: string } })?.error?.message ?? 'Could not create the category.';
            this.snack.open(msg, 'OK', { duration: 5000 });
          },
        });
      }
    }
  }

  onCancel(): void {
    this.dialogRef.close({ success: false });
  }
}
