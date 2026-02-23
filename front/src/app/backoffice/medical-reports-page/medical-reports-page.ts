import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MedicalReport, ReportStatus } from './medical-report.model';
import { MedicalReportService } from './medical-report.service';

interface UserOption {
  userId: number;
  firstName: string;
  lastName: string;
  role: string;
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

  patients: UserOption[] = [];
  doctors: UserOption[] = [];

  isFormOpen = false;
  formMode: 'create' | 'edit' = 'create';
  submitAttempted = false;
  formError = '';
  isSaving = false;

  filters = {
    query: '',
    status: '' as '' | ReportStatus,
  };

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
  ) {
    this.form = this.fb.group({
      title: ['', [Validators.required, Validators.minLength(2)]],
      patientid: [null, [Validators.required, Validators.min(1)]],
      doctorid: [null, [Validators.required, Validators.min(1)]],
      description: ['', [Validators.required, Validators.minLength(2)]],
      status: ['DRAFT', Validators.required],
      approvalByDocter: [null],
      approvedAt: [''],
    });
  }

  ngOnInit(): void {
    this.loadUserOptions();
    this.loadReports();
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
    this.form.reset({
      title: '',
      patientid: null,
      doctorid: null,
      description: '',
      status: 'DRAFT',
      approvalByDocter: null,
      approvedAt: '',
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
      description: this.selectedReport.description,
      status: this.selectedReport.status,
      approvalByDocter: this.selectedReport.approvalByDocter ?? null,
      approvedAt: this.toDateTimeLocal(this.selectedReport.approvedAt),
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
      description: value.description,
      status: value.status,
      approvalByDocter: value.approvalByDocter
        ? Number(value.approvalByDocter)
        : null,
      approvedAt: value.approvedAt
        ? this.toBackendDateTime(value.approvedAt)
        : null,
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

  getStatusClass(status: ReportStatus): string {
    if (status === 'APPROVED') {
      return 'bg-success-subtle text-success';
    }
    if (status === 'REVIEWED') {
      return 'bg-primary-subtle text-primary';
    }
    return 'bg-body-secondary text-body';
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
    this.medicalReportService.getAll().subscribe({
      next: (reports) => {
        this.reports = reports;
        this.applyFilters();

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
}
