import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { BackofficeRoutingModule } from './backoffice-routing-module';
import { Home2 } from './home2/home2';
import { ActivitiesPage } from './activities/activities';
import { CalendarPage } from './calendar/calendar';
import { ChatPage } from './chat/chat';
import { CustomersPage } from './customers/customers';
import { DealsPage } from './deals/deals';
import { EmployeePage } from './employee/employee';
import { FinancePage } from './finance/finance';
import { ProfilePage } from './profile/profile';
import { ReviewPage } from './review/review';
import { SalesPage } from './sales/sales';
import { SettingsPage } from './settings/settings';
import { TaskManagementPage } from './task-management/task-management';
import { TeamManagementPage } from './team-management/team-management';
import { UserManagementPage } from './user-management/user-management';
import { RecommendationPage } from './recommendation/recommendation';
import { MedicalReportsPageComponent } from './medical-reports-page/medical-reports-page';
import { MedicalEventsPage } from './medical-events/medical-events';
import { Header } from './header/header';
import { Footer } from './footer/footer';
import { Sidebar } from './sidebar/sidebar';
import { BackofficeLayoutComponent } from './backoffice-layout';

@NgModule({
  declarations: [
    Home2,
    ActivitiesPage,
    CalendarPage,
    ChatPage,
    CustomersPage,
    DealsPage,
    EmployeePage,
    FinancePage,
    ProfilePage,
    ReviewPage,
    SalesPage,
    SettingsPage,
    TaskManagementPage,
    TeamManagementPage,
    UserManagementPage,
    RecommendationPage,
    MedicalEventsPage,
    MedicalReportsPageComponent,
    Header,
    Footer,
    Sidebar,
    BackofficeLayoutComponent,
  ],
  imports: [CommonModule, FormsModule, ReactiveFormsModule, BackofficeRoutingModule],
})
export class BackofficeModule { }
