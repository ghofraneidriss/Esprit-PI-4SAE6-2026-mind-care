import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { Appointment, AppointmentCategory, AppointmentService, AppointmentStatus, AppointmentType } from './appointment.service';
import { AuthService } from '../auth/auth.service';

@Component({
  selector: 'app-appointment',
  standalone: false,
  template: `
    <app-header></app-header>

    <main class="main" style="padding-top: 100px; background: #f4f7f7; min-height: 100vh;">
      <div class="container py-4">
        <div class="row g-4">
          <div class="col-lg-7">
            <div class="p-4 rounded-4 shadow-sm bg-white" style="border: 1px solid rgba(0,0,0,0.06);">
              <div class="d-flex justify-content-between align-items-center mb-4 border-bottom pb-3">
                <div>
                  <h2 class="fw-bold mb-0" style="color: #1e696a;">
                    <i class="bi bi-calendar-plus me-2"></i>Book an Appointment
                  </h2>
                  <p class="text-muted small mb-0">Create a consultation request for your doctor.</p>
                </div>
                <button class="btn btn-outline-secondary btn-sm rounded-pill px-3" (click)="goToProfile()">
                  <i class="bi bi-person-circle me-1"></i> My Profile
                </button>
              </div>

              <div *ngIf="errorMessage" class="alert alert-danger">{{ errorMessage }}</div>
              <div *ngIf="successMessage" class="alert alert-success">{{ successMessage }}</div>

              <form #appointmentForm="ngForm" (ngSubmit)="createAppointment()" class="row g-3">
                <div class="col-md-6">
                  <label class="form-label fw-semibold">Doctor ID</label>
                  <input type="number" class="form-control" name="doctorId" [(ngModel)]="draft.doctorId" required>
                </div>

                <div class="col-md-6">
                  <label class="form-label fw-semibold">Appointment Date</label>
                  <input type="datetime-local" class="form-control" name="appointmentDate" [(ngModel)]="draft.appointmentDate" required>
                </div>

                <div class="col-md-6">
                  <label class="form-label fw-semibold">Type</label>
                  <select class="form-select" name="type" [(ngModel)]="draft.type" required>
                    <option [ngValue]="AppointmentType.ONLINE">Online</option>
                    <option [ngValue]="AppointmentType.IN_PERSON">In Person</option>
                  </select>
                </div>

                <div class="col-md-6">
                  <label class="form-label fw-semibold">Category</label>
                  <select class="form-select" name="category" [(ngModel)]="draft.category" required>
                    <option [ngValue]="AppointmentCategory.NEW_CONSULTATION">New Consultation</option>
                    <option [ngValue]="AppointmentCategory.DAILY_FOLLOW_UP">Daily Follow-up</option>
                  </select>
                </div>

                <div class="col-12">
                  <label class="form-label fw-semibold">Title</label>
                  <input type="text" class="form-control" name="title" [(ngModel)]="draft.title" placeholder="Optional title">
                </div>

                <div class="col-12">
                  <div class="form-check">
                    <input class="form-check-input" type="checkbox" id="isUrgent" name="isUrgent" [(ngModel)]="draft.isUrgent">
                    <label class="form-check-label" for="isUrgent">Mark as urgent</label>
                  </div>
                </div>

                <div class="col-12 d-flex gap-2">
                  <button class="btn btn-primary" type="submit" [disabled]="isSubmitting || appointmentForm.invalid">
                    <span *ngIf="!isSubmitting">Create Appointment</span>
                    <span *ngIf="isSubmitting">Saving...</span>
                  </button>
                  <button class="btn btn-outline-secondary" type="button" (click)="resetDraft()">Reset</button>
                </div>
              </form>
            </div>
          </div>

          <div class="col-lg-5">
            <div class="p-4 rounded-4 shadow-sm bg-white" style="border: 1px solid rgba(0,0,0,0.06);">
              <div class="d-flex justify-content-between align-items-center mb-3">
                <h5 class="fw-bold mb-0" style="color: #1e696a;">My Appointments</h5>
                <button class="btn btn-sm btn-outline-secondary rounded-pill" (click)="loadMyAppointments()">Refresh</button>
              </div>

              <div *ngIf="isLoading" class="text-center py-5">
                <div class="spinner-border" role="status" style="color:#1e696a;"></div>
              </div>

              <div *ngIf="!isLoading && appointments.length === 0" class="text-center py-5 text-muted">
                No appointments yet.
              </div>

              <div class="list-group list-group-flush" *ngIf="!isLoading && appointments.length > 0">
                <div class="list-group-item px-0" *ngFor="let apt of appointments">
                  <div class="d-flex justify-content-between align-items-start">
                    <div>
                      <div class="fw-semibold">#{{ apt.id }} - {{ apt.title || 'Consultation' }}</div>
                      <div class="small text-muted">{{ apt.appointmentDate | date:'medium' }}</div>
                      <div class="small text-muted">{{ apt.type }} - {{ apt.category }}</div>
                    </div>
                    <span class="badge rounded-pill"
                          [ngClass]="{
                            'bg-warning text-dark': apt.status === AppointmentStatus.PENDING,
                            'bg-success': apt.status === AppointmentStatus.CONFIRMED,
                            'bg-danger': apt.status === AppointmentStatus.CANCELLED,
                            'bg-info text-dark': apt.status === AppointmentStatus.RESCHEDULED
                          }">
                      {{ apt.status }}
                    </span>
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
export class AppointmentComponent implements OnInit {
  appointments: Appointment[] = [];
  isLoading = false;
  isSubmitting = false;
  successMessage = '';
  errorMessage = '';
  loggedUser: any = null;
  readonly AppointmentType = AppointmentType;
  readonly AppointmentCategory = AppointmentCategory;
  readonly AppointmentStatus = AppointmentStatus;

  draft: Appointment = {
    patientId: 0,
    doctorId: 0,
    appointmentDate: '',
    isUrgent: false,
    type: AppointmentType.ONLINE,
    category: AppointmentCategory.NEW_CONSULTATION,
    status: AppointmentStatus.PENDING,
    title: '',
  };

  constructor(
    private readonly appointmentService: AppointmentService,
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loggedUser = this.authService.getLoggedUser();
    if (!this.loggedUser) {
      this.router.navigate(['/auth/login']);
      return;
    }

    this.draft.patientId = this.loggedUser.userId;
    this.loadMyAppointments();
  }

  loadMyAppointments(): void {
    const patientId = this.loggedUser?.userId;
    if (!patientId) {
      this.appointments = [];
      return;
    }

    this.isLoading = true;
    this.appointmentService.getAppointmentsByPatient(patientId).subscribe({
      next: (data) => {
        this.appointments = data || [];
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'Unable to load your appointments.';
        this.isLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  createAppointment(): void {
    this.errorMessage = '';
    this.successMessage = '';
    this.isSubmitting = true;

    const payload: Appointment = {
      ...this.draft,
      patientId: this.loggedUser?.userId ?? this.draft.patientId,
      status: AppointmentStatus.PENDING,
    };

    this.appointmentService.createAppointment(payload).subscribe({
      next: () => {
        this.successMessage = 'Appointment request created successfully.';
        this.isSubmitting = false;
        this.resetDraft();
        this.loadMyAppointments();
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'Unable to create appointment.';
        this.isSubmitting = false;
        this.cdr.detectChanges();
      }
    });
  }

  resetDraft(): void {
    this.draft = {
      patientId: this.loggedUser?.userId ?? 0,
      doctorId: 0,
      appointmentDate: '',
      isUrgent: false,
      type: AppointmentType.ONLINE,
      category: AppointmentCategory.NEW_CONSULTATION,
      status: AppointmentStatus.PENDING,
      title: '',
    };
  }

  goToProfile(): void {
    this.router.navigate(['/patient-profile']);
  }
}
