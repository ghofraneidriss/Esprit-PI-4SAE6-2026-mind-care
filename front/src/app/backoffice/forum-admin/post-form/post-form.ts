import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ReactiveFormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { Router } from '@angular/router';
import { ForumService, Post, Category } from '../../../core/services/forum.service';

@Component({
  selector: 'app-post-form',
  templateUrl: './post-form.html',
  styleUrls: ['./post-form.css'],
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
  ]
})
export class PostForm implements OnInit {
  postForm: FormGroup;
  categories: Category[] = [];
  isEditMode: boolean = false;
  loading: boolean = false;
  
  // Available tags
  availableTags = [
    { id: 'question', label: 'Question', icon: 'ri-question-mark' },
    { id: 'experience', label: 'Experience', icon: 'ri-user-voice' },
    { id: 'advice', label: 'Advice', icon: 'ri-lightbulb' },
    { id: 'news', label: 'News', icon: 'ri-newspaper' },
    { id: 'support', label: 'Support', icon: 'ri-heart-pulse' },
    { id: 'research', label: 'Research', icon: 'ri-microscope' }
  ];
  
  selectedTags: string[] = [];

  constructor(
    private fb: FormBuilder,
    private forumService: ForumService,
    public dialogRef: MatDialogRef<PostForm>,
    private snackBar: MatSnackBar,
    private router: Router,
    @Inject(MAT_DIALOG_DATA) public data: { post?: Post }
  ) {
    this.postForm = this.fb.group({
      title: ['', Validators.required],
      content: ['', Validators.required],
      categoryId: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    this.loadCategories();

    if (this.data && this.data.post) {
      this.isEditMode = true;
      this.postForm.patchValue({
        title: this.data.post.title,
        content: this.data.post.content,
        categoryId: this.data.post.categoryId
      });
    }
  }

  loadCategories(): void {
    console.log('[Post Form] Loading categories from backend...');
    this.forumService.getAllCategories().subscribe({
      next: (categories) => {
        console.log('[Post Form] Categories received:', categories);
        this.categories = categories;
      },
      error: (err) => {
        console.error('[Post Form] Error loading categories:', err);
      }
    });
  }

  toggleTag(tagId: string): void {
    const index = this.selectedTags.indexOf(tagId);
    if (index === -1) {
      this.selectedTags.push(tagId);
    } else {
      this.selectedTags.splice(index, 1);
    }
  }

  isTagSelected(tagId: string): boolean {
    return this.selectedTags.includes(tagId);
  }

  onSubmit(): void {
    if (this.postForm.valid) {
      this.loading = true;
      const formData = this.postForm.value;
      
      const post = {
        id: this.isEditMode ? this.data.post?.id : undefined,
        title: formData.title,
        content: formData.content,
        categoryId: formData.categoryId,
        tags: this.selectedTags
      };

      const operation = this.isEditMode 
        ? this.forumService.updatePost(post.id!, { 
            title: post.title, 
            content: post.content, 
            userId: 1 // TODO: Get from auth service
          })
        : this.forumService.createPost({ 
            title: post.title, 
            content: post.content, 
            userId: 1 // TODO: Get from auth service
          }, post.categoryId);

      operation.subscribe({
        next: () => {
          this.showSnackBar(this.isEditMode ? 'Post updated successfully!' : 'Post created successfully!', 'success');
          this.loading = false;
          this.dialogRef.close(true);
          // Redirect to ListPost page
          this.router.navigate(['/admin/forum/ListPost']);
        },
        error: (err) => {
          console.error('[Post Form] Error saving post:', err);
          this.showSnackBar('Failed to save post. Please try again.', 'error');
          this.loading = false;
        }
      });
    }
  }

  onCancel(): void {
    this.dialogRef.close();
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
