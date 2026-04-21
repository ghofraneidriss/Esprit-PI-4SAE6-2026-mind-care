import { ChangeDetectorRef, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Appointment, AppointmentService } from '../../frontoffice/appointment/appointment.service';
import { AuthService } from '../../frontoffice/auth/auth.service';
import { PatientProfileService } from '../../frontoffice/patient-profile/patient-profile.service';

@Component({
  selector: 'app-doctor-appointment-detail',
  standalone: false,
  templateUrl: './doctor-appointment-detail.html',
  styleUrls: ['./doctor-appointment-detail.css']
})
export class DoctorAppointmentDetail implements OnInit {
  appointment: Appointment | null = null;
  patientUser: any = null;
  patientProfile: any = null;
  editMode = false;
  updatedDate = '';
  minDateTime = '';
  successMessage = '';
  errorMessage = '';
  isSaving = false;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly appointmentService: AppointmentService,
    private readonly authService: AuthService,
    private readonly profileService: PatientProfileService,
    private readonly cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const id = +(this.route.snapshot.paramMap.get('id') || 0);
    this.minDateTime = this.toDateTimeLocal(new Date());
    if (id) {
      this.loadAppointment(id);
    }
  }

  loadAppointment(id: number): void {
    this.appointmentService.getAppointmentById(id).subscribe({
      next: (data) => {
        this.appointment = data;
        this.updatedDate = this.toDateTimeLocal(new Date(data.appointmentDate));
        this.loadRelatedPatientData(data.patientId);
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'Unable to load appointment details.';
        this.cdr.detectChanges();
      }
    });
  }

  loadRelatedPatientData(patientId: number): void {
    this.authService.getUserById(patientId).subscribe({
      next: (user) => {
        this.patientUser = user;
        this.cdr.detectChanges();
      }
    });

    this.profileService.getProfileByUserId(patientId).subscribe({
      next: (profile) => {
        this.patientProfile = profile;
        this.cdr.detectChanges();
      },
      error: () => {
        this.patientProfile = null;
      }
    });
  }

  isPastAppointment(): boolean {
    if (!this.appointment?.appointmentDate) return false;
    return new Date(this.appointment.appointmentDate) < new Date();
  }

  saveDateUpdate(): void {
    if (!this.appointment?.id) return;
    this.isSaving = true;
    this.errorMessage = '';
    const nextAppointment: Appointment = {
      ...this.appointment,
      appointmentDate: this.updatedDate,
    };
    this.appointmentService.updateAppointment(this.appointment.id, nextAppointment).subscribe({
      next: (saved) => {
        this.appointment = saved;
        this.successMessage = 'Appointment rescheduled successfully.';
        this.editMode = false;
        this.isSaving = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'Unable to save the new appointment date.';
        this.isSaving = false;
        this.cdr.detectChanges();
      }
    });
  }

  sendAlert(): void {
    if (!this.patientUser?.email) {
      this.errorMessage = 'Patient email not available.';
      return;
    }
    this.appointmentService.sendAlertEmail(
      this.patientUser.email,
      'Appointment update',
      `Your appointment on ${this.appointment?.appointmentDate} has been updated.`
    ).subscribe({
      next: () => {
        this.successMessage = 'Alert sent to the patient.';
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'Unable to send alert email.';
        this.cdr.detectChanges();
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/admin/appointments']);
  }

  private toDateTimeLocal(date: Date): string {
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
  }
}
