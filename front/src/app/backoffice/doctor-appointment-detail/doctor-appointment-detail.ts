import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AppointmentService, Appointment } from '../../frontoffice/appointment/appointment.service';
import { PatientProfileService, PatientProfile } from '../../frontoffice/patient-profile/patient-profile.service';
import { AuthService, AuthUser } from '../../frontoffice/auth/auth.service';

@Component({
  selector: 'app-doctor-appointment-detail',
  standalone: false,
  templateUrl: './doctor-appointment-detail.html',
  styleUrls: ['./doctor-appointment-detail.css'],
})
export class DoctorAppointmentDetail implements OnInit {
  appointment: Appointment | null = null;
  patientUser: AuthUser | null = null;
  patientProfile: PatientProfile | null = null;

  editMode: boolean = false;
  updatedDate: string = '';
  isSaving: boolean = false;
  successMessage: string = '';
  errorMessage: string = '';
  /** Date/heure minimale pour le sélecteur de reprogrammation (maintenant) */
  minDateTime: string = new Date().toISOString().slice(0, 16);

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private appointmentService: AppointmentService,
    private profileService: PatientProfileService,
    private authService: AuthService,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.loadAppointmentDetails(+idParam);
    } else {
      this.router.navigate(['/admin/appointments']);
    }
  }

  loadAppointmentDetails(id: number): void {
    this.appointmentService.getAppointmentById(id).subscribe({
      next: (apt) => {
        this.appointment = apt;
        this.updatedDate = apt.appointmentDate.substring(0, 16); // Local datetime format without seconds
        this.cdr.detectChanges();

        // Fetch patient user info via getAllUsers fallback
        this.authService.getAllUsers().subscribe({
          next: (users: AuthUser[]) => {
            console.log('[Detail] Retrieved users array:', users);
            // Use loose inequality in case of string vs number ID
            const user = users.find((u: AuthUser) => u.userId == apt.patientId || Number(u.userId) === Number(apt.patientId));

            if (user) {
              console.log('[Detail] Found patient user:', user);
              this.patientUser = user;

              // User requested to fetch profile by email
              this.profileService.getProfileByEmail(user.email).subscribe({
                next: (profile: PatientProfile) => {
                  console.log('[Detail] Found profile:', profile);
                  this.patientProfile = profile;
                  this.cdr.detectChanges();
                },
                error: (err: any) => {
                  console.error('[Detail] Error loading patient profile by email', err);
                  // Try fetching by userId as fallback if email fails
                  this.profileService.getProfileByUserId(apt.patientId).subscribe({
                    next: (profile2: PatientProfile) => {
                      this.patientProfile = profile2;
                      this.cdr.detectChanges();
                    },
                    error: (err3: any) => this.cdr.detectChanges()
                  });
                }
              });
            } else {
              console.warn(`[Detail] User not found in users array for patientId ${apt.patientId}.`);
              this.patientUser = { userId: apt.patientId, firstName: 'Unknown', lastName: 'Patient', email: 'N/A' };
              this.cdr.detectChanges();
            }
          },
          error: (err: any) => {
            console.error('[Detail] Error loading users via getAllUsers', err);
            // Fallback to getUserById 
            this.authService.getUserById(apt.patientId).subscribe({
              next: (user: AuthUser) => {
                this.patientUser = user;
                this.profileService.getProfileByEmail(user.email).subscribe({
                  next: (p: PatientProfile) => { this.patientProfile = p; this.cdr.detectChanges(); },
                  error: (err3: any) => this.cdr.detectChanges()
                });
              },
              error: (err2: any) => {
                console.error('[Detail] Error loading user by id directly', err2);
                this.patientUser = { userId: apt.patientId, firstName: 'Unknown', lastName: 'Patient', email: 'N/A' };
                this.cdr.detectChanges();
              }
            });
          }
        });
      },
      error: (err: any) => {
        console.error('[Detail] Error loading appointment', err);
        this.router.navigate(['/admin/appointments']);
      }
    });
  }

  saveDateUpdate(): void {
    if (!this.appointment || !this.appointment.id || !this.updatedDate) return;

    // Vérification : la nouvelle date doit être dans le futur
    const chosenDate = new Date(this.updatedDate);
    if (chosenDate <= new Date()) {
      this.errorMessage = 'Please select a future date.';
      setTimeout(() => this.errorMessage = '', 4000);
      return;
    }

    this.isSaving = true;
    this.errorMessage = '';

    // Construction du nouvel objet rendez-vous avec la date formatée
    // Spring Boot attend le format ISO 8601 complet : YYYY-MM-DDTHH:mm:ss
    const newApt = { ...this.appointment };
    newApt.appointmentDate = this.updatedDate.length === 16
      ? this.updatedDate + ':00'   // Ajout des secondes si absentes (format datetime-local)
      : this.updatedDate;

    this.appointmentService.updateAppointment(this.appointment.id, newApt).subscribe({
      next: (updated: Appointment) => {
        this.appointment = updated;
        this.editMode = false;
        this.isSaving = false;
        this.successMessage = 'Appointment rescheduled successfully.';
        this.cdr.detectChanges(); // Forcer la mise à jour de la vue
        setTimeout(() => {
          this.successMessage = '';
          this.cdr.detectChanges();
        }, 3000);
      },
      error: (err: any) => {
        console.error('[Detail] Update failed:', err);
        this.isSaving = false;
        this.errorMessage = 'Failed to update the appointment. Please try again.';
        this.cdr.detectChanges();
        setTimeout(() => {
          this.errorMessage = '';
          this.cdr.detectChanges();
        }, 4000);
      }
    });
  }

  sendAlert(): void {
    if (!this.patientUser || !this.appointment) return;

    // Determine contact info
    const email = this.patientUser.email || '';
    if (!email || email === 'N/A') {
      this.successMessage = "Error: Patient has no valid email.";
      setTimeout(() => this.successMessage = '', 3000);
      return;
    }

    const patientName = `${this.patientUser.firstName} ${this.patientUser.lastName}`;
    const aptDate = new Date(this.appointment.appointmentDate).toLocaleString();

    let subject = '';
    let alertMessage = '';

    // Check if appointment is in the past
    if (this.isPastAppointment()) {
      subject = "Missed Appointment Alert - MindCare";
      alertMessage = `Dear ${patientName},\n\nWe noticed you didn't attend your appointment scheduled on ${aptDate}. ` +
        `Please contact us to reschedule and ensure your health track is up to date.\n\nBest Regards,\nMindCare Team`;
    } else {
      subject = "Upcoming Appointment Reminder - MindCare";
      alertMessage = `Dear ${patientName},\n\nThis is a gentle reminder of your upcoming appointment on ${aptDate}. ` +
        `Please don't forget to attend. We look forward to seeing you!\n\nBest Regards,\nMindCare Team`;
    }

    // Calling the real backend alert service
    this.appointmentService.sendAlertEmail(email, subject, alertMessage).subscribe({
      next: () => {
        this.successMessage = "Alert successfully sent to the patient.";
        this.cdr.detectChanges();
        setTimeout(() => {
          this.successMessage = '';
          this.cdr.detectChanges();
        }, 4000);
      },
      error: (err: any) => {
        console.error('[Detail] Failed to send alert email', err);
        this.errorMessage = "Failed to send email. Please check your Gmail configuration.";
        this.cdr.detectChanges();
        setTimeout(() => {
          this.errorMessage = '';
          this.cdr.detectChanges();
        }, 4000);
      }
    });
  }

  isPastAppointment(): boolean {
    if (!this.appointment || !this.appointment.appointmentDate) return false;
    const aptDate = new Date(this.appointment.appointmentDate);
    const now = new Date();
    return aptDate < now;
  }

  goBack(): void {
    this.router.navigate(['/admin/appointments']);
  }
}

