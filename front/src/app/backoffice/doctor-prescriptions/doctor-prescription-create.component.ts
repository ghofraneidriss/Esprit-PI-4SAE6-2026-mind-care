import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { DoctorPrescriptionService } from './doctor-prescription.service';
import { FormArray, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthService } from '../../frontoffice/auth/auth.service';
import { PatientProfileService } from '../../frontoffice/patient-profile/patient-profile.service';

/**
 * Composant de création d'ordonnance par le médecin.
 * Vérifie en temps réel les allergies du patient lors de la sélection d'un médicament.
 */
@Component({
  selector: 'app-doctor-prescription-create',
  standalone: false,
  template: `
    <!-- SIDEBAR -->
    <nav class="floating-sidebar d-flex flex-column align-items-center py-4 position-fixed z-3 h-100 bg-white shadow-sm"
        style="width: 80px; top: 0; left: 0; gap: 1.5rem; border-right: 1px solid rgba(0,0,0,0.05);">
        <a routerLink="/admin" class="mb-4 d-flex justify-content-center align-items-center rounded-circle shadow-sm"
            style="width: 45px; height: 45px; background: #2D9A9B; color: white; text-decoration: none;">
            <i class="bi bi-grid-3x3-gap-fill fs-5"></i>
        </a>
        <a routerLink="/admin" class="nav-icon-link d-flex justify-content-center align-items-center rounded-4 text-decoration-none" style="width: 48px; height: 48px; color: #64748b; font-size: 1.3rem;">
            <i class="bi bi-house"></i>
        </a>
        <a routerLink="/admin/appointments" class="nav-icon-link d-flex justify-content-center align-items-center rounded-4 text-decoration-none" style="width: 48px; height: 48px; color: #64748b; font-size: 1.3rem;">
            <i class="bi bi-calendar-check-fill"></i>
        </a>
        <a routerLink="/admin/consultations" class="nav-icon-link d-flex justify-content-center align-items-center rounded-4 text-decoration-none shadow-sm"
           style="width: 48px; height: 48px; background: #e0f2f1; border: 1px solid #2D9A9B; color: #2D9A9B; font-size: 1.3rem;">
            <i class="bi bi-clipboard-plus-fill"></i>
        </a>
        <a routerLink="/admin/patient-follow-up" class="nav-icon-link d-flex justify-content-center align-items-center rounded-4 text-decoration-none" style="width: 48px; height: 48px; color: #64748b; font-size: 1.3rem;">
            <i class="bi bi-heart-pulse-fill"></i>
        </a>
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
        <a routerLink="/auth/login" class="nav-icon-link d-flex justify-content-center align-items-center text-decoration-none mt-auto" style="width: 48px; height: 48px; color: #475569; font-size: 1.4rem;">
            <i class="bi bi-box-arrow-left"></i>
        </a>
    </nav>

    <div style="margin-left: 80px; background: #F4F7F7; min-height: 100vh; padding: 20px;">

        <!-- Header -->
        <div class="d-flex align-items-center mb-4" style="background: #2D9A9B; border-radius: 16px; padding: 25px; color: white;">
            <div class="me-3 bg-white d-flex justify-content-center align-items-center"
                 style="color: #2D9A9B; width: 60px; height: 60px; border-radius: 12px; flex-shrink:0;">
                <i class="bi bi-prescription fs-2"></i>
            </div>
            <div>
                <h3 class="fw-bold mb-0">New Medical Prescription</h3>
                <p class="mb-0 opacity-75 small">Patient: <strong>{{ patientName || 'Loading...' }}</strong> — Consultation #{{ consultationId }}</p>
            </div>
        </div>

        <!-- Patient allergies info banner (si allergies connues) -->
        <div *ngIf="patientAllergies.length > 0"
             class="d-flex align-items-start gap-3 rounded-3 p-3 mb-4"
             style="background:#fffbeb; border:1px solid #fbbf24; border-left:5px solid #f59e0b;">
          <i class="bi bi-shield-fill-exclamation fs-4" style="color:#d97706; flex-shrink:0;"></i>
          <div>
            <p class="fw-bold mb-1" style="color:#92400e; font-size:.92rem;">
              Known Patient Allergies
            </p>
            <div class="d-flex flex-wrap gap-2">
              <span *ngFor="let a of patientAllergies"
                    class="badge rounded-pill px-3 py-2"
                    style="background:#fef3c7; color:#92400e; font-size:.82rem; border: 1px solid #fbbf24;">
                {{ a }}
              </span>
            </div>
          </div>
        </div>

        <!-- Draft -->
        <div *ngIf="hasDraft" class="alert py-2 mb-3 border-0 shadow-sm d-flex justify-content-between align-items-center"
             style="background: #e0f2f1; color: #00796b; border-radius: 12px; font-weight: 500;">
            <div><i class="bi bi-pencil-square me-2"></i> Draft restored! You can continue your work.</div>
            <button class="btn btn-sm btn-outline-danger border-0" (click)="discardDraft()"><i class="bi bi-trash me-1"></i> Discard</button>
        </div>

        <!-- Form card -->
        <div class="p-4 bg-white rounded-4 shadow-sm border">
            <form [formGroup]="rxForm" (ngSubmit)="saveRx()">

                <div class="row g-4 mb-4 pb-4 border-bottom">
                    <div class="col-md-6">
                        <label class="form-label text-muted fw-bold small text-uppercase">Patient Name</label>
                        <input type="text" class="form-control bg-light fw-bold" style="color:#2D9A9B;" [value]="patientName" readonly>
                    </div>
                    <div class="col-md-6">
                        <label class="form-label text-muted fw-bold small text-uppercase">Consultation ID</label>
                        <input type="number" class="form-control bg-light" formControlName="consultationId" readonly>
                    </div>
                </div>

                <!-- Medications header -->
                <div class="d-flex justify-content-between align-items-center mb-4">
                    <div>
                        <h5 class="fw-bold mb-0 text-dark"><i class="bi bi-capsule-pill me-2" style="color:#2D9A9B;"></i>Medication List</h5>
                        <p class="text-muted small mb-0 mt-1">
                            Didn't find a medicine?
                            <a href="javascript:void(0)" (click)="goToManageMedications()" style="color:#2D9A9B;" class="text-decoration-underline fw-bold">Add it here</a>
                        </p>
                    </div>
                    <button type="button" class="btn btn-sm rounded-pill fw-bold px-3"
                            style="border:1px solid #2D9A9B; color:#2D9A9B;" (click)="addLine()">
                        <i class="bi bi-plus-circle me-1"></i> Add Line
                    </button>
                </div>

                <!-- Prescription lines -->
                <div formArrayName="prescriptionLines" class="mb-4">
                    <div *ngFor="let line of lines.controls; let i = index" [formGroupName]="i" class="mb-4">
                        <!-- 🔴 OVERLAP WARNING for this line -->
                        <div *ngIf="overlapWarnings[i] && overlapWarnings[i].length > 0"
                             class="d-flex align-items-start gap-3 rounded-3 p-3 mb-2"
                             style="background:#fdf2f8; border:1px solid #db2777; border-left:5px solid #be185d;">
                          <i class="bi bi-file-earmark-x-fill fs-4" style="color:#be185d; margin-top:2px; flex-shrink:0;"></i>
                          <div style="width:100%;">
                            <p class="fw-bold mb-2" style="color:#9d174d; font-size:.92rem;">
                              🔴 DUPLICATE PRESCRIPTION DETECTED
                            </p>
                            <div *ngFor="let conflict of overlapWarnings[i]" class="mb-2 p-2 rounded-2"
                                 style="background:#fff0f6; border:1px solid #f9a8d4;">
                              <div class="d-flex justify-content-between align-items-start">
                                <div>
                                  <span class="fw-bold" style="color:#be185d;">{{ conflict.medicineName }}</span>
                                  <span class="text-muted small ms-2">({{ conflict.medicineInn }})</span>
                                </div>
                                <span class="badge rounded-pill px-3" style="background:#fce7f3; color:#9d174d; font-size:.78rem;">
                                  Prescription #{{ conflict.conflictingPrescriptionId }}
                                </span>
                              </div>
                              <p class="mb-0 mt-1 small" style="color:#9d174d;">
                                <i class="bi bi-calendar-range me-1"></i>
                                <strong>{{ conflict.conflictStartDate }}</strong> → <strong>{{ conflict.conflictEndDate }}</strong>
                                &nbsp;|&nbsp; Dosage: <strong>{{ conflict.conflictDosage }}</strong>
                                &nbsp;|&nbsp; Status: <strong>{{ conflict.prescriptionStatus }}</strong>
                              </p>
                            </div>
                            <p class="text-muted small mb-0 mt-1">
                              This medication is already prescribed for this patient during the same period in another prescription.
                              Please verify before proceeding.
                            </p>
                          </div>
                        </div>

                        <!-- 🚨 DOCTOR SHOPPING WARNING for this line --
                             Alerte si le patient a déjà une prescription active pour ce médicament
                             prescrite par un autre médecin et dont la date de fin n'est pas encore passée. -->
                        <div *ngIf="doctorShoppingWarnings[i] && doctorShoppingWarnings[i].length > 0"
                             class="d-flex align-items-start gap-3 rounded-3 p-3 mb-2"
                             style="background:#fef2f2; border:1px solid #ef4444; border-left:5px solid #dc2626;">
                          <i class="bi bi-shield-exclamation fs-3" style="color:#dc2626; margin-top:2px; flex-shrink:0;"></i>
                          <div style="width:100%;">
                            <p class="fw-bold mb-2" style="color:#991b1b; font-size:.95rem;">
                              🚨 DOCTOR SHOPPING ALERT — Active prescription exists from another doctor
                            </p>
                            <div *ngFor="let alert of doctorShoppingWarnings[i]" class="mb-2 p-2 rounded-2"
                                 style="background:#fff5f5; border:1px solid #fca5a5;">
                              <div class="d-flex justify-content-between align-items-start flex-wrap gap-2">
                                <div>
                                  <span class="fw-bold text-danger">{{ alert.medicineName }}</span>
                                  <span class="text-muted small ms-2">({{ alert.medicineInn }})</span>
                                </div>
                                <span class="badge rounded-pill px-3 py-2" style="background:#fee2e2; color:#991b1b; font-size:.78rem; border:1px solid #fca5a5;">
                                  Prescription #{{ alert.existingPrescriptionId }} — Dr. ID #{{ alert.prescribingDoctorId }}
                                </span>
                              </div>
                              <p class="mb-0 mt-2 small" style="color:#991b1b;">
                                <i class="bi bi-calendar-x me-1"></i>
                                Current active period: <strong>{{ alert.activeStartDate }}</strong> → <strong>{{ alert.activeEndDate }}</strong>
                                &nbsp;|&nbsp; Dosage: <strong>{{ alert.existingDosage }}</strong>
                                &nbsp;|&nbsp; Status: <strong>{{ alert.existingStatus }}</strong>
                              </p>
                            </div>
                            <p class="text-muted small mb-0 mt-2">
                              This patient already has an active prescription for the same medication from a different physician.
                              The current treatment period has NOT yet ended. Please review carefully before issuing a new prescription.
                            </p>
                          </div>
                        </div>

                        <div *ngIf="allergyWarnings[i] && allergyWarnings[i].length > 0"
                             class="d-flex align-items-start gap-3 rounded-3 p-3 mb-2"
                             style="background:#fff7ed; border:1px solid #fb923c; border-left:5px solid #ea580c;">
                          <i class="bi bi-exclamation-triangle-fill fs-4" style="color:#ea580c; margin-top:2px; flex-shrink:0;"></i>
                          <div>
                            <p class="fw-bold mb-1" style="color:#c2410c; font-size:.92rem;">
                              ⚠ ALLERGY ALERT — This patient is allergic to:
                            </p>
                            <div class="d-flex flex-wrap gap-2">
                              <span *ngFor="let a of allergyWarnings[i]"
                                    class="badge rounded-pill px-3 py-2"
                                    style="background:#fee2e2; color:#991b1b; font-size:.82rem;">
                                <i class="bi bi-x-octagon-fill me-1"></i>{{ a }}
                              </span>
                            </div>
                            <p class="text-muted small mb-0 mt-2">
                              Please reconsider this medication or document a clinical justification before proceeding.
                            </p>
                          </div>
                        </div>

                        <!-- Line card -->
                        <div class="p-3 rounded-3 border bg-light"
                             [style.border-left]="(allergyWarnings[i] && allergyWarnings[i].length > 0) ? '4px solid #ea580c' : '4px solid #2D9A9B'">
                            <div class="row g-3">
                                <div class="col-md-4">
                                    <label class="form-label small fw-bold">Select Medicine</label>
                                    <select class="form-select" formControlName="medicineId"
                                            (change)="onMedicineChange(i, $event)">
                                        <option value="" disabled>Choose medicine...</option>
                                        <option *ngFor="let med of medicines" [value]="med.id">
                                            {{ med.commercialName }} ({{ med.inn }})
                                        </option>
                                    </select>
                                </div>
                                <div class="col-md-2">
                                    <label class="form-label small fw-bold">Dosage</label>
                                    <input type="text" class="form-control" formControlName="dosage" placeholder="e.g. 1 tab/day">
                                </div>
                                <div class="col-md-2">
                                    <label class="form-label small fw-bold">Start Date</label>
                                    <input type="date" class="form-control" formControlName="startDate"
                                           [min]="today"
                                           (change)="onDateChange(i)">
                                </div>
                                <div class="col-md-2">
                                    <label class="form-label small fw-bold">End Date</label>
                                    <input type="date" class="form-control" formControlName="endDate"
                                           [min]="today"
                                           (change)="onDateChange(i)">
                                </div>
                                <div class="col-md-1 d-flex align-items-end justify-content-center">
                                    <button type="button" class="btn btn-outline-danger btn-sm rounded-circle" (click)="removeLine(i)">
                                        <i class="bi bi-dash"></i>
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Global allergy summary banner -->
                <div *ngIf="hasAnyAllergyWarning()"
                     class="d-flex align-items-center gap-3 rounded-3 p-3 mb-4"
                     style="background:#fef9c3; border:1px solid #eab308;">
                  <i class="bi bi-shield-exclamation fs-5" style="color:#a16207;"></i>
                  <p class="mb-0 small fw-medium" style="color:#a16207;">
                    <strong>One or more allergy conflicts detected.</strong>
                    You can still proceed, but please verify with the patient's clinical record.
                  </p>
                </div>

                <!-- Global doctor shopping summary banner
                     Affiché si au moins une ligne déclenche une alerte de doctor shopping. -->
                <div *ngIf="hasAnyDoctorShoppingWarning()"
                     class="d-flex align-items-center gap-3 rounded-3 p-3 mb-4"
                     style="background:#fff1f2; border:1px solid #ef4444;">
                  <i class="bi bi-person-exclamation fs-5 text-danger"></i>
                  <p class="mb-0 small fw-medium text-danger">
                    <strong>⚠ Doctor Shopping Detected.</strong>
                    This patient is currently receiving the same medication from another physician.
                    Review all prescriptions before proceeding.
                  </p>
                </div>

                <div *ngIf="errorMessage" class="alert alert-danger mb-4">
                    <i class="bi bi-exclamation-octagon-fill me-2"></i> {{ errorMessage }}
                </div>

                <div class="d-flex justify-content-end gap-3 pt-4 border-top">
                    <button type="button" class="btn btn-outline-secondary px-5 fw-bold" (click)="goBack()">Cancel</button>
                    <button type="submit" class="btn px-5 py-2 fw-bold shadow text-white" style="background:#2D9A9B; border:none;"
                            [disabled]="rxForm.invalid || isSaving">
                        <span *ngIf="isSaving" class="spinner-border spinner-border-sm me-2"></span>
                        <i class="bi bi-check2-circle me-1"></i> {{ isSaving ? 'Issuing...' : 'Issue Prescription' }}
                    </button>
                </div>
            </form>
        </div>
    </div>
  `,
  styles: [`
    .nav-icon-link { color: #64748b !important; transition: all 0.2s ease; }
    .nav-icon-link:visited { color: #64748b !important; }
    .nav-icon-link:focus { color: #64748b !important; outline: none; }
    .nav-icon-link:active { color: #64748b !important; }
    .nav-icon-link:hover { background: #f8fafc; color: #1e293b !important; transform: translateY(-2px); }
    .active-nav-link, .active-nav-link:visited, .active-nav-link:focus, .active-nav-link:active { color: #2D9A9B !important; }
  `]
})
export class DoctorPrescriptionCreate implements OnInit {
  rxForm: FormGroup;
  medicines: any[] = [];
  consultationId = 0;
  patientId = 0;
  patientName = '';
  isSaving = false;
  errorMessage = '';
  hasDraft = false;

