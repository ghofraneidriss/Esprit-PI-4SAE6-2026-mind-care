import { ChangeDetectorRef, Component, Inject, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { catchError, finalize, switchMap, timeout } from 'rxjs/operators';
import { forkJoin, of } from 'rxjs';
import { ForumService, Post, Category } from '../../../core/services/forum.service';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';
import { FORUM_FALLBACK_CATEGORIES } from '../../../frontoffice/forum/forum-fallback-categories';

/** Target ~900 KB after compression to stay under default MySQL max_allowed_packet. */
const MAX_PHOTO_BYTES_AFTER_COMPRESS = 900_000;

interface PhotoDraftItem {
  previewUrl: string;
  file: File | null;
  processing: boolean;
}

@Component({
  selector: 'app-post-form',
  templateUrl: './post-form.html',
  styleUrls: ['./post-form.css'],
  standalone: false,
})
export class PostForm implements OnInit, OnDestroy {
  postForm: FormGroup;
  categories: Category[] = [];
  categoriesLoading = true;
  categoriesUnavailable = false;
  isEditMode = false;
  loading = false;

  /** Instant blob preview + optional compression step; `file` set when ready for upload. */
  photoItems: PhotoDraftItem[] = [];

  /** Server-stored images when editing (can remove or keep). */
  existingMedia: { id: number; url: string }[] = [];
  /** Existing attachment ids the user removed in this session (deleted on save). */
  private removedExistingMediaIds = new Set<number>();

  private static readonly MAX_PHOTOS = 8;

  availableTags = [
    { id: 'question', label: 'Question', icon: 'ri-question-mark' },
    { id: 'experience', label: 'Experience', icon: 'ri-user-voice' },
    { id: 'advice', label: 'Advice', icon: 'ri-lightbulb' },
    { id: 'news', label: 'News', icon: 'ri-newspaper' },
    { id: 'support', label: 'Support', icon: 'ri-heart-pulse' },
    { id: 'research', label: 'Research', icon: 'ri-microscope' },
  ];

  selectedTags: string[] = [];

  get photosBusy(): boolean {
    return this.photoItems.some((p) => p.processing);
  }

  get totalAttachedPhotos(): number {
    return this.existingMedia.length + this.photoItems.length;
  }

  constructor(
    private fb: FormBuilder,
    private forumService: ForumService,
    private authService: AuthService,
    public dialogRef: MatDialogRef<PostForm>,
    private toast: ToastService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    @Inject(MAT_DIALOG_DATA) public data: { post?: Post; categories?: Category[] }
  ) {
    this.postForm = this.fb.group({
      title: ['', Validators.required],
      content: ['', Validators.required],
      categoryId: [null as number | null, Validators.required],
    });
  }

  get displayName(): string {
    return this.authService.getDisplayName() || this.authService.getFullName() || 'User';
  }

  get userInitials(): string {
    const n = this.displayName.trim();
    if (!n) return '?';
    const parts = n.split(/\s+/).filter(Boolean);
    if (parts.length >= 2) {
      return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
    return n.slice(0, 2).toUpperCase();
  }

  ngOnInit(): void {
    const pre = this.data?.categories;
    if (pre && pre.length > 0) {
      this.forumService.primeCategoriesCache(pre);
      this.categories = [...pre];
      this.categoriesLoading = false;
      this.categoriesUnavailable = false;
      this.syncCategoryControlDisabled();
      this.afterCategoriesReady();
      this.cdr.markForCheck();
      return;
    }
    this.loadCategories();
  }

  ngOnDestroy(): void {
    this.revokeAllPhotoUrls();
  }

  private revokeAllPhotoUrls(): void {
    for (const item of this.photoItems) {
      URL.revokeObjectURL(item.previewUrl);
    }
    this.photoItems = [];
  }

  loadCategories(): void {
    this.categoriesLoading = true;
    this.cdr.markForCheck();
    this.forumService
      .getAllCategories()
      .pipe(
        timeout({ first: 8000 }),
        catchError(() => of<Category[]>([])),
        finalize(() => {
          this.categoriesLoading = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (cats) => {
          if (cats.length > 0) {
            this.categories = cats;
            this.categoriesUnavailable = false;
          } else {
            this.categories = [...FORUM_FALLBACK_CATEGORIES];
            this.categoriesUnavailable = true;
          }
          this.syncCategoryControlDisabled();
          this.afterCategoriesReady();
        },
      });
  }

  private syncCategoryControlDisabled(): void {
    const c = this.postForm.get('categoryId');
    if (!c) {
      return;
    }
    if (this.categoriesUnavailable || this.categories.length === 0) {
      c.disable({ emitEvent: false });
    } else {
      c.enable({ emitEvent: false });
    }
  }

  private afterCategoriesReady(): void {
    if (this.data?.post) {
      this.isEditMode = true;
      this.applyEditPost(this.data.post);
    }
  }

  /**
   * Staff listings use lightweight rows (no LOB body) — {@link Post.content} is often empty.
   * Always reload the full post for edit. Include archived threads so GET /posts/:id is not 404.
   */
  private applyEditPost(p: Post): void {
    const id = p.id;
    if (id == null) {
      this.patchForm(p);
      return;
    }
    const viewer = this.authService.getUserId();
    this.loading = true;
    this.forumService
      .getPostById(id, viewer ?? null, { includeInactive: true })
      .pipe(
        finalize(() => {
          this.loading = false;
          this.cdr.markForCheck();
        })
      )
      .subscribe({
        next: (full) => this.patchForm(full),
        error: () => {
          this.patchForm(p);
          this.toast.show(
            'Could not load the full post text. Check that forums-service (8082) is running.',
            'info'
          );
        },
      });
  }

  private scheduleLoadingFalse(): void {
    setTimeout(() => {
      this.loading = false;
      this.cdr.markForCheck();
    }, 0);
  }

  private patchForm(p: Post): void {
    this.postForm.patchValue({
      title: p.title,
      content: p.content,
      categoryId: p.categoryId,
    });
    if (this.isEditMode && p.id != null) {
      this.loadExistingMedia(p.id);
    }
  }

  private loadExistingMedia(postId: number): void {
    this.existingMedia = [];
    this.removedExistingMediaIds.clear();
    this.forumService.getPostMediaMeta(postId).subscribe({
      next: (list) => {
        this.existingMedia = list.map((m) => ({
          id: m.id,
          url: `${this.forumService.postMediaUrl(postId, m.id)}?_cb=${Date.now()}`,
        }));
        this.cdr.markForCheck();
      },
      error: () => {
        this.existingMedia = [];
        this.cdr.markForCheck();
      },
    });
  }

  async onPhotosSelected(ev: Event): Promise<void> {
    const input = ev.target as HTMLInputElement;
    if (!input.files?.length) {
      return;
    }
    for (let i = 0; i < input.files.length; i++) {
      const raw = input.files[i];
      if (!raw?.type.startsWith('image/')) {
        continue;
      }
      if (this.totalAttachedPhotos >= PostForm.MAX_PHOTOS) {
        this.toast.show(`You can attach up to ${PostForm.MAX_PHOTOS} photos.`, 'error');
        break;
      }

      const instantUrl = URL.createObjectURL(raw);
      const item: PhotoDraftItem = {
        previewUrl: instantUrl,
        file: null,
        processing: true,
      };
      this.photoItems.push(item);
      this.cdr.markForCheck();

      const processed = await this.compressImageForUpload(raw);

      if (processed.size > MAX_PHOTO_BYTES_AFTER_COMPRESS) {
        URL.revokeObjectURL(item.previewUrl);
        this.photoItems.pop();
        this.toast.show(
          'Image is still too large after compression. Try a smaller or different photo.',
          'error'
        );
        this.cdr.markForCheck();
        continue;
      }

      if (processed !== raw) {
        URL.revokeObjectURL(item.previewUrl);
        item.previewUrl = URL.createObjectURL(processed);
      }
      item.file = processed;
      item.processing = false;
      this.cdr.markForCheck();
    }
    input.value = '';
  }

  private compressImageForUpload(file: File): Promise<File> {
    if (!file.type.startsWith('image/')) {
      return Promise.resolve(file);
    }
    if (file.size <= MAX_PHOTO_BYTES_AFTER_COMPRESS) {
      return Promise.resolve(file);
    }
    return new Promise((resolve) => {
      const img = new Image();
      const url = URL.createObjectURL(file);
      img.onload = () => {
        URL.revokeObjectURL(url);
        const maxW = 1600;
        const maxH = 1600;
        let w = img.naturalWidth || img.width;
        let h = img.naturalHeight || img.height;
        if (w <= 0 || h <= 0) {
          resolve(file);
          return;
        }
        if (w > maxW || h > maxH) {
          const r = Math.min(maxW / w, maxH / h);
          w = Math.round(w * r);
          h = Math.round(h * r);
        }
        const canvas = document.createElement('canvas');
        canvas.width = w;
        canvas.height = h;
        const ctx = canvas.getContext('2d');
        if (!ctx) {
          resolve(file);
          return;
        }
        ctx.drawImage(img, 0, 0, w, h);
        canvas.toBlob(
          (blob) => {
            if (!blob) {
              resolve(file);
              return;
            }
            const base = file.name.replace(/\.[^.]+$/, '') || 'photo';
            const out = new File([blob], `${base}.jpg`, { type: 'image/jpeg' });
            resolve(out.size <= file.size ? out : file);
          },
          'image/jpeg',
          0.82
        );
      };
      img.onerror = () => {
        URL.revokeObjectURL(url);
        resolve(file);
      };
      img.src = url;
    });
  }

  removePhoto(index: number): void {
    const item = this.photoItems[index];
    if (item) {
      URL.revokeObjectURL(item.previewUrl);
    }
    this.photoItems.splice(index, 1);
    this.cdr.markForCheck();
  }

  removeExistingPhoto(index: number): void {
    const row = this.existingMedia[index];
    if (!row) {
      return;
    }
    this.removedExistingMediaIds.add(row.id);
    this.existingMedia.splice(index, 1);
    this.cdr.markForCheck();
  }

  toggleTag(tagId: string): void {
    const idx = this.selectedTags.indexOf(tagId);
    if (idx === -1) {
      this.selectedTags.push(tagId);
    } else {
      this.selectedTags.splice(idx, 1);
    }
  }

  isTagSelected(tagId: string): boolean {
    return this.selectedTags.includes(tagId);
  }

  onSubmit(): void {
    if (!this.authService.canManageForumPosts()) {
      this.toast.show('Only staff can publish.', 'error');
      return;
    }
    const userId = this.authService.getUserId();
    if (!userId) {
      this.toast.show('Invalid session.', 'error');
      return;
    }
    if (this.categoriesUnavailable) {
      this.toast.show('Categories are not synced with the server.', 'error');
      return;
    }
    if (this.photosBusy) {
      this.toast.show('Please wait until photos finish processing.', 'error');
      return;
    }
    this.postForm.get('categoryId')?.markAsTouched();
    this.postForm.get('title')?.markAsTouched();
    this.postForm.get('content')?.markAsTouched();
    if (!this.postForm.valid) {
      return;
    }

    this.loading = true;
    const formData = this.postForm.value;
    const categoryId = formData.categoryId as number;

    if (this.isEditMode) {
      const postId = this.data.post!.id;
      const uploadFiles = this.photoItems.map((p) => p.file).filter((f): f is File => f != null);
      this.forumService
        .updatePost(postId, {
          title: formData.title,
          content: formData.content,
          userId,
          category: { id: categoryId },
          status: 'PUBLISHED',
        })
        .pipe(
          switchMap(() => {
            const ids = [...this.removedExistingMediaIds];
            if (ids.length === 0) {
              return of(null);
            }
            return forkJoin(ids.map((mid) => this.forumService.deletePostMedia(postId, mid)));
          }),
          switchMap(() => {
            if (uploadFiles.length === 0) {
              return of(null);
            }
            return this.forumService.appendPostMedia(postId, uploadFiles);
          })
        )
        .subscribe({
          next: () => {
            this.toast.show('Post updated.', 'success');
            this.scheduleLoadingFalse();
            this.dialogRef.close(true);
            this.router.navigate(['/admin/forum/posts']);
          },
          error: (err) => {
            this.toast.showHttpError(err, 'Could not save changes.');
            this.scheduleLoadingFalse();
          },
        });
      return;
    }

    const uploadFiles = this.photoItems.map((p) => p.file).filter((f): f is File => f != null);
    const hasPhotos = uploadFiles.length > 0;
    const operation = hasPhotos
      ? this.forumService.createPostWithPhotos(
          categoryId,
          {
            title: formData.title,
            content: formData.content,
            userId,
            status: 'PUBLISHED',
          },
          uploadFiles
        )
      : this.forumService.createPost(
          {
            title: formData.title,
            content: formData.content,
            userId,
            status: 'PUBLISHED',
          },
          categoryId
        );

    operation.subscribe({
      next: () => {
        this.toast.show('Post published.', 'success');
        this.scheduleLoadingFalse();
        this.dialogRef.close(true);
        this.router.navigate(['/admin/forum/posts']);
      },
      error: (err) => {
        console.error('[Post Form]', err);
        this.toast.showHttpError(
          err,
          'Could not publish (check photo size or forums service).'
        );
        this.scheduleLoadingFalse();
      },
    });
  }

  onCancel(): void {
    this.dialogRef.close();
  }
}
