import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { Home1 } from './home1/home1';
import { AboutFrontPage } from './about/about';
import { AppointmentFrontPage } from './appointment/appointment';
import { ContactFrontPage } from './contact/contact';
import { ForgotPasswordCoverAuthPage } from './auth/forgot-password-cover/forgot-password-cover';
import { LoginCoverAuthPage } from './auth/login-cover/login-cover';
import { NewPasswordCoverAuthPage } from './auth/new-password-cover/new-password-cover';
import { RegisterCoverAuthPage } from './auth/register-cover/register-cover';
import { PatientProfile } from './patient-profile/patient-profile';
import { PatientPrescriptions } from './patient-prescriptions/patient-prescriptions.component';
import { PatientPrescriptionDetail } from './patient-prescription-detail/patient-prescription-detail.component';

const routes: Routes = [
  { path: '', component: Home1 },
  { path: 'about', component: AboutFrontPage },
  { path: 'appointment', component: AppointmentFrontPage },
  { path: 'contact', component: ContactFrontPage },
  { path: 'auth/forgot-password-cover', component: ForgotPasswordCoverAuthPage },
  { path: 'auth/login', component: LoginCoverAuthPage },
  { path: 'auth', redirectTo: 'auth/login', pathMatch: 'full' },
  { path: 'auth/login-cover', component: LoginCoverAuthPage },
  { path: 'auth/new-password-cover', component: NewPasswordCoverAuthPage },
  { path: 'auth/signup', component: RegisterCoverAuthPage },
  { path: 'auth/register-cover', component: RegisterCoverAuthPage },
  { path: 'patient-profile', component: PatientProfile },
  { path: 'patient-prescriptions/:patientId', component: PatientPrescriptions },
  { path: 'patient-prescription-detail/:id', component: PatientPrescriptionDetail },
  { path: '**', redirectTo: '' },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class FrontofficeRoutingModule { }
