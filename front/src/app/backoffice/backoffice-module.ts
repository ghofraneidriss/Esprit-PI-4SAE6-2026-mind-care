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
import { Header } from './header/header';
import { Footer } from './footer/footer';
import { FollowUpPageComponent } from './followup-page/followup-page';
import { AlertPageComponent } from './alert-page/alert-page';

// Lost Item — all views
import { LostItemListComponent } from './lost-item/lost-item-list/lost-item-list';
import { LostItemFormComponent } from './lost-item/lost-item-form/lost-item-form';
import { LostItemDetailComponent } from './lost-item/lost-item-detail/lost-item-detail';
import { SearchReportFormComponent } from './lost-item/search-report-form/search-report-form';
import { SearchReportCardComponent } from './lost-item/search-report-card/search-report-card';
import { CriticalLostItemsComponent } from './lost-item/critical-lost-items/critical-lost-items';
import { ItemAlertsComponent } from './lost-item/item-alerts/item-alerts';
import { SearchLogComponent } from './lost-item/search-log/search-log';
import { RecoveryStrategyComponent } from './lost-item/recovery-strategy/recovery-strategy';
import { ItemStatsComponent } from './lost-item/item-stats/item-stats';
import { PatientRiskComponent } from './lost-item/patient-risk/patient-risk';
import { MlRecommendationComponent } from './ml-recommendation/ml-recommendation';

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
    Header,
    Footer,
    FollowUpPageComponent,
    AlertPageComponent,
    // Lost Item
    LostItemListComponent,
    LostItemFormComponent,
    LostItemDetailComponent,
    SearchReportFormComponent,
    SearchReportCardComponent,
    CriticalLostItemsComponent,
    ItemAlertsComponent,
    SearchLogComponent,
    RecoveryStrategyComponent,
    ItemStatsComponent,
    PatientRiskComponent,
    MlRecommendationComponent,
  ],
  imports: [CommonModule, FormsModule, ReactiveFormsModule, BackofficeRoutingModule],
})
export class BackofficeModule {}
