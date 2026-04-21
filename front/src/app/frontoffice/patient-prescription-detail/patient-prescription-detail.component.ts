import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { PrescriptionService } from '../prescription.service';
import html2canvas from 'html2canvas';
import jsPDF from 'jspdf';

/**
 * Composant affichant le détail d'une prescription.
 * Si signée par le médecin → affichage style document médical + bouton Export PDF.
 */
@Component({
  selector: 'app-patient-prescription-detail',
  standalone: false,
  template: `
    <app-header></app-header>
    <main class="main" style="padding-top: 100px; background: #f4f7f7; min-height: 100vh;">
      <div class="container py-4" style="max-width: 900px;">

        <!-- Back button -->
        <div class="mb-3 d-flex justify-content-between align-items-center flex-wrap gap-2">
          <button class="btn btn-outline-secondary btn-sm rounded-pill px-3" (click)="goBack()">
            <i class="bi bi-arrow-left me-1"></i> Back to My Prescriptions
          </button>
          <!-- PDF export button — only when signed -->
          <button *ngIf="!isLoading && !loadError && prescription?.doctorSignature"
                  class="btn btn-sm rounded-pill px-4 fw-semibold d-flex align-items-center gap-2"
                  style="background: linear-gradient(135deg,#1e696a,#2a9d8f); color:white; border:none; box-shadow:0 4px 12px rgba(30,105,106,.3);"
                  (click)="exportToPdf()">
            <i class="bi bi-file-earmark-pdf-fill"></i>
            {{ isExporting ? 'Generating PDF...' : 'Download as PDF' }}
          </button>
        </div>

        <!-- LOADING STATE -->
        <div *ngIf="isLoading" class="text-center py-5">
          <div class="spinner-border" role="status"
               style="color: #1e696a; width: 3rem; height: 3rem;"></div>
          <p class="mt-3 text-muted fw-medium">Retrieving prescription details...</p>
        </div>

        <!-- ERROR STATE -->
        <div *ngIf="!isLoading && loadError"
             class="bg-white rounded-4 shadow-sm p-5 text-center">
          <i class="bi bi-exclamation-triangle-fill text-danger" style="font-size:3rem;"></i>
          <h5 class="mt-3 fw-bold text-dark">Prescription Not Found</h5>
          <p class="text-muted small">
            This prescription record could not be loaded.<br>
            It may not exist or the service is temporarily unavailable.
          </p>
          <button class="btn btn-sm rounded-pill px-4 mt-2"
                  style="background:#e0f2f1; color:#1e696a; border:1px solid #b2dfdb; font-weight:600;"
                  (click)="reload()">
            <i class="bi bi-arrow-clockwise me-1"></i> Retry
          </button>
        </div>

        <!-- ===== PRESCRIPTION DOCUMENT (styled like a medical document if signed) ===== -->
        <div *ngIf="!isLoading && !loadError && prescription" id="prescriptionDocument">

          <!-- ============================================================
               DOCUMENT MÉDICAL  (si signé → layout A4 style ordonnance)
               ============================================================ -->
          <div *ngIf="prescription.doctorSignature"
               class="bg-white shadow"
               style="border: 1px solid #e0e0e0; border-radius: 4px; max-width:840px; margin:0 auto; font-family: 'Georgia', serif;">

            <!-- Letterhead -->
            <div style="background: linear-gradient(135deg, #1e696a 0%, #2a9d8f 100%); padding: 28px 40px;">
              <div class="d-flex justify-content-between align-items-center">
                <div style="color:white;">
                  <h2 style="font-size:1.6rem; font-weight:700; margin:0; letter-spacing:1px;">MindCare</h2>
                  <p style="margin:4px 0 0; font-size:.85rem; opacity:.85;">Medical Prescription — Official Document</p>
                </div>
                <div style="color:white; text-align:right;">
                  <p style="margin:0; font-size:.82rem; opacity:.8;">Prescription No.</p>
                  <p style="margin:0; font-size:1.2rem; font-weight:700;">#PR-{{ prescription.id }}</p>
                </div>
              </div>
            </div>

            <!-- Date & status bar -->
            <div style="background:#f0faf9; padding:12px 40px; border-bottom:2px solid #b2dfdb; display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:8px;">
              <span style="font-size:.85rem; color:#555;">
                <i class="bi bi-calendar-event me-1" style="color:#1e696a;"></i>
                <strong>Date:</strong> {{ prescription.createdAt | date:'fullDate' }}
              </span>
              <span style="font-size:.85rem; background:#d1fae5; color:#065f46; padding:4px 14px; border-radius:20px; font-weight:600;">
                <i class="bi bi-patch-check-fill me-1"></i> Signed by Doctor
              </span>
            </div>

            <div style="padding: 32px 40px;">

              <!-- Patient & Consultation info -->
              <div style="display:grid; grid-template-columns:1fr 1fr; gap:24px; margin-bottom:28px; padding-bottom:20px; border-bottom:1px dashed #ccc;">
                <div>
                  <p style="font-size:.7rem; text-transform:uppercase; letter-spacing:.1em; color:#999; margin:0;">Patient ID</p>
                  <p style="font-size:1rem; font-weight:700; color:#1e293b; margin:4px 0 0;"># {{ prescription.patientId }}</p>
                </div>
                <div>
                  <p style="font-size:.7rem; text-transform:uppercase; letter-spacing:.1em; color:#999; margin:0;">Associated Consultation</p>
                  <p style="font-size:1rem; font-weight:700; color:#1e293b; margin:4px 0 0;">
                    {{ prescription.consultationId ? '#C-' + prescription.consultationId : 'N/A' }}
                  </p>
                </div>
              </div>

              <!-- Rx label -->
              <div style="margin-bottom:16px;">
                <span style="font-size:2.2rem; font-style:italic; color:#1e696a; font-weight:700; line-height:1;">℞</span>
                <h3 style="display:inline; font-size:1.05rem; font-weight:700; color:#1e293b; margin-left:8px; vertical-align:middle;">Prescribed Medications</h3>
              </div>

              <!-- No medications -->
              <div *ngIf="!prescription.prescriptionLines || prescription.prescriptionLines.length === 0"
                   style="padding:20px; text-align:center; color:#999; font-style:italic;">
                No medications listed for this prescription.
              </div>

              <!-- Medication rows -->
              <div *ngFor="let line of prescription.prescriptionLines; let i = index"
                   style="margin-bottom:16px; padding:16px 20px; border-radius:6px; background:#fafafa; border-left:4px solid #1e696a; border:1px solid #e8f5e9; border-left:4px solid #1e696a;">
                <div style="display:flex; justify-content:space-between; align-items:flex-start; flex-wrap:wrap; gap:8px;">
                  <div>
                    <p style="margin:0; font-size:1rem; font-weight:700; color:#1e293b;">
                      {{ i + 1 }}. {{ line.medicine?.commercialName || 'Unnamed medication' }}
                      <span style="font-weight:400; color:#666; font-size:.88rem;" *ngIf="line.medicine?.inn">
                        &nbsp;({{ line.medicine.inn }})
                      </span>
                    </p>
                    <p style="margin:4px 0 0; font-size:.82rem; color:#1e696a; font-weight:600;" *ngIf="line.medicine?.therapeuticFamily">
                      <i class="bi bi-folder2-open me-1"></i>{{ line.medicine.therapeuticFamily }}
                    </p>
                  </div>
                  <div style="text-align:center; min-width:80px; background:#e0f2f1; padding:6px 14px; border-radius:6px;">
                    <span style="display:block; font-size:.65rem; text-transform:uppercase; color:#555; font-weight:700; letter-spacing:.05em;">Dosage</span>
                    <span style="display:block; font-size:1rem; font-weight:700; color:#1e293b;">{{ line.dosage || '--' }}</span>
                  </div>
                </div>
                <div style="display:grid; grid-template-columns:1fr 1fr; gap:12px; margin-top:12px; padding-top:10px; border-top:1px dashed #ddd;">
                  <div>
                    <span style="font-size:.72rem; text-transform:uppercase; color:#999; font-weight:700; letter-spacing:.05em;">Start Date</span>
                    <p style="margin:2px 0 0; font-weight:600; color:#1e696a; font-size:.9rem;">
                      <i class="bi bi-play-circle-fill me-1"></i>{{ line.startDate || '--' }}
                    </p>
                  </div>
                  <div>
                    <span style="font-size:.72rem; text-transform:uppercase; color:#999; font-weight:700; letter-spacing:.05em;">End Date</span>
                    <p style="margin:2px 0 0; font-weight:600; color:#dc2626; font-size:.9rem;">
                      <i class="bi bi-stop-circle-fill me-1"></i>{{ line.endDate || '--' }}
                    </p>
                  </div>
                </div>
              </div>

              <!-- Doctor Signature section -->
              <div style="margin-top:36px; padding-top:20px; border-top:2px solid #e0e0e0; display:flex; justify-content:flex-end;">
                <div style="text-align:center; min-width:220px;">
                  <img [src]="prescription.doctorSignature"
                       alt="Doctor signature"
                       style="max-height:80px; max-width:200px; object-fit:contain; display:block; margin:0 auto 8px;" />
                  <div style="border-top:2px solid #1e696a; padding-top:6px;">
                    <p style="margin:0; font-size:.78rem; color:#555; font-weight:600;">Authorized Medical Signature</p>
                    <p style="margin:4px 0 0; font-size:.72rem; color:#999;">MindCare Medical Center</p>
                  </div>
                </div>
              </div>

              <!-- Footer note -->
              <div style="margin-top:28px; padding:10px 16px; background:#fff8f0; border-radius:6px; border:1px solid #fde8c8;">
                <p style="margin:0; font-size:.78rem; color:#92400e;">
                  <i class="bi bi-shield-exclamation me-1"></i>
                  <strong>Important:</strong> This prescription is strictly personal. Please follow the prescribed dosages exactly.
                  In case of unusual side effects, consult your doctor immediately.
                </p>
              </div>

            </div>
          </div>

          <!-- ============================================================
               VUE STANDARD (si non signé → affichage normal)
               ============================================================ -->
          <div *ngIf="!prescription.doctorSignature"
               class="bg-white rounded-4 shadow-sm overflow-hidden"
               style="border:1px solid rgba(0,0,0,0.06);">

            <!-- Header -->
            <div class="px-4 py-4 border-bottom" style="background:#f0faf9;">
              <div class="d-flex justify-content-between align-items-start flex-wrap gap-3">
                <div>
                  <h1 class="fw-bold mb-1" style="color:#1e696a; font-size:1.4rem;">
                    <i class="bi bi-card-checklist me-2"></i>Prescription Detail
                  </h1>
                  <p class="text-muted small mb-0">Review your medical instructions</p>
                </div>
                <div class="text-end">
                  <p class="mb-1 fw-bold text-dark fs-5">#PR-{{ prescription.id }}</p>
                  <span class="badge rounded-pill px-3 py-2 d-inline-flex align-items-center gap-1"
                        style="background:#fff7ed; color:#92400e;">
                    <i class="bi bi-clock-history"></i>&nbsp;Awaiting Doctor's Signature
                  </span>
                </div>
              </div>
            </div>

            <div class="p-4 p-md-5">
              <!-- Date & Consultation -->
              <div class="row g-4 mb-4">
                <div class="col-md-6">
                  <p class="text-uppercase fw-bold small text-muted mb-1" style="letter-spacing:.05em;">Prescribed On</p>
                  <p class="fw-bold text-dark">
                    <i class="bi bi-calendar-event me-2" style="color:#1e696a;"></i>
                    {{ prescription.createdAt | date:'fullDate' }}
                  </p>
                </div>
                <div class="col-md-6">
                  <p class="text-uppercase fw-bold small text-muted mb-1" style="letter-spacing:.05em;">Associated Consultation</p>
                  <p class="fw-bold text-dark">
                    <i class="bi bi-person-badge me-2" style="color:#1e696a;"></i>
                    {{ prescription.consultationId ? '#C-' + prescription.consultationId : 'N/A' }}
                  </p>
                </div>
              </div>

              <!-- Notice -->
              <div class="rounded-3 p-3 mb-4" style="background:#fff7ed; border:1px solid #fde8c8;">
                <p class="small fw-bold mb-1" style="color:#92400e;">
                  <i class="bi bi-clock-history me-1"></i> Pending Signature
                </p>
                <p class="small mb-0 text-muted">
                  This prescription has not yet been signed by your doctor. The content may still be subject to changes. You will be able to download it as a PDF once signed.
                </p>
              </div>

              <!-- Medications -->
              <h3 class="fw-bold mb-4 border-bottom pb-2" style="font-size:1.05rem; color:#1e696a;">
                <i class="bi bi-capsule-pill me-2"></i>
                Prescribed Medications
                <span class="badge rounded-pill ms-2 align-middle"
                      style="background:#e0f2f1; color:#1e696a; font-size:.75rem; font-weight:600;">
                  {{ prescription.prescriptionLines?.length || 0 }} item{{ (prescription.prescriptionLines?.length || 0) !== 1 ? 's':'' }}
                </span>
              </h3>

              <div *ngFor="let line of prescription.prescriptionLines; let i = index"
                   class="mb-4 p-4 rounded-4 bg-white"
                   style="border-left:5px solid #1e696a; border:1px solid #e0f2f1; border-left:5px solid #1e696a; box-shadow:0 2px 12px rgba(30,105,106,.06);">
                <div class="d-flex justify-content-between align-items-start flex-wrap gap-2">
                  <div>
                    <h4 class="mb-1 fw-bold" style="color:#1e293b; font-size:1.05rem;">
                      {{ line.medicine?.commercialName || 'Unnamed medication' }}
                      <span class="text-muted fw-normal fs-6 ms-1" *ngIf="line.medicine?.inn">({{ line.medicine.inn }})</span>
                    </h4>
                    <p class="small fw-bold mb-0" style="color:#1e696a;" *ngIf="line.medicine?.therapeuticFamily">
                      <i class="bi bi-folder2-open me-1"></i>{{ line.medicine.therapeuticFamily }}
                    </p>
                  </div>
                  <div class="text-center px-3 py-2 rounded-3" style="background:#f0faf9; border:1px solid #b2dfdb; min-width:90px;">
                    <span class="d-block small text-muted fw-bold" style="font-size:.7rem; letter-spacing:.05em;">DOSAGE</span>
                    <span class="d-block fw-bold fs-5 text-dark">{{ line.dosage || '--' }}</span>
                  </div>
                </div>
                <div class="row mt-3 border-top pt-3">
                  <div class="col-6">
                    <p class="small text-muted mb-1 fw-bold">Start Date</p>
                    <p class="mb-0 fw-bold" style="color:#1e696a;">
                      <i class="bi bi-play-circle-fill me-1"></i>{{ line.startDate || '--' }}
                    </p>
                  </div>
                  <div class="col-6">
                    <p class="small text-muted mb-1 fw-bold">End Date</p>
                    <p class="mb-0 fw-bold text-danger">
                      <i class="bi bi-stop-circle-fill me-1"></i>{{ line.endDate || '--' }}
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>

        </div>
      </div>
    </main>
    <app-footer></app-footer>
  `,
  styles: []
})
export class PatientPrescriptionDetail implements OnInit {
  prescription: any = null;
  isLoading = true;
  loadError = false;
  isExporting = false;
  private prescriptionId = 0;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private prescriptionService: PrescriptionService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.prescriptionId = +(this.route.snapshot.paramMap.get('id') || 0);
    this.loadPrescription();
  }

  loadPrescription(): void {
    this.isLoading = true;
    this.loadError = false;
    this.cdr.detectChanges();

    if (!this.prescriptionId) {
      this.isLoading = false;
      this.loadError = true;
      this.cdr.detectChanges();
      return;
    }

    this.prescriptionService.getPrescriptionById(this.prescriptionId).subscribe({
      next: (data) => {
        this.prescription = data;
        this.isLoading = false;
        this.loadError = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('[PatientPrescriptionDetail] Error:', err);
        this.isLoading = false;
        this.loadError = true;
        this.cdr.detectChanges();
      }
    });
  }

  reload(): void {
    this.loadPrescription();
  }

  /**
   * Exporte le document de prescription en PDF via html2canvas + jsPDF.
   * Capture le rendu HTML du document médical et le convertit en PDF A4.
   */
  async exportToPdf(): Promise<void> {
    if (this.isExporting) return;
    this.isExporting = true;
    this.cdr.detectChanges();

    try {
      const element = document.getElementById('prescriptionDocument');
      if (!element) {
        this.isExporting = false;
        this.cdr.detectChanges();
        return;
      }

      const canvas = await html2canvas(element, {
        scale: 2,           // haute résolution
        useCORS: true,      // autorise les images externes (signature)
        backgroundColor: '#ffffff',
        logging: false,
      });

      const imgData = canvas.toDataURL('image/jpeg', 0.95);
      const pdf = new jsPDF({
        orientation: 'portrait',
        unit: 'mm',
        format: 'a4',
      });

      const pageWidth = pdf.internal.pageSize.getWidth();   // 210mm
      const pageHeight = pdf.internal.pageSize.getHeight(); // 297mm
      const imgWidth = pageWidth;
      const imgHeight = (canvas.height * pageWidth) / canvas.width;

      let heightLeft = imgHeight;
      let position = 0;

      // Première page
      pdf.addImage(imgData, 'JPEG', 0, position, imgWidth, imgHeight);
      heightLeft -= pageHeight;

      // Pages supplémentaires si le contenu dépasse une page A4
      while (heightLeft > 0) {
        position = heightLeft - imgHeight;
        pdf.addPage();
        pdf.addImage(imgData, 'JPEG', 0, position, imgWidth, imgHeight);
        heightLeft -= pageHeight;
      }

      pdf.save(`Prescription_PR-${this.prescription.id}_MindCare.pdf`);
    } catch (err) {
      console.error('[PatientPrescriptionDetail] PDF export error:', err);
    } finally {
      this.isExporting = false;
      this.cdr.detectChanges();
    }
  }

  goBack(): void {
    if (this.prescription?.patientId) {
      this.router.navigate(['/patient-prescriptions', this.prescription.patientId]);
    } else {
      window.history.back();
    }
  }
}
