import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { AdminMedicineService } from './admin-medicine.service';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../../frontoffice/auth/auth.service';
import { Router } from '@angular/router';
import { DoctorPrescriptionService } from '../doctor-prescriptions/doctor-prescription.service';

@Component({
  selector: 'app-medication-management',
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
        <!-- Consultations -->
        <a routerLink="/admin/consultations" class="nav-icon-link d-flex justify-content-center align-items-center rounded-4 text-decoration-none" style="width: 48px; height: 48px; color: #64748b; font-size: 1.3rem;">
            <i class="bi bi-clipboard-plus-fill"></i>
        </a>
        <!-- Patient Follow-up -->
        <a routerLink="/admin/patient-follow-up" class="nav-icon-link d-flex justify-content-center align-items-center rounded-4 text-decoration-none" style="width: 48px; height: 48px; color: #64748b; font-size: 1.3rem;">
            <i class="bi bi-heart-pulse-fill"></i>
        </a>
        <!-- Medications (ACTIVE) -->
        <a routerLink="/admin/medications" class="nav-icon-link d-flex justify-content-center align-items-center rounded-4 text-decoration-none shadow-sm active-nav-link"
            style="width: 48px; height: 48px; background: #e0f2f1; border: 1px solid #2D9A9B; color: #2D9A9B; font-size: 1.3rem;">
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
        <!-- Back to Dashboard -->
        <div class="d-flex justify-content-start mb-4">
            <button class="nav-back-btn" routerLink="/admin">
                <i class="bi bi-arrow-left-circle me-2"></i> Back to Dashboard
            </button>
        </div>
        <div class="glass-header d-flex justify-content-between align-items-center mb-4" 
             style="background: #2D9A9B; border-radius: 16px; padding: 25px; color: white;">
            <div>
                <h3 class="fw-bold mb-0">Medication Management</h3>
                <p class="mb-0 opacity-75 small">System-wide drug catalog administration</p>
            </div>
            <div class="d-flex gap-2" *ngIf="isDoctor()">
                <input type="file" #fileInput (change)="onFileSelected($event)" accept=".xlsx, .xls, .csv" style="display: none;">
                <button class="btn btn-outline-light fw-bold px-4 border-2" (click)="fileInput.click()">
                    <i class="bi bi-file-earmark-spreadsheet me-1"></i> Import File
                </button>
                <button class="btn btn-light fw-bold px-4" style="color: #2D9A9B;" (click)="openAddModal()">
                    <i class="bi bi-plus-lg me-1"></i> Add new Medicine
                </button>
            </div>
        </div>

        <!-- Prescription Draft Resume -->
        <div *ngIf="prescriptionDraft" class="alert alert-warning py-3 mb-4 border-0 shadow-sm d-flex justify-content-between align-items-center" 
             style="background: #fff8e1; color: #856404; border-radius: 16px; border-left: 5px solid #ffc107 !important;">
            <div class="d-flex align-items-center">
                <div class="bg-white rounded-circle d-flex justify-content-center align-items-center me-3 shadow-sm" style="width: 45px; height: 45px; color: #ffc107;">
                    <i class="bi bi-prescription2 fs-4"></i>
                </div>
                <div>
                    <h6 class="fw-bold mb-0">Pending Prescription</h6>
                    <p class="small mb-0">You were prescribing for <strong>{{ prescriptionDraft.patientName }}</strong>. Ready to finish?</p>
                </div>
            </div>
            <button class="btn btn-warning fw-bold px-4 rounded-pill shadow-sm" (click)="resumePrescription()">
                <i class="bi bi-arrow-right-circle me-1"></i> Resume Now
            </button>
        </div>

        <div *ngIf="statusMessage" class="alert alert-info py-2 mb-3 border-0 shadow-sm" 
             style="background: #e0f2f1; color: #00796b; border-radius: 12px; font-weight: 500;">
            <i class="bi bi-info-circle me-2"></i> {{ statusMessage }}
        </div>

        <div *ngIf="isLoading" class="text-center py-5">
            <div class="spinner-border text-teal" role="status"></div>
            <p class="mt-2 text-muted">Loading medicines...</p>
        </div>

        <!-- Section de filtres pour le docteur -->
        <div class="row mb-3" *ngIf="isDoctor() && !isLoading">
            <div class="col-md-6">
                <!-- Champ de filtrage par nom du médicament -->
                <input type="text" class="form-control shadow-sm" placeholder="Filter by Medicine Name..." (input)="onNameFilterChange($event)">
            </div>
            <div class="col-md-6">
                <!-- Champ de filtrage par classe pharmaceutique -->
                <input type="text" class="form-control shadow-sm" placeholder="Filter by Pharmaceutical Class..." (input)="onClassFilterChange($event)">
            </div>
        </div>

        <div class="glass-table-wrapper" *ngIf="!isLoading">
            <div class="table-responsive">
                <table class="table custom-glass-table mb-0">
                    <thead>
                        <tr class="bg-light">
                            <th class="ps-4">#</th>
                            <th>Medicine Name</th>
                            <th>INN (Active Ingredient)</th>
                            <th>Category</th>
                            <th>Contraindications</th>
                            <th class="text-end pe-4" *ngIf="isDoctor()">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr *ngIf="!isLoading && filteredMedicines.length === 0">
                            <td colspan="6" class="py-5 text-center">
                                <div class="d-flex flex-column align-items-center opacity-75 text-muted">
                                    <i class="bi bi-inbox fs-1 mb-2"></i>
                                    <h6 class="fw-bold">No medications found</h6>
                                    <p class="small mb-0">The clinical catalog is currently empty. Start by adding a new medicine below.</p>
                                    <button class="btn btn-sm btn-teal mt-3 px-3" (click)="openAddModal()" *ngIf="isDoctor()">
                                         <i class="bi bi-plus-lg me-1"></i> Register first drug
                                    </button>
                                </div>
                            </td>
                        </tr>
                        <tr *ngFor="let m of filteredMedicines">
                            <td class="ps-4 fw-bold">#{{ m.id }}</td>
                            <td><span class="text-dark fw-bold">{{ m.commercialName }}</span></td>
                            <td><span class="badge bg-info-subtle text-info">{{ m.inn }}</span></td>
                            <td><span class="badge bg-secondary-subtle text-secondary">{{ m.therapeuticFamily }}</span></td>
                            <td><span class="text-muted small">{{ m.contraindications }}</span></td>
                            <td class="text-end pe-4" *ngIf="isDoctor()">
                                <button class="btn btn-sm btn-outline-teal me-2" (click)="openEditModal(m)">
                                    <i class="bi bi-pencil-square"></i>
                                </button>
                                <button class="btn btn-sm btn-outline-danger" (click)="deleteMedicine(m.id!)">
                                    <i class="bi bi-trash"></i>
                                </button>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>
    </div>

    <!-- MODAL -->
    <div class="glass-modal-backdrop" *ngIf="isModalOpen">
        <div class="glass-modal p-4 shadow-lg">
            <div class="d-flex justify-content-between align-items-center mb-4 pb-2 border-bottom">
                <h5 class="fw-bold mb-0 text-dark">
                   <i class="bi bi-capsule me-2 text-teal"></i>
                   {{ isEdit ? 'Edit Medicine' : 'Register New Medicine' }}
                </h5>
                <button type="button" class="btn-close" (click)="closeModal()"></button>
            </div>

            <form [formGroup]="medicineForm" (ngSubmit)="saveMedicine()">
                <div class="row g-3">
                    <div class="col-md-12">
                        <label class="form-label fw-bold small text-muted text-uppercase">Commercial Name</label>
                        <input type="text" class="form-control" formControlName="commercialName" placeholder="e.g. Advil">
                    </div>
                    <div class="col-md-12">
                        <label class="form-label fw-bold small text-muted text-uppercase">INN (Common Name)</label>
                        <input type="text" class="form-control" formControlName="inn" placeholder="e.g. Ibuprofen">
                    </div>
                    <div class="col-md-12">
                        <label class="form-label fw-bold small text-muted text-uppercase">Therapeutic Family</label>
                        <input type="text" class="form-control" formControlName="therapeuticFamily" placeholder="e.g. Analgesic">
                    </div>
                    <div class="col-md-12">
                        <label class="form-label fw-bold small text-muted text-uppercase">Contraindications</label>
                        <textarea class="form-control" formControlName="contraindications" rows="3" placeholder="Medical contraindications..."></textarea>
                    </div>
                </div>

                <div class="mt-4 pt-3 border-top d-flex gap-2 justify-content-end">
                    <button type="button" class="btn btn-light px-4 fw-bold" (click)="closeModal()">Cancel</button>
                    <button type="submit" class="btn btn-teal px-5 fw-bold" [disabled]="medicineForm.invalid">
                         {{ isEdit ? 'Update info' : 'Register drug' }}
                    </button>
                </div>
            </form>
        </div>
    </div>
  `,
  styles: [`
    .text-teal { color: #2D9A9B; }
    .btn-teal { background: #2D9A9B; color: white; border: none; }
    .btn-teal:hover { background: #1f6b6b; color: white; }
    /* Fix: keep non-active sidebar icons always the same gray, even after click/visit */
    .nav-icon-link { color: #64748b !important; transition: all 0.2s ease; }
    .nav-icon-link:visited { color: #64748b !important; }
    .nav-icon-link:focus { color: #64748b !important; outline: none; }
    .nav-icon-link:active { color: #64748b !important; }
    .nav-icon-link:hover { background: #f8fafc; color: #1e293b !important; transform: translateY(-2px); }
    .active-nav-link, .active-nav-link:visited, .active-nav-link:focus, .active-nav-link:active { color: #2D9A9B !important; }
    .nav-back-btn {
      background: white; border: 1px solid #e2e8f0; color: #475569; padding: 8px 18px;
      border-radius: 10px; font-weight: 600; font-size: 0.88rem; cursor: pointer;
      box-shadow: 0 1px 4px rgba(0,0,0,0.06); transition: all 0.2s ease;
    }
    .nav-back-btn:hover { background: #f8fafc; color: #1e293b; transform: translateX(-2px); }
    .glass-modal-backdrop {
      position: fixed; top: 0; left: 0; width: 100vw; height: 100vh;
      background: rgba(15, 61, 62, 0.4); z-index: 1000; display: flex; align-items: center; justify-content: center; backdrop-filter: blur(5px);
    }
    .glass-modal {
      background: white; border-radius: 20px; width: 500px; max-width: 95vw; border: 1px solid rgba(255,255,255,0.2); animation: fadeInUp 0.3s ease;
    }
    @keyframes fadeInUp { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
  `]
})
export class MedicationManagement implements OnInit {
  medicines: any[] = [];
  isLoading = true;
  isModalOpen = false;
  isEdit = false;
  currentId?: number;
  medicineForm: FormGroup;
  prescriptionDraft: any = null;

  constructor(
    private fb: FormBuilder,
    private medService: AdminMedicineService,
    private authService: AuthService,
    private rxService: DoctorPrescriptionService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {
    this.medicineForm = this.fb.group({
      commercialName: ['', Validators.required],
      inn: ['', Validators.required],
      therapeuticFamily: ['', Validators.required],
      contraindications: ['']
    });
  }

  ngOnInit(): void {
    this.loadAll();
    this.prescriptionDraft = this.rxService.getDraft();
  }

  resumePrescription(): void {
    if (this.prescriptionDraft) {
      this.router.navigate(['/admin/prescriptions/new'], {
        queryParams: {
          consultationId: this.prescriptionDraft.consultationId,
          patientId: this.prescriptionDraft.patientId,
          patientName: this.prescriptionDraft.patientName
        }
      });
    }
  }

  loadAll(): void {
    this.isLoading = true;
    this.medService.getAll().subscribe({
      next: (data) => {
        this.medicines = data;
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Error loading medicines:', err);
        this.statusMessage = "⚠️ Unable to load medicines. Please ensure the backend microservice (port 8083) is running.";
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  isDoctor(): boolean {
    return this.authService.getLoggedRole() === 'DOCTOR';
  }

  // Variables utilisées pour la recherche de médicaments
  filterName: string = '';
  filterClass: string = '';

  // Capture et mise à jour de la valeur de filtrage par nom
  onNameFilterChange(event: any): void {
    this.filterName = event.target.value.toLowerCase();
  }

  // Capture et mise à jour de la valeur de filtrage par classe
  onClassFilterChange(event: any): void {
    this.filterClass = event.target.value.toLowerCase();
  }

  // Propriété dynamique retournant les médicaments filtrés
  get filteredMedicines(): any[] {
    if (!this.isDoctor()) {
      return this.medicines;
    }
    return this.medicines.filter(m => {
      const matchName = !this.filterName || (m.commercialName && m.commercialName.toLowerCase().includes(this.filterName));
      const matchClass = !this.filterClass || (m.therapeuticFamily && m.therapeuticFamily.toLowerCase().includes(this.filterClass));
      return matchName && matchClass;
    });
  }

  statusMessage: string = '';

  onFileSelected(event: any): void {
    const file: File = event.target.files[0];
    if (file) {
      this.isLoading = true;
      this.statusMessage = 'Importing file, please wait...';
      this.medService.importExcel(file).subscribe({
        next: (response) => {
          this.statusMessage = response;
          this.loadAll();
          this.cdr.detectChanges();
        },
        error: (err) => {
          console.error(err);
          this.statusMessage = '❌ Error: Failed to process file. Ensure formatting is correct.';
          this.isLoading = false;
          this.cdr.detectChanges();
        }
      });
    }
  }

  openAddModal(): void {
    this.isEdit = false;
    this.currentId = undefined;
    this.medicineForm.reset();
    this.isModalOpen = true;
  }

  openEditModal(m: any): void {
    this.isEdit = true;
    this.currentId = m.id;
    this.medicineForm.patchValue(m);
    this.isModalOpen = true;
  }

  closeModal(): void {
    this.isModalOpen = false;
  }

  saveMedicine(): void {
    if (this.medicineForm.invalid) return;

    if (this.isEdit && this.currentId) {
      this.medService.update(this.currentId, this.medicineForm.value).subscribe({
        next: () => { this.loadAll(); this.closeModal(); }
      });
    } else {
      this.medService.create(this.medicineForm.value).subscribe({
        next: (resp) => { 
          this.statusMessage = resp.message; 
          this.loadAll(); 
          this.closeModal(); 
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.statusMessage = "❌ Error: Could not register medicine. Error status: " + err.status;
          this.isLoading = false;
          this.cdr.detectChanges();
        }
      });
    }
  }

  deleteMedicine(id: number): void {
    if (confirm('Delete this medication from system database?')) {
      this.medService.delete(id).subscribe({
        next: () => this.loadAll()
      });
    }
  }
}
