import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { LostItem, ItemStatus, ItemCategory } from '../lost-item.model';
import { LostItemService } from '../lost-item.service';
import { AuthService } from '../../../frontoffice/auth/auth.service';

@Component({
  selector: 'app-lost-item-list',
  standalone: false,
  templateUrl: './lost-item-list.html',
  styleUrls: ['./lost-item-list.css'],
})
export class LostItemListComponent implements OnInit {
  items: LostItem[] = [];
  filteredItems: LostItem[] = [];
  totalElements = 0;
  totalPages = 0;
  currentPage = 0;
  pageSize = 20;

  isLoading = false;
  pageError = '';
  successMsg = '';

  currentUserId: number | null = null;
  currentRole = '';
  isCaregiver = false;
  isAdminOrDoctor = false;

  filters = {
    status: '' as '' | ItemStatus,
    category: '' as '' | ItemCategory,
  };

  readonly statusOptions: Array<{ label: string; value: ItemStatus }> = [
    { label: 'Lost', value: 'LOST' },
    { label: 'Searching', value: 'SEARCHING' },
    { label: 'Found', value: 'FOUND' },
    { label: 'Closed', value: 'CLOSED' },
  ];

  readonly categoryOptions: Array<{ label: string; value: ItemCategory }> = [
    { label: 'Clothing', value: 'CLOTHING' },
    { label: 'Accessory', value: 'ACCESSORY' },
    { label: 'Document', value: 'DOCUMENT' },
    { label: 'Medication', value: 'MEDICATION' },
    { label: 'Electronic', value: 'ELECTRONIC' },
    { label: 'Other', value: 'OTHER' },
  ];

  constructor(
    private readonly lostItemService: LostItemService,
    private readonly authService: AuthService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    const user = this.authService.getLoggedUser();
    this.currentUserId = user?.userId ?? null;
    this.currentRole = this.authService.getLoggedRole();
    this.isCaregiver = this.currentRole === 'CAREGIVER';
    this.isAdminOrDoctor = this.currentRole === 'ADMIN' || this.currentRole === 'DOCTOR';
    this.loadItems();
  }

  get isPatient(): boolean { return this.currentRole === 'PATIENT'; }

  loadItems(): void {
    this.isLoading = true;
    this.pageError = '';

    if (this.isPatient && this.currentUserId) {
      // PATIENT: only their own items (backend also enforces via X-User-Id header)
      this.lostItemService.getPatientLostItems(
        this.currentUserId, this.filters.status || undefined, this.filters.category || undefined
      ).subscribe({
        next: (page) => {
          this.items = page.content;
          this.applyClientFilters();
          this.isLoading = false;
        },
        error: (err) => {
          this.pageError = err?.error?.message ?? 'Failed to load items.';
          this.isLoading = false;
        }
      });
    } else if (this.isCaregiver && this.currentUserId) {
      // CAREGIVER: only their assigned patients' items
      this.lostItemService.getItemsByCaregiverId(this.currentUserId).subscribe({
        next: (items) => {
          this.items = items;
          this.applyClientFilters();
          this.isLoading = false;
        },
        error: (err) => {
          this.pageError = err?.error?.message ?? 'Failed to load items.';
          this.isLoading = false;
        }
      });
    } else {
      // ADMIN / DOCTOR: all items (backend scopes via X-User-Role header)
      this.lostItemService.getAllLostItems().subscribe({
        next: (items) => {
          this.items = items;
          this.applyClientFilters();
          this.isLoading = false;
        },
        error: (err) => {
          this.pageError = err?.error?.message ?? 'Failed to load lost items.';
          this.isLoading = false;
        },
      });
    }
  }

  applyClientFilters(): void {
    this.filteredItems = this.items.filter(i => {
      const matchStatus   = !this.filters.status   || i.status   === this.filters.status;
      const matchCategory = !this.filters.category || i.category === this.filters.category;
      return matchStatus && matchCategory;
    });
    this.totalElements = this.filteredItems.length;
    this.totalPages = Math.ceil(this.totalElements / this.pageSize);
  }

  applyFilters(): void {
    this.currentPage = 0;
    this.applyClientFilters();
  }

  resetFilters(): void {
    this.filters = { status: '', category: '' };
    this.currentPage = 0;
    this.applyClientFilters();
  }

  goToPage(page: number): void {
    if (page < 0 || page >= this.totalPages) return;
    this.currentPage = page;
  }

  get pagedItems(): LostItem[] {
    const start = this.currentPage * this.pageSize;
    return this.filteredItems.slice(start, start + this.pageSize);
  }

  viewItem(id: number): void {
    this.router.navigate(['/admin/lost-items', id]);
  }

  editItem(id: number): void {
    this.router.navigate(['/admin/lost-items', id, 'edit']);
  }

  deleteItem(id: number): void {
    if (!confirm('Close this lost item? All open search reports will also be closed.')) return;
    this.lostItemService.deleteLostItem(id).subscribe({
      next: () => {
        this.successMsg = 'Item closed successfully.';
        this.loadItems();
        setTimeout(() => (this.successMsg = ''), 3000);
      },
      error: (err) => {
        this.pageError = err?.error?.message ?? 'Failed to close item.';
      },
    });
  }

  markFound(id: number): void {
    if (!confirm('Mark this item as FOUND? All open search reports will be closed.')) return;
    this.lostItemService.markAsFound(id).subscribe({
      next: () => {
        this.successMsg = 'Item marked as FOUND.';
        this.loadItems();
        setTimeout(() => (this.successMsg = ''), 3000);
      },
      error: (err) => {
        this.pageError = err?.error?.message ?? 'Failed to mark item as found.';
      },
    });
  }

  getPriorityClass(priority?: string): string {
    switch (priority) {
      case 'LOW': return 'badge bg-secondary';
      case 'MEDIUM': return 'badge bg-primary';
      case 'HIGH': return 'badge bg-warning text-dark';
      case 'CRITICAL': return 'badge bg-danger';
      default: return 'badge bg-secondary';
    }
  }

  getStatusClass(status?: string): string {
    switch (status) {
      case 'LOST': return 'badge bg-danger';
      case 'SEARCHING': return 'badge bg-warning text-dark';
      case 'FOUND': return 'badge bg-success';
      case 'CLOSED': return 'badge bg-secondary';
      default: return 'badge bg-secondary';
    }
  }

  get pages(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i);
  }
}
