import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { FrontofficeRoutingModule } from './frontoffice-routing-module';
import { Home1 } from './home1/home1';
import { AboutFrontPage } from './about/about';
import { AppointmentFrontPage } from './appointment/appointment';
import { ContactFrontPage } from './contact/contact';
import { ForgotPasswordCoverAuthPage } from './auth/forgot-password-cover/forgot-password-cover';
import { LoginCoverAuthPage } from './auth/login-cover/login-cover';
import { NewPasswordCoverAuthPage } from './auth/new-password-cover/new-password-cover';
import { RegisterCoverAuthPage } from './auth/register-cover/register-cover';
import { Header } from './header/header';
import { Footer } from './footer/footer';
import { PatientProfile } from './patient-profile/patient-profile';
import { PatientPrescriptions } from './patient-prescriptions/patient-prescriptions.component';
import { PatientPrescriptionDetail } from './patient-prescription-detail/patient-prescription-detail.component';

@NgModule({
  declarations: [
    Home1,
    AboutFrontPage,
    AppointmentFrontPage,
    ContactFrontPage,
    ForgotPasswordCoverAuthPage,
    LoginCoverAuthPage,
    NewPasswordCoverAuthPage,
    RegisterCoverAuthPage,
    Header,
    Footer,
    PatientProfile,
    PatientPrescriptions,
    PatientPrescriptionDetail,
  ],
  imports: [CommonModule, FormsModule, FrontofficeRoutingModule],
})
export class FrontofficeModule {}
