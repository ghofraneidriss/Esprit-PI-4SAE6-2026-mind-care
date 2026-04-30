import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { LostItem, ItemCategory, ItemStatus, GlobalStats, ItemAlert } from './lost-item.model';
import { LostItemService } from './lost-item.service';

@Component({
  selector: 'app-lost-item',
  standalone: false,
  templateUrl: './lost-item.html',
  styleUrls: ['./lost-item.css'],
})
export class LostItemPage implements OnInit {
  items: LostItem[] = [];
  filteredItems: LostItem[] = [];
  criticalItems: LostItem[] = [];
  alerts: ItemAlert[] = [];

  isViewOpen = false;
  viewItem: LostItem | null = null;

  isFormOpen = false;
  formMode: 'create' | 'edit' = 'create';
  editingId: number | null = null;
  submitAttempted = false;
  formError = '';
  pageError = '';
  successMsg = '';
  isSaving = false;
  form: FormGroup;

  showStats = false;
  statistics: GlobalStats | null = null;

  filters = {
    query: '',
    status: '' as '' | ItemStatus,
    category: '' as '' | ItemCategory,
  };

  readonly categories: Array<{ label: string; value: ItemCategory }> = [
    { label: '👕 Clothing', value: 'CLOTHING' },
    { label: '⌚ Accessory', value: 'ACCESSORY' },
    { label: '📄 Document', value: 'DOCUMENT' },
    { label: '💊 Medication', value: 'MEDICATION' },
    { label: '📱 Electronic', value: 'ELECTRONIC' },
    { label: '📦 Other', value: 'OTHER' },
  ];

  readonly statuses: Array<{ label: string; value: ItemStatus }> = [
    { label: 'Lost', value: 'LOST' },
    { label: 'Searching', value: 'SEARCHING' },
    { label: 'Found', value: 'FOUND' },
    { label: 'Closed', value: 'CLOSED' },
  ];

  readonly priorities = [
    { label: 'Low', value: 'LOW' },
    { label: 'Medium', value: 'MEDIUM' },
    { label: 'High', value: 'HIGH' },
    { label: 'Critical', value: 'CRITICAL' },
  ];

  constructor(private fb: FormBuilder, private lostItemService: LostItemService) {
    this.form = this.fb.group({
      title: ['', [Validators.required, Validators.minLength(2)]],
      patientId: [null, [Validators.required, Validators.min(1)]],
      caregiverId: [null],
      category: ['', Validators.required],
      description: [''],
      status: ['LOST', Validators.required],
      lastSeenDate: ['', Validators.required],
      lastSeenLocation: ['', Validators.required],
      priority: ['MEDIUM', Validators.required],
      imageUrl: [''],
    });
  }

  ngOnInit(): void {
    this.loadItems();
    this.loadAlerts();
  }

  private loadItems(): void {
    this.lostItemService.getAllItems().subscribe({
      next: (data) => {
        this.items = data;
        this.applyFilters();
        this.loadCriticalItems();
      },
      error: () => {
        this.pageError = 'Unable to load lost items. Check if lost-item-service is running on port 8089.';
      },
    });
  }

  private loadCriticalItems(): void {
    this.lostItemService.getCriticalItems().subscribe({
      next: (data) => (this.criticalItems = data),
      error: () => {},
    });
  }

  private loadAlerts(): void {
    this.lostItemService.getAlerts().subscribe({
      next: (data) => (this.alerts = data),
      error: () => {},
    });
  }

  applyFilters(): void {
    const q = this.filters.query.trim().toLowerCase();
    this.filteredItems = this.items.filter((item) => {
      const matchQuery =
        !q ||
        String(item.patientId ?? '').includes(q) ||
        (item.title ?? '').toLowerCase().includes(q) ||
        (item.description ?? '').toLowerCase().includes(q) ||
        (item.lastSeenLocation ?? '').toLowerCase().includes(q);
      const matchStatus = !this.filters.status || item.status === this.filters.status;
      const matchCategory = !this.filters.category || item.category === this.filters.category;
      return matchQuery && matchStatus && matchCategory;
    });
  }

