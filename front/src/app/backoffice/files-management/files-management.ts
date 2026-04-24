import { Component, OnInit } from '@angular/core';
import { FileRecord, FileType } from './file.model';
import { FileService } from './file.service';
import { AuthService } from '../../frontoffice/auth/auth.service';

@Component({
  selector: 'app-files-management-page',
  standalone: false,
  templateUrl: './files-management.html',
  styleUrls: ['./files-management.css'],
})
export class FilesManagementPageComponent implements OnInit {
  files: FileRecord[] = [];
  filteredFiles: FileRecord[] = [];

  filters = {
    query: '',
    type: '' as '' | FileType,
  };

  readonly typeOptions: FileType[] = ['MRI_SCAN', 'PDF_REPORT', 'IMAGE', 'OTHER'];

  isFormOpen = false;
  formMode: 'create' | 'edit' = 'create';
  submitAttempted = false;
  isSaving = false;
  formError = '';

  form: FileRecord = {
    caregiverid: null,
    fileName: '',
    fileType: 'OTHER',
  };

  constructor(
    private readonly fileService: FileService,
    public readonly authService: AuthService
  ) { }

  ngOnInit(): void {
    this.loadFiles();
  }

  get totalCount(): number {
    return this.files.length;
  }

  get mriCount(): number {
    return this.files.filter((f) => f.fileType === 'MRI_SCAN').length;
  }

  get pdfCount(): number {
    return this.files.filter((f) => f.fileType === 'PDF_REPORT').length;
  }

  get otherCount(): number {
    return this.files.filter((f) => f.fileType === 'IMAGE' || f.fileType === 'OTHER').length;
  }

  applyFilters(): void {
    const query = this.filters.query.trim().toLowerCase();

    this.filteredFiles = this.files.filter((file) => {
      const matchesQuery =
        !query ||
        String(file.fileid ?? '').includes(query) ||
        (file.fileName ?? '').toLowerCase().includes(query) ||
        String(file.caregiverid ?? '').includes(query);
      const matchesType = !this.filters.type || file.fileType === this.filters.type;
      return matchesQuery && matchesType;
    });
  }

  openCreateModal(): void {
    this.formMode = 'create';
    this.submitAttempted = false;
    this.formError = '';
    this.form = {
      caregiverid: null,
      fileName: '',
      fileType: 'OTHER',
    };
    this.isFormOpen = true;
  }

  openEditModal(file: FileRecord): void {
    this.formMode = 'edit';
    this.submitAttempted = false;
    this.formError = '';
    this.form = {
      fileid: file.fileid,
      caregiverid: file.caregiverid,
      fileName: file.fileName,
      fileType: file.fileType,
      createdAt: file.createdAt,
      updatedAt: file.updatedAt,
    };
    this.isFormOpen = true;
  }

  closeFormModal(): void {
    this.isFormOpen = false;
    this.isSaving = false;
    this.formError = '';
  }

  saveFile(): void {
    this.submitAttempted = true;
    this.formError = '';

    if (!this.form.fileName?.trim()) {
      this.formError = 'File name is required.';
      return;
    }
    if (this.form.caregiverid == null || this.form.caregiverid < 1) {
      this.formError = 'Caregiver ID is required.';
      return;
    }

    this.isSaving = true;
    const payload: Partial<FileRecord> = {
      fileid: this.form.fileid,
      caregiverid: Number(this.form.caregiverid),
      fileName: this.form.fileName.trim(),
      fileType: this.form.fileType,
    };

    if (this.formMode === 'create') {
      this.fileService.create(payload).subscribe({
        next: () => this.onPersistSuccess(),
        error: (error) => {
          this.isSaving = false;
          this.formError = error?.error?.message ?? 'Failed to create file.';
        },
      });
      return;
    }

    this.fileService.update(payload).subscribe({
      next: () => this.onPersistSuccess(),
      error: (error) => {
        this.isSaving = false;
        this.formError = error?.error?.message ?? 'Failed to update file.';
      },
    });
  }

  deleteFile(file: FileRecord): void {
    if (!file.fileid) {
      return;
    }
    if (!confirm(`Delete file #${file.fileid}?`)) {
      return;
    }

    this.fileService.delete(file.fileid).subscribe({
      next: () => this.loadFiles(),
      error: (error) => {
        this.formError = error?.error?.message ?? 'Failed to delete file.';
      },
    });
  }

  toKindClass(type: FileType): string {
    if (type === 'MRI_SCAN') {
      return 'kind-mri';
    }
    if (type === 'PDF_REPORT') {
      return 'kind-pdf';
    }
    return 'kind-other';
  }

  toKindLabel(type: FileType): string {
    if (type === 'MRI_SCAN') {
      return 'MRI';
    }
    if (type === 'PDF_REPORT') {
      return 'PDF';
    }
    return type;
  }

  private loadFiles(): void {
    this.fileService.getAll().subscribe({
      next: (data) => {
        this.files = data ?? [];
        this.applyFilters();
      },
      error: (error) => {
        this.formError =
          error?.error?.message ??
          'Unable to load files. Verify medical_report_service is running on port 8083.';
      },
    });
  }

  private onPersistSuccess(): void {
    this.closeFormModal();
    this.loadFiles();
  }
}
