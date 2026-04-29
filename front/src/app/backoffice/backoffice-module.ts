import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { BackofficeRoutingModule } from './backoffice-routing-module';
import { Home2 } from './home2/home2';
import { CalendarPage } from './calendar/calendar';
import { ChatPage } from './chat/chat';
import { ProfilePage } from './profile/profile';
import { SettingsPage } from './settings/settings';
import { Header } from './header/header';
import { Footer } from './footer/footer';
import { FilesManagementPageComponent } from './files-management/files-management';
import { DoctorAppointments } from './doctor-appointments/doctor-appointments';
import { DoctorAppointmentDetail } from './doctor-appointment-detail/doctor-appointment-detail';
import { DoctorConsultations } from './doctor-consultations/doctor-consultations';
import { DoctorPrescriptionHistory } from './doctor-prescriptions/doctor-prescription-history.component';
import { DoctorPrescriptionCreate } from './doctor-prescriptions/doctor-prescription-create.component';
import { DoctorPrescriptionDetail } from './doctor-prescriptions/doctor-prescription-detail.component';
import { MedicationManagement } from './medication-management/medication-management.component';
import { SafeUrlPipe } from './safe-url.pipe';

import { PatientFollowUpComponent } from './patient-follow-up/patient-follow-up';

import { FullCalendarModule } from '@fullcalendar/angular';

@NgModule({
  declarations: [
    Home2,
    CalendarPage,
    ChatPage,
    ProfilePage,
    SettingsPage,
    Header,
    Footer,
    FilesManagementPageComponent,
    DoctorAppointments,
    DoctorAppointmentDetail,
    DoctorConsultations,
    DoctorPrescriptionHistory,
    DoctorPrescriptionCreate,
    DoctorPrescriptionDetail,
    MedicationManagement,
    SafeUrlPipe,
    PatientFollowUpComponent
  ],
  imports: [CommonModule, FormsModule, ReactiveFormsModule, BackofficeRoutingModule, FullCalendarModule],
})
export class BackofficeModule { }
