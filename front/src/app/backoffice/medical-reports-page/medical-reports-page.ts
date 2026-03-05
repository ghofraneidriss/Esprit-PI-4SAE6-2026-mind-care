import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MedicalReport, ReportStatus } from './medical-report.model';
import { MedicalReportService } from './medical-report.service';
import { AuthService } from '../../frontoffice/auth/auth.service';

interface UserOption {
  userId: number;
  firstName: string;
  lastName: string;
  role: string;
  email?: string;
}

@Component({
  selector: 'app-medical-reports-page',
  standalone: false,
  templateUrl: './medical-reports-page.html',
  styleUrls: ['./medical-reports-page.css'],
})
export class MedicalReportsPageComponent implements OnInit {
  reports: MedicalReport[] = [];
  filteredReports: MedicalReport[] = [];
  selectedReport: MedicalReport | null = null;
  isLoading = true;

  patients: UserOption[] = [];
  doctors: UserOption[] = [];
  allFiles: any[] = []; // List of all existing files to choose from

  isFormOpen = false;
  formMode: 'create' | 'edit' = 'create';
  submitAttempted = false;
  formError = '';
  isSaving = false;
  loggedDoctorName = '';

  filters = {
    query: '',
    status: '' as '' | ReportStatus,
  };
  fileQuery = ''; // Filter for file selector

  readonly statusOptions: Array<{ label: string; value: ReportStatus }> = [
    { label: 'Draft', value: 'DRAFT' },
    { label: 'Reviewed', value: 'REVIEWED' },
    { label: 'Approved', value: 'APPROVED' },
  ];

  form: FormGroup;

  constructor(
    private readonly fb: FormBuilder,
    private readonly http: HttpClient,
    private readonly medicalReportService: MedicalReportService,
    public readonly authService: AuthService
  ) {
    this.form = this.fb.group({
      title: ['', [Validators.required, Validators.minLength(2)]],
      patientid: [null, [Validators.required, Validators.min(1)]],
      doctorid: [null, [Validators.required, Validators.min(1)]],
      doctorEmail: [''],
      description: ['', [Validators.required, Validators.minLength(2)]],
      status: ['DRAFT', Validators.required],
      approvalByDocter: [null],
      approvedAt: [''],
      fileIds: [[]] // Multi-select list of existing file IDs
    });
  }

  ngOnInit(): void {
    const loggedUser = this.authService.getLoggedUser();
    if (loggedUser && this.authService.isDoctor()) {
      this.loggedDoctorName = `${loggedUser.firstName} ${loggedUser.lastName}`;
    }
    this.loadUserOptions();
    this.loadReports();
    this.loadAllFiles();
  }

  applyFilters(): void {
    const query = this.filters.query.trim().toLowerCase();
    this.filteredReports = this.reports.filter((report) => {
      const matchesQuery =
        !query ||
        String(report.reportid ?? '').includes(query) ||
        (report.title ?? '').toLowerCase().includes(query) ||
        this.getPatientName(report).toLowerCase().includes(query) ||
        this.getDoctorName(report).toLowerCase().includes(query) ||
        (report.description ?? '').toLowerCase().includes(query);
      const matchesStatus =
        !this.filters.status || report.status === this.filters.status;

      return matchesQuery && matchesStatus;
    });

    if (
      this.selectedReport &&
      !this.filteredReports.some((r) => r.reportid === this.selectedReport?.reportid)
    ) {
      this.selectedReport = this.filteredReports[0] ?? null;
    }
  }

  selectReport(report: MedicalReport): void {
    this.selectedReport = report;
  }

  openCreateModal(): void {
    this.formMode = 'create';
    this.submitAttempted = false;
    this.formError = '';

    const loggedUser = this.authService.getLoggedUser();
    const isDoctor = this.authService.isDoctor();

    this.form.reset({
      title: '',
      patientid: null,
      doctorid: isDoctor && loggedUser ? loggedUser.userId : null,
      doctorEmail: isDoctor && loggedUser ? (loggedUser.email || '') : '',
      description: '',
      status: 'DRAFT',
      approvalByDocter: null,
      approvedAt: '',
      fileIds: []
    });
    this.isFormOpen = true;
  }

