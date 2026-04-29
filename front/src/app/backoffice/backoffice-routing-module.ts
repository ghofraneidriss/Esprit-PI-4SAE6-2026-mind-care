import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { Home2 } from './home2/home2';
import { CalendarPage } from './calendar/calendar';
import { ChatPage } from './chat/chat';
import { ProfilePage } from './profile/profile';
import { SettingsPage } from './settings/settings';
import { FilesManagementPageComponent } from './files-management/files-management';
import { DoctorAppointments } from './doctor-appointments/doctor-appointments';
import { DoctorAppointmentDetail } from './doctor-appointment-detail/doctor-appointment-detail';
import { DoctorConsultations } from './doctor-consultations/doctor-consultations';
import { PatientFollowUpComponent } from './patient-follow-up/patient-follow-up';
import { DoctorPrescriptionHistory } from './doctor-prescriptions/doctor-prescription-history.component';
import { DoctorPrescriptionCreate } from './doctor-prescriptions/doctor-prescription-create.component';
import { DoctorPrescriptionDetail } from './doctor-prescriptions/doctor-prescription-detail.component';
import { MedicationManagement } from './medication-management/medication-management.component';

const routes: Routes = [
  { path: '', component: Home2 },
  { path: 'reports', component: Home2 },
  { path: 'calendar', component: CalendarPage },
  { path: 'chat', component: ChatPage },
  { path: 'profile', component: ProfilePage },
  { path: 'settings', component: SettingsPage },
  { path: 'files-management', component: FilesManagementPageComponent },
  { path: 'appointments', component: DoctorAppointments },
  { path: 'appointments/pending', component: DoctorAppointments },
  { path: 'appointments/confirmed', component: DoctorAppointments },
  { path: 'appointments/details/:id', component: DoctorAppointmentDetail },
  { path: 'consultations', component: DoctorConsultations },
  { path: 'consultations/new', component: DoctorConsultations },
  { path: 'patient-follow-up', component: PatientFollowUpComponent },
  { path: 'patient-prescriptions/:patientId', component: DoctorPrescriptionHistory },
  { path: 'prescriptions/new', component: DoctorPrescriptionCreate },
  { path: 'prescriptions/view/:id', component: DoctorPrescriptionDetail },
  { path: 'medications', component: MedicationManagement },
  { path: '**', redirectTo: '' },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class BackofficeRoutingModule { }
