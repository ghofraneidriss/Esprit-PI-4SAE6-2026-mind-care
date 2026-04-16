import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';

import { CommonModule } from '@angular/common';

import { FormsModule } from '@angular/forms';

import { HttpClient } from '@angular/common/http';

import { forkJoin, of } from 'rxjs';

import { catchError } from 'rxjs/operators';

import { activitiesApiBase, resolveActivitiesMediaUrl } from '../../../environments/environment';

import { ToastService } from '../../core/services/toast.service';

import { ConfirmDialogService } from '../../core/services/confirm-dialog.service';

import { PhotoService } from '../../core/services/photo.service';

import { compressImageForUpload } from '../../core/utils/image-upload.util';

import {

  ADMIN_TABLE_PAGE_SIZE,

  padPageRows,

  slicePage,

  totalPageCount

} from '../../core/utils/admin-table-paging';



@Component({

  selector: 'app-off-photo-mgmt',

  standalone: true,

  imports: [CommonModule, FormsModule],

  templateUrl: './photo-management.component.html',

  styleUrls: ['../quiz-management/quiz-management.component.css', '../mgmt-shared.css', './photo-management.component.css']

})

export class PhotoManagementComponent implements OnInit, OnDestroy {

  photos: any[] = [];

  loading = true;

  showForm = false;

  editing = false;

  private readonly api = `${activitiesApiBase()}/photo-activities`;

  readonly resolveActivitiesMediaUrl = resolveActivitiesMediaUrl;

  filterSearch = '';

  filterDifficulty: '' | 'EASY' | 'MEDIUM' | 'HARD' = '';



  pageIndex = 0;

  readonly pageSize = ADMIN_TABLE_PAGE_SIZE;



  selectedIds: number[] = [];



  form: any = this.emptyForm();

  /** Fichier choisi pour upload (stockage LONGBLOB côté serveur). */

  pendingFile: File | null = null;

  private previewObjectUrl: string | null = null;



  constructor(

    private http: HttpClient,

    private photoSvc: PhotoService,

    private cdr: ChangeDetectorRef,

    private toast: ToastService,

    private confirm: ConfirmDialogService

  ) {}



  ngOnInit() {

    this.load();

  }



  ngOnDestroy(): void {

    this.revokePreview();

  }



  emptyForm() {

    return {

      id: null,

      title: '',

      description: '',

      difficulty: 'EASY',

      imageUrl: '',

      correctOptionIndex: 0,

      options: ['', '', '', '']

    };

  }



  private revokePreview(): void {

    if (this.previewObjectUrl) {

      URL.revokeObjectURL(this.previewObjectUrl);

      this.previewObjectUrl = null;

    }

  }



  clearPendingFile(): void {

    this.pendingFile = null;

    this.revokePreview();

    this.cdr.detectChanges();

  }



  onPickFile(ev: Event): void {

    const input = ev.target as HTMLInputElement;

    const f = input.files?.[0];

    if (f && f.type.startsWith('image/')) {

      this.setPendingFile(f);

    }

    input.value = '';

  }



  onDragOver(ev: DragEvent): void {

    ev.preventDefault();

    ev.stopPropagation();

  }



  onDrop(ev: DragEvent): void {

    ev.preventDefault();

    ev.stopPropagation();

    const f = ev.dataTransfer?.files?.[0];

    if (f && f.type.startsWith('image/')) {

      this.setPendingFile(f);

    }

  }



  private setPendingFile(f: File): void {

    this.revokePreview();

    this.pendingFile = f;

    this.previewObjectUrl = URL.createObjectURL(f);

    this.cdr.detectChanges();

  }



  get formPreviewSrc(): string {

    if (this.previewObjectUrl) return this.previewObjectUrl;

    const u = this.form?.imageUrl;

    return typeof u === 'string' ? resolveActivitiesMediaUrl(u) : '';

  }



  private optionsJson(): string {

    const opts = (this.form.options || []).map((o: string) => (o || '').trim());

    const filled = opts.filter((o: string) => o.length > 0);

    return JSON.stringify(filled.length >= 2 ? filled : ['', '', '', '']);

  }



  private correctAnswerFromForm(): string {

    const opts = (this.form.options || []).map((o: string) => (o || '').trim());

    const idx = Number(this.form.correctOptionIndex) || 0;

    const pick = opts[idx] || opts.find((o: string) => o.length > 0) || '';

    return pick;

  }



  private buildJsonPayload(): Record<string, unknown> {

    return {

      title: this.form.title,

      description: this.form.description || '',

      difficulty: this.form.difficulty,

      correctAnswer: this.correctAnswerFromForm(),

      options: (this.form.options || []).map((o: string) => (o || '').trim())

    };

  }



