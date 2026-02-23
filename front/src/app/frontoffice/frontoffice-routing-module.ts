import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
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

const routes: Routes = [
  { path: '', component: Home1 },
  { path: '404', component: Page404FrontPage },
  { path: 'about', component: AboutFrontPage },
  { path: 'appointment', component: AppointmentFrontPage },
  { path: 'contact', component: ContactFrontPage },
  { path: 'department-details', component: DepartmentDetailsFrontPage },
  { path: 'departments', component: DepartmentsFrontPage },
  { path: 'doctors', component: DoctorsFrontPage },
  { path: 'faq', component: FaqFrontPage },
  { path: 'gallery', component: GalleryFrontPage },
  { path: 'privacy', component: PrivacyFrontPage },
  { path: 'service-details', component: ServiceDetailsFrontPage },
  { path: 'services', component: ServicesFrontPage },
  { path: 'starter-page', component: StarterPageFrontPage },
  { path: 'terms', component: TermsFrontPage },
  { path: 'testimonials', component: TestimonialsFrontPage },
  { path: 'auth/forgot-password-cover', component: ForgotPasswordCoverAuthPage },
  { path: 'auth/login', component: LoginCoverAuthPage },
  { path: 'auth/login-cover', component: LoginCoverAuthPage },
  { path: 'auth/new-password-cover', component: NewPasswordCoverAuthPage },
  { path: 'auth/signup', component: RegisterCoverAuthPage },
  { path: 'auth/register-cover', component: RegisterCoverAuthPage },
  { path: '**', redirectTo: '' },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class FrontofficeRoutingModule {}
