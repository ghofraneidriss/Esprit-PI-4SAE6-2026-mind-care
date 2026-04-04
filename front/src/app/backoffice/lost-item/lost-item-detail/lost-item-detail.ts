import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { LostItem, SearchReport } from '../lost-item.model';
import { LostItemService } from '../lost-item.service';

@Component({
  selector: 'app-lost-item-detail',
  standalone: false,
  templateUrl: './lost-item-detail.html',
  styleUrls: ['./lost-item-detail.css'],
})
export class LostItemDetailComponent implements OnInit {
  item: LostItem | null = null;
  reports: SearchReport[] = [];
  openReportsCount = 0;

  isLoading = false;
  pageError = '';
  successMsg = '';

  showReportForm = false;
  editingReport: SearchReport | null = null;

  constructor(
    private readonly lostItemService: LostItemService,
    private readonly route: ActivatedRoute,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadItem(+id);
    }
  }

  loadItem(id: number): void {
    this.isLoading = true;
    this.pageError = '';
    this.lostItemService.getLostItemById(id).subscribe({
      next: (item) => {
        this.item = item;
        this.isLoading = false;
        this.loadReports(id);
        this.loadOpenCount(id);
      },
      error: (err) => {
        console.error('[Detail] error loading item:', err);
        this.pageError = err?.error?.message ?? 'Failed to load item. Make sure the service is running.';
        this.isLoading = false;
      },
    });
  }

  loadReports(itemId: number): void {
    this.lostItemService.getSearchReportsByLostItemId(itemId).subscribe({
      next: (reports) => (this.reports = reports),
      error: () => {},
    });
  }

  loadOpenCount(itemId: number): void {
    this.lostItemService.getOpenReportsCount(itemId).subscribe({
      next: (data) => (this.openReportsCount = data.openCount),
      error: () => {},
    });
  }

  markFound(): void {
    if (!this.item?.id) return;
    if (!confirm('Mark this item as FOUND? All open search reports will be closed.')) return;
    this.lostItemService.markAsFound(this.item.id).subscribe({
      next: (updated) => {
        this.item = updated;
        this.successMsg = 'Item marked as FOUND.';
        this.loadReports(updated.id!);
        this.openReportsCount = 0;
        setTimeout(() => (this.successMsg = ''), 3000);
      },
      error: (err) => (this.pageError = err?.error?.message ?? 'Failed.'),
    });
  }

  editItem(): void {
    if (this.item?.id) {
      this.router.navigate(['/admin/lost-items', this.item.id, 'edit']);
    }
  }

  showAddReport(): void {
    this.editingReport = null;
    this.showReportForm = true;
  }

  onReportSaved(report: SearchReport): void {
    this.showReportForm = false;
    this.editingReport = null;
    this.successMsg = 'Search report saved.';
    if (this.item?.id) {
      this.loadReports(this.item.id);
      this.loadOpenCount(this.item.id);
    }
    setTimeout(() => (this.successMsg = ''), 3000);
  }

  onReportCancelled(): void {
    this.showReportForm = false;
    this.editingReport = null;
  }

  onEditReport(report: SearchReport): void {
    this.editingReport = report;
    this.showReportForm = true;
  }

  onDeleteReport(reportId: number): void {
    if (!confirm('Delete this search report?')) return;
    this.lostItemService.deleteSearchReport(reportId).subscribe({
      next: () => {
        this.successMsg = 'Report deleted.';
        if (this.item?.id) {
          this.loadReports(this.item.id);
          this.loadOpenCount(this.item.id);
        }
        setTimeout(() => (this.successMsg = ''), 3000);
      },
      error: (err) => (this.pageError = err?.error?.message ?? 'Failed to delete.'),
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

  goBack(): void {
    this.router.navigate(['/admin/lost-items']);
  }
}
