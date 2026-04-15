import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { PrescriptionService } from '../prescription.service';

/**
 * Composant affichant l'historique des prescriptions (ordonnances) du patient.
 * - Affiche un spinner pendant le chargement
 * - Affiche un message d'erreur avec bouton "Retry" si le serveur est inaccessible
 * - Affiche "No Prescriptions Yet" si la liste est vide
 * - Affiche le statut de signature du médecin dans le tableau
 */
@Component({
  selector: 'app-patient-prescriptions',
  standalone: false,
  template: `
    <app-header></app-header>
    <main class="main" style="padding-top: 100px; background: #f4f7f7; min-height: 100vh;">
      <div class="container py-4">
        <div class="p-4 rounded-4 shadow-sm bg-white" style="border: 1px solid rgba(0,0,0,0.06);">

          <!-- ===== HEADER ===== -->
          <div class="d-flex justify-content-between align-items-center mb-4 border-bottom pb-3">
            <div>
              <h2 class="fw-bold mb-0" style="color: #1e696a;">
                <i class="bi bi-prescription2 me-2"></i>My Prescriptions
              </h2>
              <p class="text-muted small mb-0">View your medical orders history</p>
            </div>
            <button class="btn btn-outline-secondary btn-sm rounded-pill px-3" (click)="goBack()">
              <i class="bi bi-arrow-left me-1"></i> Back to Profile
            </button>
          </div>

          <!-- ===== LOADING STATE ===== -->
          <div *ngIf="isLoading" class="text-center py-5">
            <div class="spinner-border" role="status"
                 style="color: #1e696a; width: 3rem; height: 3rem;"></div>
            <p class="mt-3 text-muted fw-medium">Loading your prescriptions...</p>
          </div>

          <!-- ===== ERROR STATE ===== -->
          <div *ngIf="!isLoading && hasError" class="text-center py-5">
            <i class="bi bi-exclamation-circle text-danger" style="font-size: 3rem;"></i>
            <h5 class="mt-3 fw-bold text-dark">Unable to load prescriptions</h5>
            <p class="text-muted small">
              Could not connect to the prescription server.<br>
              Please make sure the service is running and try again.
            </p>
            <button class="btn btn-sm rounded-pill px-4 mt-2"
                    style="background:#e0f2f1; color:#1e696a; border:1px solid #b2dfdb; font-weight:600;"
                    (click)="reload()">
              <i class="bi bi-arrow-clockwise me-1"></i> Retry
            </button>
          </div>

          <!-- ===== NO PRESCRIPTIONS STATE ===== -->
          <div *ngIf="!isLoading && !hasError && prescriptions.length === 0" class="text-center py-5">
            <i class="bi bi-file-medical-fill opacity-25"
               style="font-size: 4rem; color: #1e696a;"></i>
            <h4 class="mt-4 fw-bold text-dark">No Prescriptions Yet</h4>
            <p class="text-muted" style="max-width: 420px; margin: 0 auto;">
              You don't have any medical prescriptions on file.
              Your prescriptions will appear here once a doctor issues an order for you.
            </p>
          </div>

          <!-- ===== PRESCRIPTIONS TABLE ===== -->
          <div *ngIf="!isLoading && !hasError && prescriptions.length > 0" class="table-responsive">
            <table class="table table-hover align-middle mb-0">
              <thead style="background: #f0faf9;">
                <tr>
                  <th class="border-0 ps-3 text-uppercase fw-bold small"
                      style="color:#1e696a; letter-spacing:.05em;">Prescription ID</th>
                  <th class="border-0 text-uppercase fw-bold small"
                      style="color:#1e696a; letter-spacing:.05em;">Date Issued</th>
                  <th class="border-0 text-uppercase fw-bold small"
                      style="color:#1e696a; letter-spacing:.05em;">Signature Status</th>
                  <th class="border-0 text-uppercase fw-bold small"
                      style="color:#1e696a; letter-spacing:.05em;">Items</th>
                  <th class="border-0 text-uppercase fw-bold small text-end pe-3"
                      style="color:#1e696a; letter-spacing:.05em;">Action</th>
                </tr>
              </thead>
              <tbody>
                <tr *ngFor="let p of prescriptions" style="border-bottom: 1px solid #f0f0f0;">

                  <!-- ID -->
                  <td class="ps-3 fw-bold" style="color: #1e293b;">#PR-{{ p.id }}</td>

                  <!-- Date -->
                  <td class="text-muted small">{{ p.createdAt | date:'mediumDate' }}</td>

                  <!-- Signature Status -->
                  <td>
                    <ng-container *ngIf="p.doctorSignature; else notSigned">
                      <span class="badge rounded-pill px-3 py-2 d-inline-flex align-items-center gap-1"
                            style="background:#d1fae5; color:#065f46; font-size:.78rem;">
                        <i class="bi bi-patch-check-fill"></i>&nbsp;Signed by Doctor
                      </span>
                    </ng-container>
                    <ng-template #notSigned>
                      <span class="badge rounded-pill px-3 py-2 d-inline-flex align-items-center gap-1"
                            style="background:#fff7ed; color:#92400e; font-size:.78rem;">
                        <i class="bi bi-clock-history"></i>&nbsp;Awaiting Doctor's Signature
                      </span>
                    </ng-template>
                  </td>

                  <!-- Items count -->
                  <td>
                    <span class="badge rounded-pill px-2 py-1"
                          style="background:#e0f2f1; color:#1e696a;">
                      {{ p.prescriptionLines?.length || 0 }}
                      item{{ (p.prescriptionLines?.length || 0) !== 1 ? 's' : '' }}
                    </span>
                  </td>

                  <!-- Action -->
                  <td class="text-end pe-3">
                    <button class="btn btn-sm rounded-pill px-4 fw-semibold"
                            style="background:#e0f2f1; color:#1e696a; border:1px solid #b2dfdb; transition: all .2s;"
                            (click)="viewDetail(p.id)"
                            (mouseenter)="onBtnHover($event, true)"
                            (mouseleave)="onBtnHover($event, false)">
                      <i class="bi bi-eye me-1"></i> View Details
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>

            <!-- Summary footer bar -->
            <div class="mt-3 pt-2 border-top d-flex align-items-center gap-4 flex-wrap">
              <span class="small text-muted fw-medium">
                <i class="bi bi-list-check me-1"></i>
                {{ prescriptions.length }} prescription{{ prescriptions.length !== 1 ? 's' : '' }} total
              </span>
              <span class="small fw-medium" style="color:#065f46;">
                <i class="bi bi-patch-check-fill me-1"></i>
                {{ signedCount }} signed
              </span>
              <span class="small fw-medium" style="color:#92400e;">
                <i class="bi bi-clock-history me-1"></i>
                {{ prescriptions.length - signedCount }} awaiting signature
              </span>
            </div>
          </div>

        </div>
      </div>
    </main>
    <app-footer></app-footer>
  `,
  styles: []
})
export class PatientPrescriptions implements OnInit {
  prescriptions: any[] = [];
  isLoading = true;
  hasError = false;
  patientId: number = 0;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private prescriptionService: PrescriptionService,
    private cdr: ChangeDetectorRef
  ) {}

  /** Nombre de prescriptions signées par le médecin */
  get signedCount(): number {
    return this.prescriptions.filter(p => p.doctorSignature).length;
  }

  ngOnInit(): void {
    this.patientId = +(this.route.snapshot.paramMap.get('patientId') || 0);
    this.loadPrescriptions();
  }

  loadPrescriptions(): void {
    this.isLoading = true;
    this.hasError = false;
    this.cdr.detectChanges();

    if (!this.patientId) {
      this.isLoading = false;
      this.hasError = true;
      this.cdr.detectChanges();
      return;
    }

    this.prescriptionService.getMyHistory(this.patientId).subscribe({
      next: (data) => {
        this.prescriptions = data || [];
        this.isLoading = false;
        this.hasError = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('[PatientPrescriptions] Error loading prescriptions:', err);
        this.isLoading = false;
        this.hasError = true;
        this.cdr.detectChanges();
      }
    });
  }

  reload(): void {
    this.loadPrescriptions();
  }

  /** Hover effect for View Details button — safe TypeScript cast */
  onBtnHover(event: MouseEvent, isHover: boolean): void {
    const btn = event.currentTarget as HTMLElement;
    btn.style.background = isHover ? '#1e696a' : '#e0f2f1';
    btn.style.color = isHover ? 'white' : '#1e696a';
  }

  viewDetail(id: number): void {
    this.router.navigate(['/patient-prescription-detail', id]);
  }

  goBack(): void {
    this.router.navigate(['/patient-profile']);
  }
}
