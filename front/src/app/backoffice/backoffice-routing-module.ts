import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { strictAdminGuard } from '../guards/strict-admin.guard';

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
import { FollowUpPageComponent } from './followup-page/followup-page';
import { AlertPageComponent } from './alert-page/alert-page';
import { FilesManagementPageComponent } from './files-management/files-management';
import { IncidentsPage } from './incidents/incidents';

// Lost Item
import { LostItemListComponent } from './lost-item/lost-item-list/lost-item-list';
import { LostItemFormComponent } from './lost-item/lost-item-form/lost-item-form';
import { LostItemDetailComponent } from './lost-item/lost-item-detail/lost-item-detail';
import { CriticalLostItemsComponent } from './lost-item/critical-lost-items/critical-lost-items';
import { ItemAlertsComponent } from './lost-item/item-alerts/item-alerts';
import { SearchLogComponent } from './lost-item/search-log/search-log';
import { RecoveryStrategyComponent } from './lost-item/recovery-strategy/recovery-strategy';
import { ItemStatsComponent } from './lost-item/item-stats/item-stats';
import { PatientRiskComponent } from './lost-item/patient-risk/patient-risk';
import { MlRecommendationComponent } from './ml-recommendation/ml-recommendation';

const routes: Routes = [
  { path: '', component: Home2 },
  { path: 'reports', component: Home2 },
  { path: 'calendar', component: CalendarPage },
  { path: 'chat', component: ChatPage },
  { path: 'customers', component: CustomersPage },
  { path: 'employee', component: EmployeePage },
  { path: 'profile', component: ProfilePage },
  { path: 'review', component: ReviewPage },
  { path: 'sales', component: SalesPage },
  { path: 'settings', component: SettingsPage },
  { path: 'task-management', component: TaskManagementPage },
  { path: 'team-management', component: TeamManagementPage },
  { path: 'user-management', component: UserManagementPage, canActivate: [strictAdminGuard] },
  { path: 'medical-reports', component: MedicalReportsPageComponent },
  { path: 'followups', component: FollowUpPageComponent },
  { path: 'alerts', component: AlertPageComponent },
  { path: 'files-management', component: FilesManagementPageComponent },
  { path: 'incidents', component: IncidentsPage },
  // Lost Item
  { path: 'lost-items', component: LostItemListComponent },
  { path: 'lost-items/new', component: LostItemFormComponent },
  { path: 'lost-items/critical', component: CriticalLostItemsComponent },
  { path: 'lost-items/alerts', component: ItemAlertsComponent },
  { path: 'lost-items/search-log', component: SearchLogComponent },
  { path: 'lost-items/statistics', component: ItemStatsComponent },
  { path: 'lost-items/risk/:patientId', component: PatientRiskComponent },
  { path: 'lost-items/risk', component: PatientRiskComponent },
  { path: 'lost-items/:id/recovery-strategy', component: RecoveryStrategyComponent },
  { path: 'lost-items/:id/edit', component: LostItemFormComponent },
  { path: 'lost-items/:id', component: LostItemDetailComponent },
  // ML Recommendation
  { path: 'ml-recommendation', component: MlRecommendationComponent },
  { path: '**', redirectTo: '' },
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule],
})
export class BackofficeRoutingModule { }