  openEditModal(): void {
    if (!this.selectedReport) {
      return;
    }

    this.formMode = 'edit';
    this.submitAttempted = false;
    this.formError = '';
    this.form.patchValue({
      title: this.selectedReport.title,
      patientid: this.selectedReport.patientid,
      doctorid: this.selectedReport.doctorid,
      doctorEmail: this.selectedReport.doctorEmail ?? '',
      description: this.selectedReport.description,
      status: this.selectedReport.status,
      approvalByDocter: this.selectedReport.approvalByDocter ?? null,
      approvedAt: this.toDateTimeLocal(this.selectedReport.approvedAt),
      fileIds: (this.selectedReport.files || []).map(f => f.fileid)
    });
    this.isFormOpen = true;
  }

  closeFormModal(): void {
    this.isFormOpen = false;
    this.formError = '';
    this.isSaving = false;
  }

  validateAndSave(): void {
    this.formError = '';
    this.submitAttempted = true;
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      return;
    }

    const value = this.form.value;
    const patientId = Number(value.patientid);
    const doctorId = Number(value.doctorid);

    const payload: Partial<MedicalReport> = {
      title: String(value.title || '').trim(),
      patientid: patientId,
      doctorid: doctorId,
      doctorEmail: value.doctorEmail ? String(value.doctorEmail).trim() : null,
      description: value.description,
      status: value.status,
      approvalByDocter: value.approvalByDocter
        ? Number(value.approvalByDocter)
        : null,
      approvedAt: value.approvedAt
        ? this.toBackendDateTime(value.approvedAt)
        : null,
      files: (value.fileIds || []).map((id: number) => ({ fileid: id }))
    };

    this.isSaving = true;

    if (this.formMode === 'create') {
      this.medicalReportService.create(payload).subscribe({
        next: (created) => {
          this.onPersistSuccess(created?.reportid);
        },
        error: (error) => {
          if (this.isSuccessfulButParsingFailed(error)) {
            this.onPersistSuccess();
            return;
          }
          this.isSaving = false;
          this.formError =
            error?.error?.message ??
            'Failed to create medical report. Check selected patient and doctor.';
        },
      });
      return;
    }

    if (!this.selectedReport?.reportid) {
      this.isSaving = false;
      return;
    }

