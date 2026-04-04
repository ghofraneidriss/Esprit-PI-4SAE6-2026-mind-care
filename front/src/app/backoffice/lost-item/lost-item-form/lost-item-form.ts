import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { LostItem, ItemCategory, ItemStatus, ItemPriority } from '../lost-item.model';
import { LostItemService } from '../lost-item.service';
import { UserApiService, UserSummary } from '../user-api.service';
import { AuthService } from '../../../frontoffice/auth/auth.service';

@Component({
  selector: 'app-lost-item-form',
  standalone: false,
  templateUrl: './lost-item-form.html',
  styleUrls: ['./lost-item-form.css'],
})
export class LostItemFormComponent implements OnInit {
  form: FormGroup;
  isEditMode = false;
  editingId: number | null = null;
  isSaving = false;
  formError = '';
  submitAttempted = false;

  currentRole = '';
  isCaregiver = false;
  patients: UserSummary[] = [];
  patientsLoading = false;

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

  ngOnInit(): void {
    const user = this.authService.getLoggedUser();
    this.currentRole = this.authService.getLoggedRole();
    this.isCaregiver = this.currentRole === 'CAREGIVER';

    if (user) {
      this.form.patchValue({ caregiverId: user.userId });
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
    const payload = this.form.value as Partial<LostItem>;

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
