import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { FrontofficeRoutingModule } from './frontoffice-routing-module';
import { Home1 } from './home1/home1';
import { Page404FrontPage } from './404/404';
import { AboutFrontPage } from './about/about';
import { ContactFrontPage } from './contact/contact';
import { DoctorsFrontPage } from './doctors/doctors';
import { FaqFrontPage } from './faq/faq';
import { GalleryFrontPage } from './gallery/gallery';
import { PrivacyFrontPage } from './privacy/privacy';
import { ServiceDetailsFrontPage } from './service-details/service-details';
import { StarterPageFrontPage } from './starter-page/starter-page';
import { TermsFrontPage } from './terms/terms';
import { TestimonialsFrontPage } from './testimonials/testimonials';
import { ForgotPasswordCoverAuthPage } from './auth/forgot-password-cover/forgot-password-cover';
import { LoginCoverAuthPage } from './auth/login-cover/login-cover';
import { NewPasswordCoverAuthPage } from './auth/new-password-cover/new-password-cover';
import { RegisterCoverAuthPage } from './auth/register-cover/register-cover';
import { Header } from './header/header';
import { Footer } from './footer/footer';
import { VolunteerMissionsComponent } from './volunteer-missions/volunteer-missions';
import { PatientReportsPage } from './reports/reports';
import { PatientProfile } from './patient-profile/patient-profile';
import { PatientPrescriptions } from './patient-prescriptions/patient-prescriptions.component';
import { PatientPrescriptionDetail } from './patient-prescription-detail/patient-prescription-detail.component';
import { AppointmentComponent } from './appointment/appointment';

@NgModule({
  declarations: [
    Home1,
    Page404FrontPage,
    AboutFrontPage,
    ContactFrontPage,
    DoctorsFrontPage,
    FaqFrontPage,
    GalleryFrontPage,
    PrivacyFrontPage,
    ServiceDetailsFrontPage,
    StarterPageFrontPage,
    TermsFrontPage,
    TestimonialsFrontPage,
    ForgotPasswordCoverAuthPage,
    LoginCoverAuthPage,
    NewPasswordCoverAuthPage,
    RegisterCoverAuthPage,
    Header,
    Footer,
    VolunteerMissionsComponent,
    PatientReportsPage,
    PatientProfile,
    PatientPrescriptions,
    PatientPrescriptionDetail,
    AppointmentComponent,
  ],
  imports: [CommonModule, FormsModule, FrontofficeRoutingModule],
})
export class FrontofficeModule { }
