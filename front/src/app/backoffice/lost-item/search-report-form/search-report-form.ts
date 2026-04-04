import { Component, EventEmitter, Input, OnChanges, OnInit, Output } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { SearchReport, SearchResultType, ReportStatus } from '../lost-item.model';
import { LostItemService } from '../lost-item.service';
import { AuthService } from '../../../frontoffice/auth/auth.service';

@Component({
  selector: 'app-search-report-form',
  standalone: false,
  templateUrl: './search-report-form.html',
  styleUrls: ['./search-report-form.css'],
})
export class SearchReportFormComponent implements OnInit, OnChanges {
  @Input() lostItemId!: number;
  @Input() editReport: SearchReport | null = null;
  @Output() saved = new EventEmitter<SearchReport>();
  @Output() cancelled = new EventEmitter<void>();

  form: FormGroup;
  isSaving = false;
  formError = '';
  submitAttempted = false;

  readonly searchResultOptions: Array<{ label: string; value: SearchResultType }> = [
    { label: 'Not Found', value: 'NOT_FOUND' },
    { label: 'Partially Found', value: 'PARTIALLY_FOUND' },
    { label: 'Found', value: 'FOUND' },
  ];

  constructor(
    private readonly fb: FormBuilder,
    private readonly lostItemService: LostItemService,
    private readonly authService: AuthService
  ) {
    this.form = this.fb.group({
      lostItemId: [null, Validators.required],
      reportedBy: [null, Validators.required],
      searchDate: [null, Validators.required],
      locationSearched: [''],
      searchResult: ['NOT_FOUND'],
      notes: ['', Validators.maxLength(2000)],
      status: ['OPEN'],
    });
  }

  ngOnInit(): void {
    const user = this.authService.getLoggedUser();
    this.form.patchValue({
      lostItemId: this.lostItemId,
      reportedBy: user?.userId ?? null,
    });
  }

  ngOnChanges(): void {
    if (this.editReport) {
      this.form.patchValue(this.editReport);
    } else {
      const user = this.authService.getLoggedUser();
      this.form.patchValue({
        lostItemId: this.lostItemId,
        reportedBy: user?.userId ?? null,
        searchDate: null,
        locationSearched: '',
        searchResult: 'NOT_FOUND',
        notes: '',
        status: 'OPEN',
      });
    }
  }

  isInvalid(field: string): boolean {
    const c = this.form.get(field);
    return !!c && c.invalid && (c.dirty || c.touched || this.submitAttempted);
  }

  onSubmit(): void {
    this.submitAttempted = true;
    this.formError = '';
    if (this.form.invalid) return;

    this.isSaving = true;
    const payload = this.form.value as Partial<SearchReport>;

    const request$ = this.editReport?.id
      ? this.lostItemService.updateSearchReport(this.editReport.id, payload)
      : this.lostItemService.createSearchReport(payload);

    request$.subscribe({
      next: (report) => {
        this.isSaving = false;
        this.saved.emit(report);
      },
      error: (err) => {
        this.isSaving = false;
        // Sprint 2 Feature 7: show duplicate error clearly
        if (err?.error?.errorCode === 'DUPLICATE_REPORT') {
          this.formError = err.error.message;
        } else {
          this.formError = err?.error?.message ?? 'Failed to save report. Please try again.';
        }
      },
    });
  }

  onCancel(): void {
    this.cancelled.emit();
  }
}
