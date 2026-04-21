import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { FormsModule } from '@angular/forms';
import { FrontofficeRoutingModule } from './frontoffice-routing-module';
import { Home1 } from './home1/home1';
import { Page404FrontPage } from './404/404';
import { AboutFrontPage } from './about/about';
import { AppointmentFrontPage } from './appointment/appointment';
import { ContactFrontPage } from './contact/contact';
import { DepartmentDetailsFrontPage } from './department-details/department-details';
import { DepartmentsFrontPage } from './departments/departments';
import { DoctorsFrontPage } from './doctors/doctors';
import { FaqFrontPage } from './faq/faq';
import { GalleryFrontPage } from './gallery/gallery';
import { PrivacyFrontPage } from './privacy/privacy';
import { ServiceDetailsFrontPage } from './service-details/service-details';
import { ServicesFrontPage } from './services/services';
import { StarterPageFrontPage } from './starter-page/starter-page';
import { TermsFrontPage } from './terms/terms';
import { TestimonialsFrontPage } from './testimonials/testimonials';
import { ForgotPasswordCoverAuthPage } from './auth/forgot-password-cover/forgot-password-cover';
import { LoginCoverAuthPage } from './auth/login-cover/login-cover';
import { NewPasswordCoverAuthPage } from './auth/new-password-cover/new-password-cover';
import { RegisterCoverAuthPage } from './auth/register-cover/register-cover';
import { FrontofficeActivitiesPage } from './activities/activities';
import { AlzheimerUnderstandingFrontPage } from './alzheimer-understanding/alzheimer-understanding';
import { RecommendationsPage } from './recommendations/recommendations';
import { SouvenirsFrontPage } from './souvenirs/souvenirs';
import { PuzzlePlayPage } from './puzzle-play/puzzle-play';
import { SudokuPlayPage } from './sudoku-play/sudoku-play';
import { SharedModule } from '../shared/shared.module';
import { GlobalOverlaysComponent } from '../shared/global-overlays/global-overlays.component';

import { FrontofficeLayoutComponent } from './frontoffice-layout';

@NgModule({
  declarations: [
    FrontofficeLayoutComponent,
    Home1,
    Page404FrontPage,
    AboutFrontPage,
    AppointmentFrontPage,
    ContactFrontPage,
    DepartmentDetailsFrontPage,
    DepartmentsFrontPage,
    DoctorsFrontPage,
    FaqFrontPage,
    GalleryFrontPage,
    PrivacyFrontPage,
    ServiceDetailsFrontPage,
    ServicesFrontPage,
    StarterPageFrontPage,
    TermsFrontPage,
    TestimonialsFrontPage,
    ForgotPasswordCoverAuthPage,
    LoginCoverAuthPage,
    NewPasswordCoverAuthPage,
    RegisterCoverAuthPage,
    FrontofficeActivitiesPage,
    AlzheimerUnderstandingFrontPage,
    RecommendationsPage,
    SouvenirsFrontPage,
    PuzzlePlayPage,
    SudokuPlayPage,
  ],
  imports: [CommonModule, FrontofficeRoutingModule, FormsModule, SharedModule, GlobalOverlaysComponent],
})
export class FrontofficeModule { }
