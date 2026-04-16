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
import { FrontofficeActivitiesPage } from './activities/activities';
import { PatientIncidentsComponent } from './incident-list-front/patient-incidents';
import { PatientIncidentsHistoryComponent } from './incident-list-front/patient-incidents-history';
import { IncidentReportFrontPage } from './incident-report/incident-report-front';
import { RecommendationsPage } from './recommendations/recommendations';
import { SouvenirsFrontPage } from './souvenirs/souvenirs';
import { AlzheimerUnderstandingFrontPage } from './alzheimer-understanding/alzheimer-understanding';
import { Header } from './header/header';
import { Footer } from './footer/footer';
import { FrontofficeLayoutComponent } from './frontoffice-layout';
import { PuzzlePlayPage } from './puzzle-play/puzzle-play';
import { SudokuPlayPage } from './sudoku-play/sudoku-play';

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
    FrontofficeActivitiesPage,
    RecommendationsPage,
    SouvenirsFrontPage,
    AlzheimerUnderstandingFrontPage,
    Header,
    Footer,
    FrontofficeLayoutComponent,
    PuzzlePlayPage,
    SudokuPlayPage,
  ],
  imports: [
    CommonModule,
    FormsModule,
    FrontofficeRoutingModule,
    PatientIncidentsComponent,
    PatientIncidentsHistoryComponent,
    IncidentReportFrontPage,
  ],
})
export class FrontofficeModule {}
