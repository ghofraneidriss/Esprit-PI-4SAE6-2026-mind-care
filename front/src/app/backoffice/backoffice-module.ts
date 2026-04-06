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
import { IncidentsPage } from './incidents/incidents';
import { RecommendationPage } from './recommendation/recommendation';
import { MedicalEventsPage } from './medical-events/medical-events';
import { SouvenirsPage } from './souvenirs/souvenirs';
import { Header } from './header/header';
import { Footer } from './footer/footer';
import { Sidebar } from './sidebar/sidebar';
import { BackofficeLayoutComponent } from './backoffice-layout';

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
    IncidentsPage,
    RecommendationPage,
    MedicalEventsPage,
    SouvenirsPage,
    Header,
    Footer,
    Sidebar,
    BackofficeLayoutComponent,
  ],
  imports: [CommonModule, FormsModule, ReactiveFormsModule, BackofficeRoutingModule],
})
export class BackofficeModule {}
