import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DoctorPrescriptionService } from './doctor-prescription.service';
import { AuthService } from '../../frontoffice/auth/auth.service';

/**
 * Composant permettant au médecin de voir l'historique des prescriptions d'un patient spécifique.
 */
@Component({
  selector: 'app-doctor-prescription-history',
  standalone: false,
  template: `
    <!-- SHARED FLOATING SIDEBAR NAVIGATION -->
    <nav class="floating-sidebar d-flex flex-column align-items-center py-4 position-fixed z-3 h-100 bg-white shadow-sm"
        style="width: 80px; top: 0; left: 0; gap: 1.5rem; border-right: 1px solid rgba(0,0,0,0.05);">
        <!-- Logo -->
        <a routerLink="/admin" class="mb-4 d-flex justify-content-center align-items-center rounded-circle shadow-sm"
            style="width: 45px; height: 45px; background: #2D9A9B; color: white; text-decoration: none;">
            <i class="bi bi-grid-3x3-gap-fill fs-5"></i>
        </a>
        <!-- Home -->
        <a routerLink="/admin" class="nav-icon-link d-flex justify-content-center align-items-center rounded-4 text-decoration-none" style="width: 48px; height: 48px; color: #64748b; font-size: 1.3rem;">
            <i class="bi bi-house"></i>
        </a>
        <!-- Appointments -->
        <a routerLink="/admin/appointments" class="nav-icon-link d-flex justify-content-center align-items-center rounded-4 text-decoration-none" style="width: 48px; height: 48px; color: #64748b; font-size: 1.3rem;">
            <i class="bi bi-calendar-check-fill"></i>
        </a>
        <!-- Consultations (ACTIVE) -->
        <a routerLink="/admin/consultations" class="nav-icon-link d-flex justify-content-center align-items-center rounded-4 text-decoration-none shadow-sm active-nav-link"
           style="width: 48px; height: 48px; background: #e0f2f1; border: 1px solid #2D9A9B; color: #2D9A9B; font-size: 1.3rem;">
            <i class="bi bi-clipboard-plus-fill"></i>
        </a>
        <!-- Patient Follow-up -->
        <a routerLink="/admin/patient-follow-up" class="nav-icon-link d-flex justify-content-center align-items-center rounded-4 text-decoration-none" style="width: 48px; height: 48px; color: #64748b; font-size: 1.3rem;">
            <i class="bi bi-heart-pulse-fill"></i>
        </a>
        <!-- Medications -->
        <a routerLink="/admin/medications" class="nav-icon-link d-flex justify-content-center align-items-center rounded-4 text-decoration-none" style="width: 48px; height: 48px; color: #64748b; font-size: 1.3rem;">
            <i class="bi bi-capsule"></i>
        </a>
        <!-- Separator -->
        <div style="border-bottom: 2px solid #f1f5f9; width: 30px; margin: 0.5rem 0;"></div>
        <!-- Add New Action -->
        <a href="javascript:void(0)"
            class="nav-icon-link d-flex justify-content-center align-items-center rounded-4 text-decoration-none shadow-sm cursor-pointer"
            style="width: 46px; height: 46px; color: #2D9A9B; border: 2px solid #B2DFDB; background: white;">
            <i class="bi bi-plus-lg fw-bold" style="font-size: 1.2rem;"></i>
        </a>
        <!-- Logout -->
        <a routerLink="/auth/login" class="nav-icon-link d-flex justify-content-center align-items-center text-decoration-none mt-auto" style="width: 48px; height: 48px; color: #475569; font-size: 1.4rem;">
            <i class="bi bi-box-arrow-left"></i>
        </a>
    </nav>

    <div class="glass-dashboard-wrapper" style="margin-left: 80px; background: #F4F7F7; min-height: 100vh; padding: 20px;">
        <div class="glass-header d-flex justify-content-between align-items-center mb-4" 
             style="background: #2D9A9B; border-radius: 16px; padding: 25px; color: white; box-shadow: 0 4px 20px rgba(45,154,155,0.25);">
            <div>
                <button class="btn btn-sm btn-light rounded-pill px-3 mb-2 fw-bold" style="color: #2D9A9B;" (click)="goBack()">
                   <i class="bi bi-arrow-left me-1"></i> Back to Consultations
                </button>
                <h3 class="fw-bold mb-0"><i class="bi bi-prescription2 me-2"></i>Patient Prescription History</h3>
                <p class="mb-0 opacity-75 small">Viewing records for: <strong>{{ patientName }}</strong></p>
            </div>
            <button class="btn btn-light fw-bold" style="color: #2D9A9B;" (click)="addNew()">
                <i class="bi bi-plus-lg me-1"></i> New Prescription
            </button>
        </div>

        <div *ngIf="statusMessage" class="alert alert-info py-2 mb-3 border-0 shadow-sm" 
             style="background: #e0f2f1; color: #00796b; border-radius: 12px; font-weight: 500;">
            <i class="bi bi-info-circle me-2"></i> {{ statusMessage }}
        </div>
        
        <div *ngIf="isLoading" class="text-center py-5">
            <div class="spinner-border text-teal" role="status"></div>
            <p class="mt-2 text-muted fw-bold">Fetching patient medical history...</p>
        </div>

        <div class="glass-table-wrapper" *ngIf="!isLoading">
            <div class="table-responsive">
                <table class="table custom-glass-table mb-0">
                    <thead>
                        <tr>
                            <th class="ps-4 text-uppercase small text-muted fw-bold">ID</th>
                            <th class="text-uppercase small text-muted fw-bold">Date issued</th>
                            <th class="text-uppercase small text-muted fw-bold">Associated Consultation</th>
                            <th class="text-uppercase small text-muted fw-bold">Status</th>
                            <th class="text-uppercase small text-muted fw-bold text-end pe-4">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr *ngIf="prescriptions.length === 0">
                            <td colspan="5" class="text-center py-5 text-muted fst-italic">
                                <i class="bi bi-folder2-open d-block fs-1 opacity-25 mb-2"></i>
                                No prescription history found for this patient.
                            </td>
                        </tr>
                        <tr *ngFor="let p of prescriptions">
                            <td class="ps-4 fw-bold">#PR-{{ p.id }}</td>
                            <td>{{ p.createdAt | date:'mediumDate' }} <br><small class="text-muted">{{ p.createdAt | date:'shortTime' }}</small></td>
                            <td><span class="badge bg-light text-dark shadow-sm border">#C-{{ p.consultationId }}</span></td>
                            <td>
                                <span class="badge rounded-pill px-3 py-2" 
                                      [ngClass]="p.status === 'COMPLETED' ? 'bg-success-subtle text-success' : 'bg-warning-subtle text-warning'">
                                    {{ p.status }}
                                </span>
                            </td>
                            <td class="text-end pe-4">
                                <button class="btn btn-sm btn-outline-teal me-2" (click)="viewDetail(p.id)">
                                    <i class="bi bi-eye"></i>
                                </button>
                                <button class="btn btn-sm btn-outline-danger" (click)="deleteRx(p.id)">
                                    <i class="bi bi-trash"></i>
                                </button>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
  `,
  styles: [`
    .text-teal { color: #2D9A9B; }
    .btn-outline-teal { border-color: #2D9A9B; color: #2D9A9B; border-radius: 8px; }
    .btn-outline-teal:hover { background-color: #2D9A9B; color: white; }
    .glass-table-wrapper { background: white; border-radius: 16px; border: 1px solid rgba(0,0,0,0.05); overflow: hidden; box-shadow: 0 4px 15px rgba(0,0,0,0.02); }
    .custom-glass-table th { background: #f8fafc; padding: 15px; border-bottom: 2px solid #f1f5f9; }
    .custom-glass-table td { padding: 15px; vertical-align: middle; border-bottom: 1px solid #f1f5f9; }
    .nav-icon-link { color: #64748b !important; transition: all 0.2s ease; }
    .nav-icon-link:visited { color: #64748b !important; }
    .nav-icon-link:focus { color: #64748b !important; outline: none; }
    .nav-icon-link:active { color: #64748b !important; }
    .nav-icon-link:hover { background: #f8fafc; color: #1e293b !important; transform: translateY(-2px); }
    .active-nav-link, .active-nav-link:visited, .active-nav-link:focus, .active-nav-link:active { color: #2D9A9B !important; }
  `]
})
export class DoctorPrescriptionHistory implements OnInit {
  prescriptions: any[] = [];
  isLoading = true;
  patientId: number = 0;
  patientName: string = 'Patient';
  statusMessage: string = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private rxService: DoctorPrescriptionService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // We use params observable to handle navigation between patients if needed
    this.route.params.subscribe(params => {
      this.patientId = +(params['patientId'] || 0);
      if (this.patientId && this.patientId > 0) {
        this.loadPatientName();
        this.loadHistory();
      } else {
        console.warn('[DoctorPrescriptionHistory] Invalid patientId:', this.patientId);
        this.isLoading = false;
        this.statusMessage = 'Invalid Patient ID. Please navigate from the consultations dashboard.';
        this.cdr.detectChanges();
      }
    });
  }

  loadPatientName(): void {
    if (this.patientId) {
      this.authService.getUserById(this.patientId).subscribe({
        next: (user) => {
          this.patientName = `${user.firstName} ${user.lastName}`;
          this.cdr.detectChanges();
        }
      });
    }
  }

  loadHistory(): void {
    if (this.patientId) {
      this.isLoading = true;
      this.rxService.getHistoryByPatient(this.patientId).subscribe({
        next: (data) => {
          this.prescriptions = data;
          this.isLoading = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error('[DoctorRxHistory] Load error:', err);
          this.isLoading = false;
          this.statusMessage = '⚠️ Error: Failed to load prescription history. Please ensure the backend microservice (port 8083) is running.';
          this.cdr.detectChanges();
        }
      });
    }
  }

  viewDetail(id: number): void {
    this.router.navigate(['/admin/prescriptions/view', id]);
  }

  deleteRx(id: number): void {
    if (confirm('Are you sure you want to delete this prescription?')) {
      this.rxService.deletePrescription(id).subscribe({
        next: () => {
             this.statusMessage = 'Prescription deleted successfully.';
             this.loadHistory();
        }
      });
    }
  }

  addNew(): void {
    this.router.navigate(['/admin/prescriptions/new'], { 
        queryParams: { patientId: this.patientId, patientName: this.patientName } 
    });
  }

  goBack(): void {
    this.router.navigate(['/admin/consultations']);
  }
}