  private syncCorrectIndexFromAnswer(): void {

    const ans = ((this.form.correctAnswer as string) || '').trim();

    const opts = (this.form.options || []).map((o: string) => (o || '').trim());

    const idx = opts.findIndex((o: string) => o === ans);

    this.form.correctOptionIndex = idx >= 0 ? idx : 0;

  }



  get filteredPhotos(): any[] {

    let list = [...this.photos];

    const q = (this.filterSearch || '').trim().toLowerCase();

    if (q) {

      list = list.filter(

        (p) =>

          (p.title || '').toLowerCase().includes(q) ||

          (p.description || '').toLowerCase().includes(q) ||

          (p.correctAnswer || '').toLowerCase().includes(q)

      );

    }

    if (this.filterDifficulty) {

      list = list.filter((p) => String(p.difficulty || '').toUpperCase() === this.filterDifficulty);

    }

    return list;

  }



  get pagedFilteredPhotos(): any[] {

    return slicePage(this.filteredPhotos, this.pageIndex, this.pageSize);

  }



  get photoTableRows(): (any | null)[] {

    if (this.loading || !this.filteredPhotos.length) return [];

    return padPageRows(this.pagedFilteredPhotos, this.pageSize);

  }



  get photoTotalPages(): number {

    return totalPageCount(this.filteredPhotos.length, this.pageSize);

  }



  get photoRangeLabel(): string {

    const n = this.filteredPhotos.length;

    if (!n) return '';

    const start = this.pageIndex * this.pageSize + 1;

    const end = Math.min(n, (this.pageIndex + 1) * this.pageSize);

    return `${start}–${end} of ${n}`;

  }



  photoFiltersChanged() {

    this.pageIndex = 0;

  }



  photoPrevPage() {

    if (this.pageIndex > 0) this.pageIndex--;

  }



  photoNextPage() {

    if (this.pageIndex < this.photoTotalPages - 1) this.pageIndex++;

  }



  get allFilteredSelected(): boolean {

    const f = this.pagedFilteredPhotos;

    return f.length > 0 && f.every((p) => this.selectedIds.includes(p.id));

  }



  isSelected(id: number): boolean {

    return this.selectedIds.includes(id);

  }



  toggleRow(id: number, checked: boolean) {

    if (checked) {

      if (!this.selectedIds.includes(id)) this.selectedIds = [...this.selectedIds, id];

    } else {

      this.selectedIds = this.selectedIds.filter((x) => x !== id);

    }

  }



  toggleSelectAll(checked: boolean) {

    const ids = this.pagedFilteredPhotos.map((p) => p.id);

    if (checked) {

      const set = new Set([...this.selectedIds, ...ids]);

      this.selectedIds = [...set];

    } else {

      const idSet = new Set(ids);

      this.selectedIds = this.selectedIds.filter((id) => !idSet.has(id));

    }

  }



  resetFilters() {

    this.filterSearch = '';

    this.filterDifficulty = '';

    this.selectedIds = [];

    this.pageIndex = 0;

  }



  load() {

    this.loading = true;

    this.http.get<any[]>(this.api).subscribe({

      next: (d) => {

        this.photos = d || [];

        this.loading = false;

        this.cdr.detectChanges();

      },

      error: (e) => {

        this.loading = false;

        this.toast.showHttpError(e, 'Could not load photo activities. Please refresh the page.');

        this.cdr.detectChanges();

      }

    });

  }



  openCreate() {

    this.clearPendingFile();

    this.form = this.emptyForm();

    this.editing = false;

    this.showForm = true;

  }



  openEdit(p: any) {

    this.clearPendingFile();

    this.form = JSON.parse(JSON.stringify(p));

    if (!this.form.options?.length) this.form.options = ['', '', '', ''];

    while (this.form.options.length < 4) this.form.options.push('');

    this.form.correctOptionIndex = 0;

    this.syncCorrectIndexFromAnswer();

    this.editing = true;

    this.showForm = true;

  }



  private hasExistingImage(): boolean {

    const u = (this.form?.imageUrl || '').trim();

    return u.length > 0;

  }



