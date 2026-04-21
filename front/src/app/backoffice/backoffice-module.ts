import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { BackofficeRoutingModule } from './backoffice-routing-module';
import { Home2 } from './home2/home2';
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
import { Header } from './header/header';
import { Footer } from './footer/footer';
import { BackofficeShellComponent } from './backoffice-shell/backoffice-shell';
import { IncidentsPage } from './incidents/incidents';
import { DoctorConsultations } from './doctor-consultations/doctor-consultations';
import { PatientFollowUpComponent } from './patient-follow-up/patient-follow-up';
import { DoctorPrescriptionCreate } from './doctor-prescriptions/doctor-prescription-create.component';
import { DoctorPrescriptionDetail } from './doctor-prescriptions/doctor-prescription-detail.component';
import { DoctorPrescriptionHistory } from './doctor-prescriptions/doctor-prescription-history.component';
import { MedicationManagement } from './medication-management/medication-management.component';
import { DoctorAppointments } from './doctor-appointments/doctor-appointments';
import { DoctorAppointmentDetail } from './doctor-appointment-detail/doctor-appointment-detail';

@NgModule({
  declarations: [
    Home2,
    CalendarPage,
    ChatPage,
    CustomersPage,
    EmployeePage,
    ProfilePage,
    ReviewPage,
    SalesPage,
    SettingsPage,
    TaskManagementPage,
    TeamManagementPage,
    UserManagementPage,
    MedicalReportsPageComponent,
    FilesManagementPageComponent,
    VolunteeringPageComponent,
    AssignmentHistoryPageComponent,
    Header,
    Footer,
    BackofficeShellComponent,
    IncidentsPage,
    DoctorConsultations,
    PatientFollowUpComponent,
    DoctorPrescriptionCreate,
    DoctorPrescriptionDetail,
    DoctorPrescriptionHistory,
    MedicationManagement,
    DoctorAppointments,
    DoctorAppointmentDetail,
  ],
  imports: [CommonModule, FormsModule, ReactiveFormsModule, BackofficeRoutingModule],
})
export class BackofficeModule { }