    this.medicalReportService
      .update({ ...payload, reportid: this.selectedReport.reportid })
      .subscribe({
        next: (updated) => {
          this.onPersistSuccess(updated?.reportid);
        },
        error: (error) => {
          if (this.isSuccessfulButParsingFailed(error)) {
            this.onPersistSuccess(this.selectedReport?.reportid);
            return;
          }
          this.isSaving = false;
          this.formError =
            error?.error?.message ??
            'Failed to update medical report. Check selected patient and doctor.';
        },
      });
  }

  deleteReport(report: MedicalReport): void {
    if (!report.reportid) {
      return;
    }
    if (!confirm(`Delete report #${report.reportid}?`)) {
      return;
    }

    this.medicalReportService.delete(report.reportid).subscribe(() => {
      this.loadReports(report.reportid);
    });
  }

  exportPdf(report: MedicalReport): void {
    if (!report.reportid) return;

    this.medicalReportService.exportPdf(report.reportid).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `medical_report_${report.reportid}.pdf`;
        link.click();
        window.URL.revokeObjectURL(url);
      },
      error: (error) => {
        console.error('PDF export failed', error);
        alert('Failed to export PDF. Please ensure the medical report service is running.');
      },
    });
  }

  getStatusClass(status: ReportStatus): string {
    if (status === 'APPROVED') {
      return 'bg-success-subtle text-success';
    }
    if (status === 'REVIEWED') {
      return 'bg-primary-subtle text-primary';
    }
    return 'bg-body-secondary text-body';
  }

  getStatusBadgeClass(status: ReportStatus): string {
    if (status === 'APPROVED') return 'badge-approved';
    if (status === 'REVIEWED') return 'badge-reviewed';
    return 'badge-draft';
  }

  countByStatus(status: ReportStatus): number {
    return this.reports.filter((r) => r.status === status).length;
  }

  getStepState(step: 1 | 2 | 3): 'done' | 'pending' {
    if (!this.selectedReport) {
      return 'pending';
    }

    const level =
      this.selectedReport.status === 'APPROVED'
        ? 3
        : this.selectedReport.status === 'REVIEWED'
          ? 2
          : 1;
    return step <= level ? 'done' : 'pending';
  }

  controlHasError(controlName: string): boolean {
    const control = this.form.get(controlName);
    return !!control && this.submitAttempted && control.invalid;
  }

  trackByReportId(_index: number, report: MedicalReport): number {
    return report.reportid ?? _index;
  }

  getStatusLabel(status: ReportStatus): string {
    return status === 'DRAFT'
      ? 'Draft'
      : status === 'REVIEWED'
        ? 'Reviewed'
        : 'Approved';
  }

  getPatientName(report: MedicalReport): string {
    if (report.patientName) {
      return report.patientName;
    }
    return this.resolvePatientName(report.patientid);
  }

  getDoctorName(report: MedicalReport): string {
    if (report.doctorName) {
      return report.doctorName;
    }
    return this.resolveDoctorName(report.doctorid);
  }

  private loadReports(deletedId?: number, selectId?: number): void {
    this.isLoading = true;
    this.medicalReportService.getAll().subscribe({
      next: (reports) => {
        this.reports = reports;
        this.applyFilters();
        this.isLoading = false;

        if (selectId) {
          this.selectedReport =
            this.filteredReports.find((r) => r.reportid === selectId) ??
            this.filteredReports[0] ??
            null;
          return;
        }

        if (deletedId && this.selectedReport?.reportid === deletedId) {
          this.selectedReport = this.filteredReports[0] ?? null;
          return;
        }

        if (!this.selectedReport && this.filteredReports.length > 0) {
          this.selectedReport = this.filteredReports[0];
        }
      },
      error: (error) => {
        this.isLoading = false;
        this.formError =
          error?.error?.message ??
          'Unable to load medical reports. Verify medical_report_service is running.';
      },
    });
  }

  private loadUserOptions(): void {
    this.http
      .get<UserOption[]>('http://localhost:8082/api/users')
      .subscribe({
        next: (users) => {
          const remotePatients = users.filter((u) => (u.role || '').toUpperCase() === 'PATIENT');
          const remoteDoctors = users.filter((u) => (u.role || '').toUpperCase() === 'DOCTOR');
          if (remotePatients.length > 0) {
            this.patients = remotePatients;
          }
          if (remoteDoctors.length > 0) {
            this.doctors = remoteDoctors;
          }
        },
        error: () => {
          this.formError =
            'Users service unavailable. Start users_service on port 8082 to select patient and doctor.';
        },
      });
  }

  private resolvePatientName(patientId: number | null): string {
    if (!patientId) {
      return '';
    }
    const user = this.patients.find((p) => p.userId === patientId);
    return user ? `${user.firstName} ${user.lastName}` : `Patient #${patientId}`;
  }

  private resolveDoctorName(doctorId: number | null): string {
    if (!doctorId) {
      return '';
    }
    const user = this.doctors.find((d) => d.userId === doctorId);
    return user ? `${user.firstName} ${user.lastName}` : `Doctor #${doctorId}`;
  }

  private toBackendDateTime(localDateTime: string): string {
    return localDateTime.length === 16 ? `${localDateTime}:00` : localDateTime;
  }

  private toDateTimeLocal(value?: string | null): string {
    if (!value) {
      return '';
    }
    return value.replace(' ', 'T').slice(0, 16);
  }

  private onPersistSuccess(selectId?: number): void {
    this.closeFormModal();
    this.loadReports(undefined, selectId);
  }

  private isSuccessfulButParsingFailed(error: any): boolean {
    return (
      (error?.status === 200 || error?.status === 201) &&
      typeof error?.message === 'string' &&
      error.message.toLowerCase().includes('parsing')
    );
  }

  private loadAllFiles(): void {
    this.http.get<any[]>('http://localhost:8083/api/files').subscribe({
      next: (files) => {
        this.allFiles = files;
      },
      error: (err) => {
        console.warn('Files service unavailable or failed to load files', err);
      }
    });
  }

  onFileCheck(event: any, fileId: number): void {
    const checked = event.target.checked;
    let selected = this.form.get('fileIds')?.value || [];
    if (checked) {
      if (!selected.includes(fileId)) {
        selected.push(fileId);
      }
    } else {
      selected = selected.filter((id: number) => id !== fileId);
    }
    this.form.patchValue({ fileIds: selected });
  }

  isFileSelected(fileId: number): boolean {
    const selected = this.form.get('fileIds')?.value || [];
    return selected.includes(fileId);
  }

  getFilteredFiles(): any[] {
    const q = this.fileQuery.toLowerCase().trim();
    if (!q) return this.allFiles;
    return this.allFiles.filter(f =>
      (f.fileName || '').toLowerCase().includes(q) ||
      (f.fileType || '').toLowerCase().includes(q)
    );
  }
}