  /**
   * Date du jour au format yyyy-MM-dd.
   * Utilisée comme valeur minimale pour les champs de date (startDate et endDate)
   * afin d'empêcher le médecin de choisir une date passée.
   */
  today: string = new Date().toISOString().substring(0, 10);

  /** Allergies du patient chargées depuis son profil */
  patientAllergies: string[] = [];
  /** Alertes d'allergie par index de ligne [lineIndex → liste d'allergies matching] */
  allergyWarnings: string[][] = [];
  /** Alertes de chevauchement de prescription par index de ligne */
  overlapWarnings: any[][] = [];
  /**
   * Alertes de "doctor shopping" par index de ligne.
   * Déclenché si le patient a déjà une prescription active pour ce médicament
   * chez un autre médecin (endDate >= aujourd'hui).
   */
  doctorShoppingWarnings: any[][] = [];
  /** ID du médecin connecté, utilisé pour la détection du doctor shopping */
  currentDoctorId: number = 0;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private rxService: DoctorPrescriptionService,
    private authService: AuthService,
    private profileService: PatientProfileService,
    private cdr: ChangeDetectorRef
  ) {
    this.rxForm = this.fb.group({
      consultationId: ['', Validators.required],
      patientId: ['', Validators.required],
      status: ['PENDING'],
      prescriptionLines: this.fb.array([])
    });
  }

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      this.consultationId = +(params['consultationId'] || 0);
      this.patientId = +(params['patientId'] || 0);
      this.patientName = params['patientName'] || '';

      this.rxForm.patchValue({
        consultationId: this.consultationId,
        patientId: this.patientId
      });

      if (!this.patientName && this.patientId) {
        this.loadPatientName();
      }

      // Charger les allergies du patient dès le démarrage
      if (this.patientId) {
        this.loadPatientAllergies();
      }

      // Récupérer l'ID du médecin connecté pour la détection du doctor shopping
      const currentUser = this.authService.getLoggedUser();
      this.currentDoctorId = currentUser?.userId || 0;

      const draft = this.rxService.getDraft();
      if (draft && draft.patientId === this.patientId) {
        this.restoreDraft(draft);
      } else {
        this.addLine();
      }

      this.cdr.detectChanges();
    });

    this.loadMedicines();
  }

  /**
   * Charge les allergies du patient depuis son profil (traitement_et_consultation:8081).
   * Les allergies sont ensuite comparées localement à chaque sélection de médicament.
   */
  loadPatientAllergies(): void {
    this.profileService.getProfileByUserId(this.patientId).subscribe({
      next: (profile) => {
        this.patientAllergies = profile?.allergies || [];
        console.log('[AllergyCheck] Allergies loaded for patient', this.patientId, ':', this.patientAllergies);
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.warn('[AllergyCheck] Could not load patient profile:', err.status, err.message);
        this.patientAllergies = [];
      }
    });
  }

  loadPatientName(): void {
    if (!this.patientId) return;
    this.authService.getUserById(this.patientId).subscribe({
      next: (user) => {
        this.patientName = `${user.firstName} ${user.lastName}`;
        this.cdr.detectChanges();
      }
    });
  }

  restoreDraft(draft: any): void {
    this.hasDraft = true;
    this.rxForm.patchValue({ status: draft.status || 'PENDING' });
    while (this.lines.length !== 0) this.lines.removeAt(0);
    this.allergyWarnings = [];

    (draft.lines || []).forEach((l: any) => {
      this.lines.push(this.fb.group({
        medicineId: [l.medicineId, Validators.required],
        dosage: [l.dosage, Validators.required],
        startDate: [l.startDate, Validators.required],
        endDate: [l.endDate, Validators.required]
      }));
      this.allergyWarnings.push([]);
    });
  }

  discardDraft(): void {
    this.rxService.clearDraft();
    this.hasDraft = false;
    while (this.lines.length !== 0) this.lines.removeAt(0);
    this.allergyWarnings = [];
    this.addLine();
  }

  goToManageMedications(): void {
    this.rxService.saveDraft({
      patientId: this.patientId,
      patientName: this.patientName,
      consultationId: this.consultationId,
      status: this.rxForm.value.status,
      lines: this.rxForm.value.prescriptionLines
    });
    this.router.navigate(['/admin/medications']);
  }

  get lines(): FormArray {
    return this.rxForm.get('prescriptionLines') as FormArray;
  }

  loadMedicines(): void {
    this.rxService.getAllMedicines().subscribe({
      next: (data) => {
        this.medicines = data;
        this.cdr.detectChanges();
      }
    });
  }

  addLine(): void {
    const today = new Date().toISOString().substring(0, 10);
    this.lines.push(this.fb.group({
      medicineId: ['', Validators.required],
      dosage: ['', Validators.required],
      startDate: [today, Validators.required],
      endDate: ['', Validators.required]
    }));
    this.allergyWarnings.push([]);
    this.overlapWarnings.push([]);
    this.doctorShoppingWarnings.push([]);
  }

  removeLine(index: number): void {
    if (this.lines.length > 1) {
      this.lines.removeAt(index);
      this.allergyWarnings.splice(index, 1);
      this.overlapWarnings.splice(index, 1);
      this.doctorShoppingWarnings.splice(index, 1);
    }
  }

  /**
   * Vérifie les allergies du patient contre le médicament sélectionné.
   * Comparaison locale (frontend) insensible à la casse :
   *   - Nom commercial du médicament
   *   - DCI (INN)
   *   - Famille thérapeutique
   */
  onMedicineChange(lineIndex: number, event: Event): void {
    const medicineId = +((event.target as HTMLSelectElement).value);
    this.allergyWarnings[lineIndex] = [];
    this.overlapWarnings[lineIndex] = [];
    this.doctorShoppingWarnings[lineIndex] = [];

    if (!medicineId) { this.cdr.detectChanges(); return; }
    const med = this.medicines.find(m => m.id === medicineId);
    if (!med) { this.cdr.detectChanges(); return; }

    // --- Vérification des allergies (côté client) ---
    const medName   = (med.commercialName || '').toLowerCase().trim();
    const medInn    = (med.inn || '').toLowerCase().trim();
    const medFamily = (med.therapeuticFamily || '').toLowerCase().trim();
    this.allergyWarnings[lineIndex] = this.patientAllergies.filter(allergy => {
      const a = allergy.toLowerCase().trim();
      if (!a) return false;
      return (
        (medName   && (medName.includes(a)   || a.includes(medName)))   ||
        (medInn    && (medInn.includes(a)    || a.includes(medInn)))    ||
        (medFamily && (medFamily.includes(a) || a.includes(medFamily)))
      );
    });

    // --- Vérification de chevauchement (JPQL backend) — déclenchée si les dates sont déjà remplies ---
    this.triggerOverlapCheck(lineIndex);

    // --- Détection du doctor shopping (JPQL backend) ---
    // Vérifie si ce patient a une prescription active pour ce médicament chez un autre médecin
    this.triggerDoctorShoppingCheck(lineIndex, medicineId);

    this.cdr.detectChanges();
  }

  /**
   * Déclenché quand la date de début ou de fin change sur une ligne.
   * Re-vérifie le chevauchement si un médicament est déjà sélectionné.
   */
  onDateChange(lineIndex: number): void {
    this.triggerOverlapCheck(lineIndex);
  }

  /**
   * Appelle le backend (JPQL) pour détecter les chevauchements.
   * Se déclenche uniquement si medicineId, startDate et endDate sont remplis.
   */
  triggerOverlapCheck(lineIndex: number): void {
    const line = this.lines.at(lineIndex)?.value;
    if (!line || !line.medicineId || !line.startDate || !line.endDate) return;
    if (!this.patientId) return;

    this.rxService.checkMedicineOverlap(
      this.patientId,
      +line.medicineId,
      line.startDate,
      line.endDate,
      0 // 0 = nouvelle prescription
    ).subscribe({
      next: (conflicts: any[]) => {
        this.overlapWarnings[lineIndex] = conflicts;
        this.cdr.detectChanges();
        if (conflicts.length > 0) {
          console.warn(`[OverlapCheck] Line ${lineIndex}: ${conflicts.length} conflict(s) found`, conflicts);
        }
      },
      error: () => {
        this.overlapWarnings[lineIndex] = [];
      }
    });
  }

  hasAnyAllergyWarning(): boolean {
    return this.allergyWarnings.some(w => w && w.length > 0);
  }

  /**
   * Appelle le backend pour détecter le doctor shopping.
   * Se déclenche immédiatement à la sélection d'un médicament.
   * Seules les prescriptions ACTIVES (endDate >= aujourd'hui) déclenchent une alerte.
   */
  triggerDoctorShoppingCheck(lineIndex: number, medicineId: number): void {
    if (!this.patientId || !medicineId || !this.currentDoctorId) return;

    this.rxService.checkDoctorShopping(this.patientId, medicineId, this.currentDoctorId).subscribe({
      next: (alerts: any[]) => {
        this.doctorShoppingWarnings[lineIndex] = alerts;
        this.cdr.detectChanges();
        if (alerts.length > 0) {
          console.warn(`[DoctorShopping] Line ${lineIndex}: Patient ${this.patientId} has ${alerts.length} active prescription(s) from other doctors for medicine #${medicineId}`);
        }
      },
      error: () => {
        // Ne pas bloquer l'interface en cas d'erreur du service
        this.doctorShoppingWarnings[lineIndex] = [];
      }
    });
  }

  /** Retourne vrai si au moins une ligne déclenche une alerte de doctor shopping */
  hasAnyDoctorShoppingWarning(): boolean {
    return this.doctorShoppingWarnings.some(w => w && w.length > 0);
  }

  saveRx(): void {
    if (this.rxForm.invalid) return;
    this.isSaving = true;
    this.errorMessage = '';

    const rawData = this.rxForm.value;
    const currentUser = this.authService.getLoggedUser();
    const doctorId = currentUser?.userId || 0;

    this.rxService.createPrescription({
      consultationId: rawData.consultationId,
      patientId: rawData.patientId,
      doctorId,
      status: rawData.status,
      prescriptionLines: rawData.prescriptionLines.map((l: any) => ({
        medicine: { id: l.medicineId },
        dosage: l.dosage,
        startDate: l.startDate,
        endDate: l.endDate
      }))
    }).subscribe({
      next: () => {
        this.rxService.clearDraft();
        this.isSaving = false;
        this.router.navigate(['/admin/patient-prescriptions', this.patientId]);
      },
      error: () => {
        this.errorMessage = 'Failed to issue prescription. Please check all fields.';
        this.isSaving = false;
      }
    });
  }

  goBack(): void {
    this.rxService.clearDraft();
    this.router.navigate(['/admin/consultations']);
  }
}
