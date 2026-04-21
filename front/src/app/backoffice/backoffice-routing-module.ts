import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { strictAdminGuard } from '../guards/strict-admin.guard';

import { Home2 } from './home2/home2';
import { BackofficeShellComponent } from './backoffice-shell/backoffice-shell';
import { CalendarPage } from './calendar/calendar';
import { ChatPage } from './chat/chat';
import { CustomersPage } from './customers/customers';
import { EmployeePage } from './employee/employee';
import { ProfilePage } from './profile/profile';
import { ReviewPage } from './review/review';
import { SalesPage } from './sales/sales';
import { SettingsPage } from './settings/settings';
import { TaskManagementPage } from './task-management/task-management';
import { TeamManagementPage } from './team-management/team-management';
import { UserManagementPage } from './user-management/user-management';
import { MedicalReportsPageComponent } from './medical-reports-page/medical-reports-page';
import { FilesManagementPageComponent } from './files-management/files-management';
import { VolunteeringPageComponent } from './volunteering/volunteering';
import { AssignmentHistoryPageComponent } from './assignment-history/assignment-history';
import { IncidentsPage } from './incidents/incidents';
import { DoctorConsultations } from './doctor-consultations/doctor-consultations';
import { PatientFollowUpComponent } from './patient-follow-up/patient-follow-up';
import { DoctorPrescriptionCreate } from './doctor-prescriptions/doctor-prescription-create.component';
import { DoctorPrescriptionDetail } from './doctor-prescriptions/doctor-prescription-detail.component';
import { DoctorPrescriptionHistory } from './doctor-prescriptions/doctor-prescription-history.component';
import { MedicationManagement } from './medication-management/medication-management.component';
import { DoctorAppointments } from './doctor-appointments/doctor-appointments';
import { DoctorAppointmentDetail } from './doctor-appointment-detail/doctor-appointment-detail';

const routes: Routes = [
  {
    path: '',
    component: BackofficeShellComponent,
    children: [
      { path: '', component: Home2, pathMatch: 'full' },
      { path: 'reports', component: Home2 },
      { path: 'calendar', component: CalendarPage },
      { path: 'chat', component: ChatPage },
      { path: 'customers', component: CustomersPage },
      { path: 'employee', component: EmployeePage },
      { path: 'profile', component: ProfilePage },
      { path: 'review', component: ReviewPage },
      { path: 'sales', component: SalesPage },
      { path: 'settings', component: SettingsPage },
      { path: 'task-management', component: TaskManagementPage },
      { path: 'team-management', component: TeamManagementPage },
      { path: 'user-management', component: UserManagementPage, canActivate: [strictAdminGuard] },
      { path: 'medical-reports', component: MedicalReportsPageComponent, canActivate: [strictAdminGuard] },
      { path: 'files-management', component: FilesManagementPageComponent, canActivate: [strictAdminGuard] },
      { path: 'volunteering', component: VolunteeringPageComponent },
      { path: 'assignment-history', component: AssignmentHistoryPageComponent },
      { path: 'incidents', component: IncidentsPage },
      { path: 'appointments', component: DoctorAppointments },
      { path: 'appointments/:id', component: DoctorAppointmentDetail },
      { path: 'consultations', component: DoctorConsultations },
      { path: 'patient-follow-up', component: PatientFollowUpComponent },
      { path: 'medications', component: MedicationManagement },
      { path: 'prescriptions/new', component: DoctorPrescriptionCreate },
      { path: 'prescriptions/view/:id', component: DoctorPrescriptionDetail },
      { path: 'patient-prescriptions/:patientId', component: DoctorPrescriptionHistory },
      { path: '**', redirectTo: '' },
    ],
  },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class BackofficeRoutingModule { }
