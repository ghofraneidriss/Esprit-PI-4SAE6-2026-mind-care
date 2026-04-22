import { Component, OnInit, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, takeUntil } from 'rxjs/operators';
import { LostItem, ItemCategory, ItemStatus, ItemPriority, SearchSuggestion } from '../lost-item.model';
import { LostItemService } from '../lost-item.service';
import { UserApiService, UserSummary } from '../user-api.service';
import { AuthService, AuthUser } from '../../../frontoffice/auth/auth.service';

@Component({
  selector: 'app-lost-item-form',
  standalone: false,
  templateUrl: './lost-item-form.html',
  styleUrls: ['./lost-item-form.css'],
})
export class LostItemFormComponent implements OnInit, OnDestroy {
  form: FormGroup;
  isEditMode = false;
  editingId: number | null = null;
  isSaving = false;
  formError = '';
  submitAttempted = false;

  loggedUser: AuthUser | null = null;
  currentRole = '';
  isCaregiver = false;
  patients: UserSummary[] = [];
  patientsLoading = false;

  // ── Smart Search Suggestions ──────────────────────────────────────────────
  suggestions: SearchSuggestion[] = [];
  suggestionsLoading = false;
  showSuggestions = false;
  private readonly suggestTrigger$ = new Subject<{ patientId: number | null; category: string }>();
  private readonly destroy$ = new Subject<void>();

  readonly categoryOptions: Array<{ label: string; value: ItemCategory }> = [
    { label: 'Clothing', value: 'CLOTHING' },
    { label: 'Accessory', value: 'ACCESSORY' },
    { label: 'Document', value: 'DOCUMENT' },
    { label: 'Medication', value: 'MEDICATION' },
    { label: 'Electronic', value: 'ELECTRONIC' },
    { label: 'Other', value: 'OTHER' },
  ];

  readonly statusOptions: Array<{ label: string; value: ItemStatus }> = [
    { label: 'Lost', value: 'LOST' },
    { label: 'Searching', value: 'SEARCHING' },
    { label: 'Found', value: 'FOUND' },
    { label: 'Closed', value: 'CLOSED' },
  ];

  readonly priorityOptions: Array<{ label: string; value: ItemPriority }> = [
    { label: 'Low', value: 'LOW' },
    { label: 'Medium', value: 'MEDIUM' },
    { label: 'High', value: 'HIGH' },
    { label: 'Critical', value: 'CRITICAL' },
  ];

  constructor(
    private readonly fb: FormBuilder,
    private readonly lostItemService: LostItemService,
    private readonly userApiService: UserApiService,
    private readonly authService: AuthService,
    private readonly route: ActivatedRoute,
    private readonly router: Router
  ) {
    this.form = this.fb.group({
      title: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]],
      description: ['', Validators.maxLength(500)],
      category: ['', Validators.required],
      patientId: [null, Validators.required],
      caregiverId: [null],
      lastSeenLocation: [''],
      lastSeenDate: [null],
      status: ['LOST'],
      priority: ['MEDIUM'],
      imageUrl: [''],
    });
  }

  get isPatient(): boolean { return this.currentRole === 'PATIENT'; }

  get userInitials(): string {
    if (!this.loggedUser) return '?';
    return (this.loggedUser.firstName?.charAt(0) ?? '') + (this.loggedUser.lastName?.charAt(0) ?? '');
  }

  get userFullName(): string {
    if (!this.loggedUser) return 'Unknown';
    return `${this.loggedUser.firstName ?? ''} ${this.loggedUser.lastName ?? ''}`.trim();
  }

  ngOnInit(): void {
    this.loggedUser = this.authService.getLoggedUser();
    const user = this.loggedUser;
    this.currentRole = this.authService.getLoggedRole();
    this.isCaregiver = this.currentRole === 'CAREGIVER';

    if (user) {
      this.form.patchValue({ caregiverId: user.userId });
    }

    // PATIENT: auto-fill patientId with their own userId and lock it
    if (this.isPatient && user) {
      this.form.patchValue({ patientId: user.userId });
      this.form.get('patientId')?.disable();
    }

    // CAREGIVER: load patient list for dropdown
    if (this.isCaregiver) {
      this.patientsLoading = true;
      this.userApiService.getPatients().subscribe({
        next: (list) => { this.patients = list; this.patientsLoading = false; },
        error: () => { this.patientsLoading = false; }
      });
    }

    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEditMode = true;
      this.editingId = +id;
      this.lostItemService.getLostItemById(this.editingId).subscribe({
        next: (item) => this.form.patchValue(item),
        error: () => { this.formError = 'Failed to load item.'; },
      });
    }

    // ── Wire up suggestion debounce (only on create mode) ────────────────────
    if (!this.isEditMode) {
      this.suggestTrigger$.pipe(
        debounceTime(500),
        distinctUntilChanged((a, b) => a.patientId === b.patientId && a.category === b.category),
        takeUntil(this.destroy$)
      ).subscribe(({ patientId, category }) => {
        if (patientId && category) {
          this.fetchSuggestions(patientId, category);
        } else {
          this.suggestions = [];
          this.showSuggestions = false;
        }
      });

      // React to form changes
      this.form.get('patientId')?.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
        this.triggerSuggestions();
      });
      this.form.get('category')?.valueChanges.pipe(takeUntil(this.destroy$)).subscribe(() => {
        this.triggerSuggestions();
      });
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  triggerSuggestions(): void {
    const raw = this.form.getRawValue();
    this.suggestTrigger$.next({ patientId: raw.patientId ?? null, category: raw.category ?? '' });
  }

  fetchSuggestions(patientId: number, category: string): void {
    this.suggestionsLoading = true;
    this.showSuggestions = true;
    this.lostItemService.getSearchSuggestions(patientId, category).subscribe({
      next: data => {
        this.suggestions = data;
        this.suggestionsLoading = false;
      },
      error: () => {
        this.suggestions = [];
        this.suggestionsLoading = false;
      }
    });
  }

  getConfidenceColor(score: number): string {
    if (score >= 60) return '#16a34a';
    if (score >= 35) return '#d97706';
    return '#dc2626';
  }

  dismissSuggestions(): void {
    this.showSuggestions = false;
  }

  useSuggestion(location: string): void {
    this.form.patchValue({ lastSeenLocation: location });
    this.showSuggestions = false;
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
    // getRawValue() includes disabled controls (patientId for PATIENT role)
    const payload = this.form.getRawValue() as Partial<LostItem>;

    const request$ = this.isEditMode && this.editingId
      ? this.lostItemService.updateLostItem(this.editingId, payload)
      : this.lostItemService.createLostItem(payload);

    request$.subscribe({
      next: () => {
        this.isSaving = false;
        this.router.navigate(['/admin/lost-items']);
      },
      error: (err) => {
        this.isSaving = false;
        this.formError = err?.error?.message ?? 'Failed to save. Please try again.';
      },
    });
  }

  cancel(): void {
    this.router.navigate(['/admin/lost-items']);
  }
}
