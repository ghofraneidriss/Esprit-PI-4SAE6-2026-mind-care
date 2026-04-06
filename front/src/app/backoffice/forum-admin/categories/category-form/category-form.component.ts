import { Component, Inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { ForumService, Category } from '../../../../core/services/forum.service';

export interface CategoryFormData {
  category: Category | null;
}

@Component({
  selector: 'app-category-form',
  templateUrl: './category-form.component.html',
  styleUrls: ['./category-form.component.css'],
  standalone: false
})
export class CategoryFormComponent {
  categoryForm: FormGroup;
  isEditMode: boolean = false;

  constructor(
    private fb: FormBuilder,
    private forumService: ForumService,
    public dialogRef: MatDialogRef<CategoryFormComponent>,
    @Inject(MAT_DIALOG_DATA) public data: CategoryFormData
  ) {
    this.categoryForm = this.fb.group({
      name: ['', Validators.required],
      description: ['']
    });
    
    this.isEditMode = !!data.category;
    
    if (this.isEditMode && data.category) {
      this.categoryForm.patchValue({
        name: data.category.name,
        description: data.category.description || ''
      });
    }
  }

  onSave(): void {
    if (this.categoryForm.valid) {
      const categoryData = {
        name: this.categoryForm.value.name,
        description: this.categoryForm.value.description
      };

      if (this.isEditMode && this.data.category) {
        // Update existing category
        this.forumService.updateCategory(this.data.category.id, categoryData).subscribe({
          next: (updatedCategory) => {
            this.dialogRef.close({ success: true, category: updatedCategory });
          },
          error: (err) => {
            console.error('Error updating category:', err);
          }
        });
      } else {
        // Create new category
        this.forumService.createCategory(categoryData).subscribe({
          next: (newCategory) => {
            this.dialogRef.close({ success: true, category: newCategory });
          },
          error: (err) => {
            console.error('Error creating category:', err);
          }
        });
      }
    }
  }

  onCancel(): void {
    this.dialogRef.close({ success: false });
  }
}