  async save() {

    const title = (this.form.title || '').trim();

    const description = (this.form.description || '').trim();

    if (!title || !description) {

      this.toast.show('Activity title and question text are required', 'error');

      return;

    }



    const opts = (this.form.options || []).map((o: string) => (o || '').trim());

    const filledOpts = opts.filter((o: string) => o.length > 0);

    if (filledOpts.length < 2) {

      this.toast.show('Enter at least two answer options (like a quiz)', 'error');

      return;

    }



    const correctAnswer = this.correctAnswerFromForm();

    if (!correctAnswer) {

      this.toast.show('Select the correct answer and fill the matching option text', 'error');

      return;

    }



    if (!this.editing) {

      if (!this.pendingFile) {

        this.toast.show('Add an image file (drag & drop or browse on this device)', 'error');

        return;

      }

      await this.submitCreate(title, description, correctAnswer);

      return;

    }



    if (this.pendingFile) {

      await this.submitEditWithFile(title, description, correctAnswer);

      return;

    }



    if (!this.hasExistingImage()) {

      this.toast.show('Add an image file — this activity has no stored image yet', 'error');

      return;

    }



    this.http.put(`${this.api}/${this.form.id}`, this.buildJsonPayload()).subscribe({

      next: () => this.onSaveOk(),

      error: (e) => this.onSaveErr(e)

    });

  }



  private async submitCreate(title: string, description: string, correctAnswer: string): Promise<void> {

    if (!this.pendingFile) return;

    let file = this.pendingFile;

    try {

      file = await compressImageForUpload(this.pendingFile);

    } catch (e: unknown) {

      this.toast.showHttpError(e, 'Could not process this image. Please try another JPEG or PNG file.');

      return;

    }

    const fd = new FormData();

    fd.append('file', file);

    fd.append('title', title);

    fd.append('description', description);

    fd.append('difficulty', this.form.difficulty || 'EASY');

    fd.append('correctAnswer', correctAnswer);

    fd.append('optionsJson', this.optionsJson());

    this.photoSvc.createWithImage(fd).subscribe({

      next: () => this.onSaveOk(),

      error: (e) => this.onSaveErr(e)

    });

  }



  private async submitEditWithFile(title: string, description: string, correctAnswer: string): Promise<void> {

    if (!this.pendingFile) return;

    let file = this.pendingFile;

    try {

      file = await compressImageForUpload(this.pendingFile);

    } catch (e: unknown) {

      this.toast.showHttpError(e, 'Could not process this image. Please try another JPEG or PNG file.');

      return;

    }

    const fd = new FormData();

    fd.append('file', file);

    fd.append('title', title);

    fd.append('description', description);

    fd.append('difficulty', this.form.difficulty || 'EASY');

    fd.append('correctAnswer', correctAnswer);

    fd.append('optionsJson', this.optionsJson());

    this.photoSvc.updateWithImage(this.form.id, fd).subscribe({

      next: () => this.onSaveOk(),

      error: (e) => this.onSaveErr(e)

    });

  }



  private onSaveOk(): void {

    this.toast.show('Photo activity saved', 'success');

    this.showForm = false;

    this.clearPendingFile();

    this.load();

    this.cdr.detectChanges();

  }



  private onSaveErr(e: unknown): void {

    this.toast.showHttpError(e, 'Could not save this activity. Please check your connection and try again.');

    this.cdr.detectChanges();

  }



  async deleteOne(id: number) {

    const ok = await this.confirm.confirm({

      title: 'Delete photo activity',

      message: 'Are you sure you want to delete this photo activity?',

      confirmText: 'Delete',

      cancelText: 'Cancel',

      danger: true

    });

    if (!ok) return;

    this.http.delete(`${this.api}/${id}`).subscribe({

      next: () => {

        this.toast.show('Deleted', 'success');

        this.selectedIds = this.selectedIds.filter((x) => x !== id);

        this.load();

        this.cdr.detectChanges();

      },

      error: (e) => {

        this.toast.showHttpError(e, 'Could not delete this activity. Please try again.');

        this.cdr.detectChanges();

      }

    });

  }



  async deleteSelected() {

    const ids = [...this.selectedIds];

    if (!ids.length) return;

    const ok = await this.confirm.confirm({

      title: 'Delete photo activities',

      message: `Delete ${ids.length} item(s)? This cannot be undone.`,

      confirmText: 'Delete',

      cancelText: 'Cancel',

      danger: true

    });

    if (!ok) return;

    forkJoin(

      ids.map((id) =>

        this.http.delete(`${this.api}/${id}`).pipe(

          catchError(() => of({ __failed: true, id }))

        )

      )

    ).subscribe({

      next: (results) => {

        const failed = results.filter((r: any) => r && typeof r === 'object' && r.__failed);

        if (failed.length) {

          this.toast.show(`Some items could not be deleted (${failed.length})`, 'error');

        } else {

          this.toast.show('Selected items deleted', 'success');

        }

        this.selectedIds = [];

        this.load();

        this.cdr.detectChanges();

      },

      error: (e) => {

        this.toast.showHttpError(e, 'Bulk delete could not be completed. Please try again.');

        this.cdr.detectChanges();

      }

    });

  }



  trackByIdx(i: number) {

    return i;

  }

}

