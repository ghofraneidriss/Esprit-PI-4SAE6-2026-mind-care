import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { LostItem } from '../lost-item.model';
import { LostItemService } from '../lost-item.service';
import { AuthService } from '../../../frontoffice/auth/auth.service';

@Component({
  selector: 'app-critical-lost-items',
  standalone: false,
  templateUrl: './critical-lost-items.html',
  styleUrls: ['./critical-lost-items.css'],
})
export class CriticalLostItemsComponent implements OnInit {
  items: LostItem[] = [];
  urgentCount = 0;
  isLoading = false;
  pageError = '';

  currentUserId: number | null = null;
  currentRole = '';
  isCaregiver = false;

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
    this.loadCritical();
  }

  loadCritical(): void {
    if (!this.currentUserId) return;
    this.isLoading = true;

    const request$ = this.isCaregiver
      ? this.lostItemService.getCriticalItemsByCaregiverId(this.currentUserId!)
      : this.lostItemService.getAllCriticalItems();

    request$.subscribe({
      next: (data) => {
        this.items = data.items;
        this.urgentCount = data.urgentCount;
        this.isLoading = false;
      },
      error: (err) => {
        this.pageError = err?.error?.message ?? 'Failed to load critical items.';
        this.isLoading = false;
      },
    });
  }

  viewItem(id: number): void {
    this.router.navigate(['/admin/lost-items', id]);
  }

  goBack(): void {
    this.router.navigate(['/admin/lost-items']);
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
}
