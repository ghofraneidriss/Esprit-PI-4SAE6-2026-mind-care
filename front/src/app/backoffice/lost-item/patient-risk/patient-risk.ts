import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { LostItemService } from '../lost-item.service';
import { PatientRisk, FrequencyTrend } from '../lost-item.model';

@Component({
  selector: 'app-patient-risk',
  standalone: false,
  templateUrl: './patient-risk.html',
  styleUrls: ['./patient-risk.css']
})
export class PatientRiskComponent implements OnInit {

  patientId: number | null = null;
  risk: PatientRisk | null = null;
  trend: FrequencyTrend | null = null;
  isLoading = false;
  pageError = '';

  constructor(
    private readonly svc: LostItemService,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly cdr: ChangeDetectorRef,
  ) {}

  ngOnInit(): void {
    const stored = localStorage.getItem('currentUser');
    if (stored) {
      try {
        const user = JSON.parse(stored);
        this.patientId = user.userId ?? user.id ?? null;
      } catch { /* ignore */ }
    }
    const routeId = this.route.snapshot.paramMap.get('patientId');
    if (routeId) this.patientId = +routeId;

    if (this.patientId) this.loadData();
  }

  loadData(): void {
    if (!this.patientId) return;
    this.isLoading = true;
    this.pageError = '';

    this.svc.getPatientItemRisk(this.patientId).subscribe({
      next: data => {
        this.risk = data;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.pageError = 'Failed to load risk assessment.';
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });

    this.svc.getPatientFrequencyTrend(this.patientId).subscribe({
      next: data => { this.trend = data; },
      error: () => {}
    });
  }

  goBack(): void {
    this.router.navigate(['/admin/lost-items']);
  }

  viewAlerts(): void {
    this.router.navigate(['/admin/lost-items/alerts']);
  }

  getRiskGaugeColor(): string {
    switch (this.risk?.riskLevel) {
      case 'CRITICAL': return '#dc2626';
      case 'HIGH':     return '#ea580c';
      case 'MODERATE': return '#d97706';
      default:         return '#16a34a';
    }
  }

  getRiskLevelClass(): string {
    switch (this.risk?.riskLevel) {
      case 'CRITICAL': return 'risk-critical';
      case 'HIGH':     return 'risk-high';
      case 'MODERATE': return 'risk-moderate';
      default:         return 'risk-low';
    }
  }

  getRiskBadgeClass(): string {
    switch (this.risk?.riskLevel) {
      case 'CRITICAL': return 'bg-danger';
      case 'HIGH':     return 'bg-warning text-dark';
      case 'MODERATE': return 'bg-warning text-dark';
      default:         return 'bg-success';
    }
  }

  getTrendTextClass(): string {
    switch (this.trend?.trend) {
      case 'INCREASING': return 'text-danger';
      case 'DECREASING': return 'text-success';
      default:           return 'text-warning';
    }
  }

  getTrendIcon(): string {
    switch (this.trend?.trend) {
      case 'INCREASING': return '↑';
      case 'DECREASING': return '↓';
      default:           return '→';
    }
  }

  getTrendClass(): string {
    switch (this.trend?.trend) {
      case 'INCREASING': return 'trend-up';
      case 'DECREASING': return 'trend-down';
      default:           return 'trend-stable';
    }
  }

  gaugeOffset(): number {
    const score = this.risk?.riskScore ?? 0;
    const circumference = 2 * Math.PI * 54;
    return circumference - (score / 100) * circumference;
  }
}
