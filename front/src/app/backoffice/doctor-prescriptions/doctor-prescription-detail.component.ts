import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DoctorPrescriptionService } from './doctor-prescription.service';
import { AuthService } from '../../frontoffice/auth/auth.service';

/**
 * Composant de signature et d'affichage Premium d'une ordonnance (Prescription).
 * Design "Papier" avec possibilité de téléchargement PDF et signature.
 */
@Component({
  selector: 'app-doctor-prescription-detail',
  standalone: false,
  template: `
    <!-- SHARED FLOATING SIDEBAR NAVIGATION -->
    <nav class="floating-sidebar d-flex flex-column align-items-center py-4 position-fixed z-3 h-100 bg-white shadow-sm d-print-none"
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

    <div class="prescription-container" style="margin-left: 80px; background: #F4F7F7; min-height: 100vh; padding: 40px;">
        <!-- Toolbar -->
        <div class="d-flex justify-content-between align-items-center mb-5 p-3 rounded-4 bg-white shadow-sm border border-light d-print-none">
            <div>
                <button class="btn btn-outline-secondary rounded-pill me-3 px-4 fw-bold" (click)="goBack()">
                   <i class="bi bi-arrow-left me-1"></i> Back to History
                </button>
                <span class="badge bg-teal-subtle text-teal px-3 py-2 rounded-pill fw-bold" *ngIf="prescription">
                    #PR-{{ prescription.id }} - {{ prescription.status }}
                </span>
            </div>
            
            <div class="d-flex gap-2">
                <!-- Signature buttons (only for DOCTOR role and if not already signed) -->
                <ng-container *ngIf="showSignatureControl()">
                    <label class="btn btn-signature rounded-pill px-4 fw-bold shadow-sm cursor-pointer" *ngIf="!tempSignature">
                        <i class="bi bi-vector-pen me-2"></i> Upload Signature
                        <input type="file" (change)="onFileSelected($event)" accept="image/*" style="display: none;">
                    </label>
                    <button class="btn btn-success rounded-pill px-4 fw-bold shadow-sm" *ngIf="tempSignature && prescription?.status !== 'SIGNED'" (click)="confirmAndFinalize()">
                        <i class="bi bi-check-circle me-2"></i> Register & Sign
                    </button>
                    <button class="btn btn-outline-danger rounded-pill px-4 fw-bold shadow-sm" *ngIf="tempSignature && prescription?.status !== 'SIGNED'" (click)="cancelTempSignature()">
                        <i class="bi bi-x-lg me-2"></i> Cancel
                    </button>
                </ng-container>

                <button class="btn btn-signature rounded-pill px-4 fw-bold shadow-sm" *ngIf="prescription?.status === 'SIGNED' && showSignatureControl()" (click)="clearSignature()">
                    <i class="bi bi-eraser me-2"></i> Revoke Signature
                </button>

                <button class="btn btn-teal-solid rounded-pill px-4 fw-bold shadow" (click)="downloadPDF()">
                    <i class="bi bi-file-earmark-pdf me-2"></i> Download as PDF
                </button>
            </div>
        </div>

        <!-- ===== SUCCESS TOAST ===== -->
        <div *ngIf="showSuccessMessage"
             class="d-flex align-items-center gap-3 rounded-4 shadow-sm mb-4 p-3 d-print-none"
             style="background:#d1fae5; color:#065f46; border:1px solid #6ee7b7;">
          <i class="bi bi-patch-check-fill fs-4"></i>
          <div>
            <strong>Prescription signed successfully!</strong><br>
            <span style="font-size:.88rem;">The signature has been saved. The patient can now download the signed prescription as PDF.</span>
          </div>
        </div>
        <!-- ===== ERROR TOAST ===== -->
        <div *ngIf="showErrorMessage"
             class="d-flex align-items-center gap-3 rounded-4 shadow-sm mb-4 p-3 d-print-none"
             style="background:#fee2e2; color:#991b1b; border:1px solid #fca5a5;">
          <i class="bi bi-exclamation-octagon-fill fs-4"></i>
          <div>
            <strong>Failed to save signature.</strong><br>
            <span style="font-size:.88rem;">Please check your connection to the prescription service and try again.</span>
          </div>
        </div>

        <div *ngIf="isLoading" class="text-center py-5">
            <div class="spinner-border text-teal" style="width: 3rem; height: 3rem;"></div>
            <p class="mt-3 text-muted fw-bold">Generating secure prescription view...</p>
        </div>

        <!-- The actual paper sheet -->
        <div class="prescription-paper mx-auto shadow-lg position-relative" id="prescriptionSheet" *ngIf="!isLoading && prescription">
            
            <!-- Finalized Status Badge (Ribbon Style) -->
            <div class="signed-ribbon" *ngIf="prescription.status === 'SIGNED'">
                <i class="bi bi-check-all me-1"></i> SIGNED FOR PATIENT
            </div>

            <!-- Header -->
            <div class="paper-header d-flex justify-content-between align-items-start mb-5 border-bottom pb-4">
                <div class="brand">
                    <h2 class="clininc-name fw-bold mb-0" style="color: #2D9A9B; letter-spacing: -0.5px;">
                        <i class="bi bi-shield-plus me-2"></i>MindCare
                    </h2>
                    <p class="text-muted small mb-0">Psychiatry & Geriatrics Clinic</p>
                    <p class="text-muted x-small">123 Health Ave, Medical District, NY</p>
                </div>
                <div class="doctor-details text-end">
                    <h4 class="fw-bold mb-1 text-dark">Dr. {{ doctorName }}</h4>
                    <p class="mb-0 text-muted small">Specialist in Behavioral Health</p>
                    <p class="text-muted small">License #44129-NC</p>
                </div>
            </div>

            <div class="row mb-5 py-4 px-3 rounded-3" style="background: #fbfdfd; border-left: 5px solid #2D9A9B;">
                <div class="col-7">
                    <p class="small text-muted text-uppercase fw-bold letter-spacing-1 mb-2">Patient</p>
                    <h3 class="fw-bold text-dark mb-1">{{ patientName }}</h3>
                    <p class="text-muted small mb-0" *ngIf="patientDetails">Phone: {{ patientDetails.phone || 'N/A' }} | Email: {{ patientDetails.email }}</p>
                </div>
                <div class="col-5 text-end">
                    <p class="small text-muted text-uppercase fw-bold letter-spacing-1 mb-2">Date</p>
                    <h5 class="fw-bold text-dark">{{ prescription.createdAt | date:'longDate' }}</h5>
                    <p class="small text-muted">Consultation #C-{{ prescription.consultationId }}</p>
                </div>
            </div>

            <!-- Rx Label -->
            <div class="rx-watermark mb-4" style="margin-left: -10px;">
                <span class="fw-bold opacity-25" style="color: #2D9A9B; font-family: 'Times New Roman', serif; font-style: italic; font-size: 80px; line-height: 1;">Rx</span>
            </div>

            <!-- Medications -->
            <div class="medications-list mb-5" style="min-height: 400px;">
                <table class="table table-borderless">
                    <thead>
                        <tr class="border-bottom">
                            <th class="ps-0 py-3 text-muted small text-uppercase">Medication</th>
                            <th class="py-3 text-muted small text-uppercase text-center">Dosage</th>
                            <th class="pe-0 py-3 text-muted small text-uppercase text-end">Duration</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr *ngFor="let line of prescription.prescriptionLines">
                            <td class="ps-0 py-4 align-middle">
                                <h5 class="fw-bold mb-1 text-dark">{{ line.medicine?.commercialName }}</h5>
                                <p class="text-muted small mb-0">{{ line.medicine?.inn }} - {{ line.medicine?.therapeuticFamily }}</p>
                            </td>
                            <td class="px-3 py-4 text-center align-middle">
                                <div class="badge bg-light text-teal border shadow-sm fs-6 px-3 py-2 rounded-3 fw-bold" style="min-width: 100px;">
                                    {{ line.dosage }}
                                </div>
                            </td>
                            <td class="pe-0 py-4 text-end align-middle">
                                <p class="mb-1 fw-bold text-dark">From {{ line.startDate | date:'mediumDate' }}</p>
                                <p class="small text-muted mb-0">To {{ line.endDate | date:'mediumDate' }}</p>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <!-- Footer / Signature -->
            <div class="paper-footer border-top pt-5 d-flex justify-content-between align-items-end">
                <div class="legal-notice small text-muted opacity-50" style="max-width: 300px; font-size: 11px;">
                    This document is electronically generated and belongs to the MindCare secure medical network. 
                    Verify validity at mindcare.portal/rxv-{{ prescription.id }}.
                </div>
                <div class="signature-box text-center" style="min-width: 250px;">
                    <p class="small text-muted x-small fw-bold letter-spacing-1 mb-1 text-uppercase" style="font-size: 10px;">Issuing Physician Signature</p>
                    <div class="signature-display mb-2 d-flex justify-content-center align-items-center" style="height: 100px;">
                        <img *ngIf="prescription.doctorSignature || tempSignature" [src]="prescription.doctorSignature || tempSignature" class="signature-image" alt="Signature">
                        <div *ngIf="!prescription.doctorSignature && !tempSignature" class="signature-placeholder rounded-3 border px-4 py-3 opacity-25 small" style="border: 2px dashed #ccc !important;">
                           Pending Doctor Signature
                        </div>
                    </div>
                    <div class="border-top pt-1 text-dark fw-bold">Dr. {{ doctorName }}</div>
                </div>
            </div>
        </div>

        <!-- Print Wrapper Style (Applied globally when printing) -->
        <div class="d-none d-print-block print-mode-wrapper">
             <!-- Same as above but will be visible and styled for A4 during print -->
        </div>
    </div>
  `,
  styles: [`
    .text-teal { color: #2D9A9B; }
    .bg-teal-subtle { background: #e0f2f1; }
    .btn-teal-solid { background-color: #2D9A9B; color: white; border: none; transition: 0.2s; }
    .btn-teal-solid:hover { background-color: #1a6d6d; transform: translateY(-2px); }
    .btn-signature { background: #fff; border: 2px dashed #2D9A9B; color: #2D9A9B; transition: 0.2s; }
    .btn-signature:hover { background: #2D9A9B; color: #fff; }
    
    .prescription-paper {
        background: white;
        width: 850px; 
        min-height: 1100px;
        padding: 60px 80px;
        border-radius: 4px;
        position: relative;
        font-family: 'Outfit', sans-serif;
    }
    .signed-ribbon {
        position: absolute;
        top: 40px;
        right: -10px;
        background: #2D9A9B;
        color: white;
        padding: 8px 25px;
        font-weight: 800;
        font-size: 0.9rem;
        box-shadow: 0 4px 10px rgba(45,154,155,0.3);
        transform: rotate(5deg);
        z-index: 10;
        border-radius: 4px 0 0 4px;
    }
    .signed-ribbon::after {
        content: '';
        position: absolute;
        bottom: -5px;
        right: 0;
        border-left: 10px solid #1a6d6d;
        border-bottom: 5px solid transparent;
        z-index: -1;
    }
    .letter-spacing-1 { letter-spacing: 2px; }
    .x-small { font-size: 0.7rem; }
    .signature-image { max-height: 110px; max-width: 240px; object-fit: contain; }
    .nav-icon-link { color: #64748b !important; transition: all 0.2s ease; }
    .nav-icon-link:visited { color: #64748b !important; }
    .nav-icon-link:focus { color: #64748b !important; outline: none; }
    .nav-icon-link:active { color: #64748b !important; }
    .nav-icon-link:hover { background: #f8fafc; color: #1e293b !important; transform: translateY(-2px); }
    .active-nav-link, .active-nav-link:visited, .active-nav-link:focus, .active-nav-link:active { color: #2D9A9B !important; }
    .cursor-pointer { cursor: pointer; }
    
    @media print {
        /* Hide all UI elements including toolbar and sidebar */
        .d-print-none, .sidebar, .app-sidebar, nav, header { display: none !important; }
        .d-print-block { display: block !important; }
        .signed-ribbon { right: 20px !important; transform: none !important; box-shadow: none !important; border: 1px solid #2D9A9B !important; color: #2D9A9B !important; background: transparent !important; }
        
        @page { size: A4; margin: 0; }
        body { margin: 0; padding: 0; background: white !important; }
        
        .prescription-container { margin: 0 !important; padding: 0 !important; }
        .prescription-paper { 
            width: 100% !important; 
            min-height: 100% !important; 
            margin: 0 !important; 
            box-shadow: none !important; 
            border-radius: 0 !important;
            padding: 40px !important;
            border: none !important;
        }
        .signature-placeholder { display: none !important; }
    }
  `]
})
export class DoctorPrescriptionDetail implements OnInit {
  prescription: any = null;
  isLoading = true;
  doctorName: string = 'Doctor';
  patientName: string = 'Patient';
  patientDetails: any = null;
  loadError: boolean = false;
  tempSignature: string | null = null;
  showSuccessMessage = false;
  showErrorMessage = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private rxService: DoctorPrescriptionService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const id = +(this.route.snapshot.paramMap.get('id') || 0);
    this.loadData(id);
  }

  loadData(id: number): void {
    this.rxService.getPrescriptionById(id).subscribe({
      next: (data) => {
        this.prescription = data;
        this.loadNames();
      },
      error: (err) => {
        console.error('Load error', err);
        this.isLoading = false;
        this.loadError = true;
        this.cdr.detectChanges();
      }
    });
  }

  loadNames(): void {
    // Load Patient
    if (this.prescription.patientId) {
      this.authService.getUserById(this.prescription.patientId).subscribe({
        next: (u) => {
            this.patientName = `${u.firstName} ${u.lastName}`;
            this.patientDetails = u;
            this.cdr.detectChanges();
        },
        error: () => {
            this.patientName = `Patient #${this.prescription.patientId}`;
            this.cdr.detectChanges();
        }
      });
    }

    // Load Doctor
    if (this.prescription.doctorId) {
      this.authService.getUserById(this.prescription.doctorId).subscribe({
        next: (u) => {
            this.doctorName = `${u.firstName} ${u.lastName}`;
            this.cdr.detectChanges();
        },
        error: () => this.fallbackDoctor()
      });
    } else {
        this.fallbackDoctor();
    }
    
    this.isLoading = false;
    this.cdr.detectChanges();
  }

  fallbackDoctor(): void {
    const current = this.authService.getLoggedUser();
    if (current) this.doctorName = `${current.firstName} ${current.lastName}`;
    this.cdr.detectChanges();
  }

  showSignatureControl(): boolean {
    const role = this.authService.getLoggedRole();
    if (role !== 'DOCTOR') return false;
    
    const user = this.authService.getLoggedUser();
    return !!(user && user.userId == this.prescription?.doctorId);
  }

  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      const reader = new FileReader();
      reader.onload = (e: any) => {
        this.tempSignature = e.target.result;
        this.cdr.detectChanges();
      };
      reader.readAsDataURL(file);
    }
  }

  confirmAndFinalize(): void {
    if (!this.tempSignature) return;

    // Sauvegarde locale avant l'appel API
    const prescriptionToSave = {
      ...this.prescription,
      doctorSignature: this.tempSignature,
      status: 'SIGNED'
    };

    this.rxService.updatePrescription(this.prescription.id, prescriptionToSave).subscribe({
      next: (saved) => {
        // Mettre à jour avec les données fraîchement retournées par le backend
        this.prescription = saved;
        this.tempSignature = null;
        this.cdr.detectChanges();
        // Petit feedback visuel sans alert() bloquant
        this.showSuccessMessage = true;
        setTimeout(() => { this.showSuccessMessage = false; this.cdr.detectChanges(); }, 4000);
      },
      error: (err) => {
        console.error('[DoctorPrescriptionDetail] Signature save error:', err);
        this.showErrorMessage = true;
        setTimeout(() => { this.showErrorMessage = false; this.cdr.detectChanges(); }, 5000);
        this.tempSignature = null;
        this.cdr.detectChanges();
      }
    });
  }

  cancelTempSignature(): void {
    this.tempSignature = null;
    this.cdr.detectChanges();
  }

  clearSignature(): void {
    if (confirm('Are you sure you want to revoke the signature? This will reset the status to COMPLETED.')) {
      const prescriptionToSave = {
        ...this.prescription,
        doctorSignature: null,
        status: 'COMPLETED'
      };
      this.rxService.updatePrescription(this.prescription.id, prescriptionToSave).subscribe({
        next: (saved) => {
          this.prescription = saved;
          this.cdr.detectChanges();
        },
        error: (err) => console.error('[DoctorPrescriptionDetail] Revoke error:', err)
      });
    }
  }

  downloadPDF(): void {
    window.print();
  }

  goBack(): void {
    if (this.prescription) {
        this.router.navigate(['/admin/patient-prescriptions', this.prescription.patientId]);
    } else {
        this.router.navigate(['/admin/consultations']);
    }
  }
}