  getCategoryIcon(category: ItemCategory): string {
    const icons: Record<ItemCategory, string> = {
      CLOTHING: '👕',
      ACCESSORY: '⌚',
      DOCUMENT: '📄',
      MEDICATION: '💊',
      ELECTRONIC: '📱',
      OTHER: '📦',
    };
    return icons[category] ?? '📦';
  }

  getStatusBadgeClass(status: ItemStatus): string {
    const classes: Record<string, string> = {
      LOST: 'badge-danger',
      SEARCHING: 'badge-warning',
      FOUND: 'badge-success',
      CLOSED: 'badge-secondary',
    };
    return classes[status] ?? 'badge-secondary';
  }

  openViewModal(item: LostItem): void {
    this.viewItem = item;
    this.isViewOpen = true;
  }

  closeViewModal(): void {
    this.isViewOpen = false;
    this.viewItem = null;
  }

  openCreateModal(): void {
    this.formMode = 'create';
    this.editingId = null;
    this.submitAttempted = false;
    this.formError = '';
    this.form.reset({ status: 'LOST', priority: 'MEDIUM' });
    this.isFormOpen = true;
  }

  openEditModal(item: LostItem): void {
    this.formMode = 'edit';
    this.editingId = item.id ?? null;
    this.submitAttempted = false;
    this.formError = '';
    this.form.patchValue(item);
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

    if (this.form.invalid) return;

    const v = this.form.value;
    const payload: Partial<LostItem> = {
      title: v.title,
      patientId: Number(v.patientId),
      caregiverId: v.caregiverId ? Number(v.caregiverId) : undefined,
      category: v.category,
      description: v.description || undefined,
      status: v.status,
      lastSeenDate: v.lastSeenDate,
      lastSeenLocation: v.lastSeenLocation,
      priority: v.priority,
      imageUrl: v.imageUrl || undefined,
    };

    this.isSaving = true;

    if (this.formMode === 'create') {
      this.lostItemService.createItem(payload).subscribe({
        next: () => {
          this.closeFormModal();
          this.loadItems();
          this.successMsg = 'Lost item reported successfully!';
          setTimeout(() => (this.successMsg = ''), 3000);
        },
        error: (err) => {
          this.isSaving = false;
          this.formError = err?.error?.message ?? 'Failed to create lost item.';
        },
      });
    } else {
      if (!this.editingId) {
        this.isSaving = false;
        return;
      }
      this.lostItemService.updateItem(this.editingId, payload).subscribe({
        next: () => {
          this.closeFormModal();
          this.loadItems();
          this.successMsg = 'Lost item updated successfully!';
          setTimeout(() => (this.successMsg = ''), 3000);
        },
        error: (err) => {
          this.isSaving = false;
          this.formError = err?.error?.message ?? 'Failed to update lost item.';
        },
      });
    }
  }

  deleteItem(item: LostItem): void {
    if (!item.id) return;
    if (!confirm(`Delete lost item #${item.id} (${item.title})?`)) return;
    this.lostItemService.deleteItem(item.id).subscribe({
      next: () => {
        this.loadItems();
        this.successMsg = 'Lost item deleted!';
        setTimeout(() => (this.successMsg = ''), 3000);
      },
      error: () => {
        this.pageError = 'Failed to delete lost item.';
      },
    });
  }

  markAsFound(item: LostItem): void {
    if (!item.id) return;
    this.lostItemService.markAsFound(item.id).subscribe({
      next: () => {
        this.loadItems();
        this.successMsg = `✅ Item #${item.id} marked as FOUND!`;
        setTimeout(() => (this.successMsg = ''), 3000);
      },
      error: () => {
        this.pageError = 'Failed to mark item as found.';
      },
    });
  }

  toggleStats(): void {
    this.showStats = !this.showStats;
    if (this.showStats && !this.statistics) {
      this.lostItemService.getGlobalStats().subscribe({
        next: (data) => (this.statistics = data),
        error: () => (this.pageError = 'Failed to load statistics.'),
      });
    }
  }

  controlHasError(name: string): boolean {
    const ctrl = this.form.get(name);
    return !!ctrl && this.submitAttempted && ctrl.invalid;
  }

  trackById(_: number, item: LostItem): number {
    return item.id ?? _;
  }
}
