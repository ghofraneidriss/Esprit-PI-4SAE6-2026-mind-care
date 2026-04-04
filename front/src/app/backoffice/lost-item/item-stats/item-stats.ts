import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { LostItemService } from '../lost-item.service';
import { GlobalStats, AlertStats } from '../lost-item.model';

@Component({
  selector: 'app-item-stats',
  standalone: false,
  templateUrl: './item-stats.html',
  styleUrls: ['./item-stats.css']
})
export class ItemStatsComponent implements OnInit {

  globalStats: GlobalStats | null = null;
  alertStats: AlertStats | null = null;
  isLoading = false;
  pageError = '';

  constructor(
    private readonly svc: LostItemService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.loadStats();
  }

  loadStats(): void {
    this.isLoading = true;
    this.pageError = '';

    this.svc.getGlobalStatistics().subscribe({
      next: data => {
        this.globalStats = data;
        this.isLoading = false;
      },
      error: () => {
        this.pageError = 'Failed to load statistics.';
        this.isLoading = false;
      }
    });

    this.svc.getAlertStatistics().subscribe({
      next: data => { this.alertStats = data; },
      error: () => {}
    });
  }

  goBack(): void {
    this.router.navigate(['/admin/lost-items']);
  }

  categoryEntries(): { key: string; value: number }[] {
    if (!this.globalStats?.categoryDistribution) return [];
    return Object.entries(this.globalStats.categoryDistribution)
      .map(([key, value]) => ({ key, value }))
      .sort((a, b) => b.value - a.value);
  }

  priorityEntries(): { key: string; value: number }[] {
    if (!this.globalStats?.priorityDistribution) return [];
    return Object.entries(this.globalStats.priorityDistribution)
      .map(([key, value]) => ({ key, value }))
      .sort((a, b) => b.value - a.value);
  }

  alertLevelEntries(): { key: string; value: number }[] {
    if (!this.alertStats?.levelDistribution) return [];
    return Object.entries(this.alertStats.levelDistribution)
      .map(([key, value]) => ({ key, value }))
      .sort((a, b) => b.value - a.value);
  }

  categoryBarWidth(value: number): number {
    if (!this.globalStats) return 0;
    const max = Math.max(...Object.values(this.globalStats.categoryDistribution), 1);
    return Math.round((value / max) * 100);
  }

  priorityBarWidth(value: number): number {
    if (!this.globalStats) return 0;
    const max = Math.max(...Object.values(this.globalStats.priorityDistribution), 1);
    return Math.round((value / max) * 100);
  }

  getPriorityClass(key: string): string {
    switch (key) {
      case 'CRITICAL': return 'bar-critical';
      case 'HIGH':     return 'bar-high';
      case 'MEDIUM':   return 'bar-medium';
      default:         return 'bar-low';
    }
  }

  getPriorityBadge(key: string): string {
    switch (key) {
      case 'CRITICAL': return 'badge bg-danger';
      case 'HIGH':     return 'badge bg-warning text-dark';
      case 'MEDIUM':   return 'badge bg-primary';
      default:         return 'badge bg-secondary';
    }
  }

  getPriorityBarClass(key: string): string {
    switch (key) {
      case 'CRITICAL': return 'bg-danger';
      case 'HIGH':     return 'bg-warning';
      case 'MEDIUM':   return 'bg-primary';
      default:         return 'bg-secondary';
    }
  }

  getRecoveryColor(): string {
    const rate = this.globalStats?.recoveryRate ?? 0;
    if (rate >= 70) return '#22c55e';
    if (rate >= 40) return '#f59e0b';
    return '#ef4444';
  }
}
